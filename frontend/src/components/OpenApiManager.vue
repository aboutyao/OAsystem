<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Document, Key } from '@element-plus/icons-vue'

interface ApiApp {
  id: number
  appName: string
  appKey: string
  status: string
  callCount: number
  createdAt: string
}

const loading = ref(false)
const apps = ref<ApiApp[]>([])
const showAddDialog = ref(false)
const showTokenDialog = ref(false)
const currentToken = ref('')

const newApp = ref({
  appName: '',
  description: '',
  callbackUrl: '',
})

async function loadApps() {
  loading.value = true
  try {
    const response = await fetch('/api/open-api/apps')
    const data = await response.json()
    apps.value = data.data || []
  } catch (e) {
    console.error('Failed to load apps:', e)
  } finally {
    loading.value = false
  }
}

async function addApp() {
  if (!newApp.value.appName) {
    ElMessage.warning('请填写应用名称')
    return
  }

  try {
    const response = await fetch('/api/open-api/apps', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newApp.value),
    })
    const result = await response.json()

    ElMessage.success('添加成功')
    showAddDialog.value = false
    newApp.value = { appName: '', description: '', callbackUrl: '' }
    loadApps()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

async function getToken(app: ApiApp) {
  try {
    const response = await fetch(`/api/open-api/token?appKey=${app.appKey}&appSecret=${app.appKey}`, {
      method: 'POST',
    })
    const result = await response.json()

    if (result.data?.accessToken) {
      currentToken.value = result.data.accessToken
      showTokenDialog.value = true
    } else {
      ElMessage.error('获取令牌失败')
    }
  } catch (e) {
    ElMessage.error('获取令牌失败')
  }
}

function copyToken() {
  navigator.clipboard.writeText(currentToken.value)
  ElMessage.success('已复制到剪贴板')
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(loadApps)
</script>

<template>
  <div class="open-api-manager">
    <div class="manager-header">
      <h3>API 开放平台</h3>
      <el-button type="primary" :icon="Plus" @click="showAddDialog = true">注册应用</el-button>
    </div>

    <el-table v-loading="loading" :data="apps" stripe>
      <el-table-column prop="appName" label="应用名称" />
      <el-table-column prop="appKey" label="AppKey" show-overflow-tooltip />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="callCount" label="调用次数" />
      <el-table-column prop="createdAt" label="创建时间">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button type="primary" link :icon="Key" @click="getToken(row)">获取令牌</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAddDialog" title="注册 API 应用" width="500px">
      <el-form label-width="100px">
        <el-form-item label="应用名称" required>
          <el-input v-model="newApp.appName" placeholder="应用名称" />
        </el-form-item>
        <el-form-item label="应用描述">
          <el-input v-model="newApp.description" type="textarea" placeholder="应用描述" />
        </el-form-item>
        <el-form-item label="回调URL">
          <el-input v-model="newApp.callbackUrl" placeholder="https://example.com/callback" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addApp">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showTokenDialog" title="访问令牌" width="500px">
      <div class="token-content">
        <p>请妥善保管您的访问令牌：</p>
        <el-input v-model="currentToken" readonly>
          <template #append>
            <el-button @click="copyToken">复制</el-button>
          </template>
        </el-input>
        <p class="token-tip">令牌有效期为 2 小时，过期后需要重新获取。</p>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.open-api-manager {
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

.token-content p {
  margin-bottom: 12px;
  color: var(--el-text-color-regular);
}

.token-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
