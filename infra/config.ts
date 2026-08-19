import * as pulumi from "@pulumi/pulumi";

const cfg = new pulumi.Config();
const awsCfg = new pulumi.Config("aws");

export const config = {
    region: awsCfg.require("region"),

    adminSshCidr: cfg.require("adminSshCidr"),
    keyPairName: cfg.require("keyPairName"),

    dbUsername: cfg.require("dbUsername"),
    dbPassword: cfg.requireSecret("dbPassword"),
    dbName: cfg.require("dbName"),
    dbInstanceClass: cfg.get("dbInstanceClass") ?? "db.t3.micro",
    dbAllocatedStorage: cfg.getNumber("dbAllocatedStorage") ?? 20,
    dbEngineVersion: cfg.get("dbEngineVersion") ?? "8.0",

    bastionInstanceType: cfg.get("bastionInstanceType") ?? "t3.micro",
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
