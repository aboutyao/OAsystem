<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { templateDetail } from '../../api/workflow'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const route = useRoute()
const loading = ref(false)
const templateName = ref('')
const versions = ref<JsonObject[]>([])

function statusType(status: string): string {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'DRAFT') return 'info'
  if (status === 'DEPRECATED') return 'warning'
  return ''
}

async function load() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const detail = await templateDetail(id)
    templateName.value = String(detail.templateName ?? '')
    versions.value = (detail.versions as JsonObject[]) ?? []
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">模板版本历史</h2>
        <p class="muted">模板「{{ templateName }}」的所有版本记录。</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="versions" stripe>
        <el-table-column prop="versionNo" label="版本号" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(String(row.status ?? ''))">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">{{ formatDisplayDateTime(row.publishedAt) }}</template>
        </el-table-column>
        <el-table-column prop="changeReason" label="变更说明" min-width="240" />
      </el-table>
    </el-card>
  </div>
</template>
