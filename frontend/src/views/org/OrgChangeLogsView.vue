<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { listChangeLogs } from '../../api/org'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const filters = reactive({
  targetType: '',
  changeType: '',
})

const targetTypeOptions = [
  { label: '部门', value: 'DEPT' },
  { label: '用户', value: 'USER' },
  { label: '岗位', value: 'POSITION' },
  { label: '职级', value: 'RANK' },
]

const changeTypeOptions = [
  { label: '新增', value: 'CREATE' },
  { label: '更新', value: 'UPDATE' },
  { label: '删除', value: 'DELETE' },
  { label: '启用', value: 'ENABLE' },
  { label: '停用', value: 'DISABLE' },
  { label: '离职', value: 'RESIGN' },
]

async function load() {
  loading.value = true
  try {
    const res = await listChangeLogs(
      page.value,
      size.value,
      filters.targetType || undefined,
      filters.changeType || undefined,
    )
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

void load()

function onSearch() {
  page.value = 1
  void load()
}

function onReset() {
  filters.targetType = ''
  filters.changeType = ''
  page.value = 1
  void load()
}

function formatJson(v: unknown): string {
  if (v == null) return '-'
  if (typeof v === 'string') {
    try {
      return JSON.stringify(JSON.parse(v), null, 2)
    } catch {
      return v
    }
  }
  return JSON.stringify(v, null, 2)
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">组织变更日志</h2>
        <p class="muted">查看组织架构、岗位、职级等变更历史记录。</p>
      </div>
    </div>

    <el-card shadow="never" class="oa-page__filters">
      <el-form inline @submit.prevent="onSearch">
        <el-form-item label="变更对象">
          <el-select v-model="filters.targetType" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="o in targetTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更类型">
          <el-select v-model="filters.changeType" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="o in changeTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="80" />
        <el-table-column prop="targetType" label="变更对象" width="100" />
        <el-table-column prop="targetId" label="对象ID" width="100" />
        <el-table-column prop="changeType" label="变更类型" width="100" />
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="变更前" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tooltip :content="formatJson(row.beforeData)" placement="top" :show-after="300">
              <span class="muted">{{ row.beforeData ? '查看' : '-' }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="变更后" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tooltip :content="formatJson(row.afterData)" placement="top" :show-after="300">
              <span class="muted">{{ row.afterData ? '查看' : '-' }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.operatedAt) }}</template>
        </el-table-column>
      </el-table>
      <div class="oa-page__pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          v-model:current-page="page"
          v-model:page-size="size"
          @current-change="load"
          @size-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.oa-page__filters {
  margin-bottom: 12px;
}
</style>
