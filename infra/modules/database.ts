import * as aws from "@pulumi/aws";
import { config } from "../config";

export interface DatabaseResult {
    subnetGroup: aws.rds.SubnetGroup;
    instance: aws.rds.Instance;
}

export function createDatabase(
    prodPrivateSubnet2aId: aws.ec2.Subnet["id"],
    privateSubnet2bId: aws.ec2.Subnet["id"],
    rdsSgId: aws.ec2.SecurityGroup["id"],
): DatabaseResult {
    const subnetGroup = new aws.rds.SubnetGroup("hello-prod-rds-subnet-group", {
        name: "hello-prod-rds-subnet-group",
        subnetIds: [prodPrivateSubnet2aId, privateSubnet2bId],
        tags: { Name: "hello-prod-rds-subnet-group" },
    });

    const instance = new aws.rds.Instance(
        "hello-prod-mysql",
        {
            identifier: "hello-prod-mysql",
            engine: "mysql",
            engineVersion: config.dbEngineVersion,
            instanceClass: config.dbInstanceClass,
            allocatedStorage: config.dbAllocatedStorage,
            dbName: config.dbName,
            username: config.dbUsername,
            password: config.dbPassword,
            dbSubnetGroupName: subnetGroup.name,
            vpcSecurityGroupIds: [rdsSgId],
            publiclyAccessible: false,
            multiAz: false,
            storageEncrypted: true,
            backupRetentionPeriod: 7,
            skipFinalSnapshot: false,
            finalSnapshotIdentifier: "hello-prod-mysql-final-snapshot",
            tags: { Name: "hello-prod-mysql" },
        },
        { protect: true },
    );

    return { subnetGroup, instance };
}
