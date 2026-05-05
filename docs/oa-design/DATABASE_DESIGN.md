# 企业级 OA 系统数据库设计

## 1. 文档定位

本文档基于 `OA_SYSTEM_DESIGN.md` 和 `DEVELOPMENT_PLAN.md` 编写，用于定义企业级 OA 系统最终版本的数据库结构草案。

本文档重点说明：

- 表分组与职责边界
- 核心字段规范
- 关键表结构
- 关联关系
- 状态字段
- 索引建议
- Flowable 与 OA 自建表的边界
- 历史快照、权限过滤、审计追踪设计

本文档是开发级数据库设计草案，后续可进一步转换为 Flyway / Liquibase / SQL 迁移脚本。

## 2. 数据库选型建议

推荐：

```text
MySQL 8.x
```

理由：

- 企业 OA 场景以关系型事务、查询、报表为主。
- MySQL 生态成熟，团队上手成本低。
- Flowable 对 MySQL 支持成熟。
- 1000+ 用户规模完全足够。

可选：

```text
PostgreSQL
```

如果团队更熟悉 PostgreSQL，或后期对复杂查询、JSON、全文检索有更高要求，也可以采用 PostgreSQL。

本文档字段类型以 MySQL 8.x 为主描述。

## 3. 命名规范

### 3.1 表命名

| 前缀 | 说明 |
| --- | --- |
| `sys_` | 系统基础表 |
| `org_` | 组织人员表 |
| `perm_` | 权限表 |
| `wf_` | OA 流程业务表 |
| `rule_` | 规则配置表 |
| `form_` | 表单配置表 |
| `msg_` | 消息待办表 |
| `file_` | 文件附件表 |
| `oa_` | OA 通用业务表 |
| `contract_` | 合同业务表 |
| `asset_` | 资产办公用品表 |
| `meeting_` | 会议室表 |
| `report_` | 报表与导出任务表 |
| `audit_` | 审计日志表 |
| `job_` | 定时任务与运维表 |

### 3.2 字段命名

- 使用小写下划线。
- 主键统一使用 `id`。
- 外键字段使用 `{entity}_id`，例如 `user_id`、`dept_id`。
- 状态字段统一使用 `status`。
- 逻辑删除字段统一使用 `deleted`。
- 乐观锁字段统一使用 `version`。

## 4. 通用字段规范

核心业务表建议包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `created_by` | bigint | 创建人 ID |
| `created_name_snapshot` | varchar(64) | 创建人姓名快照 |
| `created_dept_id` | bigint | 创建人部门 ID |
| `created_dept_name_snapshot` | varchar(128) | 创建人部门名称快照 |
| `created_at` | datetime | 创建时间 |
| `updated_by` | bigint | 更新人 ID |
| `updated_at` | datetime | 更新时间 |
| `deleted` | tinyint | 逻辑删除：0 否，1 是 |
| `version` | int | 乐观锁版本 |
| `status` | varchar(32) | 状态 |
| `remark` | varchar(500) | 备注 |

日志表和关系表可按需要简化。

## 5. 基础状态枚举

### 5.1 通用启停状态

```text
ENABLED     启用
DISABLED    停用
```

### 5.2 业务单据状态

```text
DRAFT       草稿
SUBMITTED   已提交
APPROVING   审批中
APPROVED    已通过
REJECTED    已驳回
WITHDRAWN   已撤回
CANCELLED   已作废
ARCHIVED    已归档
TERMINATED  已终止
```

### 5.3 审批任务状态

```text
PENDING      待处理
APPROVED     已同意
REJECTED     已驳回
TRANSFERRED  已转交
ADDED        已加签
SKIPPED      已跳过
CANCELLED    已取消
TIMEOUT      已超时
```

### 5.4 员工状态

```text
PROBATION    试用
ACTIVE       正式
DISABLED     停用
RESIGNED     离职
REHIRED      返聘
```

## 6. Flowable 表边界

系统集成 Flowable 后，数据库会包含 Flowable 自带的 `ACT_*` 表，例如：

```text
ACT_RE_*     流程定义相关
ACT_RU_*     运行时流程实例、任务、变量
ACT_HI_*     历史流程实例、历史任务、历史变量
ACT_ID_*     身份相关，可按集成方式决定是否使用
ACT_GE_*     通用数据
```

设计边界：

- Flowable 表负责流程运行时和流程历史。
- OA 系统不直接把 `ACT_*` 表暴露给前端。
- 业务模块不直接查询 `ACT_*` 表。
- 业务查询统一走 OA 自建 `wf_*` 表和流程服务层。
- 业务单据表保存 `process_instance_id`，用于关联 Flowable 流程实例。
- OA 自建 `wf_*` 表负责业务展示、权限过滤、审批快照、提醒、审计。

## 7. 系统基础表

### 7.1 sys_config 系统参数表

用于保存系统级配置，例如密码策略、文件大小、会话超时等。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `config_key` | varchar(128) | 配置键，唯一 |
| `config_value` | text | 配置值 |
| `config_type` | varchar(32) | 类型：STRING、NUMBER、BOOLEAN、JSON |
| `config_group` | varchar(64) | 配置分组 |
| `description` | varchar(255) | 描述 |
| `editable` | tinyint | 是否可后台编辑 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

索引：

- 唯一索引：`config_key`
- 普通索引：`config_group`

