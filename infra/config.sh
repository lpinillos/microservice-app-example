#!/bin/bash
apt-get update
apt-get install -y docker.io docker-compose git

usermod -aG docker adminuser

cd /home/adminuser
git clone https://github.com/lpinillos/microservice-app-example.git
cd microservice-app-example

docker-compose -f docker-compose.dev.yml up -d --build
