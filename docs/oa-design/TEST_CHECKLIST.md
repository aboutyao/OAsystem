# OA 系统已交付模块测试 Checklist

本文档对照 [TEST_PLAN.md](TEST_PLAN.md) 的关键场景，列出当前已交付模块的覆盖情况，作为联调与验收的对照表。未交付/未启用的能力会标记为「未交付」，作为后续迭代项跟踪。

> 标注说明：
>
> - ✅ 已实现并有自动化覆盖（单元测试 / 迁移测试 / 冒烟测试）
> - ☑ 已实现，仅人工验证 / 接口可调用
> - ⏳ 部分实现，存在简化或占位
> - ⛔ 未交付，留待后续迭代

## 1. 认证与会话（TEST_PLAN §5.1）

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| 正确账号密码可登录 | ✅ | `AuthServiceTest.loginUsesSeededAdminAndReturnsJwtWithPermissions` |
| 错误密码登录失败 + 计数 | ☑ | `AuthService.login` + `recordLoginFailure` 累计 `login_fail_count` |
| 连续失败达到阈值锁定账号 | ☑ | `security.login.maxFailCount` / `lockMinutes`（来自 `sys_config`） |
| 离职/停用用户不可登录 | ☑ | `AuthService.assertLoginAllowed` |
| 未登录访问接口返回 UNAUTHORIZED | ☑ | Spring Security + `BusinessException(UNAUTHORIZED)` |
| 登录成功/失败写入审计日志 | ✅ | `AuditService.recordLoginSuccess/recordLoginFailure`，`AuthServiceTest` 验证一条 SUCCESS 落库 |

## 2. 组织人员（TEST_PLAN §5.2）

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| 部门树查询/新增/编辑 | ☑ | `OrgService.deptTree/createDept/updateDept` |
| 用户分页/新增/编辑/启停/离职 | ☑ | `OrgService.listUsers/createUser/updateUser/enable/disable/resign` |
| 用户导入错误行原因 | ⛔ | 暂未交付，后续迭代 |
| 离职后待办/资产/权限处理 | ⏳ | 离职接口可用，但联动清理需结合各业务模块 |

## 3. 权限中心（TEST_PLAN §6.1 / 6.2 / 6.3）

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| 角色 / 菜单 / 按钮 CRUD | ☑ | `PermissionController` + `PermissionService` |
| 数据范围 JSON 分配 | ☑ | `assignRoleDataScopes`（前端 JSON 输入） |
| 临时授权创建 / 撤销 | ☑ | `temp-auths` 接口及视图 |
| 用户权限预览 | ☑ | `previewUserPermission` |
| `@PreAuthorize` 拒绝越权调用 | ✅ | 各 Controller 已加注解；`PermissionServiceTest` 覆盖角色装配 |

## 4. 流程基础（TEST_PLAN §7.1 - §7.3）

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| 草稿不启动流程 | ☑ | `*Service.create` 写 DRAFT，不调用 Workflow |
| 提交后启动 Flowable 实例并写首待办 | ☑ | `*Service.submit` → `WorkflowService.startInstance` |
| 审批通过/驳回/撤回 | ☑ | `WorkflowService.handleAction` |
| 金额条件分支（>= 5000 走部门负责人） | ☑ | `RuleService` + `WorkflowService` 配置；`RuleServiceTest.simulate` 覆盖 |
| 会签/或签 | ⏳ | Flowable 模板已支持，UI 暂以基础节点展示 |
| 转交/加签/委托 | ✅ | `WorkflowService.transferTask/addSign(顺序+并行)/createDelegation` + `WorkflowServiceTest` 覆盖创建/取消委托、CC 抄送、异常列表 |
| 流程图历史节点高亮 | ✅ | `WorkflowService.diagram` 通过 Flowable HistoryService 返回 `completedActivityIds` + `history` 时间轴 |
| 抄送我的 / 异常流程 | ✅ | `wf_cc_record` + `WorkflowService.ccToMe` / `listExceptions`；前端 `/workflow/instances` `/workflow/exceptions` |

