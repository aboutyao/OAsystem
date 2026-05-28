# OA System - Claude Code 规范

## 架构概览
- **后端**: Spring Boot 3.3 + Java 17 + MyBatis-Plus + Flowable 7.0
- **前端**: Vue 3.5 + TypeScript + Element Plus + Pinia + ECharts
- **数据库**: MySQL 8.0 + Redis 7 + Flyway 迁移
- **部署**: Docker Compose (6 服务: mysql/redis/minio/backend/frontend/mailhog)

## 核心原则：跨模块影响分析

**修改任何代码前，必须思考三个层次的影响：**

1. **直接调用方** — 谁调用了这个方法？
2. **跨模块间接影响** — 这个模块被哪些其他模块依赖？修改是否影响它们？
   - 例: 改 MessageService → 影响 WorkflowService/EmailService/SSE/NotificationCenter
   - 例: 改 OaLeaveMapper → 影响 LeaveService/DashboardService/CalendarService/ReportService
3. **前端级联** — 后端 API 变更是否影响前端的 API 调用、SSE 事件、UI 展示？

**口诀**: 改一处，想三层。直接调用 → 跨模块依赖 → 前端引用。

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
