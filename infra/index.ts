import * as aws from "@pulumi/aws";
import * as pulumi from "@pulumi/pulumi";

import { fqdn } from "./config";
import { createNetwork } from "./modules/network";
import { createSecurityGroups } from "./modules/securityGroups";
import { createS3Buckets } from "./modules/s3";
import { createIam } from "./modules/iam";
import { createDatabase } from "./modules/database";
import { createBastionNat } from "./modules/compute/bastionNat";
import { createSpringbootServer } from "./modules/compute/springbootServer";
import { createRedisServer } from "./modules/compute/redisServer";
import { createDnsAndCertificate } from "./modules/dnsCert";
import { createAlb } from "./modules/alb";
import { createCodeDeploy } from "./modules/codeDeploy";
import { createMonitoring } from "./modules/monitoring";

const network = createNetwork();
const sg = createSecurityGroups(network.vpc.id);
const s3 = createS3Buckets();
const iam = createIam(s3.deploymentBucket.arn, s3.appAssetsBucket.arn);
const database = createDatabase(
    network.prodPrivateSubnet2a.id,
    network.privateSubnet2b.id,
    sg.rdsSg.id,
);

const bastionNat = createBastionNat(network.prodPublicSubnet2a.id, sg.bastionSg.id);

// 프라이빗 서브넷(2a, 2b) 모두 인터넷 라우트를 기존 Bastion+NAT 인스턴스로 향하게 한다
// (실제 운영 중이던 라우팅 구성과 동일). network 모듈 생성 시점엔 bastion 인스턴스 참조가
// 없어 순환 의존이 발생하므로 여기서 별도 생성한다. protect:true로 실수 삭제를 막는다 -
// 없으면 프라이빗 서브넷의 인터넷 경로가 사라지고 메인 라우트테이블로 폴백한다.
const natRoute2a = new aws.ec2.Route(
    "hello-prod-priv-rtb-a-nat-route",
    {
        routeTableId: network.prodPrivateRouteTable2a.id,
        destinationCidrBlock: "0.0.0.0/0",
        networkInterfaceId: bastionNat.instance.primaryNetworkInterfaceId,
    },
    { protect: true },
);

const natRoute2b = new aws.ec2.Route(
    "hello-prod-priv-rtb-b-nat-route",
    {
        routeTableId: network.prodPrivateRouteTable2b.id,
        destinationCidrBlock: "0.0.0.0/0",
        networkInterfaceId: bastionNat.instance.primaryNetworkInterfaceId,
    },
    { protect: true },
);

// springboot/redis는 subnet/sg/instance profile만 참조하고 라우트를 참조하지 않아
// Pulumi 그래프상 natRoute와 병렬로 생성될 수 있다. 재해복구 시나리오처럼 라우트가 아직
// 없는 상태에서 인스턴스가 먼저 뜨면 userdata의 dnf install/wget이 인터넷 접근 없이
// 조용히 실패하므로(set -euxo pipefail) 명시적으로 순서를 고정한다.
const natRouteDeps = { dependsOn: [natRoute2a, natRoute2b] };

const springboot = createSpringbootServer(
    network.prodPrivateSubnet2a.id,
    sg.springbootSg.id,
    iam.springbootInstanceProfile.name,
    natRouteDeps,
);

const redis = createRedisServer(network.prodPrivateSubnet2a.id, sg.redisSg.id, natRouteDeps);

const dnsCert = createDnsAndCertificate();

const alb = createAlb(
    [network.prodPublicSubnet2a.id, network.publicSubnet2b.id],
    sg.albSg.id,
    network.vpc.id,
    springboot.instance.id,
    dnsCert.certificateValidation.certificateArn,
    dnsCert.zone.zoneId,
);

const codeDeploy = createCodeDeploy(iam.codeDeployServiceRole.arn, alb.targetGroup.name);

const monitoring = createMonitoring(
    alb.targetGroup.arnSuffix,
    alb.alb.arnSuffix,
    springboot.instance.id,
    redis.instance.id,
);

export const vpcId = network.vpc.id;
export const albDnsName = alb.alb.dnsName;
export const apiUrl = pulumi.interpolate`https://${fqdn}`;
export const rdsEndpointAddress = database.instance.address;
export const redisPrivateIp = redis.instance.privateIp;
export const springbootPrivateIp = springboot.instance.privateIp;
export const bastionPublicIp = bastionNat.eip.publicIp;
export const deploymentBucketArn = s3.deploymentBucket.arn;
export const appAssetsBucketArn = s3.appAssetsBucket.arn;
export const githubActionsRoleArn = iam.githubActionsDeployRole.arn;
export const codeDeployApplicationName = codeDeploy.application.name;
export const codeDeployDeploymentGroupName = codeDeploy.deploymentGroup.deploymentGroupName;
export const cloudWatchLogGroupName = monitoring.logGroup.name;
export const snsAlertsTopicArn = monitoring.alertsTopic.arn;
