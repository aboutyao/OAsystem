<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Plus, Delete, View, Warning, UploadFilled } from '@element-plus/icons-vue'

interface NotificationSetting {
  type: string
  name: string
  enabled: boolean
}

const loading = ref(false)
const settings = ref<NotificationSetting[]>([
  { type: 'WORKFLOW_APPROVAL', name: '流程审批通知', enabled: true },
  { type: 'WORKFLOW_COMMENT', name: '流程评论通知', enabled: true },
  { type: 'LEAVE_APPROVAL', name: '请假审批通知', enabled: true },
  { type: 'EXPENSE_APPROVAL', name: '报销审批通知', enabled: true },
  { type: 'MESSAGE_MENTION', name: '@提及通知', enabled: true },
  { type: 'SYSTEM_ANNOUNCEMENT', name: '系统公告', enabled: true },
])

async function loadSettings() {
  loading.value = true
  try {
    const response = await fetch('/api/notifications/settings')
    const data = await response.json()
    if (data.data) {
      settings.value = settings.value.map(s => ({
        ...s,
        enabled: data.data[s.type] !== false
      }))
    }
  } catch (e) {
    console.error('Failed to load settings:', e)
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  try {
    const settingsMap: Record<string, boolean> = {}
    settings.value.forEach(s => {
      settingsMap[s.type] = s.enabled
    })

    await fetch('/api/notifications/settings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(settingsMap),
    })
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

onMounted(loadSettings)
</script>

<template>
  <div class="notification-settings">
    <div class="settings-header">
      <h3>通知设置</h3>
      <el-button type="primary" @click="saveSettings">保存设置</el-button>
    </div>

    <div v-loading="loading" class="settings-list">
      <div v-for="setting in settings" :key="setting.type" class="setting-item">
        <div class="setting-info">
          <el-icon><Warning /></el-icon>
          <span>{{ setting.name }}</span>
        </div>
        <el-switch v-model="setting.enabled" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.notification-settings {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.settings-header h3 {
  margin: 0;
}

.settings-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: white;
  border-radius: 8px;
}

.setting-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
