---
description: 快速编译检查 — 后端+前端一步到位
allowed-tools: Bash
---

# 快速编译检查

执行后端和前端的编译/类型检查，输出简洁结果。

## 步骤

1. 后端编译：
```bash
cd /home/ubuntu/OAsystem/backend && mvn compile -q 2>&1 | tail -10
```
- 无输出 = 通过
- 有 ERROR = 列出错误

2. 前端类型检查：
```bash
cd /home/ubuntu/OAsystem/frontend && npx vue-tsc --noEmit 2>&1 | tail -20
```
- 无输出 = 通过
- 有 error = 列出错误

3. 输出总结：
```
后端编译: ✅/❌
前端类型: ✅/❌
```

如果有错误，给出修复建议和涉及的文件。
