<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getLibraryFile, listLibraryDownloadLogs } from '../../api/file-library'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const file = ref<JsonObject | null>(null)
const downloadLogs = ref<JsonObject[]>([])

const id = computed(() => Number(route.params.id))

async function load() {
  loading.value = true
  try {
    file.value = await getLibraryFile(id.value)
    downloadLogs.value = await listLibraryDownloadLogs(id.value)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/files')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">文件详情</h2>
        <p class="muted">查看文件信息与下载记录。</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/files')">返回列表</el-button>
      </div>
    </div>

    <template v-if="file">
      <el-card shadow="never" style="margin-bottom: 12px">
        <template #header>基本信息</template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文件名">{{ file.fileName }}</el-descriptions-item>
          <el-descriptions-item label="MIME 类型">{{ file.mimeType }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ file.fileSize }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDisplayDateTime(file.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="String(file.status) === 'ACTIVE' ? 'success' : 'info'">
              {{ file.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="版本数">{{ file.versionCount }}</el-descriptions-item>
          <el-descriptions-item label="文件夹">{{ file.folderName }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ file.createdByName }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never">
        <template #header>下载日志</template>
        <el-table :data="downloadLogs" stripe empty-text="暂无下载记录">
          <el-table-column prop="userName" label="下载人" width="140" />
          <el-table-column prop="ipAddress" label="IP 地址" width="160" />
          <el-table-column label="下载时间" width="180">
            <template #default="{ row }">{{ formatDisplayDateTime(row.downloadedAt) }}</template>
          </el-table-column>
          <el-table-column prop="userAgent" label="User-Agent" min-width="260" show-overflow-tooltip />
        </el-table>
      </el-card>
    </template>
  </div>
</template>