### 7.2 sys_dict_type 字典类型表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `dict_code` | varchar(64) | 字典编码，唯一 |
| `dict_name` | varchar(128) | 字典名称 |
| `status` | varchar(32) | 状态 |
| `remark` | varchar(500) | 备注 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

索引：

- 唯一索引：`dict_code`

### 7.3 sys_dict_item 字典项表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `dict_type_id` | bigint | 字典类型 ID |
| `item_label` | varchar(128) | 显示文本 |
| `item_value` | varchar(128) | 字典值 |
| `sort_order` | int | 排序 |
| `status` | varchar(32) | 状态 |
| `remark` | varchar(500) | 备注 |

索引：

- 普通索引：`dict_type_id`
- 唯一索引：`dict_type_id, item_value`

## 8. 组织人员表

### 8.1 org_dept 部门表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `parent_id` | bigint | 上级部门 ID |
| `dept_code` | varchar(64) | 部门编码 |
| `dept_name` | varchar(128) | 部门名称 |
| `dept_path` | varchar(1000) | 部门路径，如 `/1/2/3/` |
| `leader_user_id` | bigint | 部门负责人 |
| `sort_order` | int | 排序 |
| `status` | varchar(32) | 状态 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |
| `deleted` | tinyint | 逻辑删除 |

索引：

- 唯一索引：`dept_code`
- 普通索引：`parent_id`
- 普通索引：`dept_path`
- 普通索引：`leader_user_id`

### 8.2 org_position 岗位表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `position_code` | varchar(64) | 岗位编码 |
| `position_name` | varchar(128) | 岗位名称 |
| `status` | varchar(32) | 状态 |
| `sort_order` | int | 排序 |
| `remark` | varchar(500) | 备注 |

索引：

- 唯一索引：`position_code`

### 8.3 org_rank 职级表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `rank_code` | varchar(64) | 职级编码 |
| `rank_name` | varchar(128) | 职级名称 |
| `rank_level` | int | 职级序号 |
| `status` | varchar(32) | 状态 |
| `remark` | varchar(500) | 备注 |

索引：

- 唯一索引：`rank_code`

### 8.4 org_user 用户表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `username` | varchar(64) | 登录账号 |
| `password_hash` | varchar(255) | 密码哈希 |
| `employee_no` | varchar(64) | 员工编号 |
| `real_name` | varchar(64) | 姓名 |
| `mobile` | varchar(32) | 手机号 |
| `email` | varchar(128) | 邮箱 |
| `main_dept_id` | bigint | 主部门 |
| `position_id` | bigint | 岗位 |
| `rank_id` | bigint | 职级 |
| `manager_user_id` | bigint | 直属上级 |
| `employee_status` | varchar(32) | 员工状态 |
| `account_status` | varchar(32) | 账号状态 |
| `entry_date` | date | 入职日期 |
| `resign_date` | date | 离职日期 |
| `last_login_at` | datetime | 最近登录时间 |
| `password_changed_at` | datetime | 密码修改时间 |
| `login_fail_count` | int | 登录失败次数 |
| `locked_until` | datetime | 锁定截止时间 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |
| `deleted` | tinyint | 逻辑删除 |

索引：

- 唯一索引：`username`
- 唯一索引：`employee_no`
- 普通索引：`mobile`
- 普通索引：`main_dept_id`
- 普通索引：`manager_user_id`
- 普通索引：`employee_status`
- 普通索引：`account_status`

### 8.5 org_user_dept 用户部门关系表

用于支持兼岗、多部门归属。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `user_id` | bigint | 用户 ID |
| `dept_id` | bigint | 部门 ID |
| `relation_type` | varchar(32) | MAIN、PART_TIME |
| `start_date` | date | 开始日期 |
| `end_date` | date | 结束日期 |
| `created_at` | datetime | 创建时间 |

索引：

- 普通索引：`user_id`
- 普通索引：`dept_id`
- 唯一索引：`user_id, dept_id, relation_type`

### 8.6 org_change_log 组织变更日志表

记录部门调整、员工调岗、离职、直属上级变更等。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `target_type` | varchar(32) | USER、DEPT |
| `target_id` | bigint | 目标 ID |
| `change_type` | varchar(64) | 变更类型 |
| `before_data` | json | 变更前 |
| `after_data` | json | 变更后 |
| `reason` | varchar(500) | 变更原因 |
| `operator_id` | bigint | 操作人 |
| `operated_at` | datetime | 操作时间 |

索引：

- 普通索引：`target_type, target_id`
- 普通索引：`operated_at`

## 9. 权限表

### 9.1 perm_role 角色表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `role_code` | varchar(64) | 角色编码 |
| `role_name` | varchar(128) | 角色名称 |
| `role_type` | varchar(32) | SYSTEM、BUSINESS |
| `status` | varchar(32) | 状态 |
| `sort_order` | int | 排序 |
| `remark` | varchar(500) | 备注 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

索引：

- 唯一索引：`role_code`

### 9.2 perm_menu 菜单表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `parent_id` | bigint | 上级菜单 |
| `menu_code` | varchar(128) | 菜单编码 |
| `menu_name` | varchar(128) | 菜单名称 |
| `route_path` | varchar(255) | 前端路由 |
| `component` | varchar(255) | 前端组件 |
| `icon` | varchar(64) | 图标 |
| `sort_order` | int | 排序 |
| `visible` | tinyint | 是否显示 |
| `status` | varchar(32) | 状态 |

