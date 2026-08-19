import * as aws from "@pulumi/aws";

// 항상 최신 Amazon Linux 2023 x86_64 AMI를 조회한다 (AMI ID 하드코딩 금지).
export function getAmazonLinux2023Ami() {
    return aws.ec2.getAmiOutput({
        mostRecent: true,
        owners: ["amazon"],
        filters: [
            { name: "name", values: ["al2023-ami-*-x86_64"] },
            { name: "virtualization-type", values: ["hvm"] },
        ],
    });
}
