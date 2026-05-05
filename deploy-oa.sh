#!/bin/bash
cd /root/OAsystem
exec > /tmp/oa-deploy.log 2>&1
set -x

echo "=== 开始部署 OA 系统 ==="
date

# 先确保所有基础镜像拉取完成
echo "--- 拉取基础镜像 ---"
docker pull mysql:8.0
docker pull redis:7-alpine

echo "--- 开始构建和部署 ---"
docker compose up -d --build

echo "--- 部署完成，等待服务就绪 ---"
sleep 30

# 检查容器状态
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo "=== 部署日志结束 ==="
date
