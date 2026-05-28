---
description: 🛡️ 智能系统守护 — 12维度全面检查
allowed-tools: Bash, Read, Grep, Glob
---

# 🛡️ OA Guardian

执行 12 维度深度检查，输出健康报告。

## Phase 1: 系统理解
```bash
echo "=== 系统概况 ==="
echo "Services: $(find backend/src/main/java -name '*Service.java' | wc -l)"
echo "Controllers: $(find backend/src/main/java -name '*Controller.java' | wc -l)"
echo "Mappers: $(find backend/src/main/java -name '*Mapper.java' | wc -l)"
echo "前端API: $(find frontend/src/api -name '*.ts' | wc -l)"
echo "前端页面: $(find frontend/src/views -name '*.vue' -type f | wc -l)"
echo "后端测试: $(find backend/src/test -name '*Test.java' | wc -l)"
echo "前端测试: $(find frontend/src -name '*.test.*' -o -name '*.spec.*' | wc -l)"
echo "数据库迁移: $(ls backend/src/main/resources/db/migration/V*.sql | wc -l)"
```

## Phase 2: 12 维度检查

### #1 前后端契约一致性
```bash
grep -rn "RequestMapping\|GetMapping\|PostMapping\|PutMapping\|DeleteMapping" backend/src/main/java --include="*.java" | grep -oP '"/api/[^"]+' | sort -u > /tmp/be_apis.txt
grep -rn "http\.\(get\|post\|put\|delete\)" frontend/src/api/ --include="*.ts" | grep -oP "'/[^']+'" | sort -u > /tmp/fe_apis.txt
echo "后端API: $(wc -l < /tmp/be_apis.txt) 个"
echo "前端API: $(wc -l < /tmp/fe_apis.txt) 个"
echo "=== 差异 ==="
diff /tmp/be_apis.txt /tmp/fe_apis.txt 2>/dev/null || true
```

### #2 事务和资源安全
```bash
echo "=== 吞掉异常的catch ==="
grep -rn "catch.*{" backend/src/main/java --include="*.java" -A 2 | grep -B1 "continue\|return null\|// ignore" | head -10
echo "=== SSE连接 ==="
grep -rn "SseEmitter" backend/src/main/java --include="*.java"
```

### #3 并发安全
```bash
echo "=== 全量扫描 selectList(null) ==="
grep -rn "selectList(null)" backend/src/main/java --include="*.java"
```

### #4 Flyway迁移安全
```bash
echo "=== 最新迁移 ==="
ls backend/src/main/resources/db/migration/V*.sql | sort -t'V' -k2 -n | tail -3
echo "=== DROP操作 ==="
grep -rni "DROP TABLE\|DROP COLUMN" backend/src/main/resources/db/migration/ || echo "无DROP ✅"
```

### #5 API破坏性变更
```bash
git diff HEAD~3 -- backend/src/main/java 2>/dev/null | grep -E "^[-+].*Mapping" | head -10
git diff HEAD~3 -- frontend/src/api/ 2>/dev/null | grep -E "^[-+].*(interface|type|function)" | head -10
```

### #6 测试覆盖
```bash
for s in $(find backend/src/main/java -name "*Service.java" -exec basename {} .java \;); do
  t=$(find backend/src/test -name "${s}Test.java" 2>/dev/null)
  [ -z "$t" ] && echo "❌ 无测试: $s"
done
```

### #7 数据流完整性
```bash
grep -n "syncOaDocument\|updateStatus" backend/src/main/java -r --include="*.java" | head -10
```

### #8 业务逻辑正确性
```bash
grep -rn "resolveRoleAssignees\|findUserByRole\|selectUserIdByRoleCode" backend/src/main/java --include="*.java"
```

### #9 静默失败
```bash
echo "=== 后端空catch ==="
grep -rn "catch.*{" backend/src/main/java --include="*.java" -A 1 | grep -B1 "continue\|return null" | head -10
echo "=== 前端空catch ==="
grep -rn "catch.*{}" frontend/src/ --include="*.ts" --include="*.vue" | head -10
```

### #10 数据一致性
```bash
grep -rn "deleteById\|\.delete(" backend/src/main/java --include="*.java" | grep -v test | head -10
```

### #11 环境差异
```bash
grep -rn "localhost\|127.0.0.1" backend/src/main/ --include="*.java" --include="*.yml" | grep -v test | head -10
```

### #12 文档同步
```bash
echo "README API数: $(grep -oP '\| `/api/[^`]+`' README.md 2>/dev/null | sort -u | wc -l)"
echo "实际API数: $(grep -rn 'RequestMapping' backend/src/main/java --include='*.java' | grep -oP '"/api/[^"]+' | sort -u | wc -l)"
```

## Phase 3: 输出报告
汇总结果，给出健康分数和修复建议。