## 5. 规则中心（TEST_PLAN §8.1 / §8.4）

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| 规则列表/详情 | ✅ | `RuleService.listRules/ruleDetail` |
| 新建规则 + 新建版本（DRAFT）+ 发布 | ✅ | `RuleService.createRule/createVersion/publishVersion` |
| 模拟金额规则命中/未命中 | ✅ | `RuleServiceTest.seededRulesAndSimulateAmount` |
| 发布新版本自动作废旧版本 | ✅ | `publishVersion` 中 `update ... where status = PUBLISHED` 置为 DISABLED |
| 工作时间规则计算 | ⏳ | TIME 类型已建模，工作日/午休扣减留待与日历表联动 |

## 6. 业务模块（TEST_PLAN §9）

### 6.1 请假 / 报销 / 用章 / 采购

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| 请假时间校验 | ☑ | `LeaveService` |
| 报销明细合计校验 | ✅ | `ExpenseServiceTest` |
| 用章字段必填 | ☑ | `SealService` + DTO 校验 |
| 采购明细至少一项 + 金额阈值追加审批 | ☑ | `PurchaseService` |

### 6.2 合同 / 公告

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| 合同附件 / 高金额会签 | ☑ | `ContractService` |
| 公告发布 / 已读回执 | ☑ | `NoticeService` |

### 6.3 会议、资产、办公用品（TEST_PLAN §9.6）

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| 会议室时间冲突不可预约 | ☑ | `MeetingService.assertNoOverlap` |
| 会议预约取消（携带原因） | ☑ | `MeetingService.cancelBooking` |
| 资产领用/归还/维修/报废状态机 | ✅ | `AssetServiceTest.createReceiveReturnRepairScrapStateMachine` |
| 资产编号唯一 | ✅ | `AssetServiceTest.duplicateAssetNoIsRejected` |
| 办公用品出库不出现负库存 | ✅ | `SupplyServiceTest.stockInOutKeepsStockNonNegative` |
| 库存预警字段 | ☑ | `asset_supply.warning_quantity` + 前端高亮 |
| 出入库记录可追溯 | ✅ | `SupplyServiceTest.adjustResetsStockAndRecordsDelta` |

## 7. 消息中心（DEVELOPMENT_PLAN 步骤 4）

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| 仅展示当前账号接收的消息 | ✅ | `MessageService.list`（按 `receiver_id` 过滤） |
| 未读统计接入工作台 | ✅ | `MessageService.unreadCount` 同步至 `dashboardSummary` |
| 标记已读 / 批量已读 | ✅ | `MessageServiceTest.unreadCountAndMarkReadFlowsToRead` |
| 归档后不计入未读 | ✅ | `MessageServiceTest.archivedMessageDoesNotCountAsUnread` |
| 删除消息 | ☑ | `MessageService.delete` |

## 8. 文件资料库（DEVELOPMENT_PLAN 步骤 5）

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| 文件夹树 / 新建 / 重命名 | ☑ | `FileLibraryService.foldersTree/createFolder/updateFolder` |
| 文件登记 / 移动 / 新版本 / 删除 | ☑ | `FileLibraryService` 全套接口 |
| 下载日志 | ☑ | `file_download_log` 表 + `downloadLogs` 接口 |
| 真正的对象存储 + 分片上传 | ⛔ | 当前阶段以路径登记为主，留待运维上线 |

## 9. 审计与运维（TEST_PLAN §11 安全 / §14 备份）

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| 登录日志查询 + 按用户名/结果过滤 | ✅ | `AuditServiceTest.recordsLoginSuccessAndFailure` |
| 操作日志查询 + 按业务/操作人/结果过滤 | ✅ | `AuditServiceTest.recordsOperationLog` |
| 操作日志按业务模块统一写入 | ✅ | `AuditService.safeRecordOperation` 已接入 `Expense/Asset/Leave/Seal/Purchase/Contract/Notice/Message/File/Workflow`，`ExpenseServiceTest` & `AssetServiceTest` 断言审计落库 |
| 审计/运维接口受权限保护 | ✅ | 全部 `@PreAuthorize("hasAnyAuthority('*', 'audit:view')")` |
| 在线用户（近 30 分钟登录） | ☑ | `OpsHealthController.onlineUsers` |
| 系统健康 | ☑ | `OpsHealthController.health` |
| 定时任务/异常/备份记录视图 | ✅ | V23 `job_task_log` / `app_exception_log` / `backup_record` + `OpsService` + `OpsServiceTest` 覆盖增/查/过滤 |

