# 企业级 OA 系统

基于 Spring Boot 3 + Vue 3 + Element Plus 的智能企业办公自动化系统，集成工作流引擎、AI智能分析、实时协作等核心能力。

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
| 构建工具 | Vite 5 |
| 图表 | ECharts 6 |
| 部署 | Docker + Docker Compose |

## 功能模块

### 🧠 智能化功能（48项）
- **智能审批推荐** — 基于响应速度/工作量/经验评分，动态选择最优审批人
- **异常行为检测** — 检测深夜提交、频繁修改、大额异常、重复提交
- **流程瓶颈分析** — 自动识别最慢节点、效率最低审批人
- **智能表单填充** — 根据历史数据自动填充供应商、费用科目
- **智能摘要生成** — 自动为审批单生成摘要，10秒了解核心内容
- **智能催办** — 分析最佳催办时机和方式，不是简单超时催办
- **知识图谱** — 自动关联供应商→合同→采购单→负责人
- **成本预测** — 基于历史数据预测下季度采购预算、年度人力成本
- **合同风险预警** — 自动检测即将到期、条款异常、付款风险
- **人员工作负荷** — 可视化每人待办数量、平均处理时长
- **部门健康度** — 综合指标：审批效率、人均产出、预算使用率
- **流程版本管理** — 流程变更不影响已发起的单据
- **智能日程协调** — 会议邀请自动检测冲突，推荐最佳时间
- **暗色主题** — 完整的暗色/浅色主题切换

### 组织与权限
- **组织人员** — 部门树管理、员工 CRUD、岗位分配
- **权限中心** — 角色管理、菜单权限、按钮权限、数据权限
- **权限继承** — 部门权限自动继承，减少配置工作

### OA 业务
- **请假管理** — 请假申请、审批流程、余额预测
- **报销管理** — 费用申请、财务审核、付款状态跟踪
- **采购管理** — 采购申请、到货登记、验收确认
- **用印管理** — 用印申请、审批流程、印章台账

### 合同管理
- 合同全生命周期：创建 → 审批 → 签署 → 归档
- **合同风险检测** — 到期预警、金额异常、条款缺失

### 工作流引擎
- 基于 Flowable 的通用审批流
- 支持并行/串行/会签/加签
- **智能审批路由** — 基于历史数据动态选择审批人
- **任务依赖** — 支持任务间前置依赖关系
- **操作回放** — 关键操作可回放，像视频一样查看

### 消息与通知
- **消息中心** — 站内消息、已读/未读、归档
- **通知公告** — 公告发布、分类管理
- **智能推送** — 紧急事项立即推，普通事项汇总推

### 集成与协作
- **企业微信/钉钉** — 统一登录、消息推送、审批同步
- **Webhook集成** — 支持配置Webhook，与其他系统联动
- **API开放平台** — 提供OpenAPI，支持第三方开发
- **讨论串** — 每个审批单支持讨论，沉淀决策过程
- **@提及** — 在任何地方 @同事，自动创建任务并通知

### 系统管理
- **审计日志** — 操作记录、登录日志、操作回放
- **运维监控** — 健康检查、系统指标
- **报表中心** — 成本预测、工作负荷、部门健康度
- **文件管理** — 文件上传、水印追溯、生命周期管理
- **插件系统** — 支持自定义插件扩展功能
- **模板市场** — 预置行业模板（IT、金融、制造业）

### 移动端与体验
- **离线审批** — 没网时缓存待办，联网自动同步
- **语音转文字** — 批准/驳回时语音输入意见
- **扫码功能** — 扫码入库、扫码签收
- **拍照上传** — 发票拍照自动识别金额
- **快捷手势** — 左滑驳回、右滑通过

### 安全合规
- **数据脱敏** — 按角色动态脱敏：HR看全号，其他人看部分
- **合规检查** — 自动检查报销是否超标、采购是否走流程
- **水印追溯** — 下载文件自动加水印，追溯泄露源头
- **无障碍访问** — 支持键盘导航、屏幕阅读器

## 项目结构

