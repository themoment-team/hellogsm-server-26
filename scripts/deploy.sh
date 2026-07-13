#!/bin/bash

# CodeDeploy의 환경 변수를 확인합니다.
DG_NAME=$(echo $DEPLOYMENT_GROUP_NAME)

# 배포 그룹에 따라 다른 배포 스크립트를 실행합니다.
if [ "$DG_NAME" == "api-stage-hellogsm-kr" ]; then
  chmod +x /home/ec2-user/builds/scripts/stage-deploy.sh
  /home/ec2-user/builds/scripts/stage-deploy.sh

elif [ "$DG_NAME" == "api-prod-hellogsm-kr" ]; then
  chmod +x /home/ec2-user/builds/scripts/prod-deploy.sh
  /home/ec2-user/builds/scripts/prod-deploy.sh

else
  echo "알 수 없는 배포 그룹입니다: $DG_NAME"
  exit 1
fi