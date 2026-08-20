import * as aws from "@pulumi/aws";
import * as pulumi from "@pulumi/pulumi";
import { config, fqdn } from "../config";

export interface AlbResult {
    alb: aws.lb.LoadBalancer;
    targetGroup: aws.lb.TargetGroup;
    httpsListener: aws.lb.Listener;
    httpListener: aws.lb.Listener;
    dnsRecord: aws.route53.Record;
}

export function createAlb(
    publicSubnetIds: pulumi.Input<pulumi.Input<string>[]>,
    albSgId: aws.ec2.SecurityGroup["id"],
    vpcId: aws.ec2.Vpc["id"],
    springbootInstanceId: aws.ec2.Instance["id"],
    certificateArn: pulumi.Input<string>,
    zoneId: pulumi.Input<string>,
): AlbResult {
    const alb = new aws.lb.LoadBalancer("hello-prod-alb", {
        name: "hello-prod-alb",
        internal: false,
        loadBalancerType: "application",
        securityGroups: [albSgId],
        subnets: publicSubnetIds,
        tags: { Name: "hello-prod-alb" },
    });

    const targetGroup = new aws.lb.TargetGroup("hello-prod-tg", {
        name: "hello-prod-tg",
        port: 8080,
        protocol: "HTTP",
        targetType: "instance",
        vpcId,
        healthCheck: {
            enabled: true,
            // 기본 집계 /health는 db/redis/diskSpace 인디케이터를 전부 포함해 하나만 DOWN이어도
            // 503이 되고, 인스턴스가 1대뿐이라 곧바로 전체 장애로 이어진다. liveness 그룹으로
            // 앱 프로세스 생존 여부만 체크한다 (application.yml의 management.endpoint.health.group.liveness).
            path: `${config.actuatorBasePath}/health/liveness`,
            protocol: "HTTP",
            matcher: "200",
            interval: 15,
            timeout: 5,
            healthyThreshold: 2,
            unhealthyThreshold: 2,
        },
        tags: { Name: "hello-prod-tg" },
    });

    // 단일 인스턴스 정적 등록 (Auto Scaling Group 아님). CodeDeploy가 WITH_TRAFFIC_CONTROL로
    // 배포 중 이 타겟을 등록 해제/재등록하지만, 인스턴스가 1대뿐이라 배포 중 다운타임 자체는
    // 여전히 존재한다(502가 아닌 연결 거부/503으로 바뀌는 정도).
    new aws.lb.TargetGroupAttachment("hello-prod-tg-attachment", {
        targetGroupArn: targetGroup.arn,
        targetId: springbootInstanceId,
        port: 8080,
    });

    const httpsListener = new aws.lb.Listener("hello-prod-alb-listener-443", {
        loadBalancerArn: alb.arn,
        port: 443,
        protocol: "HTTPS",
        sslPolicy: "ELBSecurityPolicy-TLS13-1-2-2021-06",
        certificateArn,
        defaultActions: [{ type: "forward", targetGroupArn: targetGroup.arn }],
    });

    const httpListener = new aws.lb.Listener("hello-prod-alb-listener-80", {
        loadBalancerArn: alb.arn,
        port: 80,
        protocol: "HTTP",
        defaultActions: [
            {
                type: "redirect",
                redirect: { port: "443", protocol: "HTTPS", statusCode: "HTTP_301" },
            },
        ],
    });

    const dnsRecord = new aws.route53.Record("api-prod-alb-alias", {
        zoneId,
        name: fqdn,
        type: "A",
        allowOverwrite: true,
        aliases: [
            {
                name: alb.dnsName,
                zoneId: alb.zoneId,
                evaluateTargetHealth: true,
            },
        ],
    });

    return { alb, targetGroup, httpsListener, httpListener, dnsRecord };
}
