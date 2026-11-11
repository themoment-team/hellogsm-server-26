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

    // NAT 인스턴스에 연결된 EIP는 이미 조회한 인스턴스 결과의 publicIp를 그대로 쓴다.
    // hello-prod-eip는 과거 pulumi import로 흡수된 리소스라 AWS 측에 Name 태그가
    // 실제로 있는지 보장할 수 없고, tag:Name 필터로 따로 조회하면 태그가 없을 때
    // getEips가 에러 없이 빈 배열을 반환해 publicIps[0]이 undefined가 되어
    // bastionPublicIp 스택 출력이 조용히 사라진다. instance.publicIp를 쓰면 태그
    // 의존 없이 정확하고(EIP가 고정 연결돼 있으므로 인스턴스의 공개 IP와 항상 같다)
    // 조회도 한 번 줄어든다.
    const eip = { publicIp: instance.publicIp };

    // NAT 인스턴스가 실제로 쓰는 IAM role/instance-profile(hello-prod-nat-ec2-role,
    // hello-prod-nat-instance-profile)도 상시 보존 대상이다 - 리팩토링 당시 이 스택
    // 코드 어디에도 선언되지 않은 채 남아있어, state rm 없이 pulumi up/preview를 돌리면
    // "코드에서 사라진 관리 리소스"로 인식되어 삭제 대상으로 잡히는 버그가 있었다
    // (삭제되면 가동 중인 NAT 인스턴스가 SSM 접속/AWS API 호출 권한을 잃는다).
    // instance.iamInstanceProfile로 프로필 이름을 그대로 가져와 태그 의존 없이 조회한다.
    const role = aws.iam.getRoleOutput({ name: "hello-prod-nat-ec2-role" });
    const instanceProfile = aws.iam.getInstanceProfileOutput({ name: instance.iamInstanceProfile });

    // SSM 접속에 필요한 AmazonSSMManagedInstanceCore attach(hello-prod-nat-ssm-attach)는
    // AWS IAM에 "attachment" 전용 데이터 소스가 없어(RolePolicyAttachment는 관계일 뿐
    // 독립된 조회 대상이 아님) role에 attach된 managed policy 목록으로 간접 검증한다.
    aws.iam.getRolePolicyAttachmentsOutput({ roleName: role.name }).attachedPolicies.apply((policies) => {
        const hasSsmPolicy = policies.some(
            (p) => p.policyArn === "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
        );
        if (!hasSsmPolicy) {
            throw new Error(
                "NAT 인스턴스 role(hello-prod-nat-ec2-role)에 AmazonSSMManagedInstanceCore가 " +
                    "attach되어 있지 않습니다 - SSM 접속이 끊겼을 수 있습니다. 콘솔에서 확인 후 " +
                    "다시 attach하세요.",
            );
        }
    });

    return { instance, eip, role, instanceProfile };
}

export type BastionNatResult = ReturnType<typeof createBastionNat>;
