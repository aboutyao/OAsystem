<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listTemplates, templateDetail } from '../../api/workflow'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const detailVisible = ref(false)
const detail = ref<JsonObject | null>(null)
const detailLoading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await listTemplates(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

void load()

async function openDetail(row: JsonObject) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await templateDetail(Number(row.id))
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">流程模板</h2>
        <p class="muted">查看已发布的流程模板及其版本历史。流程定义由 Flowable 引擎驱动。</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe @row-dblclick="openDetail">
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="templateCode" label="模板编码" min-width="200" />
        <el-table-column prop="templateName" label="名称" min-width="180" />
        <el-table-column prop="businessType" label="业务类型" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看版本</el-button>
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
          @size-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="模板详情" width="720px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="模板编码">{{ detail.templateCode }}</el-descriptions-item>
            <el-descriptions-item label="名称">{{ detail.templateName }}</el-descriptions-item>
            <el-descriptions-item label="业务类型">{{ detail.businessType }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
          </el-descriptions>
          <h4 style="margin-top: 16px">版本历史</h4>
          <el-table :data="(detail.versions as JsonObject[]) || []" stripe size="small">
            <el-table-column prop="versionNo" label="版本号" width="100" />
            <el-table-column prop="flowableDefinitionId" label="Flowable Key" min-width="180" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column label="发布时间" width="170">
              <template #default="{ row }">{{ formatDisplayDateTime(row.publishedAt) }}</template>
            </el-table-column>
            <el-table-column prop="changeReason" label="变更说明" min-width="180" />
          </el-table>
        </template>
      </div>
    </el-dialog>
  </div>
</template>
