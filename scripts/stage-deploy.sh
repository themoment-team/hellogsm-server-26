#!/bin/bash

cd /home/ec2-user/builds/

docker-compose -f docker-compose.stage.yml up -d --build hellogsm-stage-server

docker system prune -a -f
docker builder prune -a -f