索引：

- 唯一索引：`menu_code`
- 普通索引：`parent_id`

### 9.3 perm_button 按钮权限表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `menu_id` | bigint | 所属菜单 |
| `button_code` | varchar(128) | 按钮编码 |
| `button_name` | varchar(128) | 按钮名称 |
| `permission_code` | varchar(128) | 权限标识 |
| `status` | varchar(32) | 状态 |

索引：

- 普通索引：`menu_id`
- 唯一索引：`permission_code`

### 9.4 perm_user_role 用户角色关系表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `user_id` | bigint | 用户 ID |
| `role_id` | bigint | 角色 ID |
| `created_at` | datetime | 创建时间 |

索引：

- 普通索引：`user_id`
- 普通索引：`role_id`
- 唯一索引：`user_id, role_id`

### 9.5 perm_role_menu 角色菜单关系表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `role_id` | bigint | 角色 ID |
| `menu_id` | bigint | 菜单 ID |

索引：

- 唯一索引：`role_id, menu_id`

### 9.6 perm_role_button 角色按钮关系表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `role_id` | bigint | 角色 ID |
| `button_id` | bigint | 按钮 ID |

索引：

- 唯一索引：`role_id, button_id`

### 9.7 perm_data_scope 数据权限表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `role_id` | bigint | 角色 ID |
| `scope_type` | varchar(32) | SELF、DEPT、DEPT_AND_CHILD、CUSTOM_DEPT、CUSTOM_USER、ALL、DYNAMIC |
| `business_type` | varchar(64) | 业务类型 |
| `created_at` | datetime | 创建时间 |

索引：

- 普通索引：`role_id`
- 普通索引：`business_type`

### 9.8 perm_data_scope_dept 数据权限部门范围表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `data_scope_id` | bigint | 数据权限 ID |
| `dept_id` | bigint | 部门 ID |

索引：

- 唯一索引：`data_scope_id, dept_id`

### 9.9 perm_field_permission 字段权限表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `role_id` | bigint | 角色 ID |
| `business_type` | varchar(64) | 业务类型 |
| `field_code` | varchar(128) | 字段编码 |
| `visible` | tinyint | 是否可见 |
| `editable` | tinyint | 是否可编辑 |
| `required` | tinyint | 是否必填 |
| `masked` | tinyint | 是否脱敏 |

索引：

- 普通索引：`role_id`
- 普通索引：`business_type`
- 唯一索引：`role_id, business_type, field_code`

### 9.10 perm_temp_auth 临时授权表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `user_id` | bigint | 被授权人 |
| `auth_type` | varchar(32) | ROLE、MENU、DATA_SCOPE |
| `target_id` | bigint | 授权对象 ID |
| `start_at` | datetime | 开始时间 |
| `end_at` | datetime | 结束时间 |
| `reason` | varchar(500) | 授权原因 |
| `status` | varchar(32) | 状态 |
| `created_by` | bigint | 创建人 |
| `created_at` | datetime | 创建时间 |

索引：

- 普通索引：`user_id`
- 普通索引：`start_at, end_at`
- 普通索引：`status`

## 10. 流程表

### 10.1 wf_process_template 流程模板表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `template_code` | varchar(64) | 模板编码 |
| `template_name` | varchar(128) | 模板名称 |
| `business_type` | varchar(64) | 业务类型 |
| `description` | varchar(500) | 描述 |
| `status` | varchar(32) | 状态 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

索引：

- 唯一索引：`template_code`
- 普通索引：`business_type`

### 10.2 wf_process_version 流程版本表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `template_id` | bigint | 流程模板 ID |
| `version_no` | int | 版本号 |
| `flowable_definition_id` | varchar(128) | Flowable 流程定义 ID |
| `bpmn_xml` | longtext | BPMN XML |
| `status` | varchar(32) | DRAFT、PUBLISHED、DISABLED |
| `published_at` | datetime | 发布时间 |
| `published_by` | bigint | 发布人 |
| `change_reason` | varchar(500) | 变更原因 |

索引：

- 普通索引：`template_id`
- 唯一索引：`template_id, version_no`
- 普通索引：`flowable_definition_id`

### 10.3 wf_process_instance 流程实例业务表

用于保存 OA 业务语义和展示快照。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `process_instance_id` | varchar(128) | Flowable 流程实例 ID |
| `template_id` | bigint | 模板 ID |
| `process_version_id` | bigint | 流程版本 ID |
| `business_type` | varchar(64) | 业务类型 |
| `business_id` | bigint | 业务单据 ID |
| `title` | varchar(255) | 流程标题 |
| `starter_id` | bigint | 发起人 |
| `starter_name_snapshot` | varchar(64) | 发起人姓名快照 |
| `starter_dept_id` | bigint | 发起部门 |
| `starter_dept_name_snapshot` | varchar(128) | 发起部门快照 |
| `current_node_name` | varchar(128) | 当前节点名称 |
| `status` | varchar(32) | 流程状态 |
| `started_at` | datetime | 发起时间 |
| `ended_at` | datetime | 结束时间 |

索引：

- 唯一索引：`process_instance_id`
- 普通索引：`business_type, business_id`
- 普通索引：`starter_id`
- 普通索引：`starter_dept_id`
- 普通索引：`status`
- 普通索引：`started_at`

