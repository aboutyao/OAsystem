# 企业级 OA 系统部署与上线指南

## 1. 文档定位

本文档定义 OA 系统最终版本的部署架构、环境要求、配置项、备份恢复、上线步骤和运维检查清单。

目标：

- 保证系统可部署。
- 保证数据可备份、可恢复。
- 保证上线过程可回退。
- 保证运维人员能定位问题。

## 2. 环境规划

建议至少准备：

| 环境 | 用途 |
| --- | --- |
| 开发环境 | 开发自测 |
| 测试环境 | QA 功能、权限、流程、规则测试 |
| 预发布环境 | 接近生产的上线演练 |
| 生产环境 | 正式使用 |

## 3. 推荐部署拓扑

### 3.1 初始生产部署

```text
Nginx
  ├── 前端静态资源
  └── 反向代理 /api

后端 Spring Boot 服务
Flowable 引擎（内嵌在后端服务中）
MySQL 8.x
Redis
文件存储目录
定时备份任务
```

### 3.2 后续扩展部署

```text
Nginx 负载均衡
  ↓
后端服务 1 / 后端服务 2
  ↓
MySQL 主从或高可用
Redis
独立文件服务或对象存储
集中日志
监控告警
```

## 4. 服务器建议

1000+ 用户初始建议：

| 组件 | 建议 |
| --- | --- |
| 应用服务器 | 4 核 8G 起 |
| 数据库服务器 | 4 核 16G 起 |
| Redis | 2 核 4G 起 |
| 文件存储 | 根据附件规模预留，建议 500G 起 |
| 带宽 | 内网优先，外网按访问规模评估 |

如果单机部署：

- 建议 8 核 32G 起。
- 数据库、文件、日志磁盘需分区或独立挂载。

## 5. 基础软件

建议版本：

```text
JDK 17+
Node.js 20+
Nginx 1.24+
MySQL 8.x
Redis 7.x
```

## 6. 目录规划

Linux 示例：

```text
/opt/oa/
  backend/
  frontend/
  config/
  logs/
  files/
  backup/
  scripts/
```

说明：

- `config/`：生产配置。
- `logs/`：应用日志。
- `files/`：附件存储。
- `backup/`：数据库和附件备份。
- `scripts/`：启动、停止、备份脚本。

## 7. 关键配置项

后端配置：

```text
server.port
spring.datasource.url
spring.datasource.username
spring.datasource.password
spring.redis.host
spring.redis.port
file.storage.path
jwt.secret 或 session 配置
logging.file.path
flowable.database-schema-update
```

前端配置：

```text
VITE_API_BASE_URL
VITE_APP_TITLE
```

安全要求：

- 生产密码不得提交到代码仓库。
- JWT 密钥不得使用默认值。
- 数据库账号最小权限。
- Redis 不暴露公网。
- 附件目录不允许 Nginx 直接公开访问。

## 8. 数据库初始化

步骤：

1. 创建数据库。
2. 设置字符集。
3. 执行 Flowable 建表脚本或由应用初始化。
4. 执行 OA 业务表迁移脚本。
5. 执行初始化数据脚本。
6. 创建超级管理员。
7. 初始化菜单、按钮、角色、字典、系统参数。

推荐字符集：

```sql
utf8mb4
```

## 9. 前端部署

构建：

```text
npm install
npm run build
```

部署：

```text
将 dist/ 上传到 Nginx 静态目录
```

Nginx 示例：

```nginx
server {
    listen 80;
    server_name oa.example.com;

    root /opt/oa/frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

生产建议启用 HTTPS。

## 10. 后端部署

构建：

```text
mvn clean package
```

启动：

```text
java -jar oa-backend.jar --spring.profiles.active=prod
```

建议使用：

- systemd
- Docker Compose
- Kubernetes，可后期

systemd 示例：

```ini
[Unit]
Description=OA Backend
After=network.target

[Service]
WorkingDirectory=/opt/oa/backend
ExecStart=/usr/bin/java -jar /opt/oa/backend/oa-backend.jar --spring.profiles.active=prod
Restart=always
User=oa

