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

    // CloudWatchAppender가 기동 시 무조건 CreateLogGroup을 호출한다(그룹이 이미 있어도).
    // CreateLogGroup은 ":*" 없는 로그그룹 ARN 형태로 평가될 수 있어 두 형태 모두 허용한다.
    const logGroupArnBase = pulumi.interpolate`arn:aws:logs:${region}:${accountId}:log-group:${LOG_GROUP_NAME}`;
    const logGroupArn = pulumi.interpolate`${logGroupArnBase}:*`;
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
                    Action: [
                        "logs:CreateLogGroup",
                        "logs:CreateLogStream",
                        "logs:PutLogEvents",
                        "logs:DescribeLogStreams",
                    ],
                    Resource: [logGroupArnBase, logGroupArn],
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

    // CodeDeploy agent 설치 스크립트(userdata/springboot-ec2.sh)가 리전별 AWS 관리 버킷에서
    // 설치 파일을 wget으로 받아온다. "codedeploy:PollHostCommand" 등은 실제로 존재하지 않는
    // IAM 액션(agent의 host command API는 codedeploy-commands-secure: 네임스페이스)이라 삭제하고,
    // 대신 AWS 공식 가이드가 요구하는 이 S3 읽기 권한을 부여한다.
    new aws.iam.RolePolicy("hello-prod-springboot-codedeploy-agent-policy", {
        role: springbootEc2Role.id,
        policy: JSON.stringify({
            Version: "2012-10-17",
            Statement: [
                {
                    Sid: "CodeDeployAgentInstaller",
                    Effect: "Allow",
                    Action: ["s3:GetObject"],
                    Resource: "arn:aws:s3:::aws-codedeploy-ap-northeast-2/*",
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
        // aws.iam.OpenIdConnectProvider는 thumbprintLists를 필수로 요구하지만, 2024-12-12부터
        // AWS는 token.actions.githubusercontent.com 같은 신뢰된 IdP에 대해 이 값을 실제로는
        // 무시한다. 갱신이 필요한 값이 아니므로 이후 값을 최신화할 필요 없음.
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
