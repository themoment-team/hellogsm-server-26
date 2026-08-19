import * as aws from "@pulumi/aws";
import * as pulumi from "@pulumi/pulumi";
import { config } from "../config";

export interface IamResult {
    springbootInstanceProfile: aws.iam.InstanceProfile;
    codeDeployServiceRole: aws.iam.Role;
    githubActionsDeployRole: aws.iam.Role;
}

const CODEDEPLOY_APPLICATION_NAME = "hellogsm-prod-codedeploy";
const CODEDEPLOY_DEPLOYMENT_GROUP_NAME = "api-prod-hellogsm-kr";
const LOG_GROUP_NAME = "hellogsm-prod-log";

export function createIam(
    deploymentBucketArn: pulumi.Input<string>,
    appAssetsBucketArn: pulumi.Input<string>,
): IamResult {
    const identity = aws.getCallerIdentityOutput({});
    const accountId = identity.accountId;
    const region = config.region;

    const logGroupArn = pulumi.interpolate`arn:aws:logs:${region}:${accountId}:log-group:${LOG_GROUP_NAME}:*`;
    const codeDeployApplicationArn = pulumi.interpolate`arn:aws:codedeploy:${region}:${accountId}:application:${CODEDEPLOY_APPLICATION_NAME}`;
    const codeDeployDeploymentGroupArn = pulumi.interpolate`arn:aws:codedeploy:${region}:${accountId}:deploymentgroup:${CODEDEPLOY_APPLICATION_NAME}/${CODEDEPLOY_DEPLOYMENT_GROUP_NAME}`;

    // ---- EC2 (Spring Boot) instance role/profile ----
    const springbootEc2Role = new aws.iam.Role("hello-prod-springboot-ec2-role", {
        name: "hello-prod-springboot-ec2-role",
        assumeRolePolicy: JSON.stringify({
            Version: "2012-10-17",
            Statement: [
                {
                    Effect: "Allow",
                    Principal: { Service: "ec2.amazonaws.com" },
                    Action: "sts:AssumeRole",
                },
            ],
        }),
    });

    new aws.iam.RolePolicy("hello-prod-springboot-ec2-policy", {
        role: springbootEc2Role.id,
        policy: pulumi.jsonStringify({
            Version: "2012-10-17",
            Statement: [
                {
                    Sid: "DeploymentBucketRead",
                    Effect: "Allow",
                    Action: ["s3:GetObject", "s3:ListBucket"],
                    Resource: [deploymentBucketArn, pulumi.interpolate`${deploymentBucketArn}/*`],
                },
                {
                    Sid: "AppAssetsBucketReadWrite",
                    Effect: "Allow",
                    Action: ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:ListBucket"],
                    Resource: [appAssetsBucketArn, pulumi.interpolate`${appAssetsBucketArn}/*`],
                },
                {
                    Sid: "CloudWatchLogs",
                    Effect: "Allow",
                    Action: ["logs:CreateLogStream", "logs:PutLogEvents", "logs:DescribeLogStreams"],
                    Resource: logGroupArn,
                },
                {
                    Sid: "SnsPublish",
                    Effect: "Allow",
                    Action: ["sns:Publish"],
                    // SnsSmsTemplate로 전화번호 지정 직접 SMS 발송(SendSmsServiceImpl)만 사용 -
                    // Topic ARN이 존재하지 않는 발행 방식이라 리소스 레벨로 좁힐 수 없음(AWS 제약).
                    Resource: "*",
                },
            ],
        }),
    });

    // CodeDeploy agent가 배포 상태를 보고하기 위해 필요한 최소 권한(AWS 관리형 정책 없음 - 인라인으로 부여)
    new aws.iam.RolePolicy("hello-prod-springboot-codedeploy-agent-policy", {
        role: springbootEc2Role.id,
        policy: JSON.stringify({
            Version: "2012-10-17",
            Statement: [
                {
                    Sid: "CodeDeployAgent",
                    Effect: "Allow",
                    Action: [
                        "codedeploy:PollHostCommand",
                        "codedeploy:PutHostCommandComplete",
                        "codedeploy:GetDeployment",
                        "codedeploy:GetApplicationRevision",
                    ],
                    Resource: "*",
                },
            ],
        }),
    });

    const springbootInstanceProfile = new aws.iam.InstanceProfile("hello-prod-springboot-instance-profile", {
        role: springbootEc2Role.name,
    });

    // ---- CodeDeploy service role ----
    const codeDeployServiceRole = new aws.iam.Role("hello-prod-codedeploy-service-role", {
        name: "hello-prod-codedeploy-service-role",
        assumeRolePolicy: JSON.stringify({
            Version: "2012-10-17",
            Statement: [
                {
                    Effect: "Allow",
                    Principal: { Service: "codedeploy.amazonaws.com" },
                    Action: "sts:AssumeRole",
                },
            ],
        }),
    });

    new aws.iam.RolePolicyAttachment("hello-prod-codedeploy-service-role-attach", {
        role: codeDeployServiceRole.name,
        policyArn: "arn:aws:iam::aws:policy/service-role/AWSCodeDeployRole",
    });

    // ---- GitHub OIDC provider + deploy role ----
    const githubOidcProvider = new aws.iam.OpenIdConnectProvider("github-actions-oidc", {
        url: "https://token.actions.githubusercontent.com",
        clientIdLists: ["sts.amazonaws.com"],
        // GitHub Actions OIDC 루트 CA 지문 (2023년 기준 공개된 값)
        thumbprintLists: ["6938fd4d98bab03faadb97b34396831e3780aea1"],
    });

    const githubActionsDeployRole = new aws.iam.Role("hello-prod-github-actions-deploy-role", {
        name: "hello-prod-github-actions-deploy-role",
        assumeRolePolicy: pulumi.jsonStringify({
            Version: "2012-10-17",
            Statement: [
                {
                    Effect: "Allow",
                    Principal: { Federated: githubOidcProvider.arn },
                    Action: "sts:AssumeRoleWithWebIdentity",
                    Condition: {
                        StringEquals: {
                            "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
                        },
                        StringLike: {
                            "token.actions.githubusercontent.com:sub": `repo:${config.githubOrg}/${config.githubRepo}:ref:refs/heads/main`,
                        },
                    },
                },
            ],
        }),
    });

    new aws.iam.RolePolicy("hello-prod-github-actions-deploy-policy", {
        role: githubActionsDeployRole.id,
        policy: pulumi.jsonStringify({
            Version: "2012-10-17",
            Statement: [
                {
                    Sid: "UploadDeploymentArtifact",
                    Effect: "Allow",
                    Action: ["s3:PutObject"],
                    Resource: pulumi.interpolate`${deploymentBucketArn}/prod/*`,
                },
                {
                    Sid: "TriggerCodeDeploy",
                    Effect: "Allow",
                    Action: [
                        "codedeploy:CreateDeployment",
                        "codedeploy:GetDeployment",
                        "codedeploy:GetApplication",
                        "codedeploy:RegisterApplicationRevision",
                    ],
                    Resource: [codeDeployApplicationArn, codeDeployDeploymentGroupArn],
                },
                {
                    Sid: "ReadDeploymentConfig",
                    Effect: "Allow",
                    Action: ["codedeploy:GetDeploymentConfig"],
                    Resource: "*",
                },
            ],
        }),
    });

    return { springbootInstanceProfile, codeDeployServiceRole, githubActionsDeployRole };
}