[Install]
WantedBy=multi-user.target
```

## 11. 备份策略

### 11.1 数据库备份

建议：

- 每日全量备份。
- 关键业务可增加 binlog 或增量备份。
- 备份保留 30-90 天。
- 备份文件压缩。
- 备份结果写日志。

示例：

```text
backup/db/oa_20260428.sql.gz
```

### 11.2 附件备份

建议：

- 每日增量备份。
- 每周全量备份。
- 保留历史版本。
- 附件备份与数据库备份时间点尽量一致。

### 11.3 恢复演练

至少每季度演练一次：

- 恢复数据库。
- 恢复附件。
- 验证登录。
- 验证流程详情。
- 验证附件下载。
- 验证权限。

## 12. 日志与监控

必须记录：

- 应用启动日志。
- 接口访问日志。
- 错误日志。
- 登录日志。
- 操作日志。
- 审计日志。
- Flowable 流程异常。
- 定时任务日志。
- 备份日志。

监控项：

- 服务存活。
- CPU、内存、磁盘。
- 接口错误率。
- 接口响应时间。
- 数据库连接池。
- Redis 状态。
- 附件目录容量。
- 定时任务执行结果。
- 备份任务执行结果。

## 13. 上线前检查

### 13.1 功能检查

- 登录可用。
- 用户、部门、角色已初始化。
- 权限矩阵已配置。
- 流程模板已发布。
- 规则版本已发布。
- 字典和系统参数已配置。
- 文件上传下载可用。
- 待办消息可用。
- 核心业务模块可用。

### 13.2 安全检查

- 默认密码已修改。
- 超级管理员数量受控。
- 生产密钥已替换。
- 数据库不暴露公网。
- Redis 不暴露公网。
- 附件不可直链访问。
- HTTPS 已启用。
- 高危操作审计可用。

### 13.3 数据检查

- 组织架构已导入。
- 用户已导入。
- 岗位职级已配置。
- 字典数据已配置。
- 流程规则已配置。
- 报表口径已确认。

### 13.4 备份检查

- 数据库备份成功。
- 附件备份成功。
- 备份文件可读取。
- 恢复演练通过。

## 14. 上线步骤

建议步骤：

```text
冻结需求和代码
  ↓
完成预发布验收
  ↓
备份生产数据库和附件
  ↓
部署后端服务
  ↓
部署前端静态资源
  ↓
执行数据库迁移
  ↓
执行初始化数据脚本
  ↓
检查健康接口
  ↓
管理员登录验证
  ↓
业务冒烟测试
  ↓
开放用户访问
  ↓
