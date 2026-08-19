import * as aws from "@pulumi/aws";
import * as fs from "fs";
import * as path from "path";
import { config } from "../../config";
import { getAmazonLinux2023Ami } from "../ami";

export interface RedisServerResult {
    instance: aws.ec2.Instance;
}

const userData = fs.readFileSync(path.join(__dirname, "../../userdata/redis-ec2.sh"), "utf-8");

export function createRedisServer(
    privateSubnetId: aws.ec2.Subnet["id"],
    redisSgId: aws.ec2.SecurityGroup["id"],
): RedisServerResult {
    const ami = getAmazonLinux2023Ami();

    const instance = new aws.ec2.Instance("hello-prod-redis", {
        ami: ami.id,
        instanceType: config.redisInstanceType,
        subnetId: privateSubnetId,
        vpcSecurityGroupIds: [redisSgId],
        keyName: config.keyPairName,
        userData,
        tags: { Name: "hello-prod-redis" },
    });

    return { instance };
}