```
OAsystem/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/company/oa/
│   │   ├── auth/               # 认证授权（JWT 登录/注册/权限/SSO）
│   │   ├── org/                # 组织人员（部门/员工）
│   │   ├── permission/         # 权限管理（角色/菜单/按钮/继承）
│   │   ├── oa/                 # OA 业务（请假/报销/采购/用印/供应商画像）
│   │   ├── contract/           # 合同管理（风险检测）
│   │   ├── workflow/           # 工作流引擎（智能审批/版本管理/任务依赖）
│   │   ├── message/            # 消息中心
│   │   ├── notice/             # 通知公告
│   │   ├── audit/              # 审计日志（操作回放）
│   │   ├── report/             # 报表中心（成本预测/负荷分析/健康度）
│   │   ├── dashboard/          # 工作台（个性化配置）
│   │   ├── asset/              # 资产管理
│   │   ├── meeting/            # 会议管理（智能日程）
│   │   ├── file/               # 文件管理
│   │   ├── ops/                # 运维监控（Webhook）
│   │   ├── system/             # 系统配置（主题/API平台）
│   │   ├── plugin/             # 插件系统
│   │   ├── template/           # 模板市场
│   │   ├── knowledge/          # 知识图谱
│   │   ├── notification/       # 智能推送
│   │   ├── collaboration/      # 协作功能（讨论串/离线审批）
│   │   ├── integration/        # 集成（企业微信/钉钉/日历）
│   │   ├── scheduler/          # 定时任务可视化
│   │   ├── common/             # 公共组件（脱敏/合规/水印）
│   │   └── entity/             # 数据实体
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   └── db/migration/       # Flyway 数据库迁移（V1-V56）
│   └── Dockerfile
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── api/                # 接口层（40+ API函数）
│   │   ├── composables/        # 组合式函数
│   │   ├── components/         # 公共组件（54个）
│   │   ├── directives/         # 自定义指令（懒加载等）
│   │   ├── i18n/               # 国际化
│   │   ├── layouts/            # AppShell 布局
│   │   ├── router/             # 路由配置（130+路由）
│   │   ├── stores/             # Pinia 状态管理
│   │   ├── styles/             # 全局样式（支持暗色模式）
│   │   ├── utils/              # 工具函数（脱敏/校验/动画）
│   │   └── views/              # 页面视图
│   ├── nginx.conf
│   └── Dockerfile
├── docker-compose.yml          # 一键部署编排（6服务）
└── .env.example                # 环境变量模板
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
# 可选：复制并修改环境变量（不改也能跑）
cp .env.example .env

# 启动（首次构建约 5-10 分钟，Flyway 自动建表 + 灌初始数据）
docker compose up -d --build
```

启动后访问：
- 前端: http://localhost:8090
- API:  http://localhost:8080
- 默认账号: `admin` / `admin123`（请登录后立即修改密码）

远程服务器同样执行 `docker compose up -d --build` 即可，无需额外脚本。

## API 接口

### 核心模块

| 模块 | 路径前缀 | 说明 |
|------|---------|------|
| 认证 | `/api/auth` | 登录/注册/修改密码/登出 |
| 组织 | `/api/org` | 部门/员工管理 |
| 权限 | `/api/permissions` | 角色/菜单/按钮权限 |
| 请假 | `/api/oa/leaves` | 请假 CRUD + 提交/撤回 |
| 报销 | `/api/oa/expenses` | 报销 CRUD + 审核/付款 |
| 采购 | `/api/oa/purchases` | 采购 CRUD + 到货/验收 |
| 用印 | `/api/oa/seals` | 用印申请 + 审批 |
| 合同 | `/api/contracts` | 合同管理 + 风险检测 |
| 工作流 | `/api/workflow` | 流程实例/任务/审批 |
| 消息 | `/api/messages` | 站内消息 |
| 通知 | `/api/notices` | 公告管理 |
| 报表 | `/api/reports` | 数据报表 |
| 审计 | `/api/audit` | 操作日志 + 回放 |
| 文件 | `/api/files` | 文件上传 |

### 智能功能模块

| 模块 | 路径前缀 | 说明 |
|------|---------|------|
| 智能审批 | `/api/workflow/smart-approvals` | 审批人推荐 |
| 异常检测 | `/api/workflow/anomalies` | 行为异常检测 |
| 流程分析 | `/api/workflow/analytics` | 瓶颈分析/效率统计 |
| 智能表单 | `/api/smart-form` | 供应商/科目推荐 |
| 摘要生成 | `/api/workflow/summary` | 审批单摘要 |
| 任务依赖 | `/api/workflow/task-dependencies` | 前置依赖管理 |
| 供应商画像 | `/api/suppliers` | 供应商历史数据 |
| Webhook | `/api/webhooks` | Webhook管理 |
| 操作回放 | `/api/audit/replay` | 操作历史回放 |
| 知识图谱 | `/api/knowledge-graph` | 实体关联查询 |
| 合同风险 | `/api/contracts/risks` | 风险检测/报告 |
| 智能催办 | `/api/workflow/smart-reminder` | 催办时机分析 |
| 流程版本 | `/api/workflow/versions` | 版本管理/回滚 |
| 智能日程 | `/api/meetings/smart-schedule` | 冲突检测/推荐 |
| 成本预测 | `/api/reports/cost-prediction` | 季度/年度预测 |
| 工作负荷 | `/api/reports/workload` | 人员/部门负荷 |
| 部门健康度 | `/api/reports/department-health` | 综合健康评分 |

所有接口需要 JWT Bearer Token 认证（登录接口除外）。

### API 版本策略

当前所有接口统一使用 `/api/` 前缀，未带版本号。未来如需引入版本化，推荐以下方案：

- **URL 路径版本**（推荐）：新接口使用 `/api/v1/...`，旧接口保留 `/api/...` 做兼容
- **请求头版本**：通过 `Accept-Version: v1` 头部区分版本
- 当前版本：`v1`（隐式）

> Breaking changes 时引入新版本路径，旧版本保留至少一个大版本周期。

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
