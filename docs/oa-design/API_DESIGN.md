# 企业级 OA 系统 API 设计

## 1. 文档定位

本文档基于 `OA_SYSTEM_DESIGN.md`、`DEVELOPMENT_PLAN.md`、`DATABASE_DESIGN.md` 编写，用于定义企业级 OA 系统最终版本的后端 API 契约草案。

本文档重点说明：

- API 命名规范
- 统一请求与响应格式
- 认证与权限约定
- 分页、排序、筛选规范
- 错误码规范
- 各模块接口清单
- 关键接口请求和响应示例
- 幂等、审计、导出、附件、安全约束

后续可基于本文档生成 OpenAPI / Swagger 文档。

## 2. 基础约定

### 2.1 API 前缀

```text
/api
```

示例：

```text
POST /api/auth/login
GET  /api/org/users
GET  /api/workflow/tasks/todo
```

### 2.2 HTTP 方法

| 方法 | 说明 |
| --- | --- |
| `GET` | 查询 |
| `POST` | 新增、提交、动作类操作 |
| `PUT` | 全量或主要字段更新 |
| `PATCH` | 局部更新、状态变更 |
| `DELETE` | 删除或逻辑删除 |

### 2.3 命名风格

- URL 使用小写中划线。
- JSON 字段使用小驼峰。
- 业务动作使用明确动词，例如 `/approve`、`/reject`、`/publish`。
- 批量操作使用 `/batch-*`。

### 2.4 版本策略

初始版本：

```text
/api
```

如果后续出现不兼容变更，可升级为：

```text
/api/v2
```

## 3. 统一响应格式

### 3.1 成功响应

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {},
  "requestId": "8f4b1c0d9a",
  "timestamp": "2026-04-28T03:00:00+08:00"
}
```

### 3.2 失败响应

```json
{
  "code": "WORKFLOW_TASK_NOT_ASSIGNED_TO_USER",
  "message": "当前用户不是该审批任务处理人",
  "data": null,
  "requestId": "8f4b1c0d9a",
  "timestamp": "2026-04-28T03:00:00+08:00"
}
```

### 3.3 分页响应

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "page": 1,
    "size": 20,
    "total": 100,
    "items": []
  },
  "requestId": "8f4b1c0d9a",
  "timestamp": "2026-04-28T03:00:00+08:00"
}
```

## 4. 通用请求规范

### 4.1 分页参数

```text
page: 页码，从 1 开始，默认 1
size: 每页数量，默认 20，最大 100
sort: 排序字段
order: asc / desc
```

示例：

```text
GET /api/org/users?page=1&size=20&sort=createdAt&order=desc
```

### 4.2 时间格式

统一使用 ISO 8601：

```text
2026-04-28T09:00:00+08:00
```

日期使用：

```text
2026-04-28
```

### 4.3 金额格式

金额使用数字，不使用字符串：

```json
{
  "amount": 8000.00
}
```

后端使用 `decimal(14,2)` 或 `BigDecimal` 处理。

### 4.4 幂等键

关键写操作需要支持幂等键：

```text
Idempotency-Key: uuid
```

适用操作：

- 提交流程
- 审批通过
- 审批驳回
- 撤回
- 转交
- 加签
- 文件绑定
- 导出任务创建

## 5. 认证与权限

### 5.1 认证头

```text
Authorization: Bearer <token>
```

### 5.2 权限校验原则

后端必须校验：

- 是否登录
- 是否有接口权限
- 是否有菜单或按钮权限
- 是否有数据权限
- 是否有字段权限
- 是否有附件权限
- 是否为当前审批任务处理人
- 是否有导出权限

### 5.3 当前用户上下文

```http
GET /api/auth/me
```

返回示例：

```json
{
  "id": 10001,
  "username": "zhangsan",
  "realName": "张三",
  "mainDeptId": 20001,
  "mainDeptName": "技术部",
  "roles": ["EMPLOYEE"],
  "permissions": ["oa:leave:create", "workflow:task:approve"]
}
```

