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
export function createCodeDeploy(serviceRoleArn: aws.iam.Role["arn"]): CodeDeployResult {
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
                deploymentOption: "WITHOUT_TRAFFIC_CONTROL",
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
