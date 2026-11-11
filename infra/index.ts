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
import { createEcr } from "./modules/ecr";

const network = createNetwork();
const sg = createSecurityGroups(network.vpc.id);
const s3 = createS3Buckets();
const ecr = createEcr();
const iam = createIam(s3.deploymentBucket.arn, s3.appAssetsBucket.arn, ecr.repository.arn);
const database = createDatabase(
    network.prodPrivateSubnet2a.id,
    network.privateSubnet2b.id,
    sg.rdsSg.id,
);

// Bastion+NAT 인스턴스와 프라이빗 서브넷의 NAT 라우트는 모두 상시 보존 대상(데이터 소스)으로
// 바뀌어 Pulumi가 더 이상 생성/수정하지 않는다 - 매 시즌 springboot/redis를 올리기 전에도
// 항상 이미 존재하므로, 과거처럼 라우트 생성 순서를 맞추기 위한 dependsOn도 더 이상 필요 없다.
const bastionNat = createBastionNat();

// CodeDeploy HalfAtATime 롤링 배포를 위해 서로 다른 AZ(2a/2b)에 2대를 띄운다.
// 세션은 이미 Redis 외부 저장소(spring.session.redis)라 sticky session 없이
// 두 인스턴스 사이에서 트래픽이 분산돼도 안전하다.
// springbootA는 기존 운영 중인 단일 인스턴스와 동일한 pulumi 리소스 이름("hello-prod-springboot")을
// 그대로 유지한다 - 이름을 바꾸면 pulumi가 기존 운영 인스턴스를 삭제하고 새로 만들려 시도해
// 실제 서비스 다운타임이 발생한다. springbootB만 순수 추가 리소스다.
const springbootA = createSpringbootServer(
    "hello-prod-springboot",
    network.prodPrivateSubnet2a.id,
    sg.springbootSg.id,
    iam.springbootInstanceProfile.name,
);

const springbootB = createSpringbootServer(
    "hello-prod-springboot-b",
    network.privateSubnet2b.id,
    sg.springbootSg.id,
    iam.springbootInstanceProfile.name,
);

const springbootInstanceIds = [springbootA.instance.id, springbootB.instance.id];

const redis = createRedisServer(network.prodPrivateSubnet2a.id, sg.redisSg.id);

const dnsCert = createDnsAndCertificate();

const alb = createAlb(
    [network.prodPublicSubnet2a.id, network.publicSubnet2b.id],
    sg.albSg.id,
    network.vpc.id,
    springbootInstanceIds,
    dnsCert.certificateValidation.certificateArn,
    dnsCert.zone.zoneId,
);

const codeDeploy = createCodeDeploy(iam.codeDeployServiceRole.arn, alb.targetGroup.name);

const monitoring = createMonitoring(
    alb.targetGroup.arnSuffix,
    alb.alb.arnSuffix,
    springbootInstanceIds,
    redis.instance.id,
);

export const vpcId = network.vpc.id;
export const albDnsName = alb.alb.dnsName;
export const apiUrl = pulumi.interpolate`https://${fqdn}`;
export const rdsEndpointAddress = database.instance.address;
export const redisPrivateIp = redis.instance.privateIp;
export const springbootPrivateIpA = springbootA.instance.privateIp;
export const springbootPrivateIpB = springbootB.instance.privateIp;
export const bastionPublicIp = bastionNat.eip.publicIp;
export const deploymentBucketArn = s3.deploymentBucket.arn;
export const appAssetsBucketArn = s3.appAssetsBucket.arn;
export const githubActionsRoleArn = iam.githubActionsDeployRole.arn;
export const codeDeployApplicationName = codeDeploy.application.name;
export const codeDeployDeploymentGroupName = codeDeploy.deploymentGroup.deploymentGroupName;
export const cloudWatchLogGroupName = monitoring.logGroupName;
export const snsAlertsTopicArn = monitoring.alertsTopic.arn;
export const ecrRepositoryUrl = ecr.repository.repositoryUrl;