### 10.4 wf_task 审批任务业务表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `flowable_task_id` | varchar(128) | Flowable 任务 ID |
| `process_instance_id` | varchar(128) | Flowable 流程实例 ID |
| `wf_instance_id` | bigint | OA 流程实例 ID |
| `node_id` | varchar(128) | 节点 ID |
| `node_name` | varchar(128) | 节点名称 |
| `assignee_id` | bigint | 审批人 |
| `assignee_name_snapshot` | varchar(64) | 审批人姓名快照 |
| `assignee_dept_id` | bigint | 审批人部门 |
| `task_type` | varchar(32) | APPROVE、CC、ADD_SIGN |
| `status` | varchar(32) | 任务状态 |
| `due_at` | datetime | 截止时间 |
| `completed_at` | datetime | 完成时间 |
| `created_at` | datetime | 创建时间 |

索引：

- 普通索引：`flowable_task_id`
- 普通索引：`wf_instance_id`
- 普通索引：`assignee_id, status`
- 普通索引：`due_at`
- 普通索引：`created_at`

### 10.5 wf_task_record 审批记录表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `wf_instance_id` | bigint | 流程实例 ID |
| `task_id` | bigint | 任务 ID |
| `action` | varchar(32) | SUBMIT、APPROVE、REJECT、TRANSFER、ADD_SIGN、WITHDRAW、TERMINATE |
| `operator_id` | bigint | 操作人 |
| `operator_name_snapshot` | varchar(64) | 操作人姓名快照 |
| `node_name` | varchar(128) | 节点名称 |
| `comment` | varchar(1000) | 审批意见 |
| `attachment_ids` | varchar(1000) | 附件 ID 列表 |
| `operated_at` | datetime | 操作时间 |

索引：

- 普通索引：`wf_instance_id`
- 普通索引：`task_id`
- 普通索引：`operator_id`
- 普通索引：`operated_at`

### 10.6 wf_delegate 委托设置表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `delegator_id` | bigint | 委托人 |
| `delegatee_id` | bigint | 被委托人 |
| `business_type` | varchar(64) | 业务类型，可为空表示全部 |
| `start_at` | datetime | 开始时间 |
| `end_at` | datetime | 结束时间 |
| `status` | varchar(32) | 状态 |
| `reason` | varchar(500) | 原因 |

索引：

- 普通索引：`delegator_id`
- 普通索引：`delegatee_id`
- 普通索引：`start_at, end_at`

### 10.7 wf_timeout_rule 超时规则表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `template_id` | bigint | 流程模板 ID |
| `node_code` | varchar(128) | 节点编码 |
| `timeout_minutes` | int | 超时分钟数 |
| `remind_before_minutes` | int | 提前提醒分钟数 |
| `escalate_enabled` | tinyint | 是否升级提醒 |
| `status` | varchar(32) | 状态 |

索引：

- 普通索引：`template_id, node_code`

## 11. 规则配置表

### 11.1 rule_group 规则分组表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `group_code` | varchar(64) | 分组编码 |
| `group_name` | varchar(128) | 分组名称 |
| `description` | varchar(500) | 描述 |
| `status` | varchar(32) | 状态 |

索引：

- 唯一索引：`group_code`

### 11.2 rule_definition 规则定义表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `group_id` | bigint | 分组 ID |
| `rule_code` | varchar(64) | 规则编码 |
| `rule_name` | varchar(128) | 规则名称 |
| `rule_type` | varchar(32) | AMOUNT、TIME、FLOW、FORM、MESSAGE、FILE、EXPORT |
| `business_type` | varchar(64) | 业务类型 |
| `description` | varchar(500) | 描述 |
| `status` | varchar(32) | 状态 |

索引：

- 唯一索引：`rule_code`
- 普通索引：`group_id`
- 普通索引：`business_type`

### 11.3 rule_version 规则版本表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `rule_id` | bigint | 规则定义 ID |
| `version_no` | int | 版本号 |
| `rule_content` | json | 规则内容 |
| `natural_language` | varchar(1000) | 自然语言说明 |
| `status` | varchar(32) | DRAFT、PUBLISHED、DISABLED |
| `effective_at` | datetime | 生效时间 |
| `expired_at` | datetime | 失效时间 |
| `published_by` | bigint | 发布人 |
| `published_at` | datetime | 发布时间 |
| `change_reason` | varchar(500) | 变更原因 |

索引：

- 普通索引：`rule_id`
- 唯一索引：`rule_id, version_no`
- 普通索引：`effective_at, expired_at`
- 普通索引：`status`

### 11.4 rule_audit_log 规则审计日志表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `rule_id` | bigint | 规则 ID |
| `rule_version_id` | bigint | 规则版本 ID |
| `action` | varchar(32) | CREATE、UPDATE、PUBLISH、DISABLE、ROLLBACK |
| `before_data` | json | 变更前 |
| `after_data` | json | 变更后 |
| `reason` | varchar(500) | 变更原因 |
| `operator_id` | bigint | 操作人 |
| `operated_at` | datetime | 操作时间 |

索引：

- 普通索引：`rule_id`
- 普通索引：`rule_version_id`
- 普通索引：`operated_at`

## 12. 表单表

### 12.1 form_template 表单模板表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `template_code` | varchar(64) | 模板编码 |
| `template_name` | varchar(128) | 模板名称 |
| `business_type` | varchar(64) | 业务类型 |
| `status` | varchar(32) | 状态 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

索引：

