<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listOperationLogs } from '../../api/audit'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const filters = reactive({
  businessType: '',
})

const businessTypeOptions = [
  { label: '全部文件相关', value: '' },
  { label: 'FILE_UPLOAD', value: 'FILE_UPLOAD' },
  { label: 'FILE_DELETE', value: 'FILE_DELETE' },
  { label: 'FILE_MOVE', value: 'FILE_MOVE' },
  { label: 'DOWNLOAD', value: 'DOWNLOAD' },
]

async function load() {
  loading.value = true
  try {
    const res = await listOperationLogs({
      page: page.value,
      size: size.value,
      businessType: filters.businessType || undefined,
    })
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
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
  filters.businessType = ''
  page.value = 1
  void load()
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">文件下载日志</h2>
        <p class="muted">记录文件上传、删除、移动、下载等操作。</p>
      </div>
    </div>

    <el-card shadow="never" class="oa-page__filters">
      <el-form inline @submit.prevent="onSearch">
        <el-form-item label="业务类型">
          <el-select v-model="filters.businessType" clearable placeholder="全部" style="width: 180px">
            <el-option
              v-for="opt in businessTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
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
        <el-table-column prop="operatorName" label="操作人" width="140" />
        <el-table-column prop="operationType" label="操作类型" width="140" />
        <el-table-column prop="businessType" label="业务类型" width="140" />
        <el-table-column prop="targetId" label="目标ID" width="100" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="String(row.result) === 'SUCCESS' ? 'success' : 'danger'">
              {{ String(row.result) === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP" width="140" />
        <el-table-column label="时间" width="170">
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
