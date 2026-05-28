# 技术架构文档

## 系统架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   前端      │────▶│   后端      │────▶│   数据库    │
│  Vue 3      │     │ Spring Boot │     │  MySQL 8.0  │
│  Element+   │     │  Flowable   │     │  Redis 7    │
└─────────────┘     └─────────────┘     └─────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │  文件存储   │
                    │   MinIO     │
                    └─────────────┘
```

## 数据库设计

### 核心表

| 模块 | 表名 | 说明 |
|------|------|------|
| 组织 | `sys_user` | 用户表 |
| 组织 | `sys_dept` | 部门表 |
| 权限 | `perm_role` | 角色表 |
| 权限 | `perm_menu` | 菜单表 |
| 工作流 | `wf_process_template` | 流程模板 |
| 工作流 | `wf_process_instance` | 流程实例 |
| 工作流 | `wf_task` | 审批任务 |
| OA | `oa_leave` | 请假记录 |
| OA | `oa_expense` | 报销记录 |
| 合同 | `contract_info` | 合同信息 |
| 消息 | `msg_message` | 站内消息 |
| 通知 | `oa_notice` | 通知公告 |
| 审计 | `audit_operation_log` | 操作日志 |
| 审计 | `audit_login_log` | 登录日志 |

### 实体继承

所有业务实体继承 `VersionedEntity`，包含：
- `id` — 主键（Snowflake ID）
- `createdAt` — 创建时间
- `updatedAt` — 更新时间
- `version` — 乐观锁版本号

## 权限模型

### 权限层级

```
菜单权限 ──▶ 按钮权限 ──▶ 数据权限 ──▶ 字段权限
```

### 权限检查

- `@PreAuthorize("hasAnyAuthority('*', 'org:view')")` — 方法级权限
- `OaPermissionUtils.assertOwner()` — 数据归属校验
- `OaPermissionUtils.assertViewAllowed()` — 数据查看权限

## 工作流引擎

### Flowable 集成

- 流程模板 → 流程版本 → 流程实例 → 用户任务
- 支持审批、驳回、撤回、加签、转交、委托
- 流程图节点高亮 + 时间轴展示

### 审批流程

```
发起申请 → 直属上级审批 → [条件分支] → HR备案 → 完成
                │
                └── 金额>10000 → 总经理审批
```

## 审计日志

### @Auditable AOP

```java
@Auditable(action = "LEAVE_CREATE", entityType = "LEAVE", description = "创建请假申请")
public Map<String, Object> create(...) { ... }
```

### 日志保留

- 登录日志：90 天
- 操作日志：90 天
- 异常日志：30 天

## 安全设计

- JWT Token 认证（2小时有效期）
- 二步验证（TOTP）
- 密码过期策略
- 登录失败锁定
- CORS 配置
- 幂等性控制（X-Idempotency-Key）
- 敏感字段脱敏
- 操作审计（@Auditable AOP 自动记录）

## 前端特性

- 骨架屏加载效果
- 路由切换进度条
- 页面过渡动画（fade + translateY）
- ECharts 数据可视化
- 深色模式支持
- 响应式布局（移动端适配）
- 全局搜索（人员/请假/文件）

## 部署架构

### Docker Compose

| 服务 | 端口 | 说明 |
|------|------|------|
| frontend | 8090 | Nginx + Vue SPA |
| backend | 8080 | Spring Boot |
| mysql | 3306 | MySQL 8.0 |
| redis | 6379 | Redis 7 |
| minio | 9000/9001 | 文件存储 |
| mailhog | 1025/8025 | 邮件测试 |

## 开发规范

### 后端

- Controller 只做参数接收和返回
- 业务规则放在 Service
- 所有列表接口必须分页
- 所有写操作必须记录操作人和时间
- 关键状态变更必须事务处理

### 前端

- 页面按业务域拆分
- API 调用统一封装
- 权限指令控制菜单和按钮显示
- 表格支持分页、筛选、重置
- 错误、空状态、加载状态统一
