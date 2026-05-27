<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { doneTasks, instanceDetail, instanceTimeline, instanceDiagram } from '../../api/workflow'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const detailVisible = ref(false)
const detail = ref<JsonObject | null>(null)
const timeline = ref<JsonObject[]>([])
const diagram = ref<JsonObject | null>(null)
const detailLoading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await doneTasks(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

void load()

async function openDetail(row: JsonObject) {
  const id = Number(row.wfInstanceId)
  if (!id) {
    ElMessage.warning('无法获取流程实例信息')
    return
  }
  detailVisible.value = true
  detailLoading.value = true
  try {
    const [d, t, g] = await Promise.all([
      instanceDetail(id),
      instanceTimeline(id),
      instanceDiagram(id),
    ])
    detail.value = d
    timeline.value = t
    diagram.value = g
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function isCompletedActivity(id: string | undefined) {
  if (!id || !diagram.value) return false
  const arr = (diagram.value.completedActivityIds as string[]) || []
  return arr.includes(id)
}

function isActiveActivity(id: string | undefined) {
  if (!id || !diagram.value) return false
  const arr = (diagram.value.activeActivityIds as string[]) || []
  return arr.includes(id)
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">我的已办</h2>
        <p class="muted">已处理或已取消的审批任务。</p>
      </div>
      <el-button @click="router.push('/todos')">返回待办</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="taskId" label="任务号" width="100" />
        <el-table-column label="标题" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="openDetail(row)">
              {{ row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="nodeName" label="节点" width="140" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="完成时间" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.completedAt) }}</template>
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

    <el-dialog v-model="detailVisible" title="流程实例详情" width="900px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="业务">{{ detail.businessType }} #{{ detail.businessId }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
            <el-descriptions-item label="发起人">{{ detail.starterName }}</el-descriptions-item>
            <el-descriptions-item label="当前节点">{{ detail.currentNodeName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="发起时间">{{ formatDisplayDateTime(detail.startedAt) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ formatDisplayDateTime(detail.endedAt) }}</el-descriptions-item>
          </el-descriptions>

          <h4 style="margin-top: 16px">流程时间轴</h4>
          <el-timeline>
            <el-timeline-item
              v-for="(item, idx) in timeline"
              :key="idx"
              :timestamp="formatDisplayDateTime(item.operatedAt)"
              :type="item.action === 'APPROVE' ? 'success' : item.action === 'REJECT' ? 'danger' : 'primary'"
            >
              <strong>{{ item.action }}</strong> · {{ item.operatorName }} · 节点 {{ item.nodeName || '-' }}
              <div v-if="item.comment" class="muted">{{ item.comment }}</div>
            </el-timeline-item>
          </el-timeline>

          <h4 style="margin-top: 16px">流程图（节点高亮）</h4>
          <el-table :data="(diagram?.history as JsonObject[]) || []" stripe size="small">
            <el-table-column prop="activityId" label="节点ID" min-width="120">
              <template #default="{ row }">
                <el-tag
                  size="small"
                  :type="isActiveActivity(row.activityId as string) ? 'warning' : isCompletedActivity(row.activityId as string) ? 'success' : 'info'"
                >
                  {{ row.activityId }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="activityName" label="节点名" min-width="120" />
            <el-table-column prop="activityType" label="类型" width="120" />
            <el-table-column prop="assignee" label="处理人" width="100" />
            <el-table-column label="开始时间" width="170">
              <template #default="{ row }">{{ formatDisplayDateTime(row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="结束时间" width="170">
              <template #default="{ row }">{{ formatDisplayDateTime(row.endTime) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-dialog>
  </div>
</template>
