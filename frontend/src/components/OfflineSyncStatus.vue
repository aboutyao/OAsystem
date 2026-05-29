<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Connection, Refresh, Warning } from '@element-plus/icons-vue'

const isOnline = ref(navigator.onLine)
const syncStatus = ref({ pendingCount: 0, syncedCount: 0, failedCount: 0 })
const isSyncing = ref(false)

function updateOnlineStatus() {
  isOnline.value = navigator.onLine
  if (isOnline.value) {
    syncOfflineData()
  }
}

async function loadSyncStatus() {
  try {
    const response = await fetch('/api/offline/status')
    const data = await response.json()
    syncStatus.value = data.data || { pendingCount: 0, syncedCount: 0, failedCount: 0 }
  } catch (e) {
    console.error('Failed to load sync status:', e)
  }
}

async function syncOfflineData() {
  if (!isOnline.value || isSyncing.value) return

  isSyncing.value = true
  try {
    const response = await fetch('/api/offline/sync', { method: 'POST' })
    const result = await response.json()

    if (result.data?.successCount > 0) {
      ElMessage.success(`同步成功 ${result.data.successCount} 条`)
    }
    if (result.data?.failCount > 0) {
      ElMessage.warning(`${result.data.failCount} 条同步失败`)
    }

    loadSyncStatus()
  } catch (e) {
    ElMessage.error('同步失败')
  } finally {
    isSyncing.value = false
  }
}

onMounted(() => {
  loadSyncStatus()
  window.addEventListener('online', updateOnlineStatus)
  window.addEventListener('offline', updateOnlineStatus)
})

onUnmounted(() => {
  window.removeEventListener('online', updateOnlineStatus)
  window.removeEventListener('offline', updateOnlineStatus)
})
</script>

<template>
  <div class="offline-sync-status" :class="{ 'is-offline': !isOnline }">
    <div class="status-indicator">
      <el-icon v-if="isOnline" color="#67C23A"><Connection /></el-icon>
      <el-icon v-else color="#F56C6C"><Warning /></el-icon>
      <span>{{ isOnline ? '在线' : '离线' }}</span>
    </div>

    <div v-if="syncStatus.pendingCount > 0" class="pending-info">
      <span class="pending-count">{{ syncStatus.pendingCount }}</span> 条待同步
      <el-button
        v-if="isOnline"
        type="primary"
        size="small"
        :icon="Refresh"
        :loading="isSyncing"
        @click="syncOfflineData"
      >
        同步
      </el-button>
    </div>

    <div v-if="syncStatus.failedCount > 0" class="failed-info">
      <el-icon color="#F56C6C"><Warning /></el-icon>
      {{ syncStatus.failedCount }} 条同步失败
    </div>
  </div>
</template>

<style scoped>
.offline-sync-status {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  font-size: 13px;
}

.offline-sync-status.is-offline {
  background: var(--el-color-danger-light-9);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.pending-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pending-count {
  font-weight: bold;
  color: var(--el-color-primary);
}

.failed-info {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--el-color-danger);
}
</style>
