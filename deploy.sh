#!/bin/bash
set -e

# ============================================================
#  OA System 一键部署脚本（腾讯云）
#  用法: chmod +x deploy.sh && ./deploy.sh
# ============================================================

# -------- 请修改为你的服务器 IP --------
SERVER_IP="${SERVER_IP:-111.229.144.40}"
SSH_USER="${SSH_USER:-root}"
REMOTE_DIR="/opt/oa-system"
# -------------------------------------

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log()  { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[✗]${NC} $1"; exit 1; }

# --- 检查 IP ---
if [ "$SERVER_IP" = "你的服务器IP" ]; then
    read -p "请输入服务器 IP: " SERVER_IP
    [ -z "$SERVER_IP" ] && err "未输入 IP"
fi

SSH_CMD="ssh -o StrictHostKeyChecking=no ${SSH_USER}@${SERVER_IP}"
SCP_CMD="scp -o StrictHostKeyChecking=no -r"

echo ""
echo "=========================================="
echo "  OA System 部署到 ${SERVER_IP}"
echo "=========================================="
echo ""

# --- 1. 检测本地项目目录 ---
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ ! -f "$SCRIPT_DIR/docker-compose.yml" ]; then
    err "请在项目根目录执行此脚本"
fi
log "项目目录: $SCRIPT_DIR"

# --- 2. 生成随机密码 ---
MYSQL_ROOT_PWD=$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 20)
MYSQL_APP_PWD=$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 20)
JWT_SECRET=$(openssl rand -base64 48 | tr -dc 'A-Za-z0-9!@#$%^&*' | head -c 48)
ADMIN_PWD="Admin@$(openssl rand -base64 8 | tr -dc 'A-Za-z0-9' | head -c 8)!"

log "密码已生成"

# --- 3. 打包项目 ---
echo ""
log "打包项目文件..."
cd "$SCRIPT_DIR"
tar czf /tmp/oa-system.tar.gz \
    --exclude='node_modules' \
    --exclude='target' \
    --exclude='.git' \
    --exclude='oa-system.tar.gz' \
    -C "$SCRIPT_DIR" .

log "打包完成: $(du -h /tmp/oa-system.tar.gz | cut -f1)"

# --- 4. 检查远程连接 ---
echo ""
log "测试服务器连接..."
$SSH_CMD "echo ok" > /dev/null 2>&1 || err "无法连接到 ${SERVER_IP}，请检查 SSH 密钥配置"
log "连接成功"

# --- 5. 安装 Docker（如未安装）---
echo ""
log "检查 Docker 环境..."
$SSH_CMD "
    if command -v docker &> /dev/null; then
        echo 'Docker 已安装: ' \$(docker --version)
    else
        echo '安装 Docker...'
        curl -fsSL https://get.docker.com | sh
        systemctl enable docker
        systemctl start docker
        echo 'Docker 安装完成'
    fi

    if command -v docker compose &> /dev/null || docker compose version &> /dev/null 2>&1; then
        echo 'Docker Compose 已安装'
    else
        echo '安装 Docker Compose 插件...'
        apt-get update && apt-get install -y docker-compose-plugin
    fi
"

# --- 6. 上传代码 ---
echo ""
log "上传项目到服务器..."
$SSH_CMD "mkdir -p $REMOTE_DIR"
$SCP_CMD /tmp/oa-system.tar.gz ${SSH_USER}@${SERVER_IP}:${REMOTE_DIR}/
rm -f /tmp/oa-system.tar.gz

# --- 7. 解压 + 创建 .env ---
echo ""
log "配置环境变量..."
$SSH_CMD "
    cd $REMOTE_DIR
    tar xzf oa-system.tar.gz
    rm -f oa-system.tar.gz

    cat > .env << ENVEOF
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PWD}
MYSQL_USER=oa_app
MYSQL_PASSWORD=${MYSQL_APP_PWD}
OA_JWT_SECRET=${JWT_SECRET}
OA_INITIAL_ADMIN_PASSWORD=${ADMIN_PWD}
ENVEOF

    chmod 600 .env
    echo '环境变量已写入'
"

# --- 8. 启动服务 ---
echo ""
log "构建并启动服务（首次约 3-5 分钟）..."
$SSH_CMD "
    cd $REMOTE_DIR
    docker compose up -d --build 2>&1
"

# --- 9. 等待健康检查 ---
echo ""
log "等待服务就绪..."
for i in $(seq 1 30); do
    sleep 2
    HTTP_CODE=$($SSH_CMD "curl -s -o /dev/null -w '%{http_code}' http://localhost:80" 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "304" ]; then
        log "服务已就绪！"
        break
    fi
    if [ "$i" -eq 30 ]; then
        warn "服务仍在启动中，请稍后手动检查"
    fi
done

# --- 10. 输出结果 ---
echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "  访问地址:  http://${SERVER_IP}"
echo "  默认账号:  admin"
echo "  初始密码:  ${ADMIN_PWD}"
echo ""
echo "  MySQL Root 密码:  ${MYSQL_ROOT_PWD}"
echo "  MySQL App  密码:  ${MYSQL_APP_PWD}"
echo ""
echo "  ⚠️  请保存以上密码，建议登录后立即修改管理员密码"
echo "=========================================="
echo ""

# 保存密码到本地文件
cat > "$SCRIPT_DIR/.deploy-credentials.txt" << EOF
OA System 部署凭证
==================
服务器: ${SERVER_IP}
前端:   http://${SERVER_IP}
账号:   admin
密码:   ${ADMIN_PWD}

MySQL Root: ${MYSQL_ROOT_PWD}
MySQL App:  ${MYSQL_APP_PWD}
JWT Secret: ${JWT_SECRET}
EOF
log "凭证已保存到 .deploy-credentials.txt"
warn "请妥善保管 .deploy-credentials.txt，部署完成后建议删除"
