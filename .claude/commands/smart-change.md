---
description: 🔮 智能变更 — 跨模块影响分析 + 修改后验证
allowed-tools: Bash, Read, Grep, Glob, Edit, Write
---

# 🔮 Smart Change

修改代码前进行跨模块影响分析，修改后验证正确性。

## Phase 1: 跨模块影响分析（修改前）

用户告诉你要修改的目标后，执行:

### 1.1 直接影响（同模块）
```bash
grep -rn "目标方法名" backend/src/main/java --include="*.java" | grep -v "import\|//\|test"
grep -rn "目标方法名" frontend/src/ --include="*.ts" --include="*.vue"
```

### 1.2 跨模块间接影响（关键！）
修改一个 Service 时，必须检查所有依赖它的模块:
```bash
# 例: 修改 MessageService，检查谁依赖了消息模块
grep -rn "MessageService\|messageService\|msg_message" backend/src/main/java --include="*.java" | grep -v "import\|//\|test" | grep -v "MessageService.java"
grep -rn "MessageService\|messageService" backend/src/main/java --include="*.java" | grep -oP 'import com\.company\.oa\.\w+' | sort -u
```

### 1.3 前端级联影响
后端 API 变更时，检查前端所有引用点:
```bash
# API 路径变更
grep -rn "旧API路径" frontend/src/ --include="*.ts" --include="*.vue"
# 字段名变更
grep -rn "旧字段名" frontend/src/ --include="*.ts" --include="*.vue"
# SSE/WebSocket 事件
grep -rn "事件名" frontend/src/composables/ --include="*.ts"
```

### 1.4 输出跨模块影响报告
```
📊 跨模块影响分析:
  目标: Module.Service.method()

  直接调用方:
    ├── workflow.WorkflowService
    └── contract.ContractService

  跨模块间接影响:
    ├── 通知模块: NotificationSseController (SSE推送)
    ├── 邮件模块: EmailService (邮件通知)
    ├── 审计模块: AuditService (操作记录)
    └── 前端: NotificationCenter (分组展示)

  数据流: Controller → Service → Mapper → DB
  状态关联: [相关状态字段]
  测试覆盖: [有/无]

  ⚠️ 风险点:
    1. 修改返回字段可能影响 SSE 推送格式
    2. 新增必填字段需要前端同步
```
6. **等待用户确认后再执行修改**

## Phase 2: 验证（修改后）

### 智能设计检查（关键！）
修改涉及新功能时，必须检查是否符合"预测性+主动性+个性化"：
- ❌ 只展示数据表格 → 应该加趋势预测/风险预警
- ❌ 等用户点按钮 → 应该主动推送/自动触发
- ❌ 所有人同一界面 → 应该按角色/行为个性化
- ❌ 事后统计 → 应该事前预警
- ❌ 静态规则 → 应该基于历史数据动态调整

### 编译检查
```bash
cd /home/ubuntu/OAsystem/backend && mvn compile -q 2>&1 | tail -5
cd /home/ubuntu/OAsystem/frontend && npx vue-tsc --noEmit 2>&1 | tail -10
```

### API契约检查
如果修改了 Controller，检查前端对应 API 文件是否需要更新。

### 字段一致性检查
如果修改了 Entity 或 DTO，检查前端是否引用了旧字段名:
```bash
grep -rn "旧字段名" frontend/src/ --include="*.ts" --include="*.vue"
```

### 跨模块一致性检查（关键！）
修改后验证所有间接影响是否已处理:
```bash
# 检查 SSE 推送是否受影响
grep -rn "发送消息\|send(" backend/src/main/java --include="*.java" | grep -v test | head -10
# 检查审计日志是否需要更新
grep -rn "@Auditable\|safeRecordOperation" backend/src/main/java --include="*.java" | head -10
# 检查前端通知是否受影响
grep -rn "notification\|ElNotification" frontend/src/ --include="*.ts" --include="*.vue" | head -10
```

### 状态机检查
如果修改了状态相关代码，确认所有状态分支都已处理。

### 输出验证报告
```
✅ 编译检查: 通过/失败
✅ API契约: 一致/不一致
✅ 字段一致性: 一致/不一致
✅ 跨模块一致性: 已处理/有遗漏
✅ 状态机: 完整/有遗漏
```