## 6. 错误码规范

### 6.1 通用错误码

| 错误码 | 说明 |
| --- | --- |
| `SUCCESS` | 成功 |
| `BAD_REQUEST` | 请求参数错误 |
| `UNAUTHORIZED` | 未登录 |
| `FORBIDDEN` | 无权限 |
| `NOT_FOUND` | 数据不存在 |
| `CONFLICT` | 状态冲突 |
| `VALIDATION_FAILED` | 参数校验失败 |
| `IDEMPOTENCY_CONFLICT` | 幂等冲突 |
| `INTERNAL_ERROR` | 系统异常 |

### 6.2 业务错误码

| 错误码 | 说明 |
| --- | --- |
| `USER_ACCOUNT_LOCKED` | 账号已锁定 |
| `USER_PASSWORD_EXPIRED` | 密码已过期 |
| `PERMISSION_DATA_SCOPE_DENIED` | 无数据权限 |
| `FILE_ACCESS_DENIED` | 无附件访问权限 |
| `WORKFLOW_TASK_NOT_ASSIGNED_TO_USER` | 当前用户不是审批人 |
| `WORKFLOW_TASK_ALREADY_COMPLETED` | 任务已处理 |
| `WORKFLOW_INSTANCE_ENDED` | 流程已结束 |
| `RULE_VERSION_NOT_EFFECTIVE` | 规则版本未生效 |
| `FORM_FIELD_REQUIRED` | 表单字段必填 |
| `EXPORT_TOO_MANY_ROWS` | 导出数据过多 |

## 7. 认证接口

### 7.1 登录

```http
POST /api/auth/login
```

请求：

```json
{
  "username": "zhangsan",
  "password": "password123",
  "captchaId": "captcha-id",
  "captchaCode": "abcd"
}
```

响应：

```json
{
  "accessToken": "token",
  "expiresIn": 7200,
  "user": {
    "id": 10001,
    "realName": "张三"
  }
}
```

### 7.2 退出

```http
POST /api/auth/logout
```

### 7.3 当前用户

```http
GET /api/auth/me
```

### 7.3.1 当前用户可见菜单

用于前端侧栏渲染；数据来自 `perm_menu` 与角色菜单分配。`SUPER_ADMIN` 返回全部启用且可见菜单。

```http
GET /api/auth/menus
```

响应项字段：`id`、`parentId`、`menuCode`、`menuName`、`routePath`、`sortOrder`（与 `UI_PAGES` 路由一致）。

### 7.4 修改密码

```http
POST /api/auth/change-password
```

请求：

```json
{
  "oldPassword": "old",
  "newPassword": "new"
}
```

### 7.5 重置密码

```http
POST /api/auth/users/{userId}/reset-password
```

权限：

```text
system:user:reset-password
```

## 8. 组织人员接口

### 8.1 部门

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/org/depts/tree` | 部门树 |
| `GET` | `/api/org/depts/{id}` | 部门详情 |
| `POST` | `/api/org/depts` | 新增部门 |
| `PUT` | `/api/org/depts/{id}` | 更新部门 |
| `PATCH` | `/api/org/depts/{id}/enable` | 启用部门 |
| `PATCH` | `/api/org/depts/{id}/disable` | 停用部门 |
| `GET` | `/api/org/depts/{id}/users` | 部门人员 |

### 8.2 用户

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/org/users` | 用户列表 |
| `GET` | `/api/org/users/{id}` | 用户详情 |
| `POST` | `/api/org/users` | 新增用户 |
| `PUT` | `/api/org/users/{id}` | 更新用户 |
| `PATCH` | `/api/org/users/{id}/enable` | 启用用户 |
| `PATCH` | `/api/org/users/{id}/disable` | 停用用户 |
| `PATCH` | `/api/org/users/{id}/resign` | 离职 |
| `POST` | `/api/org/users/import` | 用户导入 |
| `POST` | `/api/org/users/export` | 用户导出 |
| `GET` | `/api/org/users/contacts` | 通讯录 |