观察日志和监控
```

## 15. 冒烟测试

上线后立即验证：

- 登录。
- 首页加载。
- 待办加载。
- 发起请假。
- 发起报销。
- 审批通过。
- 审批驳回。
- 查看流程进度。
- 上传附件。
- 下载附件。
- 发布公告。
- 查看报表。
- 查看日志。

## 16. 回退方案

必须准备：

- 上一版本后端包。
- 上一版本前端静态资源。
- 数据库备份。
- 附件备份。
- 配置文件备份。

回退原则：

- 如果只影响前端，优先回退前端。
- 如果后端启动失败，回退后端包。
- 如果数据库迁移已执行，需要按迁移脚本准备回滚方案。
- 如果产生新业务数据，回退前必须评估数据兼容性。

## 17. 运维日常

每日检查：

- 服务状态。
- 错误日志。
- 磁盘空间。
- 备份结果。
- 定时任务结果。

每周检查：

- 慢 SQL。
- 附件增长。
- 用户异常登录。
- 审批异常流程。

每月检查：

- 权限高危变更。
- 审计日志容量。
- 报表性能。
- 备份可用性。

## 18. 故障处理

### 18.1 登录失败

检查：

- 用户状态。
- 密码错误次数。
- 账号锁定时间。
- Redis 状态。
- 后端日志。

### 18.2 流程不流转

检查：

- Flowable 流程实例。
- 当前任务。
- 流程变量。
- 条件表达式。
- 审批人规则。
- OA `wf_*` 表。

### 18.3 附件不可下载

检查：

- 文件是否存在。
- 存储路径是否正确。
- 用户附件权限。
- 业务数据权限。
- 下载日志。

### 18.4 报表慢

检查：

- 查询条件。
- 索引。
- 数据量。
- 慢 SQL。
- 是否需要异步统计。

## 19. 上线验收

上线成功标准：

- 核心功能冒烟通过。
- 无 P0/P1 故障。
- 备份任务成功。
- 监控正常。
- 日志正常写入。
- 业务用户可正常登录和审批。
- 管理员可查看异常和审计。

## 20. 内部交付 / 试用版部署（Trial）

试用版用于甲方/PM 在干净环境中按 [TRIAL_GUIDE.md](TRIAL_GUIDE.md) 的演示脚本验证功能。配置以"最低复杂度可演示"为目标，正式交付版改用 §3 的拓扑。

### 20.1 最小依赖

| 依赖 | 版本 | 部署方式 |
| --- | --- | --- |
| JDK | 17 | 解压即用 |
| Maven | 3.9+ | 仅构建期需要 |
| Node.js | 20 LTS | 仅构建期需要（npm 9+） |
| MySQL | 8.0.x | 单实例（推荐 docker compose 或本机 MySQL） |
| Redis | 7.x（可选） | 用于 token 黑名单与限流，缺省可关闭 |

### 20.2 数据库初始化

1. 创建数据库：

   ```sql
   CREATE DATABASE oa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'oa_app'@'%' IDENTIFIED BY '<强随机密码>';
   GRANT ALL PRIVILEGES ON oa_system.* TO 'oa_app'@'%';
   FLUSH PRIVILEGES;
   ```

2. 启动后端时由 Flyway 自动跑 `V1—V23`：管理员账号 `admin` 由 `V2__seed_core_data.sql` 写入，初始密码见 `application.yml` 的 `oa.security.initialPassword`，**首次登录后必须改密**。

3. 试用阶段的种子数据（流程模板、表单模板、编号规则、运维任务样例等）已在 V20—V23 自动写入，无需额外手工脚本。

### 20.3 后端环境变量（试用版）

可通过 `application-trial.yml` 或 `--spring.config.additional-location` 注入：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${OA_DB_HOST}:3306/oa_system?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${OA_DB_USER}
    password: ${OA_DB_PASSWORD}
  flyway:
    enabled: true
    locations: classpath:db/migration
  redis:
    host: ${OA_REDIS_HOST:127.0.0.1}
    port: ${OA_REDIS_PORT:6379}

oa:
  security:
    jwtSecret: ${OA_JWT_SECRET}        # 必须为 32 字节以上的强随机字符串
    accessTokenExpireSeconds: 7200
    initialPassword: ${OA_INITIAL_ADMIN_PASSWORD}
  storage:
    type: LOCAL
    rootDir: ${OA_FILE_ROOT:/var/oa/files}
  trial:
    enabled: true                       # 试用版开关，例如允许 quick-actions 显示模拟流程入口
```

启动命令示例：

```bash
java -jar oa-system-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=trial \
  --spring.config.additional-location=file:./conf/application-trial.yml
```

### 20.4 前端构建与部署

```bash
cd frontend
npm install
VITE_API_BASE=/api npm run build
```

将 `frontend/dist/` 部署到 Nginx：

```nginx
server {
    listen 80;
    server_name oa-trial.example.com;

    root /var/www/oa-trial;
    index index.html;
    location / { try_files $uri $uri/ /index.html; }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 20.5 试用版冒烟流程

1. `mvn test`：H2 单元测试全绿（约 50+ 用例）。
2. `mvn verify`（需 Docker）：MySQL Testcontainers IT (`MysqlMigrationIT`) 跑通 V1—V23 全迁移。
3. 启动后端 + Nginx，浏览器访问 `http://<host>/login`，按 [TRIAL_GUIDE.md](TRIAL_GUIDE.md) §2 演示脚本逐步验证。
4. 检查 `/api/ops/health` 返回 `UP`、`/api/audit/operation-logs` 在演示期间持续有新记录。

### 20.6 试用版与正式版差异（必须告知甲方）

| 维度 | 试用版 | 正式交付版 |
| --- | --- | --- |
| 性能压测 | 未做 | 1000-5000 并发用户 / 10w-100w 单据 |
| UAT 全量 | 仅核心路径 | 业务部门全量验收并签字 |
| 备份恢复演练 | 仅文档 + V23 备份记录登记接口 | 真实演练（数据库 + 附件 + 流程） |
| 移动端 | 桌面分辨率优先 | 增量进行响应式专项 |
| 培训 | 演示脚本 + Trial Guide | 培训手册 + 操作视频 |
| 报表导出 | 同步小数据 | 走 `sys_export_task` 异步调度 |
| 高危操作二次确认 | 已实现 | 同左 |

### 20.7 试用环境清理

试用结束后，可以快速重置：

```sql
DROP DATABASE oa_system;
CREATE DATABASE oa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

下次启动后端时，Flyway 会自动重新跑全部迁移。
