<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getHealth,
  listBackupRecords,
  listExceptions,
  listJobLogs,
  listOnlineUsers,
  refreshCache,
  type HealthStatus,
} from '../../api/ops'
import type { JsonObject, PageResponse } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const health = ref<HealthStatus | null>(null)
const onlineUsers = ref<JsonObject[]>([])

const jobLogs = ref<JsonObject[]>([])
const jobLogTotal = ref(0)
const jobLogPage = ref(1)
const jobLogSize = ref(20)
const jobLogStatus = ref<string>('')

const exceptions = ref<JsonObject[]>([])
const excTotal = ref(0)
const excPage = ref(1)
const excSize = ref(20)
const excSeverity = ref<string>('')

const backups = ref<JsonObject[]>([])
const backupTotal = ref(0)
const backupPage = ref(1)
const backupSize = ref(20)
const backupStatus = ref<string>('')

const loading = ref(false)
const refreshing = ref(false)

function unwrap<T>(p: PageResponse<T>): { items: T[]; total: number } {
  return { items: p.items ?? [], total: p.total ?? 0 }
}

async function loadHealth() {
  health.value = await getHealth()
  onlineUsers.value = await listOnlineUsers().catch(() => [] as JsonObject[])
}

async function loadJobLogs() {
  const r = await listJobLogs({
    page: jobLogPage.value,
    size: jobLogSize.value,
    status: jobLogStatus.value || undefined,
  }).catch(() => ({ page: 1, size: 20, total: 0, items: [] }) as PageResponse<JsonObject>)
  const u = unwrap(r)
  jobLogs.value = u.items
  jobLogTotal.value = u.total
}

async function loadExceptions() {
  const r = await listExceptions({
    page: excPage.value,
    size: excSize.value,
    severity: excSeverity.value || undefined,
  }).catch(() => ({ page: 1, size: 20, total: 0, items: [] }) as PageResponse<JsonObject>)
  const u = unwrap(r)
  exceptions.value = u.items
  excTotal.value = u.total
}

async function loadBackups() {
  const r = await listBackupRecords({
    page: backupPage.value,
    size: backupSize.value,
    status: backupStatus.value || undefined,
  }).catch(() => ({ page: 1, size: 20, total: 0, items: [] }) as PageResponse<JsonObject>)
  const u = unwrap(r)
  backups.value = u.items
  backupTotal.value = u.total
}

async function load() {
  loading.value = true
  try {
    await Promise.all([loadHealth(), loadJobLogs(), loadExceptions(), loadBackups()])
  } finally {
    loading.value = false
  }
}

void load()

async function onRefreshCache() {
  refreshing.value = true
  try {
    await refreshCache()
    ElMessage.success('缓存已下发刷新指令')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    refreshing.value = false
  }
}

function statusType(status: string): 'success' | 'info' | 'warning' | 'danger' {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'RUNNING':
      return 'warning'
    case 'FAILED':
      return 'danger'
    default:
      return 'info'
  }
}