## 10. 安全（TEST_PLAN §11）

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| 越权访问返回 FORBIDDEN | ☑ | `@PreAuthorize` + 全局异常 |
| 数据范围最小可见 | ☑ | 各 Service 在 `where` 中拼接 `responsible_user_id = ?` 或 `created_by = ?` |
| 普通管理员不能删除审计日志 | ☑ | 当前未提供删除接口 |
| SQL 注入 / XSS 防护 | ☑ | 全部使用参数化 SQL；前端 Element Plus 默认转义 |
| 高危操作二次确认 | ☑ | 前端 `ElMessageBox.confirm` 已统一接入（删除/报废/取消等） |

## 11. 数据库与迁移

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| Flyway V1—V23 全部迁移成功 | ✅ | `DatabaseMigrationTest.coreMigrationsCreateFoundationTablesAndSeedAdmin` |
| `perm_menu` = 22，`perm_role_menu` = 36 | ✅ | 同上 |
| `audit_login_log` / `audit_operation_log` 初始 0 条（V1 已建表） | ✅ | 同上 |
| V20 wf_cc_record / wf_delegation 建表 | ✅ | 同上断言 |
| V21 sys_number_rule 至少 4 条预置 | ✅ | 同上断言 |
| V22 form_template / form_version 至少 2 条预置 | ✅ | 同上断言 |
| V23 job_task_log / backup_record 至少 2 条 / 1 条预置 | ✅ | 同上断言 |

## 12. 已知未交付（后续迭代候选）

- 用户/部门 Excel 导入与错误行回执（`sys_import_task` 已建表，前端 `/system/import-export` 仅展示日志）。
- 工作时间规则与节假日/调休日历联动（`SystemService.WorkCalendar` 表已就绪，规则引擎层联动留待）。
- 文件资料库对象存储 + 分片上传 + 病毒扫描。
- 运维监控：真实任务调度 / 异常聚合 / 备份编排（试用版以 `OpsService` 接口 + 手动 / 脚本登记为主）。
- 性能与移动端响应式专项。
- 报表导出真正异步任务（`sys_export_task` 表已就绪，试用版仅同步小数据导出）。

## 14. 试用版 — 三角色权限矩阵端到端验证

> 试用版三角色：`SUPER_ADMIN`（超级管理员，权限 `*`）、`SYSTEM_ADMIN`（系统/流程/规则管理员）、`EMPLOYEE`（普通员工）。
> 验证维度：菜单、按钮（前端 `v-permission` / 后端 `@PreAuthorize`）、数据范围（`assertOwner` / `responsible_user_id`）、字段（敏感字段在 `getMaskedValue`）。

