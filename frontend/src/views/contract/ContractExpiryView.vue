<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listContracts } from '../../api/contracts'
import type { JsonObject } from '../../api/types'
import { formatDisplayDate, statusLabel } from '../oa/oa-shared'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const DAYS_WARNING = 30

function daysUntilExpiry(endDate: unknown): number | null {
  if (!endDate) return null
  const d = new Date(String(endDate))
  if (Number.isNaN(d.getTime())) return null
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  d.setHours(0, 0, 0, 0)
  return Math.ceil((d.getTime() - now.getTime()) / 86400000)
}

function expiryTagType(days: number | null): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (days === null) return 'info'
  if (days < 0) return 'danger'
  if (days <= DAYS_WARNING) return 'warning'
  return 'success'
}

function expiryLabel(days: number | null): string {
  if (days === null) return '—'
  if (days < 0) return `已过期 ${Math.abs(days)} 天`
  if (days === 0) return '今日到期'
  return `剩余 ${days} 天`
}

async function load() {
  loading.value = true
  try {
    const res = await listContracts(page.value, size.value)
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
  router.push(`/contracts/${Number(row.id)}`)
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">合同到期提醒</h2>
        <p class="muted">展示所有合同到期状态；{{ DAYS_WARNING }} 天内到期的合同将高亮提醒。</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="编号" width="88" />
        <el-table-column prop="contractNo" label="合同编号" min-width="140" />
        <el-table-column prop="contractName" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="counterparty" label="相对方" width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ statusLabel(String(row.status ?? '')) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始日" width="120">
          <template #default="{ row }">{{ formatDisplayDate(row.startDate) }}</template>
        </el-table-column>
        <el-table-column label="结束日" width="120">
          <template #default="{ row }">
            <span :style="{ fontWeight: daysUntilExpiry(row.endDate) !== null && daysUntilExpiry(row.endDate)! <= DAYS_WARNING ? '600' : '400' }">
              {{ formatDisplayDate(row.endDate) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="到期状态" width="140">
          <template #default="{ row }">
            <el-tag size="small" :type="expiryTagType(daysUntilExpiry(row.endDate))">
              {{ expiryLabel(daysUntilExpiry(row.endDate)) }}
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
