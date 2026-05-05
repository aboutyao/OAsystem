# 企业级 OA 系统页面与路由设计

## 1. 文档定位

本文档定义 OA 系统最终版本的页面清单、路由规划、页面职责、主要操作和响应式边界，用于指导前端工程搭建、菜单配置、权限控制和接口联调。

## 2. 页面设计原则

- 工作台优先展示待办、消息、常用入口和申请状态。
- 管理类页面以 PC 端为主。
- 移动端优先保证待办、审批、公告、消息、详情查看和简单申请。
- 每个页面只有一个主要操作。
- 列表统一支持筛选、分页、排序、重置。
- 表单统一支持校验、草稿、附件和离开提醒。
- 高危操作必须二次确认。

## 3. 路由结构

```text
/login
/dashboard
/todos
/applications
/messages
/notices

/system
/org
/permission
/workflow
/rules
/forms

/oa/leaves
/oa/expenses
/oa/seals
/oa/purchases
/contracts

/meetings
/assets
/supplies
/files
/reports
/audit
/ops
```

## 4. 公共布局

### 4.1 AppShell

组成：

- 左侧菜单
- 顶部导航
- 全局搜索
- 消息入口
- 用户菜单
- 面包屑
- 内容区

### 4.2 移动端布局

组成：

- 顶部标题栏
- 底部主导航，可选
- 卡片式待办
- 底部固定操作栏

## 5. 认证页面

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 登录页 | `/login` | 用户名、密码、验证码 |
| 修改密码 | `/account/change-password` | 修改本人密码 |
| 个人中心 | `/account/profile` | 个人信息、消息偏好 |

## 6. 工作台

### 6.1 首页

路由：

```text
/dashboard
```

内容：

- 我的待办
- 我的申请
- 抄送我的
- 最新公告
- 未读消息
- 常用入口
- 数据卡片

主要操作：

- 快速审批
- 发起申请
- 查看流程进度
- 查看公告

## 7. 待办与消息

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 我的待办 | `/todos` | 当前待处理任务 |
| 我的已办 | `/todos/done` | 已处理任务 |
| 我发起的 | `/applications` | 本人发起流程 |
| 抄送我的 | `/applications/cc` | 抄送流程 |
| 消息中心 | `/messages` | 系统消息、审批提醒 |

移动端重点优化：

- 待办卡片
- 审批详情
- 底部通过/驳回按钮
- 时间轴流程进度

## 8. 系统管理

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 系统参数 | `/system/configs` | 密码、会话、文件大小等 |
| 数据字典 | `/system/dicts` | 字典类型和字典项 |
| 编号规则 | `/system/number-rules` | 单据编号规则 |
| 导入导出任务 | `/system/import-export` | 导入预校验和导出任务 |

## 9. 组织人员

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 组织架构 | `/org/depts` | 部门树、部门详情 |
| 用户管理 | `/org/users` | 用户列表、编辑、停用、离职 |
| 岗位管理 | `/org/positions` | 岗位配置 |
| 职级管理 | `/org/ranks` | 职级配置 |
| 通讯录 | `/org/contacts` | 员工通讯录 |
| 组织变更记录 | `/org/change-logs` | 调岗、离职、部门变更 |

## 10. 权限中心

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 角色管理 | `/permission/roles` | 角色增删改 |
| 菜单管理 | `/permission/menus` | 菜单树 |
| 按钮权限 | `/permission/buttons` | 操作权限 |
| 数据权限 | `/permission/data-scopes` | 数据范围配置 |
| 字段权限 | `/permission/field-permissions` | 字段显隐、编辑、脱敏 |
| 临时授权 | `/permission/temp-auths` | 临时授权和回收 |
| 权限预览 | `/permission/preview` | 查看用户最终权限 |

## 11. 流程中心

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 流程模板 | `/workflow/templates` | 流程模板列表 |
| 流程设计 | `/workflow/templates/:id/designer` | 节点、条件、审批人 |
| 流程版本 | `/workflow/templates/:id/versions` | 发布和历史版本 |
| 流程模拟 | `/workflow/simulator` | 输入条件模拟路径 |
| 流程实例 | `/workflow/instances` | 流程运行监控 |
| 异常流程 | `/workflow/exceptions` | 找不到审批人、任务异常 |

流程详情组件：

- 流程摘要
- 流程图
- 时间轴
- 审批意见
- 当前处理人
- 催办记录

## 12. 规则中心

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 规则分组 | `/rules/groups` | 规则分类 |
| 规则列表 | `/rules` | 金额、时间、流程、消息规则 |
| 规则编辑 | `/rules/:id/edit` | 条件和动作配置 |
| 规则版本 | `/rules/:id/versions` | 发布、停用、回滚 |
| 规则模拟 | `/rules/simulator` | 输入条件查看命中结果 |
| 工作日历 | `/rules/work-calendar` | 工作日、节假日、调休日 |