function severityType(severity: string): 'success' | 'info' | 'warning' | 'danger' {
  switch (severity) {
    case 'WARN':
      return 'warning'
    case 'INFO':
      return 'info'
    case 'CRITICAL':
      return 'danger'
    default:
      return 'danger'
  }
}
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">运维监控</h2>
        <p class="muted">健康状态、在线用户、任务/异常/备份四视图；当前在线根据最近 30 分钟登录估算。</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" :loading="refreshing" @click="onRefreshCache">下发缓存刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" class="ops-section">
      <template #header>系统健康</template>
      <el-descriptions :column="3" border v-if="health">
        <el-descriptions-item label="状态">
          <el-tag :type="health.status === 'UP' ? 'success' : 'danger'">{{ health.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="服务">{{ health.service }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatDisplayDateTime(health.time) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="ops-section">
      <template #header>在线用户（近 30 分钟）</template>
      <el-table :data="onlineUsers" stripe size="small" empty-text="近 30 分钟无活跃登录">
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="username" label="账号" width="160" />
        <el-table-column prop="realName" label="姓名" width="140" />
        <el-table-column prop="deptName" label="部门" min-width="160" />
        <el-table-column label="最近登录" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="ops-section">
      <template #header>
        <div class="card-header">
          <span>定时任务日志</span>
          <div class="filters">
            <el-select v-model="jobLogStatus" placeholder="状态" size="small" clearable style="width: 120px"
              @change="() => { jobLogPage = 1; loadJobLogs() }">
              <el-option label="SUCCESS" value="SUCCESS" />
              <el-option label="FAILED" value="FAILED" />
              <el-option label="RUNNING" value="RUNNING" />
            </el-select>
          </div>
        </div>
      </template>
      <el-table :data="jobLogs" stripe size="small" empty-text="暂无记录">
        <el-table-column prop="jobCode" label="任务编码" width="160" />
        <el-table-column prop="jobName" label="任务名" min-width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.startAt) }}</template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column prop="successCount" label="成功" width="80" />
        <el-table-column prop="failCount" label="失败" width="80" />
        <el-table-column prop="failReason" label="失败原因" min-width="200" show-overflow-tooltip />
      </el-table>
      <div class="pagination">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="jobLogTotal"
          v-model:current-page="jobLogPage"
          v-model:page-size="jobLogSize"
          :page-sizes="[10, 20, 50]"
          @current-change="loadJobLogs"
          @size-change="loadJobLogs"
        />
      </div>
    </el-card>

    <el-card shadow="never" class="ops-section">
      <template #header>
        <div class="card-header">
          <span>异常记录</span>
          <div class="filters">
            <el-select v-model="excSeverity" placeholder="严重度" size="small" clearable style="width: 120px"
              @change="() => { excPage = 1; loadExceptions() }">
              <el-option label="ERROR" value="ERROR" />
              <el-option label="WARN" value="WARN" />
              <el-option label="INFO" value="INFO" />
              <el-option label="CRITICAL" value="CRITICAL" />
            </el-select>
          </div>
        </div>
      </template>
      <el-table :data="exceptions" stripe size="small" empty-text="无异常">
        <el-table-column prop="id" label="#" width="80" />
        <el-table-column prop="severity" label="级别" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="severityType(row.severity)">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="exceptionClass" label="类型" width="220" show-overflow-tooltip />
        <el-table-column prop="exceptionMessage" label="消息" min-width="200" show-overflow-tooltip />
        <el-table-column prop="requestUri" label="请求" min-width="180" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.occurredAt) }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="excTotal"
          v-model:current-page="excPage"
          v-model:page-size="excSize"
          :page-sizes="[10, 20, 50]"
          @current-change="loadExceptions"
          @size-change="loadExceptions"
        />
      </div>
    </el-card>

    <el-card shadow="never" class="ops-section">
      <template #header>
        <div class="card-header">
          <span>备份记录</span>
          <div class="filters">
            <el-select v-model="backupStatus" placeholder="状态" size="small" clearable style="width: 120px"
              @change="() => { backupPage = 1; loadBackups() }">
              <el-option label="SUCCESS" value="SUCCESS" />
              <el-option label="FAILED" value="FAILED" />
              <el-option label="RUNNING" value="RUNNING" />
            </el-select>
          </div>
        </div>
      </template>
      <el-table :data="backups" stripe size="small" empty-text="无备份记录">
        <el-table-column prop="backupType" label="类型" width="140" />
        <el-table-column prop="backupPath" label="路径" min-width="240" show-overflow-tooltip />
        <el-table-column prop="backupSize" label="大小(B)" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.startedAt) }}</template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column prop="failReason" label="失败原因" min-width="180" show-overflow-tooltip />
      </el-table>
      <div class="pagination">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="backupTotal"
          v-model:current-page="backupPage"
          v-model:page-size="backupSize"
          :page-sizes="[10, 20, 50]"
          @current-change="loadBackups"
          @size-change="loadBackups"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.ops-section {
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
