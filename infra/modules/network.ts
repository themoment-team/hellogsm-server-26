import * as aws from "@pulumi/aws";

export interface NetworkResult {
    vpc: aws.ec2.Vpc;
    igw: aws.ec2.InternetGateway;
    prodPublicSubnet2a: aws.ec2.Subnet;
    publicSubnet2b: aws.ec2.Subnet;
    prodPrivateSubnet2a: aws.ec2.Subnet;
    privateSubnet2b: aws.ec2.Subnet;
    prodPublicRouteTable2a: aws.ec2.RouteTable;
    publicRouteTable2b: aws.ec2.RouteTable;
    prodPrivateRouteTable2a: aws.ec2.RouteTable;
    prodPrivateRouteTable2b: aws.ec2.RouteTable;
}

// 이 모듈이 다루는 VPC/서브넷/IGW/라우트테이블은 전부 기존에 실제로 존재하는(2024년부터 운영되어온)
// 네트워크를 그대로 가져온 것이다 (pulumi import로 state에 흡수, README 참고).
// protect:true로 걸어 실수로 삭제되는 일이 없도록 한다 - hello-pub-rtb는 stage 환경과 공유되는
// 라우트테이블이라 특히 주의가 필요하다.
export function createNetwork(): NetworkResult {
    const vpc = new aws.ec2.Vpc(
        "hello-vpc",
        {
            cidrBlock: "172.16.0.0/24",
            enableDnsSupport: true,
            enableDnsHostnames: true,
            tags: { Name: "hello-vpc" },
        },
        { protect: true },
    );

    const igw = new aws.ec2.InternetGateway(
        "hellogsm-igw",
        {
            vpcId: vpc.id,
            tags: { Name: "hellogsm-igw" },
        },
        { protect: true },
    );

    const prodPublicSubnet2a = new aws.ec2.Subnet(
        "hello-prod-public-2a",
        {
            vpcId: vpc.id,
            cidrBlock: "172.16.0.0/27",
            availabilityZone: "ap-northeast-2a",
            // 실제 기존 서브넷 설정(false)을 그대로 유지 - NAT 인스턴스는 EIP를 별도로 붙여 쓰므로 무관
            mapPublicIpOnLaunch: false,
            tags: { Name: "hello-prod-public-2a" },
        },
        { protect: true },
    );

    const publicSubnet2b = new aws.ec2.Subnet(
        "hello-public-subnet-2b",
        {
            vpcId: vpc.id,
            cidrBlock: "172.16.0.32/27",
            availabilityZone: "ap-northeast-2b",
            // 실제 기존 서브넷 설정(false)을 그대로 유지
            mapPublicIpOnLaunch: false,
            tags: { Name: "hello-public-subnet-2b" },
        },
        { protect: true },
    );

    const prodPrivateSubnet2a = new aws.ec2.Subnet(
        "hello-prod-private-2a",
        {
            vpcId: vpc.id,
            cidrBlock: "172.16.0.64/27",
            availabilityZone: "ap-northeast-2a",
            mapPublicIpOnLaunch: false,
            tags: { Name: "hello-prod-private-2a" },
        },
        { protect: true },
    );

    const privateSubnet2b = new aws.ec2.Subnet(
        "hello-private-subnet-2b",
        {
            vpcId: vpc.id,
            cidrBlock: "172.16.0.96/27",
            availabilityZone: "ap-northeast-2b",
            mapPublicIpOnLaunch: false,
            tags: { Name: "hello-private-subnet-2b" },
        },
        { protect: true },
    );

    // 2a 퍼블릭 전용 라우트테이블
    const prodPublicRouteTable2a = new aws.ec2.RouteTable(
        "hello-prod-pub-rtb-a",
        {
            vpcId: vpc.id,
            routes: [{ cidrBlock: "0.0.0.0/0", gatewayId: igw.id }],
            tags: { Name: "hello-prod-pub-rtb-a" },
        },
        { protect: true },
    );

    new aws.ec2.RouteTableAssociation("hello-prod-public-2a-assoc", {
        subnetId: prodPublicSubnet2a.id,
        routeTableId: prodPublicRouteTable2a.id,
    });

    // 2b 퍼블릭 라우트테이블 - hello-dev-public-2a(stage NAT)와 공유되므로 절대 삭제 금지
    const publicRouteTable2b = new aws.ec2.RouteTable(
        "hello-pub-rtb",
        {
            vpcId: vpc.id,
            routes: [{ cidrBlock: "0.0.0.0/0", gatewayId: igw.id }],
            tags: { Name: "hello-pub-rtb" },
        },
        { protect: true },
    );

    new aws.ec2.RouteTableAssociation("hello-public-subnet-2b-assoc", {
        subnetId: publicSubnet2b.id,
        routeTableId: publicRouteTable2b.id,
    });

    // 프라이빗 라우트테이블 2개 모두 인터넷 라우트(0.0.0.0/0)는 NAT 인스턴스 ENI로 향한다.
    // NAT 인스턴스는 index.ts에서 생성/조합되므로 여기서는 로컬 라우트만 두고,
    // 인터넷 라우트는 index.ts에서 별도 aws.ec2.Route로 추가한다.
    const prodPrivateRouteTable2a = new aws.ec2.RouteTable(
        "hello-prod-priv-rtb-a",
        {
            vpcId: vpc.id,
            tags: { Name: "hello-prod-priv-rtb-a" },
        },
        { protect: true },
    );

    new aws.ec2.RouteTableAssociation("hello-prod-private-2a-assoc", {
        subnetId: prodPrivateSubnet2a.id,
        routeTableId: prodPrivateRouteTable2a.id,
    });

    const prodPrivateRouteTable2b = new aws.ec2.RouteTable(
        "hello-prod-priv-rtb-b",
        {
            vpcId: vpc.id,
            tags: { Name: "hello-prod-priv-rtb-b" },
        },
        { protect: true },
    );

    new aws.ec2.RouteTableAssociation("hello-private-subnet-2b-assoc", {
        subnetId: privateSubnet2b.id,
        routeTableId: prodPrivateRouteTable2b.id,
    });

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
