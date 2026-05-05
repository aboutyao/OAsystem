<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listSupplies, listSupplyRecords } from '../../api/supplies'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const route = useRoute()
const router = useRouter()

const supplyId = computed<number | undefined>(() => {
  const raw = route.query.supplyId
  const n = raw == null ? NaN : Number(raw)
  return Number.isFinite(n) && n > 0 ? n : undefined
})

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const supplies = ref<JsonObject[]>([])
const filterSupplyId = ref<number | undefined>(supplyId.value)

const TYPE_LABEL: Record<string, string> = {
  IN: '入库',
  OUT: '出库',
  RETURN: '退回',
  ADJUST: '调整',
}

const TYPE_TAG: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
  IN: 'success',
  OUT: 'warning',
  RETURN: 'info',
  ADJUST: 'danger',
}

function typeLabel(t: unknown) {
  const code = String(t ?? '')
  return TYPE_LABEL[code] ?? code
}

function typeTag(t: unknown) {
  const code = String(t ?? '')
  return TYPE_TAG[code] ?? 'info'
}

async function loadSupplies() {
  const res = await listSupplies({ page: 1, size: 500 })
  supplies.value = res.items
}

async function load() {
  loading.value = true
  try {
    rows.value = await listSupplyRecords(filterSupplyId.value)
  } finally {
    loading.value = false
  }
}

void loadSupplies()
void load()

watch(filterSupplyId, (v) => {
  router.replace({ path: '/supplies/records', query: v ? { supplyId: String(v) } : {} })
  void load()
})

watch(
  () => route.query.supplyId,
  (raw) => {
    const n = raw == null ? NaN : Number(raw)
    const next = Number.isFinite(n) && n > 0 ? n : undefined
    if (next !== filterSupplyId.value) {
      filterSupplyId.value = next
    }
  },
)
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">出入库记录</h2>
        <p class="muted">
          按用品筛选；最多展示最近 500 条。
          <RouterLink to="/supplies">返回办公用品</RouterLink>
        </p>
      </div>
      <div class="oa-page__actions">
        <el-select
          v-model="filterSupplyId"
          clearable
          filterable
          placeholder="按用品筛选"
          style="width: 240px"
        >
          <el-option
            v-for="s in supplies"
            :key="Number(s.id)"
            :label="`${s.supplyCode} · ${s.supplyName}`"
            :value="Number(s.id)"
          />
        </el-select>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.operatedAt) }}</template>
        </el-table-column>
        <el-table-column label="用品" min-width="200">
          <template #default="{ row }">
            <span>{{ row.supplyCode ?? '—' }}</span>
            <span v-if="row.supplyName"> · {{ row.supplyName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.recordType)">{{ typeLabel(row.recordType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="userName" label="领用人" width="120" />
        <el-table-column prop="operatedByName" label="操作人" width="120" />
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>