| 模块 | SUPER_ADMIN | SYSTEM_ADMIN | EMPLOYEE | 验证点 |
| --- | --- | --- | --- | --- |
| 工作台 | ✅ 全量统计 | ✅ 全量 + 异常流程 | ✅ 自己的待办/发起/CC/消息 | `DashboardService.summary/countException` 按角色分支；`DashboardServiceTest` 覆盖 |
| 流程模板 | 列表/发布/作废 | 列表/发布/作废 | 仅可见已发布版本 | `WorkflowController` `@PreAuthorize("hasAnyAuthority('*','workflow:manage')")` |
| 流程实例 | 全部 | 全部 | 仅自己发起或参与 | `WorkflowService.listInstances` 拼 `where starter_id = ?` |
| 流程任务（待办/已办） | 任意 | 任意 | 仅 `assignee_id = 自己` 的任务 | `WorkflowService.todoTasks` |
| 流程异常 | ✅ 全部 | ✅ 全部 | 仅自己发起 | `WorkflowService.listExceptions` 暂统一返回，前端只暴露给 `*` / `workflow:manage` |
| 委托 | ✅ 创建/取消/列表 | ✅ 同左 | ✅ 创建/取消自己的 | `WorkflowController` `@PreAuthorize("isAuthenticated()")`，业务层用 `delegator_id = currentUser` 守卫 |
| 报销 / 请假 / 用章 / 采购 / 合同 | 任意 | 任意（按权限） | 仅自己的单据 | 各 `*Service.list/detail` `assertOwner`/`assertViewAllowed` |
| 公告 | 发布/撤回 | 发布/撤回（受 `notice:publish` 控制） | 仅查看 | `NoticeController` `@PreAuthorize` |
| 消息 / 文件 | 任意（仅自己接收/上传） | 任意 | 仅自己 | `MessageService.list` 按 `receiver_id`，`FileLibraryService.detail` 校验 |
| 资产 / 办公用品 | 全量 | 全量 | 仅领用记录可见 | `AssetService.list/recordsForUser` |
| 报表 | ✅ 7 子页 | ✅ 7 子页 | ⛔（菜单不显示） | `ReportController` `@PreAuthorize("hasAnyAuthority('*','report:view')")` |
| 系统配置 / 字典 | ✅ 全部 | ✅ 全部 | ⛔ | `SystemController` `@PreAuthorize("hasAnyAuthority('*','system:config')")` |
| 编号规则 / 工作日历 / 导入导出 | ✅ | ✅ | ⛔ | `SystemController` 同上 |
| 表单模板 / 字段规则 | ✅ | ✅ | ⛔ | `FormController` `@PreAuthorize("hasAnyAuthority('*','form:design')")` |
| 审计登录 / 操作日志 | ✅ | ✅（`audit:view`） | ⛔ | `AuditController` `@PreAuthorize` |
| 运维健康 | ✅ | ✅ | ⛔（菜单不显示） | `OpsHealthController` `@PreAuthorize("hasAnyAuthority('*','audit:view')")` |
| 缓存刷新 / 模拟流程 | ✅ | ✅ | ⛔ | `OpsHealthController.refreshCache` `@PreAuthorize` |

字段级脱敏（`PERMISSION_MATRIX §4`）：

- 用户手机号 / 身份证：`org_user.phone` / `id_card` 字段在非自己且非 `org:view-all` 时，前端在用户列表显示掩码（`***`）。当前已通过 `OrgService.maskSensitiveFields` 统一处理。
- 报销 / 合同金额超阈值字段：仅 `*` 与 `finance:view` 可见明细（实现于具体业务列表 SQL）。
- 文件库附件路径：仅 `*` 与有 `file:download` 的角色返回 `storagePath`，其它角色仅返回 `fileName`（`FileLibraryService.detail` 中已截断）。

E2E 演示路径（手动跑通）：

1. SUPER_ADMIN 登录 → 工作台 → 创建租约模板（流程模板）→ 发布。
2. EMPLOYEE 登录 → 发起请假 / 报销 → 查看「我发起的」。
3. SYSTEM_ADMIN 登录 → 待办列表 → 通过 / 加签（顺序）/ 转交 → 流程实例时间轴查看。
4. SUPER_ADMIN 登录 → 委托给 SYSTEM_ADMIN（业务范围：LEAVE）→ 由 EMPLOYEE 发起请假 → 待办自动落到 SYSTEM_ADMIN（受托）。
5. SYSTEM_ADMIN 登录 → 报表 → 工作流效率 / 待办分布 / 各业务 6 子页 全部可见非空。
6. SUPER_ADMIN 登录 → 审计 → 验证上述操作均落库。

> 任何步骤出现 `403`，先检查 `@PreAuthorize` 与角色 `perm_role_menu` 装配。任何 `500` 先看 `app_exception_log`（V23）。

## 13. 验收出口对照（TEST_PLAN §15）

- P0/P1 缺陷：跟随版本测试登记。
- 越权类：全部 Controller 加 `@PreAuthorize`，按模块 grep 复查。
- 数据丢失：所有删除接口为软删 `deleted = 1` 或受限角色调用。
- 流程错乱：基于 Flowable 模板，单据状态由服务层统一控制。
- 附件泄露：下载需登录 + 角色，下载写 `file_download_log`。
- 核心路径用户验收：登录 → 工作台 → 申请 → 审批 → 完成 → 消息提醒。
- 备份恢复演练：以 `DEPLOYMENT_GUIDE.md` 为准（运维侧）。
- 性能测试：未在当前阶段交付，单独立项。
