#!/bin/bash
apt-get update
apt-get install -y docker.io docker-compose git

usermod -aG docker azureuser

cd /home/azureuser
git clone https://github.com/lpinillos/microservice-app-example.git
cd microservice-app-example

docker-compose -f docker-compose.yml up -d --build
