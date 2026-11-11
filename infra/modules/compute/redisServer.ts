import * as aws from "@pulumi/aws";
import * as pulumi from "@pulumi/pulumi";
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
    instanceProfileName: aws.iam.InstanceProfile["name"],
    opts?: pulumi.ResourceOptions,
): RedisServerResult {
    const ami = getAmazonLinux2023Ami();

    const instance = new aws.ec2.Instance(
        "hello-prod-redis",
        {
            ami: ami.id,
            instanceType: config.redisInstanceType,
            subnetId: privateSubnetId,
            vpcSecurityGroupIds: [redisSgId],
            keyName: config.keyPairName,
            iamInstanceProfile: instanceProfileName,
            userData,
            tags: { Name: "hello-prod-redis" },
        },
        {
            ...opts,
            // springbootServer.ts와 동일한 이유 - mostRecent AMI 조회 결과가 바뀌면 ForceNew라
            // 가동 중인 Redis가 교체(데이터 유실)된다. 2026-11 정리 때 이 옵션이 없어서
            // pulumi preview가 실제로 이 인스턴스를 replace 대상으로 잡는 걸 발견했다.
            ignoreChanges: ["ami", "userData"],
        },
    );

    return { instance };
}
