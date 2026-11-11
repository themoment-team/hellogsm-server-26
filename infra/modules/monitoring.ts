import * as aws from "@pulumi/aws";
import { config } from "../config";

const LOG_GROUP_NAME = "hellogsm-prod-log";

export interface MonitoringResult {
    logGroupName: string;
    alertsTopic: aws.sns.Topic;
}

// Discord 웹훅 연동은 이번 스코프 밖 - SNS Topic 생성까지만 담당한다.
export function createMonitoring(
    targetGroupArnSuffix: aws.lb.TargetGroup["arnSuffix"],
    albArnSuffix: aws.lb.LoadBalancer["arnSuffix"],
    springbootInstanceIds: aws.ec2.Instance["id"][],
    redisInstanceId: aws.ec2.Instance["id"],
): MonitoringResult {
    // 2024년부터 실제 운영 로그(약 1GB 이상)가 쌓여있는 기존 로그 그룹이다. 삭제 시
    // 로그가 전부 유실되고, 운영 사이클과 무관하게 상시 보존해야 하는 자원이라 더 이상
    // Pulumi가 관리하지 않고 데이터 소스로만 참조한다.
    const logGroupName = LOG_GROUP_NAME;

    // retentionInDays는 관리 대상에서 빠진 뒤로 콘솔에서 바뀌어도 drift를 발견할 방법이
    // 없었다. config.logRetentionDays(logback-spring.xml의 retentionTimeDays=0과 정합을
    // 맞추는 값, "0" = 만료 없음)와 실제 값을 비교해 preview/up 시점에 불일치를 드러낸다.
    aws.cloudwatch.getLogGroupOutput({ name: LOG_GROUP_NAME }).retentionInDays.apply((actual) => {
        if (actual !== config.logRetentionDays) {
            throw new Error(
                `CloudWatch 로그그룹 ${LOG_GROUP_NAME}의 retentionInDays가 기대값과 다릅니다 ` +
                    `(기대: ${config.logRetentionDays}, 실제: ${actual}). 콘솔에서 변경되었을 수 ` +
                    `있습니다 - logback-spring.xml의 retentionTimeDays와 정합을 다시 확인하세요.`,
            );
        }
    });

    // SNS 알람 토픽은 비용이 거의 없고, destroy/up으로 재생성하면 이메일 구독자가 매 시즌
    // 재확인(컨펌 클릭)을 다시 해야 하는 불편이 생긴다. protect:true로 상시 보존한다 -
    // 알람(MetricAlarm)은 매 시즌 새로 뜨는 인스턴스/ALB를 가리켜야 해서 ephemeral로 둔다.
    const alertsTopic = new aws.sns.Topic(
        "hello-prod-alerts",
        { name: "hello-prod-alerts" },
        { protect: true },
    );

    if (config.snsAlarmEmail) {
        new aws.sns.TopicSubscription(
            "hello-prod-alerts-email",
            { topic: alertsTopic.arn, protocol: "email", endpoint: config.snsAlarmEmail },
            { protect: true },
        );
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

    // 인스턴스가 2대라 한 대 상태이상 자체는 즉시 전체 장애가 아니지만, 롤링 배포 중
    // 나머지 한 대가 이상이면 다운타임으로 이어지므로 인스턴스별로 개별 알람을 둔다.
    // 첫 번째 알람은 기존 단일 인스턴스 알람과 동일한 이름을 유지해 불필요한 삭제/재생성을 피한다.
    const alarmNames = ["hello-prod-springboot-status-check-failed", "hello-prod-springboot-status-check-failed-b"];
    springbootInstanceIds.forEach((instanceId, i) => {
        new aws.cloudwatch.MetricAlarm(alarmNames[i], {
            name: alarmNames[i],
            namespace: "AWS/EC2",
            metricName: "StatusCheckFailed",
            dimensions: { InstanceId: instanceId },
            statistic: "Maximum",
            period: 60,
            evaluationPeriods: 2,
            threshold: 1,
            comparisonOperator: "GreaterThanOrEqualToThreshold",
            treatMissingData: "breaching",
            alarmActions: [alertsTopic.arn],
            okActions: [alertsTopic.arn],
        });
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

    return { logGroupName, alertsTopic };
}
