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

    // tag:Name만으로 조회하면, stage 등 다른 VPC에 같은 이름 태그가 우연히 생겨도 그걸
    // 그대로 진짜로 취급한다. vpcId로 조회 범위를 hello-vpc로 좁혀 다른 VPC의 리소스가
    // 섞여 들어올 가능성을 없앤다.
    const prodPublicSubnet2a = aws.ec2.getSubnetOutput({ ...byName("hello-prod-public-2a"), vpcId: vpc.id });
    const publicSubnet2b = aws.ec2.getSubnetOutput({ ...byName("hello-public-subnet-2b"), vpcId: vpc.id });
    const prodPrivateSubnet2a = aws.ec2.getSubnetOutput({ ...byName("hello-prod-private-2a"), vpcId: vpc.id });
    const privateSubnet2b = aws.ec2.getSubnetOutput({ ...byName("hello-private-subnet-2b"), vpcId: vpc.id });

    // igw/라우트테이블(hello-prod-pub-rtb-a, hello-pub-rtb, hello-prod-priv-rtb-a/b)은
    // NAT 라우트 리소스가 사라지면서 이 스택 어디에서도 id를 참조하지 않는다. 소비자 없는
    // 데이터 소스를 남겨두면 preview/up/destroy마다 불필요한 AWS API 호출이 추가되고,
    // 태그가 바뀌거나 리소스가 안 보이면 아무도 쓰지 않는 값 때문에 스택 전체가
    // "no match found"로 실패한다 - 그래서 조회 자체를 제거했다.
    return {
        vpc,
        prodPublicSubnet2a,
        publicSubnet2b,
        prodPrivateSubnet2a,
        privateSubnet2b,
    };
}

export type NetworkResult = ReturnType<typeof createNetwork>;
