import * as aws from "@pulumi/aws";
import { config } from "../config";

export interface CodeDeployResult {
    application: aws.codedeploy.Application;
    deploymentGroup: aws.codedeploy.DeploymentGroup;
}

// hellogsm-prod-codedeploy / api-prod-hellogsm-kr는 2024년부터 실제 배포 이력이 쌓여있는
// 기존 리소스라 pulumi import로 흡수한다 (README 참고, GitHub Actions CD 워크플로가 이 정확한
// 이름을 참조하므로 이름을 바꿀 수도 없음). 기존에는 Auto Scaling Group 기반이었으나 그 ASG는
// 이미 삭제된 상태라 EC2 태그 필터 기반으로 갱신한다. protect:true로 실수 삭제를 막는다.
export function createCodeDeploy(
    serviceRoleArn: aws.iam.Role["arn"],
    targetGroupName: aws.lb.TargetGroup["name"],
): CodeDeployResult {
    const application = new aws.codedeploy.Application(
        "hellogsm-prod-codedeploy",
        {
            name: "hellogsm-prod-codedeploy",
            computePlatform: "Server",
        },
        { protect: true },
    );

    const deploymentGroup = new aws.codedeploy.DeploymentGroup(
        "api-prod-hellogsm-kr",
        {
            appName: application.name,
            deploymentGroupName: "api-prod-hellogsm-kr",
            serviceRoleArn,
            deploymentConfigName: "CodeDeployDefault.AllAtOnce",
            ec2TagSets: [
                {
                    ec2TagFilters: [
                        { key: "Name", type: "KEY_AND_VALUE", value: config.codeDeployTagValue },
                    ],
                },
            ],
            deploymentStyle: {
                deploymentType: "IN_PLACE",
                // WITHOUT_TRAFFIC_CONTROL이면 배포 중(stop→rm→build→run 수십 초~수 분)에도
                // ALB가 죽은 타겟에 계속 트래픽을 보내 502가 그대로 노출되고, 그 사이
                // hello-prod-alb-unhealthy-hosts 알람이 배포마다 발화해 알람 신뢰도가 떨어진다.
                // WITH_TRAFFIC_CONTROL + loadBalancerInfo로 배포 중 타겟을 등록 해제시킨다
                // (AWSCodeDeployRole에 RegisterTargets/DeregisterTargets/DescribeTargetHealth가
                // 이미 포함되어 있어 추가 IAM 변경은 필요 없음). 인스턴스가 1대라 다운타임 자체가
                // 없어지진 않지만 502가 즉각적인 연결 거부(503) 정도로 바뀐다.
                deploymentOption: "WITH_TRAFFIC_CONTROL",
            },
            loadBalancerInfo: {
                targetGroupInfos: [{ name: targetGroupName }],
            },
            autoRollbackConfiguration: {
                enabled: true,
                events: ["DEPLOYMENT_FAILURE"],
            },
        },
        { protect: true },
    );

    return { application, deploymentGroup };
}
