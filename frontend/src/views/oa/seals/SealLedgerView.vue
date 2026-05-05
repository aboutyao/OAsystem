<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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

const statusFilter = ref('')
const keyword = ref('')

const statusOptions = [
  { value: '', label: '全部' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'APPROVING', label: '审批中' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'WITHDRAWN', label: '已撤回' },
  { value: 'CANCELLED', label: '已作废' },
]

const filteredRows = computed(() => {
  let list = rows.value
  if (statusFilter.value) {
    list = list.filter((r) => String(r.status ?? '') === statusFilter.value)
  }
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(
      (r) =>
        String(r.sealName ?? '').toLowerCase().includes(kw) ||
        String(r.fileTitle ?? '').toLowerCase().includes(kw) ||
        String(r.id).includes(kw),
    )
  }
  return list
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

function statusTagType(status: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'APPROVED': return 'success'
    case 'APPROVING': return 'warning'
    case 'REJECTED':
    case 'CANCELLED': return 'danger'
    case 'DRAFT': return 'info'
    default: return ''
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">用章台账</h2>
        <p class="muted">全部用章申请记录；可按状态与关键字筛选。</p>
      </div>
    </div>

    <el-card shadow="never">
      <div class="oa-filter-bar">
        <el-radio-group v-model="statusFilter" size="small">
          <el-radio-button v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </el-radio-button>
        </el-radio-group>
        <el-input
          v-model="keyword"
          placeholder="搜索印章名称、文件标题..."
          clearable
          style="width: 240px"
          size="small"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <el-table v-loading="loading" :data="filteredRows" stripe>
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
        <el-table-column label="申请人" width="100">
          <template #default="{ row }">{{ row.applicantName ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(String(row.status ?? ''))">
              {{ statusLabel(String(row.status ?? '')) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="oa-page__pager">
        <el-pagination
          layout="total, sizes, prev, pager, next"
          :total="total"
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          @current-change="load"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>
