<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSeals, returnSeal } from '../../../api/oa-seals'
import type { JsonObject } from '../../../api/types'
import { formatDisplayDateTime, statusLabel } from '../oa-shared'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const approvedRows = computed(() => {
  return rows.value.filter((r) => String(r.status ?? '') === 'APPROVED')
})

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

function goDetail(row: JsonObject) {
  router.push(`/oa/seals/${Number(row.id)}`)
}

function needsReturn(row: JsonObject): boolean {
  return Number(row.outFlag) === 1 && !row.returnedAt
}

async function handleReturn(row: JsonObject) {
  try {
    await ElMessageBox.confirm('确认此印章已归还？', '登记归还', { type: 'warning' })
  } catch {
    return
  }
  try {
    await returnSeal(Number(row.id))
    ElMessage.success('已登记归还')
    load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

function returnStatus(row: JsonObject): string {
  if (Number(row.outFlag) !== 1) return '无需归还'
  return row.returnedAt ? '已归还' : '待归还'
}

function returnTagType(row: JsonObject): '' | 'success' | 'warning' | 'info' {
  if (Number(row.outFlag) !== 1) return 'info'
  return row.returnedAt ? 'success' : 'warning'
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">印章归还</h2>
        <p class="muted">已通过审批且外带的印章，需跟踪归还状态。</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="approvedRows" stripe>
        <el-table-column prop="id" label="编号" width="88" />
        <el-table-column prop="sealType" label="印章类型" width="120" />
        <el-table-column prop="sealName" label="印章名称" width="120" />
        <el-table-column prop="fileTitle" label="文件标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="使用时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.useAt) }}</template>
        </el-table-column>
        <el-table-column label="外带" width="72">
          <template #default="{ row }">{{ Number(row.outFlag) === 1 ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="归还状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="returnTagType(row)">{{ returnStatus(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="归还时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.returnedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">查看</el-button>
            <el-button
              v-if="needsReturn(row)"
              link
              type="success"
              @click="handleReturn(row)"
            >登记归还</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && approvedRows.length === 0" description="暂无待归还的印章" />

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
