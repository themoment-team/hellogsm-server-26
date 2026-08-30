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
            // protect:true를 우회(pulumi state unprotect)해서라도 실수로 삭제될 경우의
            // 마지막 방어선. deletionProtection은 AWS API 레벨에서 한 번 더 막아준다 -
            // 삭제하려면 이 값을 먼저 false로 내려야 해서 최소 2단계 확인을 강제한다.
            deletionProtection: true,
            skipFinalSnapshot: false,
            // 고정 식별자라 이 이름의 스냅샷이 이미 존재하는 상태에서 삭제하면
            // DBSnapshotAlreadyExists로 실패한다. 재삭제 전 기존 스냅샷을 지우거나
            // 이 값을 갱신할 것.
            finalSnapshotIdentifier: "hello-prod-mysql-final-snapshot",
            tags: { Name: "hello-prod-mysql" },
        },
        { protect: true },
    );

    return { subnetGroup, instance };
}
