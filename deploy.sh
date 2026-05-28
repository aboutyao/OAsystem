#!/bin/bash
# OA 系统部署脚本
# 用法:
#   ./deploy.sh              # 首次部署或更新应用
#   ./deploy.sh backend      # 只更新后端
#   ./deploy.sh frontend     # 只更新前端
#   ./deploy.sh infra        # 更新基础设施（MySQL/Redis/MinIO）
#   ./deploy.sh full         # 完全重置（删除所有数据）

set -e

ACTION="${1:-update}"

echo "=============================="
echo "  OA 系统部署"
echo "  模式: $ACTION"
echo "=============================="
echo ""

# 检查 Docker
if ! docker info > /dev/null 2>&1; then
    echo "错误: Docker 未运行"
    exit 1
fi

# 检查是否存在 docker-compose.yml
if [ ! -f docker-compose.yml ]; then
    echo "错误: 未找到 docker-compose.yml"
    exit 1
fi

case $ACTION in

# ============================================================
# 完全重置：删除所有容器、数据卷、镜像，从零开始
# ============================================================
full)
    echo "[1/4] 停止并删除所有容器和数据卷..."
    docker compose down -v --rmi all 2>/dev/null || true

    echo "[2/4] 清理残留..."
    docker compose down --remove-orphans 2>/dev/null || true

    echo "[3/4] 构建镜像并启动所有服务..."
    docker compose up -d --build

    echo "[4/4] 等待所有服务就绪..."
    sleep 15
    ;;

# ============================================================
# 首次部署：构建并启动所有服务（保留已有数据）
# ============================================================
init)
    echo "[1/2] 构建镜像并启动所有服务..."
    docker compose up -d --build

    echo "[2/2] 等待所有服务就绪..."
    sleep 15
    ;;

# ============================================================
# 更新应用：只重建后端和前端，保留 MySQL/Redis/MinIO 数据
# ============================================================
update)
    echo "[1/3] 停止应用服务..."
    docker compose stop backend frontend

    echo "[2/3] 删除旧容器并重建..."
    docker compose rm -f backend frontend
    docker compose up -d --build backend frontend

    echo "[3/3] 等待后端启动..."
    sleep 10
    ;;

# ============================================================
# 只更新后端
# ============================================================
backend)
    echo "[1/3] 停止后端..."
    docker compose stop backend

    echo "[2/3] 删除旧容器并重建..."
    docker compose rm -f backend
    docker compose up -d --build backend

    echo "[3/3] 等待后端启动..."
    sleep 10
    ;;

# ============================================================
# 只更新前端
# ============================================================
frontend)
    echo "[1/3] 停止前端..."
    docker compose stop frontend

    echo "[2/3] 删除旧容器并重建..."
    docker compose rm -f frontend
    docker compose up -d --build frontend

    echo "[3/3] 等待前端启动..."
    sleep 5
    ;;

# ============================================================
# 更新基础设施：MySQL/Redis/MinIO 配置变更时使用
# 注意：会删除数据卷，数据会丢失
# ============================================================
infra)
    echo "警告: 此操作会删除 MySQL/Redis/MinIO 的所有数据!"
    read -p "确认执行? (y/N): " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        echo "已取消"
        exit 0
    fi

    echo "[1/3] 停止所有服务..."
    docker compose down

    echo "[2/3] 删除数据卷..."
    docker volume rm oasystem_mysql_data oasystem_redis_data oasystem_minio_data 2>/dev/null || true

    echo "[3/3] 重新启动所有服务..."
    docker compose up -d
    sleep 15
    ;;

# ============================================================
# 未知命令
# ============================================================
*)
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  (无参数)  首次部署或更新应用（默认）"
    echo "  backend   只更新后端"
    echo "  frontend  只更新前端"
    echo "  infra     更新基础设施（删除数据）"
    echo "  full      完全重置（删除所有数据）"
    exit 1
    ;;
esac

# ============================================================
# 健康检查
# ============================================================
echo ""
echo "=============================="
echo "  健康检查"
echo "=============================="

# 检查所有容器状态
echo ""
echo "容器状态:"
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

# 后端健康检查
echo ""
echo -n "后端 API: "
if curl -s http://localhost:8080/api/ops/health > /dev/null 2>&1; then
    echo "正常"
else
    echo "等待中..."
    # 再等 30 秒
    for i in {1..30}; do
        if curl -s http://localhost:8080/api/ops/health > /dev/null 2>&1; then
            echo "后端 API: 正常"
            break
        fi
        sleep 1
    done
fi

# 前端检查
echo -n "前端页面: "
HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8090/ 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
    echo "正常"
else
    echo "等待中..."
fi

echo ""
echo "=============================="
echo "  部署完成"
echo "=============================="
echo "前端: http://localhost:8090"
echo "后端: http://localhost:8080"
echo "默认账号: admin / admin123"
echo ""
