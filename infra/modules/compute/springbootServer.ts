import * as aws from "@pulumi/aws";
import * as pulumi from "@pulumi/pulumi";
import * as fs from "fs";
import * as path from "path";
import { config } from "../../config";
import { getAmazonLinux2023Ami } from "../ami";

export interface SpringbootServerResult {
    instance: aws.ec2.Instance;
}

const userData = fs.readFileSync(path.join(__dirname, "../../userdata/springboot-ec2.sh"), "utf-8");

export function createSpringbootServer(
    privateSubnetId: aws.ec2.Subnet["id"],
    springbootSgId: aws.ec2.SecurityGroup["id"],
    instanceProfileName: aws.iam.InstanceProfile["name"],
    opts?: pulumi.ResourceOptions,
): SpringbootServerResult {
    const ami = getAmazonLinux2023Ami();

    const instance = new aws.ec2.Instance(
        "hello-prod-springboot",
        {
            ami: ami.id,
            instanceType: config.springbootInstanceType,
            subnetId: privateSubnetId,
            vpcSecurityGroupIds: [springbootSgId],
            keyName: config.keyPairName,
            iamInstanceProfile: instanceProfileName,
            userData,
            // CodeDeploy 배포 그룹의 EC2 태그 필터(config.codeDeployTagValue)와 반드시 일치해야 한다.
            tags: { Name: config.codeDeployTagValue },
        },
        {
            ...opts,
            // ami: mostRecent AMI 조회 결과가 바뀌면 ForceNew라 인스턴스가 교체된다.
            // userData: 스크립트를 고치면 replace는 아니지만 stop/start가 발생하는데,
            // scripts/prod-deploy.sh의 docker run에 --restart 정책이 없어 재기동 후
            // 앱 컨테이너가 스스로 안 올라온다 (CD를 수동으로 다시 돌리기 전까지 전면 장애).
            // 둘 다 배포 중인 prod 인스턴스에 의도치 않은 영향을 주므로 무시한다.
            ignoreChanges: ["ami", "userData"],
        },
    );

    return { instance };
}
