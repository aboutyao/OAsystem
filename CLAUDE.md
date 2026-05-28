# OA System - Claude Code 规范

## 架构概览
- **后端**: Spring Boot 3.3 + Java 17 + MyBatis-Plus + Flowable 7.0
- **前端**: Vue 3.5 + TypeScript + Element Plus + Pinia + ECharts
- **数据库**: MySQL 8.0 + Redis 7 + Flyway 迁移
- **部署**: Docker Compose (6 服务: mysql/redis/minio/backend/frontend/mailhog)

## 产品设计哲学：预测性 + 主动性 + 个性化

**功能设计的核心判断标准：**

| 维度 | 差的实现（被动） | 好的实现（智能） |
|------|-----------------|-----------------|
| 预测性 | 展示历史数据 | 预测趋势："年假将在 8 月耗尽" |
| 主动性 | 等用户操作 | 主动推送："SLA 还剩 2 小时" |
| 个性化 | 所有人看同一界面 | 按行为习惯调整：快捷入口按使用频率排序 |

**设计功能时的三问：**
1. 系统能否**预测**会发生什么？（趋势分析、风险预警）
2. 系统能否**主动**推送信息？（不等用户问，主动告诉用户）
3. 系统能否**个性化**体验？（不同角色看不同内容，不同用户有不同建议）

**反模式（必须避免）：**
- ❌ 只展示数据表格（用户自己会看）
- ❌ 等用户点按钮才响应（应该自动触发）
- ❌ 所有用户看到相同界面（应该千人千面）
- ❌ 事后统计（应该事前预警）
- ❌ 静态规则（应该基于历史数据动态调整）

**口诀**: 功能不只"能用"，要"好用"再到"聪明"。

## 七大核心原则

### 原则 1: 跨模块影响分析
修改任何代码前，必须思考三个层次的影响：
1. **直接调用方** — 谁调用了这个方法？
2. **跨模块间接影响** — 这个模块被哪些其他模块依赖？修改是否影响它们？
   - 改 MessageService → 影响 WorkflowService/EmailService/SSE/NotificationCenter
   - 改 OaLeaveMapper → 影响 LeaveService/DashboardService/CalendarService/ReportService
3. **前端级联** — 后端 API 变更是否影响前端的 API 调用、SSE 事件、UI 展示？

**口诀**: 改一处，想三层。

### 原则 2: 数据安全第一
- 删除用软删除 (`deleted=1`)，永远不硬删
- 迁移前备份，迁移后验证
- 有外键关联的删除必须处理级联
- 不可逆操作（DROP TABLE）需要二次确认

### 原则 3: 向后兼容
- 新增字段不删旧字段
- 新增 API 不改旧 API 签名
- 需要 breaking change 时给过渡期（旧版本保留至少一个大版本）
- 删除或重命名字段时，先检查前端是否引用

### 原则 4: 错误不静默
- catch 块必须有处理（日志/上抛/用户提示）
- 禁止空 `catch {}` 和 `catch { // ignore }`
- 前端必须给用户反馈（`ElMessage.error`）
- 后端异常必须记录到 `app_exception_log`

### 原则 5: 安全默认
- 每个新 Controller 端点必须加 `@PreAuthorize`
- 每个用户输入必须校验（`@NotBlank`/`@NotNull` 等）
- SQL 必须用参数化查询，禁止字符串拼接
- 敏感数据（密码/密钥）不得硬编码

### 原则 6: 性能有底线
- 每个查询必须有明确的过滤意图（WHERE 条件）
  - 软删除表必须过滤 `deleted=0`
  - 多角色系统必须按数据权限过滤（SELF/DEPT/ALL）
  - 业务表会持续增长，全表扫描会拖垮数据库
  - 统计聚合查询可以不加 WHERE，但必须有注释说明原因
- 列表查询必须有分页
- 禁止 `selectList(null)` 全量加载
- 循环内禁止查库（N+1 问题）

### 原则 7: 可观测性
- 关键操作必须有审计日志（`@Auditable` 或 `auditService.safeRecordOperation`）
- 异常必须记录到 `app_exception_log`
- 状态变更必须记录到操作日志
- 新增 API → 更新 README 的接口表
- 新增环境变量 → 更新 `.env.example`
- 修改行为 → 更新 `ARCHITECTURE.md`

## 代码规范

### Java 后端
- Service 类必须加 `@Service`，只读方法加 `@Transactional(readOnly = true)`
- 所有 ID 使用 `SequenceService.nextId(tableName)` 生成
- Entity → Map 转换统一使用 `OaEntityMapper.toMap(entity)`
- Controller 使用 `@RequestMapping("/api/xxx")` 前缀
- 异常使用 `BusinessException(ErrorCode.XXX, "中文消息")`
- Mapper 使用 `@Mapper` + `BaseMapper<Entity>` 或 `@Select`/`@Update` 注解

### Vue 前端
- 组件使用 `<script setup lang="ts">` + Composition API
- API 调用统一通过 `http` (src/api/http.ts) 封装的 Axios 实例
- 状态管理使用 Pinia (`defineStore`)
- 样式使用 CSS 变量 (`--oa-primary`, `--oa-bg-white` 等)，确保暗色模式兼容
- 组件 props 使用 `defineProps<{...}>()` 类型声明

### 数据库迁移
- 迁移文件命名: `V{N}__描述.sql` (N 递增，当前最大 V45)
- 新增角色/权限的 ID 必须从现有最大值之后开始
- 种子数据使用 `INSERT ... ON DUPLICATE KEY UPDATE` 确保幂等

## 提交前检查清单

每次修改代码后，必须确认：

1. **编译**: `cd backend && mvn compile -q` 无错误
2. **前端**: `cd frontend && npx vue-tsc --noEmit` 无类型错误
3. **Git**: 不提交 `.env`、`node_modules/`、`target/`、`*.local`
4. **权限**: 新增角色权限时，ID 不与 V2-V45 中的现有 ID 冲突
5. **CSS**: 新组件使用 CSS 变量，不硬编码颜色值
6. **API**: 新增后端接口时同步更新前端 API 文件 (src/api/*.ts)
7. **安全**: 新端点是否有 @PreAuthorize？输入是否有校验？
8. **审计**: 关键操作是否有审计日志？
9. **文档**: README 接口表是否更新？新增环境变量是否记录？

## 已知陷阱

- `WorkflowDocumentSyncer` 已删除，不要引用
- `SysSequenceMapper.incrementAndGet()` 仅用于 MySQL 8+，使用 `SELECT ... FOR UPDATE` 模式
- `ApprovalRuleEngine.findUserByRole()` 使用 `UserMapper.selectUserIdByRoleCode()` 查询，不要回退到全量扫描
- 前端 `PlaceholderView` 被权限系统引用为组件字符串，不能删除
- `.env` 已被 gitignore 但历史中可能有残留，生产环境必须轮换密码

## 常用命令

```bash
# 后端编译
cd backend && mvn compile -q

# 后端测试
cd backend && mvn test

# 前端类型检查
cd frontend && npx vue-tsc --noEmit

# 前端构建
cd frontend && npm run build

# Docker 部署
./deploy.sh init    # 首次
./deploy.sh         # 更新
```
