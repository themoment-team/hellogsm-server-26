import * as aws from "@pulumi/aws";

// Bastion+NAT 인스턴스("hello-prod-nat")는 입학 전형 시즌과 무관하게 상시 보존하는
// 리소스다 - 이 인스턴스는 운영 중인 EC2/RDS 접근용 Bastion이자 프라이빗 서브넷의
// NAT 역할을 겸하고 있어, 매 시즌 destroy/up으로 흔들면 안 된다(AMI/userData를 건드리면
// stop/start가 발생해 iptables MASQUERADE 설정이 날아갈 위험도 있음 - README 참고).
// 그래서 더 이상 Pulumi가 생성/삭제를 관리하지 않고 데이터 소스로만 참조한다.
export function createBastionNat() {
    const instance = aws.ec2.getInstanceOutput({
        filters: [{ name: "tag:Name", values: ["hello-prod-nat"] }],
    });

    // 이 프로바이더 버전은 단일 EIP 데이터 소스(getEip)가 없어 getEips(복수형)로 조회한다.
    // hello-prod-eip는 NAT 인스턴스에 고정 연결된 단일 EIP라 결과가 항상 정확히 1개다.
    const eips = aws.ec2.getEipsOutput({
        tags: { Name: "hello-prod-eip" },
    });
    const eip = { publicIp: eips.publicIps[0] };

    return { instance, eip };
}

export type BastionNatResult = ReturnType<typeof createBastionNat>;
