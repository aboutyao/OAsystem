---
description: 全面质量保障检查 — 编译/类型/权限/安全/规范
allowed-tools: Bash, Read, Grep, Glob
---

# 质量保障检查 (Quality Assurance)

对当前变更进行全面质量检查。按以下步骤逐一执行，每步输出结果（通过/失败/警告）。

## 检查流程

### Step 1: 变更分析
- 运行 `git diff --name-only` 和 `git diff --stat` 查看所有变更文件
- 分类：后端Java / 前端Vue-TS / SQL迁移 / 配置文件 / 测试
- 识别变更影响的模块

### Step 2: 后端编译检查
- 如果有 Java 文件变更：`cd backend && mvn compile -q 2>&1`
- 检查输出是否有 ERROR
- 如果编译失败，列出具体错误行号和原因

### Step 3: 前端类型检查
- 如果有 .vue 或 .ts 文件变更：`cd frontend && npx vue-tsc --noEmit 2>&1`
- 检查输出是否有 error TS
- 如果类型检查失败，列出具体错误

### Step 4: SQL 迁移安全检查
- 如果有 `V*__*.sql` 文件新增：
  - 检查 ID 是否与现有迁移冲突（grep 最大 ID）
  - 检查是否有 DROP TABLE（危险操作）
  - 检查 INSERT 是否有 ON DUPLICATE KEY（幂等性）

### Step 5: 安全检查
- 检查是否有 `.env` 文件被 git add
- 检查是否有硬编码密码/密钥（grep -r "password.*=" --include="*.java" --include="*.ts"）
- 检查 CORS 配置是否允许 `*`
- 检查是否有 SQL 拼接（grep "concat\|+.*+.*SELECT" --include="*.java"）

### Step 6: 规范检查
- 新增的 Java Service 是否有 `@Service` 注解
- 新增的 Vue 组件是否使用 `<script setup lang="ts">`
- 新增的 CSS 是否使用变量而非硬编码颜色
- 新增 API 是否前后端同步（后端 Controller + 前端 api/*.ts）

### Step 7: 测试覆盖检查
- 如果新增了 Service 类，检查是否有对应测试
- 如果修改了现有 Service，检查测试是否需要更新

### Step 8: 提交检查
- 检查 git status 中是否有不该提交的文件（.env, node_modules, target）
- 检查是否有未跟踪的敏感文件

## 输出格式

```
=== QA Report ===
[PASS/FAIL/WARN] Step 1: 变更分析 — X 个文件变更
[PASS/FAIL/WARN] Step 2: 后端编译
[PASS/FAIL/WARN] Step 3: 前端类型检查
[PASS/FAIL/WARN] Step 4: SQL 迁移安全
[PASS/FAIL/WARN] Step 5: 安全检查
[PASS/FAIL/WARN] Step 6: 规范检查
[PASS/FAIL/WARN] Step 7: 测试覆盖
[PASS/FAIL/WARN] Step 8: 提交检查

总计: X 通过, Y 失败, Z 警告
```

如果有 FAIL 项，给出具体修复建议。
