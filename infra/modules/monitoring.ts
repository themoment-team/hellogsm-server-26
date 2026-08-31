import * as aws from "@pulumi/aws";
import { config } from "../config";

export interface MonitoringResult {
    logGroup: aws.cloudwatch.LogGroup;
    alertsTopic: aws.sns.Topic;
}

// Discord 웹훅 연동은 이번 스코프 밖 - SNS Topic 생성까지만 담당한다.
export function createMonitoring(
    targetGroupArnSuffix: aws.lb.TargetGroup["arnSuffix"],
    albArnSuffix: aws.lb.LoadBalancer["arnSuffix"],
    springbootInstanceId: aws.ec2.Instance["id"],
    redisInstanceId: aws.ec2.Instance["id"],
): MonitoringResult {
    // 2024년부터 실제 운영 로그(약 1GB)가 쌓여있는 기존 로그 그룹을 pulumi import로 흡수한다
    // (README 참고). protect:true로 실수 삭제를 막는다 - 삭제 시 로그가 전부 유실된다.
    const logGroup = new aws.cloudwatch.LogGroup(
        "hellogsm-prod-log",
        {
            name: "hellogsm-prod-log",
            // 0 = 만료 없음 (logback-spring.xml의 retentionTimeDays=0과 정합)
            retentionInDays: config.logRetentionDays,
            tags: { Name: "hellogsm-prod-log" },
        },
        { protect: true },
    );

    const alertsTopic = new aws.sns.Topic("hello-prod-alerts", {
        name: "hello-prod-alerts",
    });

    if (config.snsAlarmEmail) {
        new aws.sns.TopicSubscription("hello-prod-alerts-email", {
            topic: alertsTopic.arn,
            protocol: "email",
            endpoint: config.snsAlarmEmail,
        });
    }

    new aws.cloudwatch.MetricAlarm("hello-prod-alb-unhealthy-hosts", {
        name: "hello-prod-alb-unhealthy-hosts",
        namespace: "AWS/ApplicationELB",
        metricName: "UnHealthyHostCount",
        dimensions: { TargetGroup: targetGroupArnSuffix, LoadBalancer: albArnSuffix },
        statistic: "Maximum",
        period: 60,
        evaluationPeriods: 2,
        threshold: 1,
        comparisonOperator: "GreaterThanOrEqualToThreshold",
        treatMissingData: "breaching",
        alarmActions: [alertsTopic.arn],
        okActions: [alertsTopic.arn],
    });

    new aws.cloudwatch.MetricAlarm("hello-prod-alb-5xx", {
        name: "hello-prod-alb-5xx",
        namespace: "AWS/ApplicationELB",
        metricName: "HTTPCode_Target_5XX_Count",
        dimensions: { TargetGroup: targetGroupArnSuffix, LoadBalancer: albArnSuffix },
        statistic: "Sum",
        period: 60,
        evaluationPeriods: 1,
        threshold: 5,
        comparisonOperator: "GreaterThanOrEqualToThreshold",
        treatMissingData: "notBreaching",
        alarmActions: [alertsTopic.arn],
    });

    new aws.cloudwatch.MetricAlarm("hello-prod-springboot-status-check-failed", {
        name: "hello-prod-springboot-status-check-failed",
        namespace: "AWS/EC2",
        metricName: "StatusCheckFailed",
        dimensions: { InstanceId: springbootInstanceId },
        statistic: "Maximum",
        period: 60,
        evaluationPeriods: 2,
        threshold: 1,
        comparisonOperator: "GreaterThanOrEqualToThreshold",
        treatMissingData: "breaching",
        alarmActions: [alertsTopic.arn],
        okActions: [alertsTopic.arn],
    });

    new aws.cloudwatch.MetricAlarm("hello-prod-redis-status-check-failed", {
        name: "hello-prod-redis-status-check-failed",
        namespace: "AWS/EC2",
        metricName: "StatusCheckFailed",
        dimensions: { InstanceId: redisInstanceId },
        statistic: "Maximum",
        period: 60,
        evaluationPeriods: 2,
        threshold: 1,
        comparisonOperator: "GreaterThanOrEqualToThreshold",
        treatMissingData: "breaching",
        alarmActions: [alertsTopic.arn],
        okActions: [alertsTopic.arn],
    });

    return { logGroup, alertsTopic };
}
