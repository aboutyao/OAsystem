<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { instanceDetail, instanceTimeline, startedByMe } from '../../api/workflow'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime, statusLabel } from '../oa/oa-shared'
import { businessDocPath } from '../../utils/businessDocPath'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const drawer = ref(false)
const drawerTitle = ref('')
const inst = ref<JsonObject | null>(null)
const timeline = ref<JsonObject[]>([])
const timelineLoading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await startedByMe(page.value, size.value)
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

function goCc() {
  router.push('/applications/cc')
}

function openDoc(row: JsonObject) {
  const bt = String(row.businessType ?? '')
  const bid = Number(row.businessId)
  const path = businessDocPath(bt, bid)
  if (!path) {
    ElMessage.info('该业务类型暂无详情页')
    return
  }
  router.push(path)
}

async function openFlow(row: JsonObject) {
  const wid = Number(row.wfInstanceId)
  drawerTitle.value = String(row.title ?? '流程')
  drawer.value = true
  inst.value = null
  timeline.value = []
  timelineLoading.value = true
  try {
    const [d, t] = await Promise.all([instanceDetail(wid), instanceTimeline(wid)])
    inst.value = d
    timeline.value = Array.isArray(t) ? t : []
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    drawer.value = false
  } finally {
    timelineLoading.value = false
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">我发起的</h2>
        <p class="muted">查看本人发起的流程实例与进度。</p>
      </div>
      <el-button @click="goCc">抄送我的</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="wfInstanceId" label="实例" width="88" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="businessType" label="业务" width="100" />
        <el-table-column prop="currentNodeName" label="当前节点" width="140" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ statusLabel(String(row.status ?? '')) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发起时间" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.startedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDoc(row)">业务单据</el-button>
            <el-button link type="primary" @click="openFlow(row)">流程</el-button>
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

    <el-drawer v-model="drawer" :title="drawerTitle" size="420px">
      <div v-loading="timelineLoading">
        <template v-if="inst">
          <el-descriptions :column="1" border size="small" class="app-drawer__desc">
            <el-descriptions-item label="状态">{{ statusLabel(String(inst.status ?? '')) }}</el-descriptions-item>
            <el-descriptions-item label="当前节点">{{ inst.currentNodeName ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="业务类型">{{ inst.businessType }}</el-descriptions-item>
            <el-descriptions-item label="业务主键">{{ inst.businessId }}</el-descriptions-item>
          </el-descriptions>
          <h4 class="app-drawer__h">时间轴</h4>
          <el-timeline v-if="timeline.length">
            <el-timeline-item v-for="(ev, idx) in timeline" :key="idx" :timestamp="formatDisplayDateTime(ev.operatedAt)">
              <strong>{{ ev.action }}</strong>
              <span v-if="ev.nodeName"> · {{ ev.nodeName }}</span>
              <div class="muted">{{ ev.operatorName }}</div>
              <div v-if="ev.comment">{{ ev.comment }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无记录" />
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.app-drawer__desc {
  margin-bottom: 16px;
}
.app-drawer__h {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
}
</style>
