<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Check, Close, Setting } from '@element-plus/icons-vue'

interface Plugin {
  id: number
  name: string
  version: string
  description: string
  author: string
  status: string
  createdAt: string
}

const loading = ref(false)
const plugins = ref<Plugin[]>([])

async function loadPlugins() {
  loading.value = true
  try {
    const response = await fetch('/api/plugins')
    const data = await response.json()
    plugins.value = data.data || []
  } catch (e) {
    console.error('Failed to load plugins:', e)
  } finally {
    loading.value = false
  }
}

async function togglePlugin(plugin: Plugin) {
  const action = plugin.status === 'ENABLED' ? 'disable' : 'enable'
  const actionText = action === 'enable' ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(`确定要${actionText}插件 "${plugin.name}" 吗？`, `确认${actionText}`, { type: 'warning' })
    await fetch(`/api/plugins/${plugin.id}/${action}`, { method: 'POST' })
    ElMessage.success(`${actionText}成功`)
    loadPlugins()
  } catch (e) {
    // 取消
  }
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(loadPlugins)
</script>

<template>
  <div class="plugin-manager">
    <div class="manager-header">
      <h3>插件管理</h3>
    </div>

    <el-table v-loading="loading" :data="plugins" stripe>
      <el-table-column prop="name" label="插件名称" />
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="author" label="作者" width="100" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : row.status === 'DISABLED' ? 'info' : 'warning'" size="small">
            {{ row.status === 'ENABLED' ? '已启用' : row.status === 'DISABLED' ? '已禁用' : '已注册' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button
            v-if="row.status !== 'ENABLED'"
            type="success"
            link
            :icon="Check"
            @click="togglePlugin(row)"
          >启用</el-button>
          <el-button
            v-else
            type="warning"
            link
            :icon="Close"
            @click="togglePlugin(row)"
          >禁用</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.plugin-manager {
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
