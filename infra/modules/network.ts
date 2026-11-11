import * as aws from "@pulumi/aws";

function byName(name: string) {
    return { filters: [{ name: "tag:Name", values: [name] }] };
}

// VPC/서브넷/IGW/라우트테이블은 전부 기존에 실제로 존재하는(2024년부터 운영되어온) 네트워크다.
// 입학 전형 시즌이 끝나도 상시 보존해야 하는, 비용이 거의 없는 기초 자원이라 이제 Pulumi가
// 생성/수정/삭제를 관리하지 않고 데이터 소스로만 읽어서 참조한다 - 매 운영 사이클의
// `pulumi up`/`pulumi destroy --exclude-protected` 대상에서 완전히 제외된다.
// (과거에는 pulumi import + protect:true로 관리했으나, 이제는 아예 state에서 빼낸다.
// infra/README.md의 "보존 인프라를 Pulumi state에서 제거하기" 절 참고)
// hello-pub-rtb는 stage 환경(hello-dev-public-2a)과 공유되는 라우트테이블이라 애초에
// 이 스택이 손댈 일이 없어야 한다는 점에서도 데이터 소스화가 더 안전하다.
export function createNetwork() {
    const vpc = aws.ec2.getVpcOutput(byName("hello-vpc"));
    const igw = aws.ec2.getInternetGatewayOutput(byName("hellogsm-igw"));

    const prodPublicSubnet2a = aws.ec2.getSubnetOutput(byName("hello-prod-public-2a"));
    const publicSubnet2b = aws.ec2.getSubnetOutput(byName("hello-public-subnet-2b"));
    const prodPrivateSubnet2a = aws.ec2.getSubnetOutput(byName("hello-prod-private-2a"));
    const privateSubnet2b = aws.ec2.getSubnetOutput(byName("hello-private-subnet-2b"));

    const prodPublicRouteTable2a = aws.ec2.getRouteTableOutput(byName("hello-prod-pub-rtb-a"));
    const publicRouteTable2b = aws.ec2.getRouteTableOutput(byName("hello-pub-rtb"));
    const prodPrivateRouteTable2a = aws.ec2.getRouteTableOutput(byName("hello-prod-priv-rtb-a"));
    const prodPrivateRouteTable2b = aws.ec2.getRouteTableOutput(byName("hello-prod-priv-rtb-b"));

    return {
        vpc,
        igw,
        prodPublicSubnet2a,
        publicSubnet2b,
        prodPrivateSubnet2a,
        privateSubnet2b,
        prodPublicRouteTable2a,
        publicRouteTable2b,
        prodPrivateRouteTable2a,
        prodPrivateRouteTable2b,
    };
}

export type NetworkResult = ReturnType<typeof createNetwork>;
