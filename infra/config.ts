import * as pulumi from "@pulumi/pulumi";

const cfg = new pulumi.Config();
const awsCfg = new pulumi.Config("aws");

export const config = {
    region: awsCfg.require("region"),

    adminSshCidr: cfg.require("adminSshCidr"),
    // springboot/redis(신규 생성) 인스턴스에만 쓰인다. Bastion+NAT는 기존 운영 인스턴스를
    // import한 것이라 실제 키페어("hello-prod-bastion")가 compute/bastionNat.ts에
    // 하드코딩되어 있고 이 값을 쓰지 않는다.
    keyPairName: cfg.require("keyPairName"),

    dbUsername: cfg.require("dbUsername"),
    dbPassword: cfg.requireSecret("dbPassword"),
    dbName: cfg.require("dbName"),
    dbInstanceClass: cfg.get("dbInstanceClass") ?? "db.t3.micro",
    dbAllocatedStorage: cfg.getNumber("dbAllocatedStorage") ?? 20,
    dbEngineVersion: cfg.get("dbEngineVersion") ?? "8.0",

    springbootInstanceType: cfg.get("springbootInstanceType") ?? "t3.small",
    redisInstanceType: cfg.get("redisInstanceType") ?? "t3.micro",

    domainName: cfg.require("domainName"),
    subdomain: cfg.require("subdomain"),

    deploymentBucketName: cfg.require("deploymentBucketName"),
    appAssetsBucketName: cfg.require("appAssetsBucketName"),

    actuatorBasePath: cfg.require("actuatorBasePath"),

    githubOrg: cfg.require("githubOrg"),
    githubRepo: cfg.require("githubRepo"),

    codeDeployTagValue: cfg.get("codeDeployTagValue") ?? "hello-prod-springboot",

    snsAlarmEmail: cfg.get("snsAlarmEmail"),
    logRetentionDays: cfg.getNumber("logRetentionDays") ?? 0,
};

export const fqdn = `${config.subdomain}.${config.domainName}`;
