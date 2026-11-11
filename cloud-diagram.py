# 2026-11 업데이트: Pulumi(TypeScript) IaC로 전환된 prod 인프라(themoment-team/hellogsm-server-26,
# infra/ 디렉터리) 기준으로 Production/CI-CD/Monitoring 구간을 다시 그렸다. Stage 구간은 이번
# 업데이트에서 재확인하지 않아 작년 구조 그대로 유지했다 - 실제와 다를 수 있으니 별도 확인 필요.
from diagrams import Diagram, Cluster, Edge
from diagrams.aws.compute import EC2, ECR
from diagrams.aws.database import RDS
from diagrams.aws.devtools import Codedeploy
from diagrams.aws.integration import SNS
from diagrams.aws.management import Cloudwatch
from diagrams.aws.network import ALB, InternetGateway
from diagrams.aws.storage import S3
from diagrams.onprem.client import User
from diagrams.onprem.vcs import Github

graph_attr = {
    "fontsize": "16",
    "fontname": "Arial",
    "splines": "ortho",
    "nodesep": "0.8",
    "ranksep": "1.2",
    "pad": "0.8",
    "bgcolor": "#f8f9fa"
}

node_attr = {
    "fontsize": "12",
    "fontname": "Arial",
    "shape": "box",
    "style": "rounded,filled",
    "margin": "0.3,0.1"
}

edge_attr = {
    "fontsize": "9",
    "fontname": "Arial",
    "penwidth": "2",
    "labeldistance": "0.5",
    "labelangle": "0"
}

with Diagram("HelloGSM-2027 Cloud Architecture",
             show=False,
             direction="TB",
             graph_attr=graph_attr,
             node_attr=node_attr,
             edge_attr=edge_attr):
    user = User("사용자")

    internet_gateway = InternetGateway("Internet Gateway")
    alb = ALB("Application\nLoad Balancer")

    # GitHub Actions가 고정 액세스 키 대신 OIDC로 임시 자격증명을 받아, CI가 미리 빌드한
    # 이미지를 ECR에 push하고(EC2는 더 이상 인스턴스 위에서 직접 docker build하지 않음),
    # 배포 리비전(appspec+스크립트)만 S3에 올려 CodeDeploy가 트리거되는 구조로 바뀌었다.
    with Cluster("CI/CD Pipeline", graph_attr={"bgcolor": "#e3f2fd", "style": "rounded", "margin": "10"}):
        github = Github("GitHub Actions\n(OIDC)")
        ecr = ECR("ECR")
        s3 = S3("S3 Bucket\n(배포 리비전)")
        codedeploy = Codedeploy("CodeDeploy")

        github >> Edge(label="image push", color="#2196f3") >> ecr
        github >> Edge(label="revision upload", color="#2196f3") >> s3
        s3 >> Edge(label="trigger", color="#2196f3") >> codedeploy

    # Discord 웹훅 대신 CloudWatch 알람 -> SNS Topic -> 이메일 구독으로 전환됐다.
    with Cluster("Monitoring", graph_attr={"bgcolor": "#fff3e0", "style": "rounded", "margin": "10"}):
        cloudwatch = Cloudwatch("CloudWatch")
        sns = SNS("SNS Topic")
        ops_email = User("운영팀 이메일")

        cloudwatch >> Edge(label="alert", color="#ff9800") >> sns
        sns >> Edge(label="email", color="#ff9800") >> ops_email

    with Cluster("VPC", graph_attr={"bgcolor": "#f3e5f5", "style": "rounded", "margin": "15"}):
        with Cluster("Production", graph_attr={"bgcolor": "#e8f5e8", "style": "rounded", "margin": "10"}):
            with Cluster("Public (AZ-2a)", graph_attr={"bgcolor": "#c8e6c9", "style": "rounded", "margin": "8"}):
                prod_bastion_nat = EC2("Bastion + NAT\nInstance")

            # springboot A/B 2대로 CodeDeploy HalfAtATime 롤링 배포(세션은 Redis 외부
            # 스토어라 sticky session 불필요). B는 stage와 공유하는 AZ-2b 서브넷에 떠서
            # 가용영역이 분산된다.
            with Cluster("Private (AZ-2a, prod 전용)", graph_attr={"bgcolor": "#ffcdd2", "style": "rounded", "margin": "8"}):
                prod_app_a = EC2("Spring Boot A")
                prod_redis = EC2("Redis")

            with Cluster("Private (AZ-2b, stage와 공유)", graph_attr={"bgcolor": "#ffe0b2", "style": "rounded", "margin": "8"}):
                prod_app_b = EC2("Spring Boot B")

            prod_db = RDS("MySQL 8.4")

        with Cluster("Stage", graph_attr={"bgcolor": "#e3f2fd", "style": "rounded", "margin": "10"}):
            with Cluster("Public", graph_attr={"bgcolor": "#bbdefb", "style": "rounded", "margin": "8"}):
                dev_bastion_nat = EC2("Bastion + NAT\nInstance")

            with Cluster("Private", graph_attr={"bgcolor": "#ffcdd2", "style": "rounded", "margin": "8"}):
                dev_app = EC2("Spring Boot\n+ Redis\n+ MySQL")

    user >> Edge(label="https", color="#4caf50") >> internet_gateway
    internet_gateway >> Edge(color="#4caf50") >> alb

    alb >> Edge(label="prod", color="#2196f3") >> prod_app_a
    alb >> Edge(label="prod", color="#2196f3") >> prod_app_b
    alb >> Edge(label="stage", color="#03a9f4") >> dev_app

    prod_app_a >> Edge(label="cache", color="#e91e63") >> prod_redis
    prod_app_b >> Edge(label="cache", color="#e91e63") >> prod_redis
    prod_app_a >> Edge(label="db", color="#4caf50") >> prod_db
    prod_app_b >> Edge(label="db", color="#4caf50") >> prod_db

    codedeploy >> Edge(label="deploy", color="#ff5722") >> prod_app_a
    codedeploy >> Edge(label="deploy", color="#ff5722") >> prod_app_b
    codedeploy >> Edge(label="deploy", color="#ff5722") >> dev_app
    ecr >> Edge(label="pull image", color="#ff5722") >> prod_app_a
    ecr >> Edge(label="pull image", color="#ff5722") >> prod_app_b

    prod_app_a >> Edge(label="out", color="#795548") >> prod_bastion_nat
    prod_app_b >> Edge(label="out", color="#795548") >> prod_bastion_nat
    dev_app >> Edge(label="out", color="#795548") >> dev_bastion_nat

    prod_bastion_nat >> Edge(label="ssh", color="#9c27b0") >> prod_app_a
    prod_bastion_nat >> Edge(label="ssh", color="#9c27b0") >> prod_app_b
    prod_bastion_nat >> Edge(label="ssh", color="#9c27b0") >> prod_redis
    prod_bastion_nat >> Edge(label="db", color="#9c27b0") >> prod_db
    dev_bastion_nat >> Edge(label="ssh", color="#9c27b0") >> dev_app

    cloudwatch >> Edge(label="check", color="#ff9800") >> alb
    cloudwatch >> Edge(label="metric", color="#ff9800") >> prod_app_a
    cloudwatch >> Edge(label="metric", color="#ff9800") >> prod_app_b
    cloudwatch >> Edge(label="metric", color="#ff9800") >> dev_app