用户创建请求示例：

```json
{
  "username": "zhangsan",
  "employeeNo": "E10001",
  "realName": "张三",
  "mobile": "13800000000",
  "email": "zhangsan@example.com",
  "mainDeptId": 20001,
  "positionId": 30001,
  "rankId": 40001,
  "managerUserId": 10000,
  "roleIds": [1, 2]
}
```

### 8.3 岗位与职级

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/org/positions` | 岗位列表 |
| `POST` | `/api/org/positions` | 新增岗位 |
| `PUT` | `/api/org/positions/{id}` | 更新岗位 |
| `GET` | `/api/org/ranks` | 职级列表 |
| `POST` | `/api/org/ranks` | 新增职级 |
| `PUT` | `/api/org/ranks/{id}` | 更新职级 |

## 9. 权限接口

### 9.1 角色

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/permission/roles` | 角色列表 |
| `GET` | `/api/permission/roles/{id}` | 角色详情 |
| `POST` | `/api/permission/roles` | 新增角色 |
| `PUT` | `/api/permission/roles/{id}` | 更新角色 |
| `DELETE` | `/api/permission/roles/{id}` | 删除角色 |
| `POST` | `/api/permission/roles/{id}/menus` | 分配菜单 |
| `POST` | `/api/permission/roles/{id}/buttons` | 分配按钮 |
| `POST` | `/api/permission/roles/{id}/data-scopes` | 分配数据权限 |

### 9.2 菜单与按钮

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/permission/menus/tree` | 菜单树 |
| `POST` | `/api/permission/menus` | 新增菜单 |
| `PUT` | `/api/permission/menus/{id}` | 更新菜单 |
| `GET` | `/api/permission/buttons` | 按钮权限列表 |
| `POST` | `/api/permission/buttons` | 新增按钮权限 |

### 9.3 权限预览

```http
GET /api/permission/users/{userId}/preview
```

返回：

```json
{
  "menus": [],
  "buttons": [],
  "dataScopes": [],
  "fieldPermissions": []
}
```

### 9.4 临时授权

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/permission/temp-auths` | 临时授权列表 |
| `POST` | `/api/permission/temp-auths` | 新增临时授权 |
| `PATCH` | `/api/permission/temp-auths/{id}/revoke` | 撤销临时授权 |

## 10. 字典与系统参数接口

### 10.1 字典

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/system/dict-types` | 字典类型列表 |
| `POST` | `/api/system/dict-types` | 新增字典类型 |
| `PUT` | `/api/system/dict-types/{id}` | 更新字典类型 |
| `GET` | `/api/system/dict-types/{code}/items` | 字典项 |
| `POST` | `/api/system/dict-items` | 新增字典项 |
| `PUT` | `/api/system/dict-items/{id}` | 更新字典项 |

### 10.2 系统参数

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/system/configs` | 参数列表 |
| `PUT` | `/api/system/configs/{key}` | 更新参数 |
| `GET` | `/api/system/configs/{key}` | 获取参数 |

## 11. 文件接口

### 11.1 上传文件

```http
POST /api/files/upload
Content-Type: multipart/form-data
```

参数：

```text
file: 文件
businessType: 业务类型，可选
businessId: 业务 ID，可选
fieldCode: 字段编码，可选
```

响应：

```json
{
  "fileId": 90001,
  "fileName": "invoice.pdf",
  "fileSize": 102400,
  "fileExt": "pdf"
}
```

### 11.2 下载文件

```http
GET /api/files/{id}/download
```

要求：

- 必须校验附件权限。
- 必须记录下载日志。

### 11.3 文件接口清单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/files/{id}` | 文件信息 |
| `GET` | `/api/files/{id}/download` | 下载文件 |
| `GET` | `/api/files/{id}/preview` | 预览文件 |
| `DELETE` | `/api/files/{id}` | 删除文件 |
| `POST` | `/api/files/{id}/versions` | 上传新版本 |
| `GET` | `/api/files/{id}/download-logs` | 下载日志 |

