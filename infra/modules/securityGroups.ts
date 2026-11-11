import * as aws from "@pulumi/aws";

export function createSecurityGroups(vpcId: aws.ec2.Vpc["id"]) {
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

    // NAT 인스턴스(hello-prod-nat)에 붙어있는 보안그룹 - NAT 인스턴스 자체가 이제
    // compute/bastionNat.ts에서 데이터 소스로만 참조되는 상시 보존 대상이라, 이 SG도
    // 더 이상 Pulumi가 생성/수정/삭제를 관리하지 않고 데이터 소스로만 읽는다.
    // 관리자 SSH ingress(CIDR) 룰은 이 스택 밖에서 수동으로 관리된다 - preview는
    // 이 룰의 변경을 감지하지 못하므로 바뀌어도 drift로 드러나지 않는다 (README 참고).
    const bastionSg = aws.ec2.getSecurityGroupOutput({
        filters: [{ name: "tag:Name", values: ["hellogsm-nat-sg"] }],
    });

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

    // RDS(hello-prod-mysql)는 이번 상용 인프라 정리 범위에서 제외되어 계속 운영 중이다 -
    // 이 SG를 ephemeral 리소스들과 함께 destroy하면 RDS 인스턴스가 보안그룹 없이 남아
    // 장애로 이어지므로 protect:true로 막는다 (database.ts와 별개로 이 SG만 보호).
    const rdsSg = new aws.ec2.SecurityGroup(
        "hello-prod-rds-sg",
        {
            vpcId,
            description: "RDS MySQL - allow 3306 from Spring Boot and Bastion",
            egress: [{ protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] }],
            tags: { Name: "hello-prod-rds-sg" },
        },
        { protect: true },
    );

    // 순환 참조(springboot <-> redis/rds) 회피를 위해 SG 룰은 별도 리소스로 분리 생성.
    // 주의: springbootSg/redisSg/rdsSg는 인라인 ingress를 쓰지 않는다 - AWS 프로바이더는
    // 한 SG에 인라인 룰과 독립 SecurityGroupRule 리소스를 섞어 쓰는 걸 금지하며, 섞이면
    // 아래 독립 룰들이 조용히 덮어써진다. 이 SG들에는 절대 인라인 ingress를 추가하지 말 것.
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

    // springbootSg는 ephemeral이라 이 룰도 protect:true를 걸면 안 된다 - springbootSg를
    // destroy하는 순간 "아직 그 SG를 참조하는 룰이 있다"는 AWS DependencyViolation으로
    // 막혀버린다. rdsSg(protect:true)는 그대로 남고, 이 룰만 springboot와 함께
    // 사라졌다가 다음 시즌 springboot가 다시 뜰 때 함께 재생성되는 게 맞는 동작이다.
    new aws.ec2.SecurityGroupRule("rds-from-springboot-3306", {
        type: "ingress",
        securityGroupId: rdsSg.id,
        sourceSecurityGroupId: springbootSg.id,
        protocol: "tcp",
        fromPort: 3306,
        toPort: 3306,
    });

    // 아키텍처 다이어그램상 Bastion+NAT -> db -> MySQL 경로 - 관리자가 Bastion을 경유해
    // SSH 터널링으로 RDS에 직접 접근(점검/마이그레이션 등)할 수 있도록 허용.
    // rdsSg/bastionSg 둘 다 상시 보존 대상(protect:true 또는 데이터 소스)이라, 이 룰도
    // destroy 대상에서 빼야 한다 - 안 그러면 ephemeral 정리 때마다 Bastion의 RDS
    // 점검 경로가 끊겼다가 다음 pulumi up에서야 복구된다.
    new aws.ec2.SecurityGroupRule(
        "rds-from-bastion-3306",
        {
            type: "ingress",
            securityGroupId: rdsSg.id,
            sourceSecurityGroupId: bastionSg.id,
            protocol: "tcp",
            fromPort: 3306,
            toPort: 3306,
        },
        { protect: true },
    );

    return { albSg, bastionSg, springbootSg, redisSg, rdsSg };
}
