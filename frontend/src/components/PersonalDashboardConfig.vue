<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Plus, Delete, View, Warning, UploadFilled } from '@element-plus/icons-vue'

interface DashboardModule {
  key: string
  name: string
  visible: boolean
  sortOrder: number
}

const loading = ref(false)
const modules = ref<DashboardModule[]>([])
const isEditing = ref(false)

const availableModules = [
  { key: 'todos', name: '我的待办' },
  { key: 'started', name: '我发起的' },
  { key: 'cc', name: '抄送我的' },
  { key: 'messages', name: '消息中心' },
  { key: 'calendar', name: '日程' },
  { key: 'leave-balance', name: '假期余额' },
  { key: 'quick-actions', name: '快捷入口' },
  { key: 'notices', name: '公告通知' },
  { key: 'recent-approvals', name: '最近审批' },
]

async function loadConfig() {
  loading.value = true
  try {
    const response = await fetch('/api/dashboard/config')
    const data = await response.json()
    modules.value = data.data?.modules || getDefaultModules()
  } catch (e) {
    modules.value = getDefaultModules()
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  try {
    await fetch('/api/dashboard/config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ modules: modules.value }),
    })
    ElMessage.success('保存成功')
    isEditing.value = false
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

function toggleVisibility(module: DashboardModule) {
  module.visible = !module.visible
}

function moveUp(index: number) {
  if (index > 0) {
    const temp = modules.value[index]
    modules.value[index] = modules.value[index - 1]
    modules.value[index - 1] = temp
    updateSortOrder()
  }
}

function moveDown(index: number) {
  if (index < modules.value.length - 1) {
    const temp = modules.value[index]
    modules.value[index] = modules.value[index + 1]
    modules.value[index + 1] = temp
    updateSortOrder()
  }
}

function updateSortOrder() {
  modules.value.forEach((m, i) => m.sortOrder = i)
}

function getDefaultModules(): DashboardModule[] {
  return availableModules.map((m, i) => ({
    ...m,
    visible: true,
    sortOrder: i,
  }))
}

onMounted(loadConfig)
</script>

<template>
  <div class="personal-dashboard-config">
    <div class="config-header">
      <h3>个性化仪表盘</h3>
      <el-button v-if="!isEditing" type="primary" :icon="Document" @click="isEditing = true">自定义</el-button>
      <template v-else>
        <el-button @click="isEditing = false">取消</el-button>
        <el-button type="primary" @click="saveConfig">保存</el-button>
      </template>
    </div>

    <div v-loading="loading" class="modules-list">
      <div
        v-for="(module, index) in modules"
        :key="module.key"
        class="module-item"
        :class="{ 'is-hidden': !module.visible }"
      >
        <div class="module-info">
          <span class="module-name">{{ module.name }}</span>
          <el-tag v-if="!module.visible" type="info" size="small">已隐藏</el-tag>
        </div>

        <div v-if="isEditing" class="module-actions">
          <el-button text :icon="View" @click="toggleVisibility(module)" />
          <el-button text :disabled="index === 0" @click="moveUp(index)">↑</el-button>
          <el-button text :disabled="index === modules.length - 1" @click="moveDown(index)">↓</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.personal-dashboard-config {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.config-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.config-header h3 {
  margin: 0;
}

.modules-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.module-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: white;
  border-radius: 8px;
  transition: all 0.2s;
}

.module-item.is-hidden {
  opacity: 0.5;
}

.module-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.module-name {
  font-weight: 500;
}

.module-actions {
  display: flex;
  gap: 4px;
}
</style>