## 12. 流程接口

### 12.1 流程模板

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/workflow/templates` | 流程模板列表 |
| `GET` | `/api/workflow/templates/{id}` | 流程模板详情 |
| `POST` | `/api/workflow/templates` | 新增流程模板 |
| `PUT` | `/api/workflow/templates/{id}` | 更新流程模板 |
| `POST` | `/api/workflow/templates/{id}/versions` | 创建流程版本 |
| `POST` | `/api/workflow/versions/{id}/publish` | 发布流程版本 |
| `POST` | `/api/workflow/versions/{id}/simulate` | 模拟流程路径 |

### 12.2 发起流程

```http
POST /api/workflow/instances
```

请求：

```json
{
  "businessType": "EXPENSE",
  "businessId": 50001,
  "title": "张三的差旅费报销",
  "variables": {
    "amount": 8000,
    "deptId": 20001
  }
}
```

响应：

```json
{
  "wfInstanceId": 70001,
  "processInstanceId": "flowable-instance-id",
  "status": "APPROVING",
  "currentNodeName": "直属上级审批"
}
```

### 12.3 审批任务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/workflow/tasks/todo` | 我的待办 |
| `GET` | `/api/workflow/tasks/done` | 我的已办 |
| `GET` | `/api/workflow/instances/started-by-me` | 我发起的 |
| `GET` | `/api/workflow/instances/cc-to-me` | 抄送我的 |
| `GET` | `/api/workflow/instances/{id}` | 流程详情 |
| `GET` | `/api/workflow/instances/{id}/timeline` | 流程时间轴 |
| `GET` | `/api/workflow/instances/{id}/diagram` | 流程图 |

### 12.4 审批动作

#### 通过

```http
POST /api/workflow/tasks/{taskId}/approve
```

请求：

```json
{
  "comment": "同意",
  "attachmentIds": []
}
```

#### 驳回

```http
POST /api/workflow/tasks/{taskId}/reject
```

请求：

```json
{
  "comment": "请补充发票附件",
  "rejectTo": "STARTER"
}
```

#### 其他动作

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/workflow/tasks/{taskId}/transfer` | 转交 |
| `POST` | `/api/workflow/tasks/{taskId}/add-sign` | 加签 |
| `POST` | `/api/workflow/tasks/{taskId}/remind` | 催办 |
| `POST` | `/api/workflow/instances/{id}/withdraw` | 撤回 |
| `POST` | `/api/workflow/instances/{id}/terminate` | 终止 |

## 13. 规则配置接口

### 13.1 规则定义

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/rules` | 规则列表 |
| `GET` | `/api/rules/{id}` | 规则详情 |
| `POST` | `/api/rules` | 新增规则 |
| `PUT` | `/api/rules/{id}` | 更新规则 |
| `POST` | `/api/rules/{id}/versions` | 创建规则版本 |
| `POST` | `/api/rule-versions/{id}/publish` | 发布规则版本 |
| `POST` | `/api/rule-versions/{id}/disable` | 停用规则版本 |
| `POST` | `/api/rule-versions/{id}/simulate` | 规则模拟 |
| `GET` | `/api/rules/{id}/audit-logs` | 规则审计日志 |

### 13.2 规则模拟请求

```json
{
  "businessType": "EXPENSE",
  "input": {
    "amount": 8000,
    "deptId": 20001,
    "expenseType": "TRAVEL"
  }
}
```

响应：

```json
{
  "matched": true,
  "matchedRules": [
    {
      "ruleCode": "EXPENSE_AMOUNT_GT_5000",
      "description": "报销金额大于 5000，追加部门负责人审批"
    }
  ],
  "actions": [
    {
      "type": "ADD_APPROVER",
      "target": "DEPT_LEADER"
    }
  ]
}
```

