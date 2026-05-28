---
description: 🔮 智能变更 — 先理解再修改，修改后验证
allowed-tools: Bash, Read, Grep, Glob, Edit, Write
---

# 🔮 Smart Change

修改代码前先理解上下文，修改后验证正确性。

## Phase 1: 理解（修改前）

用户告诉你要修改的目标后，执行:

1. 读取目标文件完整内容
2. 找到所有调用方:
```bash
grep -rn "目标方法名" backend/src/main/java --include="*.java" | grep -v "import\|//\|test"
grep -rn "目标方法名" frontend/src/ --include="*.ts" --include="*.vue"
```
3. 找到依赖的 Mapper/Entity
4. 读取相关测试文件（如果存在）
5. 输出影响报告:
```
📊 影响分析:
  目标: ClassName.methodName()
  调用方: [列表]
  依赖: [列表]
  测试覆盖: [有/无]
  ⚠️ 风险点: [描述]
```
6. **等待用户确认后再执行修改**

## Phase 2: 验证（修改后）

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

### 状态机检查
如果修改了状态相关代码，确认所有状态分支都已处理。

### 输出验证报告
```
✅ 编译检查: 通过/失败
✅ API契约: 一致/不一致
✅ 字段一致性: 一致/不一致
✅ 状态机: 完整/有遗漏
```