- 唯一索引：`template_code`
- 普通索引：`business_type`

### 12.2 form_version 表单版本表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `template_id` | bigint | 表单模板 ID |
| `version_no` | int | 版本号 |
| `schema_json` | json | 表单结构 |
| `status` | varchar(32) | DRAFT、PUBLISHED、DISABLED |
| `published_by` | bigint | 发布人 |
| `published_at` | datetime | 发布时间 |

索引：

- 普通索引：`template_id`
- 唯一索引：`template_id, version_no`

### 12.3 form_field_rule 字段规则表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `form_version_id` | bigint | 表单版本 ID |
| `node_code` | varchar(128) | 流程节点编码 |
| `field_code` | varchar(128) | 字段编码 |
| `visible` | tinyint | 是否可见 |
| `editable` | tinyint | 是否可编辑 |
| `required` | tinyint | 是否必填 |
| `masked` | tinyint | 是否脱敏 |

索引：

- 普通索引：`form_version_id`
- 唯一索引：`form_version_id, node_code, field_code`

### 12.4 form_instance_snapshot 表单实例快照表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `business_type` | varchar(64) | 业务类型 |
| `business_id` | bigint | 业务 ID |
| `form_version_id` | bigint | 表单版本 ID |
| `snapshot_json` | json | 提交时表单快照 |
| `created_at` | datetime | 创建时间 |

索引：

- 唯一索引：`business_type, business_id`
- 普通索引：`form_version_id`

## 13. 消息与待办表

### 13.1 msg_message 消息表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `receiver_id` | bigint | 接收人 |
| `message_type` | varchar(32) | TODO、RESULT、NOTICE、SYSTEM、REMIND、RISK |
| `title` | varchar(255) | 标题 |
| `content` | text | 内容 |
| `business_type` | varchar(64) | 业务类型 |
| `business_id` | bigint | 业务 ID |
| `wf_instance_id` | bigint | 流程实例 ID |
| `read_status` | varchar(32) | UNREAD、READ |
| `archive_status` | varchar(32) | NORMAL、ARCHIVED |
| `created_at` | datetime | 创建时间 |
| `read_at` | datetime | 阅读时间 |

索引：

- 普通索引：`receiver_id, read_status`
- 普通索引：`business_type, business_id`
- 普通索引：`created_at`

### 13.2 msg_remind_record 催办提醒记录表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `wf_instance_id` | bigint | 流程实例 |
| `task_id` | bigint | 审批任务 |
| `remind_type` | varchar(32) | MANUAL、TIMEOUT、BEFORE_TIMEOUT |
| `sender_id` | bigint | 发送人 |
| `receiver_id` | bigint | 接收人 |
| `content` | varchar(500) | 内容 |
| `created_at` | datetime | 创建时间 |

索引：

- 普通索引：`wf_instance_id`
- 普通索引：`task_id`
- 普通索引：`receiver_id`
- 普通索引：`created_at`

## 14. 文件表

### 14.1 file_info 文件表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `file_name` | varchar(255) | 原始文件名 |
| `storage_name` | varchar(255) | 存储文件名 |
| `file_ext` | varchar(32) | 扩展名 |
| `mime_type` | varchar(128) | MIME 类型 |
| `file_size` | bigint | 文件大小 |
| `storage_type` | varchar(32) | LOCAL、OSS |
| `storage_path` | varchar(1000) | 存储路径 |
| `checksum` | varchar(128) | 文件校验值 |
| `upload_user_id` | bigint | 上传人 |
| `status` | varchar(32) | NORMAL、DELETED、PENDING_CLEAN |
| `created_at` | datetime | 创建时间 |

索引：

- 普通索引：`upload_user_id`
- 普通索引：`checksum`
- 普通索引：`created_at`

### 14.2 file_relation 文件业务关系表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `file_id` | bigint | 文件 ID |
| `business_type` | varchar(64) | 业务类型 |
| `business_id` | bigint | 业务 ID |
| `field_code` | varchar(128) | 所属字段 |
| `created_at` | datetime | 创建时间 |

索引：

- 普通索引：`file_id`
- 普通索引：`business_type, business_id`
- 唯一索引：`file_id, business_type, business_id, field_code`

### 14.3 file_version 文件版本表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `business_type` | varchar(64) | 业务类型 |
| `business_id` | bigint | 业务 ID |
| `file_id` | bigint | 文件 ID |
| `version_no` | int | 版本号 |
| `change_reason` | varchar(500) | 变更说明 |
| `created_by` | bigint | 创建人 |
| `created_at` | datetime | 创建时间 |

索引：

- 普通索引：`business_type, business_id`
- 唯一索引：`business_type, business_id, version_no`

### 14.4 file_download_log 文件下载日志表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `file_id` | bigint | 文件 ID |
| `user_id` | bigint | 下载人 |
| `business_type` | varchar(64) | 业务类型 |
| `business_id` | bigint | 业务 ID |
| `ip_address` | varchar(64) | IP |
| `user_agent` | varchar(500) | User-Agent |
| `downloaded_at` | datetime | 下载时间 |

索引：

- 普通索引：`file_id`
- 普通索引：`user_id`
- 普通索引：`downloaded_at`

## 15. OA 业务表