## 14. 表单接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/forms/templates` | 表单模板列表 |
| `POST` | `/api/forms/templates` | 新增表单模板 |
| `PUT` | `/api/forms/templates/{id}` | 更新表单模板 |
| `POST` | `/api/forms/templates/{id}/versions` | 创建表单版本 |
| `POST` | `/api/forms/versions/{id}/publish` | 发布表单版本 |
| `GET` | `/api/forms/runtime/{businessType}` | 获取运行时表单 |
| `POST` | `/api/forms/snapshots` | 保存表单快照 |
| `GET` | `/api/forms/snapshots/{businessType}/{businessId}` | 表单快照 |

## 15. 消息与待办接口

### 15.1 消息

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/messages` | 消息列表 |
| `GET` | `/api/messages/unread-count` | 未读数量 |
| `PATCH` | `/api/messages/{id}/read` | 标记已读 |
| `PATCH` | `/api/messages/batch-read` | 批量已读 |
| `PATCH` | `/api/messages/{id}/archive` | 归档 |
| `DELETE` | `/api/messages/{id}` | 删除消息 |

### 15.2 待办聚合

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/dashboard/todos` | 首页待办 |
| `GET` | `/api/dashboard/summary` | 首页摘要 |
| `GET` | `/api/dashboard/quick-actions` | 常用入口 |

## 16. 通知公告接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/notices` | 公告列表 |
| `GET` | `/api/notices/{id}` | 公告详情 |
| `POST` | `/api/notices` | 新增公告 |
| `PUT` | `/api/notices/{id}` | 更新公告 |
| `POST` | `/api/notices/{id}/publish` | 发布公告 |
| `POST` | `/api/notices/{id}/withdraw` | 撤回公告 |
| `POST` | `/api/notices/{id}/read` | 标记已读 |
| `POST` | `/api/notices/{id}/confirm` | 确认阅读 |
| `GET` | `/api/notices/{id}/read-stats` | 阅读统计 |

## 17. OA 业务接口

### 17.1 通用业务接口约定

业务模块通常包含：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/{module}` | 列表 |
| `GET` | `/api/{module}/{id}` | 详情 |
| `POST` | `/api/{module}` | 保存草稿或新增 |
| `PUT` | `/api/{module}/{id}` | 更新 |
| `POST` | `/api/{module}/{id}/submit` | 提交审批 |
| `POST` | `/api/{module}/{id}/cancel` | 作废 |
| `POST` | `/api/{module}/export` | 导出 |

提交审批时，后端需要：

- 校验业务字段。
- 校验规则。
- 保存表单快照。
- 启动流程实例。
- 生成待办和消息。
- 写操作日志。

### 17.2 请假

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/oa/leaves` | 请假列表 |
| `GET` | `/api/oa/leaves/{id}` | 请假详情 |
| `POST` | `/api/oa/leaves` | 新增请假草稿 |
| `PUT` | `/api/oa/leaves/{id}` | 更新请假 |
| `POST` | `/api/oa/leaves/{id}/submit` | 提交请假 |
| `POST` | `/api/oa/leaves/{id}/withdraw` | 撤回请假 |
| `POST` | `/api/oa/leaves/{id}/cancel` | 作废请假 |
| `GET` | `/api/oa/leaves/calculate-duration` | 计算请假时长 |
| `POST` | `/api/oa/leaves/export` | 导出请假 |

### 17.3 报销

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/oa/expenses` | 报销列表 |
| `GET` | `/api/oa/expenses/{id}` | 报销详情 |
| `POST` | `/api/oa/expenses` | 新增报销草稿 |
| `PUT` | `/api/oa/expenses/{id}` | 更新报销 |
| `POST` | `/api/oa/expenses/{id}/submit` | 提交报销 |
| `POST` | `/api/oa/expenses/{id}/withdraw` | 撤回报销 |
| `POST` | `/api/oa/expenses/{id}/cancel` | 作废报销 |
| `POST` | `/api/oa/expenses/{id}/mark-paid` | 标记付款 |
| `GET` | `/api/oa/expenses/{id}/print` | 打印报销单 |
| `POST` | `/api/oa/expenses/export` | 导出报销 |

### 17.4 用章

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/oa/seal-applies` | 用章列表 |
| `GET` | `/api/oa/seal-applies/{id}` | 用章详情 |
| `POST` | `/api/oa/seal-applies` | 新增用章 |
| `PUT` | `/api/oa/seal-applies/{id}` | 更新用章 |
| `POST` | `/api/oa/seal-applies/{id}/submit` | 提交用章 |
| `POST` | `/api/oa/seal-applies/{id}/return` | 外带归还 |
| `POST` | `/api/oa/seal-applies/export` | 导出用章 |

