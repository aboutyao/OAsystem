# 企业级 OA 系统

基于 Spring Boot 3 + Vue 3 + Element Plus 的企业办公自动化系统，集成工作流引擎、权限管理、审计日志等核心能力。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.3 / Java 17 |
| ORM | MyBatis-Plus 3.5 |
| 工作流 | Flowable 7.0 |
| 安全 | Spring Security + JWT |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 |
| 前端框架 | Vue 3.5 + TypeScript |
| UI 组件 | Element Plus 2.13 |
| 状态管理 | Pinia 3.0 |
| 构建工具 | Vite 6 |
| 图表 | ECharts 6 |
| 部署 | Docker + Docker Compose |

## 功能模块

### 组织与权限
- **组织人员** — 部门树管理、员工 CRUD、岗位分配
- **权限中心** — 角色管理、菜单权限、按钮权限、数据权限
- **规则分组** — 业务规则配置与分组管理

### OA 业务
- **请假管理** — 请假申请、审批流程、工作日历联动
- **报销管理** — 费用申请、财务审核、付款状态跟踪
- **采购管理** — 采购申请、到货登记、验收确认
- **用印管理** — 用印申请、审批流程、印章台账

### 合同管理
- 合同全生命周期：创建 → 审批 → 签署 → 归档

### 工作流引擎
- 基于 Flowable 的通用审批流
- 支持并行/串行/会签/加签
- 流程可视化（节点高亮 + 时间轴）
- 流程模拟器

### 消息与通知
- **消息中心** — 站内消息、已读/未读、归档
- **通知公告** — 公告发布、分类管理

### 系统管理
- **审计日志** — 操作记录、登录日志
- **运维监控** — 健康检查、系统指标
- **报表中心** — 请假/报销/工作流效率报表
- **文件管理** — 文件上传与分类

### 个人设置
- 个人信息、修改密码

## 项目结构

```
OAsystem/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/company/oa/
│   │   ├── auth/               # 认证授权（JWT 登录/注册/权限）
│   │   ├── org/                # 组织人员（部门/员工）
│   │   ├── permission/         # 权限管理（角色/菜单/按钮）
│   │   ├── oa/                 # OA 业务（请假/报销/采购/用印）
│   │   ├── contract/           # 合同管理
│   │   ├── workflow/           # 工作流引擎（Flowable）
│   │   ├── message/            # 消息中心
│   │   ├── notice/             # 通知公告
│   │   ├── audit/              # 审计日志 + @Auditable AOP
│   │   ├── report/             # 报表中心
│   │   ├── dashboard/          # 工作台
│   │   ├── asset/              # 资产管理
│   │   ├── meeting/            # 会议管理
│   │   ├── file/               # 文件管理
│   │   ├── ops/                # 运维监控
│   │   ├── system/             # 系统配置
│   │   ├── common/             # 公共组件
│   │   │   ├── service/        # PageUtils / ConfigService / EntityMapper / TreeUtils
│   │   │   ├── entity/         # BaseEntity / VersionedEntity
│   │   │   ├── config/         # 安全配置 / CORS / 全局异常
│   │   │   └── web/            # ApiResponse / GlobalExceptionHandler
│   │   └── entity/             # 数据实体
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   └── db/migration/       # Flyway 数据库迁移
│   └── Dockerfile
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── api/                # 接口层（axios 封装）
│   │   ├── composables/        # useListPage / useOaActions
│   │   ├── components/         # 公共组件（OaApprovalTimeline / OaPageHeader）
│   │   ├── layouts/            # AppShell 布局（侧边栏 + 顶栏）
│   │   ├── router/             # 路由配置
│   │   ├── stores/             # Pinia 状态管理
│   │   ├── styles/             # 全局样式 + Design Tokens
│   │   └── views/              # 页面视图
│   │       ├── oa/             # 请假/报销/采购/用印
│   │       ├── workflow/       # 工作流
│   │       ├── permission/     # 权限管理
│   │       ├── org/            # 组织人员
│   │       └── ...             # 其他模块
│   ├── nginx.conf
│   └── Dockerfile
├── docker-compose.yml          # 一键部署编排
├── deploy.sh                   # 远程服务器部署脚本
└── server-deploy.sh            # 服务器端部署脚本
```

## 快速开始

### 本地开发

**后端**
```bash
cd backend
# 确保 MySQL 8.0 和 Redis 已启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 访问 http://localhost:8080
```

**前端**
```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

### Docker 一键部署

```bash
# 1. 配置环境变量
cat > .env << EOF
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_USER=oa_app
MYSQL_PASSWORD=your_db_password
OA_JWT_SECRET=your_jwt_secret_at_least_32_bytes
OA_INITIAL_ADMIN_PASSWORD=Admin@123!
EOF

# 2. 启动所有服务
docker compose up -d --build

# 3. 访问
# 前端: http://localhost
# API:  http://localhost:8080
# 默认账号: admin / Admin@123!
```

### 远程服务器部署

```bash
# 方式一：使用部署脚本
chmod +x deploy.sh && ./deploy.sh

# 方式二：手动部署
scp -r ./* root@your-server:/opt/oa-system/
ssh root@your-server
cd /opt/oa-system && bash server-deploy.sh
```

## API 接口

| 模块 | 路径前缀 | 说明 |
|------|---------|------|
| 认证 | `/api/auth` | 登录/注册/修改密码/登出 |
| 组织 | `/api/org` | 部门/员工管理 |
| 权限 | `/api/permissions` | 角色/菜单/按钮权限 |
| 请假 | `/api/oa/leaves` | 请假 CRUD + 提交/撤回 |
| 报销 | `/api/oa/expenses` | 报销 CRUD + 审核/付款 |
| 采购 | `/api/oa/purchases` | 采购 CRUD + 到货/验收 |
| 用印 | `/api/oa/seals` | 用印申请 + 审批 |
| 合同 | `/api/contracts` | 合同管理 |
| 工作流 | `/api/workflow` | 流程实例/任务/审批 |
| 消息 | `/api/messages` | 站内消息 |
| 通知 | `/api/notices` | 公告管理 |
| 报表 | `/api/reports` | 数据报表 |
| 审计 | `/api/audit` | 操作日志 |
| 文件 | `/api/files` | 文件上传 |

所有接口需要 JWT Bearer Token 认证（登录接口除外）。

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_URL` | 数据库连接 | `jdbc:mysql://localhost:3306/oa_system` |
| `DB_USERNAME` | 数据库用户 | `oa` |
| `DB_PASSWORD` | 数据库密码 | - |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `JWT_SECRET` | JWT 签名密钥 | - |
| `JWT_EXPIRES_IN_SECONDS` | Token 有效期 | `7200` |
| `FILE_STORAGE_PATH` | 文件存储路径 | `/opt/oa/files` |
| `SPRING_PROFILES_ACTIVE` | 环境配置 | `dev` |

## 开发规范

### 后端
- 实体继承 `VersionedEntity`，使用 `SequenceService` 生成 ID
- 分页使用 `PageUtils.clamp()`，配置读取使用 `ConfigService`
- 实体转 Map 使用 `EntityMapper.toMap()`
- 审计日志使用 `@Auditable` 注解（AOP 自动记录）
- 权限校验使用 `assertOwner()` / `assertViewAllowed()`

### 前端
- 列表页使用 `useListPage` composable
- OA 操作确认使用 `useOaActions` composable
- 数据类型使用 `api/types.ts` 中的接口定义
- 审批时间线使用 `OaApprovalTimeline` 组件

## License

Private / Internal Use