### 15.1 oa_notice 通知公告表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `title` | varchar(255) | 标题 |
| `content` | longtext | 内容 |
| `category` | varchar(64) | 分类 |
| `publish_scope_type` | varchar(32) | ALL、DEPT、ROLE、USER |
| `top_flag` | tinyint | 是否置顶 |
| `top_until` | datetime | 置顶截止 |
| `publish_at` | datetime | 发布时间 |
| `withdraw_at` | datetime | 撤回时间 |
| `status` | varchar(32) | DRAFT、PUBLISHED、WITHDRAWN、ARCHIVED |
| `created_by` | bigint | 创建人 |
| `created_at` | datetime | 创建时间 |

索引：

- 普通索引：`status`
- 普通索引：`publish_at`
- 普通索引：`category`

### 15.2 oa_notice_read 公告阅读记录表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `notice_id` | bigint | 公告 ID |
| `user_id` | bigint | 用户 ID |
| `read_at` | datetime | 阅读时间 |
| `confirmed` | tinyint | 是否确认阅读 |

索引：

- 唯一索引：`notice_id, user_id`
- 普通索引：`user_id`

### 15.3 oa_leave 请假申请表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `process_instance_id` | varchar(128) | Flowable 流程实例 ID |
| `rule_version_id` | bigint | 规则版本 ID |
| `leave_type` | varchar(64) | 请假类型 |
| `start_at` | datetime | 开始时间 |
| `end_at` | datetime | 结束时间 |
| `duration_hours` | decimal(10,2) | 请假小时 |
| `duration_days` | decimal(10,2) | 请假天数 |
| `reason` | varchar(1000) | 请假原因 |
| `handover_note` | varchar(1000) | 交接说明 |
| `status` | varchar(32) | 状态 |
| `created_by` | bigint | 申请人 |
| `created_name_snapshot` | varchar(64) | 申请人快照 |
| `created_dept_id` | bigint | 申请部门 |
| `created_dept_name_snapshot` | varchar(128) | 部门快照 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |
| `deleted` | tinyint | 逻辑删除 |
| `version` | int | 乐观锁 |

索引：

- 普通索引：`created_by`
- 普通索引：`created_dept_id`
- 普通索引：`status`
- 普通索引：`start_at, end_at`
- 普通索引：`process_instance_id`

### 15.4 oa_expense 报销申请表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `process_instance_id` | varchar(128) | Flowable 流程实例 ID |
| `rule_version_id` | bigint | 规则版本 ID |
| `expense_no` | varchar(64) | 报销编号 |
| `expense_type` | varchar(64) | 报销类型 |
| `total_amount` | decimal(14,2) | 申请金额 |
| `paid_amount` | decimal(14,2) | 实付金额 |
| `payee_account` | varchar(255) | 收款账户，可加密 |
| `payment_status` | varchar(32) | UNPAID、PAID |
| `paid_at` | datetime | 付款时间 |
| `reason` | varchar(1000) | 报销说明 |
| `status` | varchar(32) | 状态 |
| `created_by` | bigint | 申请人 |
| `created_name_snapshot` | varchar(64) | 申请人快照 |
| `created_dept_id` | bigint | 申请部门 |
| `created_dept_name_snapshot` | varchar(128) | 部门快照 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |
| `deleted` | tinyint | 逻辑删除 |
| `version` | int | 乐观锁 |

索引：

- 唯一索引：`expense_no`
- 普通索引：`created_by`
- 普通索引：`created_dept_id`
- 普通索引：`status`
- 普通索引：`payment_status`
- 普通索引：`process_instance_id`

### 15.5 oa_expense_item 报销明细表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `expense_id` | bigint | 报销 ID |
| `fee_type` | varchar(64) | 费用类型 |
| `fee_date` | date | 费用日期 |
| `amount` | decimal(14,2) | 金额 |
| `description` | varchar(500) | 说明 |
| `sort_order` | int | 排序 |

索引：

- 普通索引：`expense_id`

### 15.6 oa_seal_apply 用章申请表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `process_instance_id` | varchar(128) | 流程实例 ID |
| `seal_type` | varchar(64) | 印章类型 |
| `seal_name` | varchar(128) | 印章名称 |
| `file_title` | varchar(255) | 用章文件名称 |
| `use_reason` | varchar(1000) | 用章事由 |
| `use_at` | datetime | 用章时间 |
| `out_flag` | tinyint | 是否外带 |
| `return_at` | datetime | 归还时间 |
| `status` | varchar(32) | 状态 |
| `created_by` | bigint | 申请人 |
| `created_dept_id` | bigint | 部门 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

索引：

- 普通索引：`seal_type`
- 普通索引：`created_by`
- 普通索引：`status`
- 普通索引：`process_instance_id`

### 15.7 oa_purchase 采购申请表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `process_instance_id` | varchar(128) | 流程实例 ID |
| `purchase_no` | varchar(64) | 采购编号 |
| `purchase_type` | varchar(64) | 采购类型 |
| `supplier_name` | varchar(255) | 供应商 |
| `budget_subject` | varchar(128) | 预算科目 |
| `total_amount` | decimal(14,2) | 总金额 |
| `arrival_status` | varchar(32) | NOT_ARRIVED、PARTIAL、ARRIVED |
| `acceptance_status` | varchar(32) | PENDING、PASSED、FAILED |
| `reason` | varchar(1000) | 采购说明 |
| `status` | varchar(32) | 状态 |
| `created_by` | bigint | 申请人 |
| `created_dept_id` | bigint | 部门 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

索引：

