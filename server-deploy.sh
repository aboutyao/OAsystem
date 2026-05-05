#!/bin/bash
set -e

# =============================================
#  OA System 服务器端部署脚本
#  用法: SSH 登录服务器后执行此脚本
# =============================================

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
log()  { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }

echo ""
echo "=========================================="
echo "  OA System 服务器端部署"
echo "=========================================="
echo ""

# --- 1. 安装 Docker ---
log "检查 Docker..."
if command -v docker &> /dev/null; then
    log "Docker 已安装: $(docker --version)"
else
    log "安装 Docker..."
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker && systemctl start docker
    log "Docker 安装完成"
fi

if docker compose version &> /dev/null 2>&1; then
    log "Docker Compose 已安装"
else
    log "安装 Docker Compose..."
    apt-get update && apt-get install -y docker-compose-plugin
fi

# --- 2. 创建项目目录 ---
log "创建项目目录..."
mkdir -p /opt/oa-system && cd /opt/oa-system

# --- 3. 生成密码 ---
MYSQL_ROOT_PWD=$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 20)
MYSQL_APP_PWD=$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 20)
JWT_SECRET=$(openssl rand -base64 48 | tr -dc 'A-Za-z0-9!@#$%^&*' | head -c 48)
ADMIN_PWD="Admin@$(openssl rand -base64 8 | tr -dc 'A-Za-z0-9' | head -c 8)!"

cat > .env << EOF
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PWD}
MYSQL_USER=oa_app
MYSQL_PASSWORD=${MYSQL_APP_PWD}
OA_JWT_SECRET=${JWT_SECRET}
OA_INITIAL_ADMIN_PASSWORD=${ADMIN_PWD}
EOF
chmod 600 .env
log "环境变量已生成"

# --- 4. 下载代码（需要你替换为实际的代码上传方式） ---
echo ""
warn "请将项目代码上传到 /opt/oa-system/"
warn "方式1: 从本地 SCP 上传"
warn "方式2: 从 Git 仓库克隆"
warn ""
warn "上传完成后按回车继续..."
read -r

# --- 5. 启动服务 ---
log "构建并启动服务（首次约 3-5 分钟）..."
docker compose up -d --build 2>&1

# --- 6. 等待就绪 ---
log "等待服务就绪..."
for i in $(seq 1 30); do
    sleep 2
    HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:80 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "304" ]; then
        log "服务已就绪！"
        break
    fi
done

# --- 7. 输出结果 ---
echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "  访问地址:  http://$(curl -s ifconfig.me 2>/dev/null || echo '你的服务器IP')"
echo "  默认账号:  admin"
echo "  初始密码:  ${ADMIN_PWD}"
echo ""
echo "  MySQL Root: ${MYSQL_ROOT_PWD}"
echo "  MySQL App:  ${MYSQL_APP_PWD}"
echo ""
echo "  ⚠️  请保存以上密码！"
echo "=========================================="

# 保存凭证
cat > /opt/oa-system/.deploy-credentials.txt << EOF
OA System 部署凭证
==================
前端:   http://$(curl -s ifconfig.me 2>/dev/null || echo '服务器IP')
账号:   admin
密码:   ${ADMIN_PWD}
MySQL Root: ${MYSQL_ROOT_PWD}
MySQL App:  ${MYSQL_APP_PWD}
EOF
