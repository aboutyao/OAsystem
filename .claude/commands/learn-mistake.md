---
description: 🧠 从错误中学习 — 分析bug修复，提取模式，防止重蹈覆辙
allowed-tools: Bash, Read, Grep, Glob
---

# 🧠 Learn from Mistakes

分析最近的 bug 修复，提取可复用的错误模式。

## Step 1: 发现最近的修复
```bash
git log --oneline -20 | grep -i "fix\|bug\|修复\|hotfix"
```

## Step 2: 分析每个修复
对每个 fix commit:
```bash
git show <commit_hash> --stat
git show <commit_hash> -- backend/ frontend/
```
提取: 修复了什么? 根因是什么? 修复模式是什么?

## Step 3: 提取错误模式
将修复归纳为可检测的模式，例如:
- selectList(null) 全量扫描 → 改为条件查询
- long 与 null 比较 → 改为 Long 包装类
- catch 块吞异常 → 添加日志或上抛
- 前后端字段不一致 → 同步更新 api/*.ts
- 缺少软删除过滤 → 添加 deleted=0

## Step 4: 全局扫描同类问题
```bash
echo "=== selectList(null) ==="
grep -rn "selectList(null)" backend/src/main/java --include="*.java"
echo "=== 空catch ==="
grep -rn "catch.*{" backend/src/main/java --include="*.java" -A 1 | grep -B1 "continue\|return null" | head -10
echo "=== 前端空catch ==="
grep -rn "catch.*{}" frontend/src/ --include="*.ts" --include="*.vue" | head -10
```

## Step 5: 输出报告
```
🧠 错误学习报告
═══════════════════════════════════════
最近修复: X 个
提取模式: Y 个
全局扫描:
  - [模式1]: N 处同类问题
  - [模式2]: N 处同类问题

📋 建议修复:
  1. 文件:行号 — 问题描述
  2. 文件:行号 — 问题描述
═══════════════════════════════════════
```
