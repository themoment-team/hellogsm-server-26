#!/bin/bash
set -euxo pipefail

dnf install -y docker
systemctl enable docker
systemctl start docker

docker run -d \
  --name hellogsm-prod-redis \
  --restart unless-stopped \
  -p 6379:6379 \
  redis:7-alpine

timedatectl set-timezone Asia/Seoul
