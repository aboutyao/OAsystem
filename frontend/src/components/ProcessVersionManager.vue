<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Rollback } from '@element-plus/icons-vue'

interface ProcessVersion {
  id: number
  versionNumber: number
  changeDescription: string
  status: string
  createdAt: string
  publishedAt: string | null
}

const props = defineProps<{
  templateId: number
}>()

const loading = ref(false)
const versions = ref<ProcessVersion[]>([])

async function loadVersions() {
  loading.value = true
  try {
    const response = await fetch(`/api/workflow/templates/${props.templateId}/versions`)
    const data = await response.json()
    versions.value = data.data || []
  } catch (e) {
    console.error('Failed to load versions:', e)
  } finally {
    loading.value = false
  }
}

async function createVersion() {
  try {
    const response = await fetch(`/api/workflow/templates/${props.templateId}/versions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ changeDescription: '新版本' }),
    })
    ElMessage.success('版本已创建')
    loadVersions()
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

async function publishVersion(version: ProcessVersion) {
  try {
    await ElMessageBox.confirm(`确定要发布版本 ${version.versionNumber} 吗？`, '确认发布', { type: 'warning' })
    await fetch(`/api/workflow/versions/${version.id}/publish`, { method: 'POST' })
    ElMessage.success('发布成功')
    loadVersions()
  } catch (e) {
    // 取消
  }
}

async function rollbackVersion(version: ProcessVersion) {
  try {
    await ElMessageBox.confirm(`确定要回滚到版本 ${version.versionNumber} 吗？`, '确认回滚', { type: 'warning' })
    await fetch(`/api/workflow/versions/${version.id}/rollback`, { method: 'POST' })
    ElMessage.success('回滚成功')
    loadVersions()
  } catch (e) {
    // 取消
  }
}

function getStatusType(status: string): 'success' | 'warning' | 'info' {
  switch (status) {
    case 'ACTIVE': return 'success'
    case 'DRAFT': return 'warning'
    default: return 'info'
  }
}

function getStatusLabel(status: string): string {
  switch (status) {
    case 'ACTIVE': return '已发布'
    case 'DRAFT': return '草稿'
    case 'INACTIVE': return '已停用'
    default: return status
  }
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(loadVersions)
</script>

<template>
  <div class="process-version-manager">
    <div class="manager-header">
      <h3>版本管理</h3>
      <el-button type="primary" :icon="Plus" @click="createVersion">创建新版本</el-button>
    </div>

    <el-table v-loading="loading" :data="versions" stripe>
      <el-table-column prop="versionNumber" label="版本号" width="80">
        <template #default="{ row }">v{{ row.versionNumber }}</template>
      </el-table-column>
      <el-table-column prop="changeDescription" label="变更说明" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="publishedAt" label="发布时间">
        <template #default="{ row }">{{ formatDate(row.publishedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'ACTIVE'" type="success" link :icon="Upload" @click="publishVersion(row)">发布</el-button>
          <el-button v-if="row.status !== 'ACTIVE'" type="warning" link :icon="Rollback" @click="rollbackVersion(row)">回滚</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.process-version-manager {
  padding: 16px;
}

.manager-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.manager-header h3 {
  margin: 0;
}
</style>