- 唯一索引：`purchase_no`
- 普通索引：`created_by`
- 普通索引：`created_dept_id`
- 普通索引：`status`

### 15.8 oa_purchase_item 采购明细表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `purchase_id` | bigint | 采购 ID |
| `item_name` | varchar(255) | 物品名称 |
| `specification` | varchar(255) | 规格 |
| `quantity` | decimal(14,2) | 数量 |
| `unit` | varchar(32) | 单位 |
| `unit_price` | decimal(14,2) | 单价 |
| `amount` | decimal(14,2) | 金额 |
| `sort_order` | int | 排序 |

索引：

- 普通索引：`purchase_id`

## 16. 合同、会议、资产表

### 16.1 contract_info 合同表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `process_instance_id` | varchar(128) | 流程实例 ID |
| `contract_no` | varchar(64) | 合同编号 |
| `contract_name` | varchar(255) | 合同名称 |
| `contract_type` | varchar(64) | 合同类型 |
| `counterparty` | varchar(255) | 合同相对方 |
| `amount` | decimal(14,2) | 合同金额 |
| `start_date` | date | 开始日期 |
| `end_date` | date | 结束日期 |
| `sign_date` | date | 签署日期 |
| `status` | varchar(32) | 合同状态 |
| `created_by` | bigint | 创建人 |
| `created_dept_id` | bigint | 部门 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

索引：

- 唯一索引：`contract_no`
- 普通索引：`contract_type`
- 普通索引：`counterparty`
- 普通索引：`end_date`
- 普通索引：`status`

### 16.2 meeting_room 会议室表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `room_name` | varchar(128) | 会议室名称 |
| `location` | varchar(255) | 位置 |
| `capacity` | int | 容量 |
| `equipment` | varchar(500) | 设备 |
| `status` | varchar(32) | 状态 |
| `remark` | varchar(500) | 备注 |

索引：

- 普通索引：`status`

### 16.3 meeting_booking 会议室预约表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `room_id` | bigint | 会议室 ID |
| `title` | varchar(255) | 会议主题 |
| `start_at` | datetime | 开始时间 |
| `end_at` | datetime | 结束时间 |
| `organizer_id` | bigint | 组织人 |
| `participant_count` | int | 参会人数 |
| `status` | varchar(32) | BOOKED、CANCELLED、FINISHED |
| `cancel_reason` | varchar(500) | 取消原因 |
| `created_at` | datetime | 创建时间 |

索引：

- 普通索引：`room_id, start_at, end_at`
- 普通索引：`organizer_id`
- 普通索引：`status`

### 16.4 asset_info 固定资产表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `asset_no` | varchar(64) | 资产编号 |
| `asset_name` | varchar(255) | 资产名称 |
| `asset_category` | varchar(64) | 资产分类 |
| `model` | varchar(128) | 型号 |
| `purchase_date` | date | 购入日期 |
| `purchase_amount` | decimal(14,2) | 购入金额 |
| `responsible_user_id` | bigint | 责任人 |
| `dept_id` | bigint | 所属部门 |
| `status` | varchar(32) | IDLE、IN_USE、REPAIRING、SCRAPPED |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

索引：

- 唯一索引：`asset_no`
- 普通索引：`asset_category`
- 普通索引：`responsible_user_id`
- 普通索引：`dept_id`
- 普通索引：`status`

### 16.5 asset_record 资产流转记录表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `asset_id` | bigint | 资产 ID |
| `record_type` | varchar(32) | RECEIVE、RETURN、REPAIR、SCRAP、TRANSFER |
| `from_user_id` | bigint | 原责任人 |
| `to_user_id` | bigint | 新责任人 |
| `reason` | varchar(500) | 原因 |
| `operated_by` | bigint | 操作人 |
| `operated_at` | datetime | 操作时间 |

索引：

- 普通索引：`asset_id`
- 普通索引：`record_type`
- 普通索引：`operated_at`

### 16.6 asset_supply 办公用品表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `supply_code` | varchar(64) | 用品编码 |
| `supply_name` | varchar(255) | 用品名称 |
| `category` | varchar(64) | 分类 |
| `unit` | varchar(32) | 单位 |
| `stock_quantity` | decimal(14,2) | 当前库存 |
| `warning_quantity` | decimal(14,2) | 预警库存 |
| `status` | varchar(32) | 状态 |

索引：

- 唯一索引：`supply_code`
- 普通索引：`category`
- 普通索引：`status`

### 16.7 asset_supply_record 办公用品出入库记录表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `supply_id` | bigint | 用品 ID |
| `record_type` | varchar(32) | IN、OUT、RETURN、ADJUST |
| `quantity` | decimal(14,2) | 数量 |
| `user_id` | bigint | 领用人 |
| `reason` | varchar(500) | 原因 |
| `operated_by` | bigint | 操作人 |
| `operated_at` | datetime | 操作时间 |

索引：

- 普通索引：`supply_id`
- 普通索引：`record_type`
- 普通索引：`operated_at`

## 17. 报表、导入导出、审计表

### 17.1 report_export_task 导出任务表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `task_no` | varchar(64) | 任务编号 |
| `export_type` | varchar(64) | 导出类型 |
| `query_condition` | json | 查询条件 |
| `file_id` | bigint | 导出文件 ID |
| `status` | varchar(32) | PENDING、RUNNING、SUCCESS、FAILED |
| `error_message` | varchar(1000) | 失败原因 |
| `created_by` | bigint | 创建人 |
| `created_at` | datetime | 创建时间 |
| `finished_at` | datetime | 完成时间 |

