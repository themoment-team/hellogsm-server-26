#!/bin/bash
set -euxo pipefail

# 1. Docker 설치 및 기동
dnf install -y docker
systemctl enable docker
systemctl start docker
usermod -aG docker ec2-user

# AWS CLI v2는 Amazon Linux 2023 AMI에 기본 내장되어 있어(rpm 패키지 아님, dnf에 없음)
# 별도 설치가 필요 없다 (prod-deploy.sh의 ecr get-login-password에서 바로 사용 가능, 확인됨).

# 2. CodeDeploy agent 설치 (Amazon Linux 2023 기준)
dnf install -y ruby wget
cd /home/ec2-user
wget https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latest/install
chmod +x ./install
./install auto
systemctl enable codedeploy-agent
systemctl start codedeploy-agent

# 3. 시간대 설정 (앱/로그 타임스탬프 일치)
timedatectl set-timezone Asia/Seoul