### 17.5 采购

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/oa/purchases` | 采购列表 |
| `GET` | `/api/oa/purchases/{id}` | 采购详情 |
| `POST` | `/api/oa/purchases` | 新增采购 |
| `PUT` | `/api/oa/purchases/{id}` | 更新采购 |
| `POST` | `/api/oa/purchases/{id}/submit` | 提交采购 |
| `POST` | `/api/oa/purchases/{id}/withdraw` | 撤回采购 |
| `POST` | `/api/oa/purchases/{id}/cancel` | 作废采购 |
| `POST` | `/api/oa/purchases/{id}/confirm-arrival` | 到货确认 |
| `POST` | `/api/oa/purchases/{id}/accept` | 验收 |
| `POST` | `/api/oa/purchases/export` | 导出采购 |

## 18. 合同接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/contracts` | 合同列表 |
| `GET` | `/api/contracts/{id}` | 合同详情 |
| `POST` | `/api/contracts` | 新增合同 |
| `PUT` | `/api/contracts/{id}` | 更新合同 |
| `POST` | `/api/contracts/{id}/submit` | 提交合同审批 |
| `POST` | `/api/contracts/{id}/sign` | 标记签署 |
| `POST` | `/api/contracts/{id}/terminate` | 终止合同 |
| `POST` | `/api/contracts/{id}/renew` | 续签 |
| `GET` | `/api/contracts/{id}/versions` | 合同附件版本 |
| `POST` | `/api/contracts/export` | 导出合同 |

## 19. 会议、资产、办公用品接口

### 19.1 会议室

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/meetings/rooms` | 会议室列表 |
| `POST` | `/api/meetings/rooms` | 新增会议室 |
| `PUT` | `/api/meetings/rooms/{id}` | 更新会议室 |
| `GET` | `/api/meetings/bookings` | 预约列表 |
| `POST` | `/api/meetings/bookings` | 新增预约 |
| `POST` | `/api/meetings/bookings/{id}/cancel` | 取消预约 |
| `GET` | `/api/meetings/rooms/{id}/availability` | 可预约时间 |

### 19.2 固定资产

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/assets` | 资产列表 |
| `GET` | `/api/assets/{id}` | 资产详情 |
| `POST` | `/api/assets` | 新增资产 |
| `PUT` | `/api/assets/{id}` | 更新资产 |
| `POST` | `/api/assets/{id}/receive` | 领用 |
| `POST` | `/api/assets/{id}/return` | 归还 |
| `POST` | `/api/assets/{id}/repair` | 维修 |
| `POST` | `/api/assets/{id}/scrap` | 报废 |
| `GET` | `/api/assets/{id}/records` | 流转记录 |

### 19.3 办公用品

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/supplies` | 用品列表 |
| `POST` | `/api/supplies` | 新增用品 |
| `PUT` | `/api/supplies/{id}` | 更新用品 |
| `POST` | `/api/supplies/{id}/stock-in` | 入库 |
| `POST` | `/api/supplies/{id}/stock-out` | 出库 |
| `POST` | `/api/supplies/{id}/return` | 退回 |
| `GET` | `/api/supplies/{id}/records` | 出入库记录 |

## 20. 文件资料库接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/file-library/folders` | 文件夹树 |
| `POST` | `/api/file-library/folders` | 新增文件夹 |
| `PUT` | `/api/file-library/folders/{id}` | 更新文件夹 |
| `GET` | `/api/file-library/files` | 文件列表 |
| `POST` | `/api/file-library/files` | 上传资料库文件 |
| `GET` | `/api/file-library/files/{id}` | 文件详情 |
| `POST` | `/api/file-library/files/{id}/versions` | 上传新版本 |
| `POST` | `/api/file-library/files/{id}/move` | 移动文件 |
| `DELETE` | `/api/file-library/files/{id}` | 删除文件 |
| `GET` | `/api/file-library/files/{id}/download-logs` | 下载日志 |

