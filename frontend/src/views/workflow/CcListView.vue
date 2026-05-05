<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ccToMe } from '../../api/workflow'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

async function load() {
  loading.value = true
  try {
    const res = await ccToMe(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()

function handleSizeChange() {
  page.value = 1
  load()
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">抄送我的</h2>
        <p class="muted">仅查看；后端接口就绪后将展示抄送流程。</p>
      </div>
      <el-button @click="router.push('/applications')">返回我发起的</el-button>
    </div>

    <el-card shadow="never">
      <el-empty v-if="!loading && rows.length === 0" description="暂无抄送记录" />
      <el-table v-else v-loading="loading" :data="rows" stripe>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.startedAt) }}</template>
        </el-table-column>
      </el-table>
      <div v-if="total > 0" class="oa-page__pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          v-model:current-page="page"
          v-model:page-size="size"
          @current-change="load"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>
