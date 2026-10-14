#!/bin/bash

CONTAINER_NAME=hellogsm-prod-server
REGION=ap-northeast-2
IMAGE_URI=$(cat /home/ec2-user/builds/ECR_IMAGE_URI.txt)
ECR_REGISTRY=$(echo $IMAGE_URI | cut -d'/' -f1)

echo "> ECR 로그인" >> /home/ec2-user/deploy.log
aws ecr get-login-password --region $REGION | sudo docker login --username AWS --password-stdin $ECR_REGISTRY

echo "> 이미지 pull: $IMAGE_URI" >> /home/ec2-user/deploy.log
sudo docker pull $IMAGE_URI

echo "> 현재 실행 중인 Docker 컨테이너 ID 확인" >> /home/ec2-user/deploy.log
RUNNING_CONTAINER_ID=$(docker ps -q -f name=$CONTAINER_NAME)
EXISTING_CONTAINER_ID=$(docker ps -a -q -f name=$CONTAINER_NAME)

if [ -z $RUNNING_CONTAINER_ID ]
then
  echo "> 현재 구동중인 Docker 컨테이너가 없으므로 종료하지 않습니다." >> /home/ec2-user/deploy.log
else
  echo "> sudo docker stop $RUNNING_CONTAINER_ID"
  sudo docker stop $RUNNING_CONTAINER_ID
fi

if [ -z $EXISTING_CONTAINER_ID ]
then
  echo "> 현재 존재하는 Docker 컨테이너가 없으므로 삭제하지 않습니다." >> /home/ec2-user/deploy.log
else
  echo "> sudo docker rm $EXISTING_CONTAINER_ID"
  sudo docker rm $EXISTING_CONTAINER_ID
fi

sudo docker run -d --name $CONTAINER_NAME -p 8080:8080 $IMAGE_URI
