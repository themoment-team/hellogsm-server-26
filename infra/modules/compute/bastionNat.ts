import * as aws from "@pulumi/aws";

export interface BastionNatResult {
    instance: aws.ec2.Instance;
    eip: aws.ec2.Eip;
}

// 기존에 실제 운영 중인 Bastion+NAT 인스턴스(i-094592acf0b762882, "hello-prod-nat")를
// pulumi import로 그대로 흡수한다 (README 참고). AMI/인스턴스 타입/키페어는 새로
// 결정하지 않고 실제 라이브 인스턴스의 현재 값과 정확히 맞춰 replace가 발생하지 않도록 한다.
export function createBastionNat(
    publicSubnetId: aws.ec2.Subnet["id"],
    bastionSgId: aws.ec2.SecurityGroup["id"],
): BastionNatResult {
    const instance = new aws.ec2.Instance(
        "hello-prod-nat",
        {
            ami: "ami-0d211b58ff9d9bb61",
            instanceType: "t4g.micro",
            subnetId: publicSubnetId,
            vpcSecurityGroupIds: [bastionSgId],
            keyName: "hello-prod-bastion",
            // NAT 겸용 인스턴스는 자신이 발신지/목적지가 아닌 트래픽도 전달해야 하므로 필수
            sourceDestCheck: false,
            tags: { Name: "hello-prod-nat" },
        },
        { protect: true },
    );

    const eip = new aws.ec2.Eip(
        "hello-prod-eip",
        {
            instance: instance.id,
            domain: "vpc",
            tags: { Name: "hello-prod-eip" },
        },
        { protect: true },
    );

    return { instance, eip };
}
