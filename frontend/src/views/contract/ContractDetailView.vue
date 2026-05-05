<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  contractVersions,
  getContract,
  renewContract,
  signContract,
  submitContract,
  terminateContract,
} from '../../api/contracts'
import type { JsonObject } from '../../api/types'
import { formatDisplayDate, statusLabel } from '../oa/oa-shared'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const row = ref<JsonObject | null>(null)
const versions = ref<JsonObject[]>([])

const id = computed(() => Number(route.params.id))
const status = computed(() => (row.value ? String(row.value.status ?? '') : ''))

onMounted(async () => {
  try {
    await reload()
    versions.value = await contractVersions(id.value)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/contracts')
  } finally {
    loading.value = false
  }
})

async function reload() {
  try {
    row.value = await getContract(id.value)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  }
}

function goEdit() {
  router.push(`/contracts/${id.value}/edit`)
}

async function onSubmit() {
  try {
    await ElMessageBox.confirm('确认提交合同审批？', '提交', { type: 'warning' })
    await submitContract(id.value)
    ElMessage.success('已提交')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '提交失败')
  }
}

async function onSign() {
  try {
    await ElMessageBox.confirm('确认标记为已签署？', '签署', { type: 'warning' })
    await signContract(id.value)
    ElMessage.success('已标记签署')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onTerminate() {
  try {
    await ElMessageBox.confirm('确认终止该合同？', '终止', { type: 'warning' })
    await terminateContract(id.value)
    ElMessage.success('已终止')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onRenew() {
  try {
    await ElMessageBox.confirm('将基于当前合同生成续签草稿，是否继续？', '续签', { type: 'warning' })
    const created = await renewContract(id.value)
    ElMessage.success('已生成续签草稿')
    router.push(`/contracts/${Number(created.id)}`)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head" v-if="row">
      <div>
        <h2 class="oa-page__title">合同 {{ row.contractNo }}</h2>
        <p class="muted"><el-tag effect="light" :type="status === 'APPROVED' || status === 'SIGNED' ? 'success' : status === 'APPROVING' ? 'warning' : status === 'REJECTED' || status === 'TERMINATED' ? 'danger' : 'info'">{{ statusLabel(status) }}</el-tag></p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/contracts')">返回列表</el-button>
        <el-button v-if="status === 'DRAFT'" type="primary" @click="goEdit">编辑</el-button>
        <el-button v-if="status === 'DRAFT'" type="success" @click="onSubmit">提交审批</el-button>
        <el-button v-if="status === 'APPROVED'" type="primary" @click="onSign">标记签署</el-button>
        <el-button
          v-if="status === 'APPROVING' || status === 'APPROVED' || status === 'SIGNED'"
          type="danger"
          plain
          @click="onTerminate"
        >
          终止
        </el-button>
        <el-button v-if="status === 'APPROVED' || status === 'SIGNED'" @click="onRenew">续签草稿</el-button>
      </div>
    </div>

    <el-card v-if="row" shadow="never">
      <h3 class="oa-section-title">基本信息</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="名称" :span="2">{{ row.contractName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ row.contractType }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ row.amount }}</el-descriptions-item>
        <el-descriptions-item label="相对方" :span="2">{{ row.counterparty }}</el-descriptions-item>
        <el-descriptions-item label="开始日">{{ formatDisplayDate(row.startDate) }}</el-descriptions-item>
        <el-descriptions-item label="结束日">{{ formatDisplayDate(row.endDate) }}</el-descriptions-item>
        <el-descriptions-item label="签署日">{{ formatDisplayDate(row.signDate) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag effect="light" size="small" :type="status === 'APPROVED' || status === 'SIGNED' ? 'success' : status === 'APPROVING' ? 'warning' : status === 'REJECTED' || status === 'TERMINATED' ? 'danger' : 'info'">
            {{ statusLabel(status) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <h3 class="oa-section-title">附件版本</h3>
      <el-empty v-if="versions.length === 0" description="暂无版本记录" />
      <el-table v-else :data="versions" size="small">
        <el-table-column prop="versionNo" label="版本号" width="80" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="changeReason" label="变更原因" min-width="200" />
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.publishedAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
/* styles inherited from main.css */
</style>
