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
            // EC2 태그 필터는 값이 같은 모든 인스턴스를 매칭하므로, springboot A/B 두 인스턴스가
            // 모두 동일한 Name 태그(config.codeDeployTagValue)를 갖는 한 이 필터 하나로 둘 다
            // 배포 대상에 들어온다 (springbootServer.ts 참고).
            // 실제 배포 트리거는 .github/workflows/hellogsm-prod-cd.yml의
            // `aws deploy create-deployment --deployment-config-name ...`이 이 기본값을 덮어쓰므로,
            // 롤링 배포 적용 여부는 그 워크플로 값을 기준으로 봐야 한다 (여기는 콘솔 수동 배포 시 기본값).
            deploymentConfigName: "CodeDeployDefault.HalfAtATime",
            ec2TagSets: [
                {
                    ec2TagFilters: [
                        { key: "Name", type: "KEY_AND_VALUE", value: config.codeDeployTagValue },
                    ],
                },
            ],
            deploymentStyle: {
                deploymentType: "IN_PLACE",
                // WITHOUT_TRAFFIC_CONTROL이면 배포 중(stop→rm→pull→run 수십 초~수 분)에도
                // ALB가 죽은 타겟에 계속 트래픽을 보내 502가 그대로 노출되고, 그 사이
                // hello-prod-alb-unhealthy-hosts 알람이 배포마다 발화해 알람 신뢰도가 떨어진다.
                // WITH_TRAFFIC_CONTROL + loadBalancerInfo로 배포 중 타겟을 등록 해제시킨다
                // (AWSCodeDeployRole에 RegisterTargets/DeregisterTargets/DescribeTargetHealth가
                // 이미 포함되어 있어 추가 IAM 변경은 필요 없음). 인스턴스가 2대이고 HalfAtATime이라
                // 한 대씩 교체되는 동안 나머지 한 대가 계속 트래픽을 받아 사용자 체감 다운타임이 없다.
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
