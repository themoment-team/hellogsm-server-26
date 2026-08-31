import * as aws from "@pulumi/aws";

// 항상 최신 Amazon Linux 2023 x86_64 AMI를 조회한다 (AMI ID 하드코딩 금지).
// "al2023-ami-*-x86_64"는 minimal("al2023-ami-minimal-*")/ecs("al2023-ami-ecs-hvm-*") 변형까지
// 매치된다 - minimal은 루트 볼륨 기본값이 8GiB가 아니라 2GiB라 docker build 중 디스크가 찰 수
// 있어 표준 이미지로 필터를 좁힌다. mostRecent:true이므로 새 AMI가 나오면 이 값이 바뀌고,
// aws.ec2.Instance의 ami는 ForceNew라 인스턴스가 교체된다 - springboot는
// ignoreChanges로 별도 보호한다 (compute/springbootServer.ts 참고).
export function getAmazonLinux2023Ami() {
    return aws.ec2.getAmiOutput({
        mostRecent: true,
        owners: ["amazon"],
        filters: [
            { name: "name", values: ["al2023-ami-2023.*-kernel-6.1-x86_64"] },
            { name: "virtualization-type", values: ["hvm"] },
        ],
    });
}
