import * as aws from "@pulumi/aws";
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
): SpringbootServerResult {
    const ami = getAmazonLinux2023Ami();

    const instance = new aws.ec2.Instance("hello-prod-springboot", {
        ami: ami.id,
        instanceType: config.springbootInstanceType,
        subnetId: privateSubnetId,
        vpcSecurityGroupIds: [springbootSgId],
        keyName: config.keyPairName,
        iamInstanceProfile: instanceProfileName,
        userData,
        // CodeDeploy 배포 그룹의 EC2 태그 필터(config.codeDeployTagValue)와 반드시 일치해야 한다.
        tags: { Name: config.codeDeployTagValue },
    });

    return { instance };
}
