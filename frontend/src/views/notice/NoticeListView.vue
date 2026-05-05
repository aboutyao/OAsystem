<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listNotices } from '../../api/notices'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime, statusLabel } from '../oa/oa-shared'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const mineOnly = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await listNotices(page.value, size.value, mineOnly.value || undefined, undefined)
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

watch(mineOnly, () => {
  page.value = 1
  void load()
})

function goDetail(row: JsonObject) {
  router.push(`/notices/${Number(row.id)}`)
}

function goCreate() {
  router.push('/notices/create')
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">通知公告</h2>
        <p class="muted">已发布公告全员可见；「我的草稿」仅本人拟稿。</p>
      </div>
      <div class="oa-page__actions">
        <el-switch v-model="mineOnly" active-text="我的草稿" inactive-text="已发布" />
        <el-button type="primary" @click="goCreate">新建公告</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="置顶" width="72">
          <template #default="{ row }">{{ Number(row.topFlag) === 1 ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ statusLabel(String(row.status ?? '')) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.publishAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="oa-page__pager">
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
