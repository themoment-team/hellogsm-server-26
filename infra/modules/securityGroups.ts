import * as aws from "@pulumi/aws";

export interface SecurityGroupsResult {
    albSg: aws.ec2.SecurityGroup;
    bastionSg: aws.ec2.SecurityGroup;
    springbootSg: aws.ec2.SecurityGroup;
    redisSg: aws.ec2.SecurityGroup;
    rdsSg: aws.ec2.SecurityGroup;
}

export function createSecurityGroups(vpcId: aws.ec2.Vpc["id"]): SecurityGroupsResult {
    const albSg = new aws.ec2.SecurityGroup("hello-prod-alb-sg", {
        vpcId,
        description: "ALB - allow inbound HTTP/HTTPS from internet",
        ingress: [
            { protocol: "tcp", fromPort: 80, toPort: 80, cidrBlocks: ["0.0.0.0/0"] },
            { protocol: "tcp", fromPort: 443, toPort: 443, cidrBlocks: ["0.0.0.0/0"] },
        ],
        egress: [{ protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] }],
        tags: { Name: "hello-prod-alb-sg" },
    });

    // 기존에 실제 운영 중인 NAT 인스턴스가 붙어 있는 보안그룹을 그대로 import한다
    // (pulumi import, README 참고). 현재 룰이 0.0.0.0/0 전체 허용으로 다소 느슨하지만,
    // "있는 그대로 흡수"가 목표이므로 임의로 좁히지 않고 실제 상태를 그대로 코드에 반영한다.
    const bastionSg = new aws.ec2.SecurityGroup(
        "hellogsm-nat-sg",
        {
            vpcId,
            // description은 AWS에서 변경 시 리소스 교체(delete+recreate)를 유발하는 불변 필드라
            // 실제 값("hellogsm-nat-sg")과 정확히 동일하게 맞춘다.
            description: "hellogsm-nat-sg",
            ingress: [
                { protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0", "172.16.0.0/24"] },
            ],
            egress: [{ protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] }],
            tags: { Name: "hellogsm-nat-sg" },
        },
        { protect: true },
    );

    const springbootSg = new aws.ec2.SecurityGroup("hello-prod-springboot-sg", {
        vpcId,
        description: "Spring Boot EC2 - allow 8080 from ALB, SSH from Bastion",
        egress: [{ protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] }],
        tags: { Name: "hello-prod-springboot-sg" },
    });

    const redisSg = new aws.ec2.SecurityGroup("hello-prod-redis-sg", {
        vpcId,
        description: "Redis EC2 - allow 6379 from Spring Boot, SSH from Bastion",
        egress: [{ protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] }],
        tags: { Name: "hello-prod-redis-sg" },
    });

    const rdsSg = new aws.ec2.SecurityGroup("hello-prod-rds-sg", {
        vpcId,
        description: "RDS MySQL - allow 3306 from Spring Boot",
        egress: [{ protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] }],
        tags: { Name: "hello-prod-rds-sg" },
    });

    // 순환 참조(springboot <-> redis/rds) 회피를 위해 SG 룰은 별도 리소스로 분리 생성
    new aws.ec2.SecurityGroupRule("springboot-from-alb-8080", {
        type: "ingress",
        securityGroupId: springbootSg.id,
        sourceSecurityGroupId: albSg.id,
        protocol: "tcp",
        fromPort: 8080,
        toPort: 8080,
    });

    new aws.ec2.SecurityGroupRule("springboot-from-bastion-ssh", {
        type: "ingress",
        securityGroupId: springbootSg.id,
        sourceSecurityGroupId: bastionSg.id,
        protocol: "tcp",
        fromPort: 22,
        toPort: 22,
    });

    new aws.ec2.SecurityGroupRule("redis-from-springboot-6379", {
        type: "ingress",
        securityGroupId: redisSg.id,
        sourceSecurityGroupId: springbootSg.id,
        protocol: "tcp",
        fromPort: 6379,
        toPort: 6379,
    });

    new aws.ec2.SecurityGroupRule("redis-from-bastion-ssh", {
        type: "ingress",
        securityGroupId: redisSg.id,
        sourceSecurityGroupId: bastionSg.id,
        protocol: "tcp",
        fromPort: 22,
        toPort: 22,
    });

    new aws.ec2.SecurityGroupRule("rds-from-springboot-3306", {
        type: "ingress",
        securityGroupId: rdsSg.id,
        sourceSecurityGroupId: springbootSg.id,
        protocol: "tcp",
        fromPort: 3306,
        toPort: 3306,
    });

    return { albSg, bastionSg, springbootSg, redisSg, rdsSg };
}
