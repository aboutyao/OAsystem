# 企业级 OA 系统

基于 Spring Boot 3 + Vue 3 + Element Plus 的企业办公自动化系统。

## 快速开始

### Docker 一键部署

```bash
cd OAsystem
docker compose up -d --build
```

启动后访问：
- 前端: http://localhost:8090
- API: http://localhost:8080
- 默认账号: `admin` / `admin123`

### 本地开发

**后端**
```bash
cd backend
# 确保 MySQL 8.0 和 Redis 已启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**前端**
```bash
cd frontend
npm install
npm run dev
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.3 / Java 17 / MyBatis-Plus 3.5 / Flowable 7.0 |
| 安全 | Spring Security + JWT + 二步验证 |
| 数据库 | MySQL 8.0 / Redis 7 |
| 前端 | Vue 3.5 + TypeScript + Element Plus 2.13 + Pinia 3.0 |
| 构建 | Vite 5 / Docker + Docker Compose |

## 功能模块

### 组织与权限
- 部门树管理、员工 CRUD、岗位分配
- 角色管理、菜单权限、按钮权限、数据权限
- 员工信息 Excel 导入/导出

### OA 业务
- 请假管理 — 申请、审批、假期余额、数据导出
- 报销管理 — 费用申请、财务审核、付款跟踪、数据导出
- 采购管理 — 采购申请、到货登记、验收确认
- 用印管理 — 用印申请、审批流程、印章台账
- 合同管理 — 合同全生命周期

### 工作流引擎
- 基于 Flowable 的通用审批流
- 支持并行/串行/会签/加签/委托
- 流程可视化（节点高亮 + 时间轴）
- 审批意见模板、批量审批、催办功能

### 消息与通知
- 站内消息、邮件通知、实时推送
- 免打扰时段、消息推送设置
- 通知公告（富文本编辑器、定时发布、已读回执、附件上传）

### 系统管理
- 审计日志（@Auditable AOP 自动记录）
- 运维监控（健康检查、异常日志、备份记录）
- 报表中心（流程效率、请假/报销/合同/资产统计）
- 文件管理（MinIO 存储）

### 工作台
- 统计卡片（待办/审批/消息/抄送/异常）
- ECharts 数据可视化图表
- 假期余额展示
- 快捷入口（根据权限过滤）
- 骨架屏加载效果

### 个人设置
- 个人信息、头像展示、修改密码、二步验证
- 操作日志查看

## 项目结构

```
OAsystem/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/company/oa/
│   │   ├── auth/               # 认证授权
│   │   ├── org/                # 组织人员
│   │   ├── permission/         # 权限管理
│   │   ├── oa/                 # OA 业务（请假/报销/采购/用印）
│   │   ├── contract/           # 合同管理
│   │   ├── workflow/           # 工作流引擎
│   │   ├── message/            # 消息中心
│   │   ├── notice/             # 通知公告
│   │   ├── audit/              # 审计日志
│   │   ├── report/             # 报表中心
│   │   ├── dashboard/          # 工作台
│   │   ├── asset/              # 资产管理
│   │   ├── meeting/            # 会议管理
│   │   ├── file/               # 文件管理
│   │   ├── ops/                # 运维监控
│   │   └── common/             # 公共组件
│   └── Dockerfile
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── api/                # 接口层
│   │   ├── composables/        # 组合式函数
│   │   ├── components/         # 公共组件
│   │   ├── layouts/            # 布局
│   │   ├── router/             # 路由
│   │   ├── stores/             # 状态管理
│   │   ├── styles/             # 样式
│   │   └── views/              # 页面视图
│   └── Dockerfile
├── docker-compose.yml          # 一键部署编排
└── docs/                       # 项目文档
```

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_URL` | 数据库连接 | `jdbc:mysql://localhost:3306/oa_system` |
| `DB_USERNAME` | 数据库用户 | `oa_app` |
| `DB_PASSWORD` | 数据库密码 | `oa_app_123` |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PASSWORD` | Redis 密码 | `redis123` |
| `JWT_SECRET` | JWT 签名密钥 | - |
| `MINIO_ENDPOINT` | MinIO 地址 | `http://localhost:9000` |

## API 接口

所有接口需要 JWT Bearer Token 认证（登录接口除外）。

| 模块 | 路径前缀 | 说明 |
|------|---------|------|
| 认证 | `/api/auth` | 登录/注册/修改密码/登出 |
| 组织 | `/api/org` | 部门/员工管理 |
| 权限 | `/api/permissions` | 角色/菜单/按钮权限 |
| 请假 | `/api/oa/leaves` | 请假 CRUD + 提交/撤回 |
| 报销 | `/api/oa/expenses` | 报销 CRUD + 审核/付款 |
| 工作流 | `/api/workflow` | 流程实例/任务/审批 |
| 消息 | `/api/messages` | 站内消息 |
| 通知 | `/api/notices` | 公告管理 |
| 报表 | `/api/reports` | 数据报表 |
| 审计 | `/api/audit` | 操作日志 |
