<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listSeals } from '../../../api/oa-seals'
import type { JsonObject } from '../../../api/types'
import { formatDisplayDateTime, statusLabel } from '../oa-shared'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

async function load() {
  loading.value = true
  try {
    const res = await listSeals(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function handleSizeChange() {
  page.value = 1
  load()
}

function goCreate() {
  router.push('/oa/seals/create')
}

function goDetail(row: JsonObject) {
  router.push(`/oa/seals/${Number(row.id)}`)
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">用章申请</h2>
        <p class="muted">用章类型、文件标题与使用时间。</p>
      </div>
      <el-button type="primary" @click="goCreate">新建申请</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="编号" width="88" />
        <el-table-column prop="sealType" label="印章类型" width="120" />
        <el-table-column prop="sealName" label="印章名称" width="120" />
        <el-table-column prop="fileTitle" label="文件标题" min-width="160" />
        <el-table-column label="使用时间" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.useAt) }}</template>
        </el-table-column>
        <el-table-column label="外带" width="72">
          <template #default="{ row }">{{ Number(row.outFlag) === 1 ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ statusLabel(String(row.status ?? '')) }}</el-tag>
          </template>
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
