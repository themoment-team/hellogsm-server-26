from diagrams import Diagram, Cluster, Edge
from diagrams.aws.compute import EC2
from diagrams.aws.database import RDS
from diagrams.aws.devtools import Codedeploy
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

with Diagram("HelloGSM-2025 Cloud Architecture",
             show=False,
             direction="TB",
             graph_attr=graph_attr,
             node_attr=node_attr,
             edge_attr=edge_attr):
    user = User("사용자")

    internet_gateway = InternetGateway("Internet Gateway")
    alb = ALB("Application\nLoad Balancer")

    with Cluster("CI/CD Pipeline", graph_attr={"bgcolor": "#e3f2fd", "style": "rounded", "margin": "10"}):
        github = Github("GitHub\nRepository")
        s3 = S3("S3 Bucket")
        codedeploy = Codedeploy("CodeDeploy")

        github >> Edge(label="push", color="#2196f3") >> s3
        s3 >> Edge(label="build", color="#2196f3") >> codedeploy

    with Cluster("Monitoring", graph_attr={"bgcolor": "#fff3e0", "style": "rounded", "margin": "10"}):
        cloudwatch = Cloudwatch("CloudWatch")
        discord = User("Discord")

        cloudwatch >> Edge(label="alert", color="#ff9800") >> discord

    with Cluster("VPC", graph_attr={"bgcolor": "#f3e5f5", "style": "rounded", "margin": "15"}):
        with Cluster("Production", graph_attr={"bgcolor": "#e8f5e8", "style": "rounded", "margin": "10"}):
            with Cluster("Public", graph_attr={"bgcolor": "#c8e6c9", "style": "rounded", "margin": "8"}):
                prod_bastion_nat = EC2("Bastion + NAT\nInstance")

            with Cluster("Private", graph_attr={"bgcolor": "#ffcdd2", "style": "rounded", "margin": "8"}):
                prod_app = EC2("Spring Boot")
                prod_redis = EC2("Redis")
                prod_db = RDS("MySQL")

        with Cluster("Stage", graph_attr={"bgcolor": "#e3f2fd", "style": "rounded", "margin": "10"}):
            with Cluster("Public", graph_attr={"bgcolor": "#bbdefb", "style": "rounded", "margin": "8"}):
                dev_bastion_nat = EC2("Bastion + NAT\nInstance")

            with Cluster("Private", graph_attr={"bgcolor": "#ffcdd2", "style": "rounded", "margin": "8"}):
                dev_app = EC2("Spring Boot\n+ Redis\n+ MySQL")

    user >> Edge(label="https", color="#4caf50") >> internet_gateway
    internet_gateway >> Edge(color="#4caf50") >> alb

    alb >> Edge(label="prod", color="#2196f3") >> prod_app
    alb >> Edge(label="stage", color="#03a9f4") >> dev_app

    prod_app >> Edge(label="cache", color="#e91e63") >> prod_redis
    prod_app >> Edge(label="db", color="#4caf50") >> prod_db

    codedeploy >> Edge(label="deploy", color="#ff5722") >> prod_app
    codedeploy >> Edge(label="deploy", color="#ff5722") >> dev_app

    prod_app >> Edge(label="out", color="#795548") >> prod_bastion_nat
    dev_app >> Edge(label="out", color="#795548") >> dev_bastion_nat

    prod_bastion_nat >> Edge(label="ssh", color="#9c27b0") >> prod_app
    prod_bastion_nat >> Edge(label="ssh", color="#9c27b0") >> prod_redis
    prod_bastion_nat >> Edge(label="db", color="#9c27b0") >> prod_db
    dev_bastion_nat >> Edge(label="ssh", color="#9c27b0") >> dev_app

    cloudwatch >> Edge(label="check", color="#ff9800") >> alb
    cloudwatch >> Edge(label="metric", color="#ff9800") >> prod_app
    cloudwatch >> Edge(label="metric", color="#ff9800") >> dev_app