索引：

- 唯一索引：`task_no`
- 普通索引：`export_type`
- 普通索引：`created_by`
- 普通索引：`status`

### 17.2 audit_login_log 登录日志表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `user_id` | bigint | 用户 ID |
| `username` | varchar(64) | 登录账号 |
| `ip_address` | varchar(64) | IP |
| `user_agent` | varchar(500) | User-Agent |
| `login_result` | varchar(32) | SUCCESS、FAILED |
| `fail_reason` | varchar(500) | 失败原因 |
| `logged_at` | datetime | 登录时间 |

索引：

- 普通索引：`user_id`
- 普通索引：`username`
- 普通索引：`logged_at`

### 17.3 audit_operation_log 操作日志表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `request_id` | varchar(64) | 请求 ID |
| `operator_id` | bigint | 操作人 |
| `operation_type` | varchar(64) | 操作类型 |
| `business_type` | varchar(64) | 业务类型 |
| `business_id` | bigint | 业务 ID |
| `request_method` | varchar(16) | HTTP 方法 |
| `request_uri` | varchar(500) | 请求地址 |
| `request_params` | json | 请求参数，需脱敏 |
| `result` | varchar(32) | SUCCESS、FAILED |
| `error_message` | varchar(1000) | 错误信息 |
| `ip_address` | varchar(64) | IP |
| `operated_at` | datetime | 操作时间 |

索引：

- 普通索引：`request_id`
- 普通索引：`operator_id`
- 普通索引：`business_type, business_id`
- 普通索引：`operated_at`

### 17.4 job_task_log 定时任务日志表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键 |
| `job_code` | varchar(64) | 任务编码 |
| `job_name` | varchar(128) | 任务名称 |
| `status` | varchar(32) | SUCCESS、FAILED |
| `start_at` | datetime | 开始时间 |
| `end_at` | datetime | 结束时间 |
| `error_message` | varchar(1000) | 错误信息 |

索引：

- 普通索引：`job_code`
- 普通索引：`start_at`
- 普通索引：`status`

## 18. 数据权限查询基础

为了支持高效数据权限过滤，业务表必须保留：

- `created_by`
- `created_dept_id`
- `status`
- `created_at`
- `process_instance_id`

常见查询条件：

```text
本人数据：created_by = 当前用户
本部门数据：created_dept_id = 当前用户主部门
本部门及下级：created_dept_id in 当前部门子树
审批相关：存在 wf_task 或 wf_task_record 关联当前用户
抄送相关：存在 wf_cc_record 或 msg_message 关联当前用户
```

部门树查询建议：

- `org_dept.dept_path` 保存层级路径。
- 查询下级部门可使用 `dept_path like '/1/2/%'`。
- 数据量较大时可增加部门闭包表。

## 19. 编号规则

业务编号不直接写死在业务代码中，应由规则配置中心维护。

常见编号：

```text
BX202604280001  报销编号
CG202604280001  采购编号
HT202604280001  合同编号
QJ202604280001  请假编号
YZ202604280001  用章编号
```

编号生成要求：

- 按业务类型配置前缀。
- 支持日期格式。
- 支持流水号长度。
- 支持按部门编号，可选。
- 必须通过唯一索引防重复。
- 高并发生成编号时必须加锁或使用独立序列表。

## 20. 索引设计原则

必须建立索引的字段：

- 主键 `id`
- 唯一编码字段
- 外键关联字段
- `created_by`
- `created_dept_id`
- `status`
- `created_at`
- `process_instance_id`
- 待办查询中的 `assignee_id, status`
- 消息查询中的 `receiver_id, read_status`

注意事项：

- 不要给所有字段都建索引。
- 报表类复杂查询可后期按慢 SQL 增加索引。
- 日志大表需要按时间归档或分区。
- 模糊搜索可先用普通 `like`，后期再考虑全文索引或搜索服务。

## 21. 数据保留与归档

建议：

- 登录日志保留至少 180 天。
- 操作日志保留至少 180 天。
- 审计日志保留不少于 1 年。
- 核心业务单据长期保留。
- 附件按业务保留策略处理。
- 消息可按时间归档。
- 大日志表按月份归档。

禁止：

- 物理删除核心业务单据。
- 物理删除审批记录。
- 普通管理员直接删除审计日志。
- 未经鉴权直接访问附件。

## 22. 后续待细化

后续进入建表脚本阶段时，需要继续细化：

- 字段长度最终确认。
- 所有枚举值最终确认。
- 数据库字符集和排序规则。
- 主键生成策略。
- 外键是否使用数据库约束。
- 金额字段精度。
- JSON 字段兼容性。
- Flowable 建表脚本版本。
- Flyway / Liquibase 迁移脚本。
- 初始化数据脚本。

## 23. 总结

数据库设计必须服务于三个核心目标：

- 支撑最终完整 OA 功能。
- 支撑复杂权限、流程、规则和审计。
- 保证历史数据在人员、部门、流程、规则变化后仍可追溯。

本设计采用“Flowable 管底层流程，OA 自建表管业务语义”的方式，避免业务模块直接依赖流程引擎内部表。同时通过快照、版本、审计、逻辑删除和数据权限字段，保证系统长期可维护、可扩展、可追责。
