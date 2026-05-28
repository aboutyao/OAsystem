---
description: 🔍 深度审查 — 架构/安全/性能/数据流四维度
allowed-tools: Bash, Read, Grep, Glob
---

# 🔍 Deep Review

对当前变更进行四维度深度审查。

## 维度 1: 架构审查
```bash
echo "=== 模块间依赖 ==="
for mod in workflow contract meeting file asset message oa; do
  deps=$(grep -rn "import com.company.oa" backend/src/main/java/com/company/oa/$mod/ --include="*.java" 2>/dev/null | grep -v "import com.company.oa.$mod" | grep -oP 'import com\.company\.oa\.\w+' | sort -u | tr '\n' ', ')
  [ -n "$deps" ] && echo "$mod → $deps"
done
```

## 维度 2: 安全审查
```bash
echo "=== SQL注入风险 ==="
grep -rn "concat\|\"+.*+\".*SELECT\|\"+.*+\".*WHERE" backend/src/main/java --include="*.java" | head -5
echo "=== 敏感信息 ==="
grep -rn "password\|secret\|token" backend/src/main/java --include="*.java" | grep -v "test\|Test\|hash\|Hash\|//" | head -5
echo "=== 权限注解 ==="
grep -rn "@PreAuthorize" backend/src/main/java --include="*.java" | wc -l
```

## 维度 3: 性能审查
```bash
echo "=== N+1查询风险 ==="
grep -rn "for.*{" backend/src/main/java --include="*.java" -A 3 | grep -A 2 "Mapper\|selectList\|selectById" | head -10
echo "=== 无分页大查询 ==="
grep -rn "selectList" backend/src/main/java --include="*.java" | grep -v "limit\|LIMIT\|page\|Page" | head -5
```

## 维度 4: 数据流审查
```bash
echo "=== 事务边界 ==="
grep -rn "@Transactional" backend/src/main/java --include="*.java" | grep -v "readOnly" | head -10
echo "=== 状态同步 ==="
grep -rn "syncOaDocument\|updateStatus" backend/src/main/java --include="*.java" | head -10
```

## 输出报告
```
🔍 Deep Review Report
═══════════════════════════════════════
Architecture: [PASS/WARN/FAIL] — [详情]
Security: [PASS/WARN/FAIL] — [详情]
Performance: [PASS/WARN/FAIL] — [详情]
Data Flow: [PASS/WARN/FAIL] — [详情]
═══════════════════════════════════════
```