## 13. 表单中心

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 表单模板 | `/forms/templates` | 表单模板 |
| 表单设计 | `/forms/templates/:id/designer` | 字段、分组、明细、附件 |
| 表单版本 | `/forms/templates/:id/versions` | 发布和历史版本 |
| 字段权限 | `/forms/field-rules` | 节点字段规则 |

## 14. OA 业务页面

### 14.1 请假

| 页面 | 路由 |
| --- | --- |
| 请假列表 | `/oa/leaves` |
| 请假新建 | `/oa/leaves/create` |
| 请假详情 | `/oa/leaves/:id` |
| 请假编辑 | `/oa/leaves/:id/edit` |
| 请假统计 | `/oa/leaves/report` |

### 14.2 报销

| 页面 | 路由 |
| --- | --- |
| 报销列表 | `/oa/expenses` |
| 报销新建 | `/oa/expenses/create` |
| 报销详情 | `/oa/expenses/:id` |
| 报销编辑 | `/oa/expenses/:id/edit` |
| 财务复核 | `/oa/expenses/finance-review` |
| 报销统计 | `/oa/expenses/report` |

### 14.3 用章

| 页面 | 路由 |
| --- | --- |
| 用章列表 | `/oa/seals` |
| 用章新建 | `/oa/seals/create` |
| 用章详情 | `/oa/seals/:id` |
| 印章台账 | `/oa/seals/ledger` |
| 外带归还 | `/oa/seals/returns` |

### 14.4 采购

| 页面 | 路由 |
| --- | --- |
| 采购列表 | `/oa/purchases` |
| 采购新建 | `/oa/purchases/create` |
| 采购详情 | `/oa/purchases/:id` |
| 到货确认 | `/oa/purchases/arrival` |
| 验收管理 | `/oa/purchases/acceptance` |

## 15. 合同页面

| 页面 | 路由 |
| --- | --- |
| 合同列表 | `/contracts` |
| 合同新建 | `/contracts/create` |
| 合同详情 | `/contracts/:id` |
| 合同编辑 | `/contracts/:id/edit` |
| 合同归档 | `/contracts/archive` |
| 到期提醒 | `/contracts/expiry-reminders` |
| 合同统计 | `/contracts/report` |

## 16. 行政资产页面

| 页面 | 路由 |
| --- | --- |
| 会议室列表 | `/meetings/rooms` |
| 会议室预约 | `/meetings/bookings` |
| 资产台账 | `/assets` |
| 资产详情 | `/assets/:id` |
| 资产流转 | `/assets/:id/records` |
| 办公用品 | `/supplies` |
| 用品出入库 | `/supplies/records` |

## 17. 文件资料库

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 文件资料库 | `/files` | 文件夹和文件列表 |
| 文件详情 | `/files/:id` | 文件版本、权限、下载日志 |
| 回收站 | `/files/recycle-bin` | 删除恢复 |

## 18. 报表统计

| 页面 | 路由 |
| --- | --- |
| 流程效率统计 | `/reports/workflow-efficiency` |
| 待办统计 | `/reports/todos` |
| 请假统计 | `/reports/leaves` |
| 报销统计 | `/reports/expenses` |
| 合同统计 | `/reports/contracts` |
| 资产统计 | `/reports/assets` |
| 用户活跃统计 | `/reports/users` |

## 19. 审计与运维

| 页面 | 路由 |
| --- | --- |
| 登录日志 | `/audit/login-logs` |
| 操作日志 | `/audit/operation-logs` |
| 权限变更日志 | `/audit/permission-logs` |
| 规则变更日志 | `/audit/rule-logs` |
| 文件下载日志 | `/audit/file-download-logs` |
| 健康检查 | `/ops/health` |
| 在线用户 | `/ops/online-users` |
| 定时任务日志 | `/ops/job-logs` |
| 异常中心 | `/ops/exceptions` |
| 备份记录 | `/ops/backups` |

## 20. 响应式边界

移动端优先支持：

- 登录
- 工作台
- 我的待办
- 审批详情
- 审批通过/驳回
- 消息中心
- 公告查看
- 简单申请
- 附件查看

移动端不重点支持：

- 复杂权限配置
- 流程设计器
- 规则设计器
- 表单设计器
- 批量导入导出
- 大型报表
- 大量表格维护

## 21. 页面验收标准

- 菜单按权限显示。
- 无权限路由不可访问。
- 列表支持分页、筛选、重置。
- 表单有校验、草稿、离开提醒。
- 高危操作二次确认。
- 空状态有明确提示。
- 错误状态有可理解文案。
- 移动端待办和审批可用。