## 21. 报表与导入导出接口

### 21.1 报表

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/reports/workflow-efficiency` | 流程效率统计 |
| `GET` | `/api/reports/todo-summary` | 待办统计 |
| `GET` | `/api/reports/leave-summary` | 请假统计 |
| `GET` | `/api/reports/expense-summary` | 报销统计 |
| `GET` | `/api/reports/contract-summary` | 合同统计 |
| `GET` | `/api/reports/asset-summary` | 资产统计 |

### 21.2 导入导出

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/imports/preview` | 导入预校验 |
| `POST` | `/api/imports/commit` | 确认导入 |
| `POST` | `/api/exports` | 创建导出任务 |
| `GET` | `/api/exports` | 导出任务列表 |
| `GET` | `/api/exports/{id}` | 导出任务详情 |
| `GET` | `/api/exports/{id}/download` | 下载导出文件 |

创建导出任务请求：

```json
{
  "exportType": "EXPENSE",
  "queryCondition": {
    "status": "APPROVED",
    "startDate": "2026-04-01",
    "endDate": "2026-04-30"
  }
}
```

## 22. 审计与运维接口

### 22.1 审计日志

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/audit/login-logs` | 登录日志 |
| `GET` | `/api/audit/operation-logs` | 操作日志 |
| `GET` | `/api/audit/rule-logs` | 规则变更日志 |
| `GET` | `/api/audit/permission-logs` | 权限变更日志 |
| `GET` | `/api/audit/file-download-logs` | 文件下载日志 |

### 22.2 运维

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/ops/health` | 健康检查 |
| `GET` | `/api/ops/online-users` | 在线用户 |
| `POST` | `/api/ops/cache/refresh` | 刷新缓存 |
| `GET` | `/api/ops/job-logs` | 定时任务日志 |
| `GET` | `/api/ops/exceptions` | 异常中心 |
| `GET` | `/api/ops/backup-records` | 备份记录 |

## 23. 高风险接口要求

以下接口必须满足二次确认、审计日志和权限校验：

- 删除用户
- 停用账号
- 重置密码
- 修改角色权限
- 修改数据权限
- 发布流程版本
- 发布规则版本
- 终止流程
- 作废单据
- 批量导出
- 下载敏感附件
- 刷新系统缓存

## 24. 接口验收标准

- 所有接口返回统一格式。
- 所有列表接口支持分页。
- 所有写接口记录操作日志。
- 所有高风险接口记录审计日志。
- 所有导出接口受数据权限控制。
- 所有附件下载接口受附件权限控制。
- 所有审批动作校验当前任务处理人。
- 所有关键写接口支持幂等。
- 所有业务错误有稳定错误码。
- 前端不需要直接访问 Flowable 内部接口。

## 25. 后续待细化

后续进入详细开发前，需要继续细化：

- 每个接口完整字段定义。
- 每个接口权限标识。
- 每个接口错误码清单。
- 每个业务模块筛选条件。
- 导入模板字段。
- 导出字段清单。
- OpenAPI / Swagger 文档。
- 前端 API SDK 封装。

## 26. 总结

API 设计的核心原则是：前端只面对 OA 业务语义，不直接感知 Flowable 内部结构、数据库表结构或复杂权限实现。所有权限、流程、规则、审计、安全都在后端统一收口。

本文档先定义最终版本的接口边界和契约，后续应结合数据库设计和页面原型继续细化为可直接开发的 OpenAPI 文档。
