<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelSeal, getSeal, returnSeal, submitSeal, withdrawSeal } from '../../../api/oa-seals'
import type { JsonObject } from '../../../api/types'
import { useOaActions } from '../../../composables/useOaActions'
import { formatDisplayDateTime, statusLabel } from '../oa-shared'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const row = ref<JsonObject | null>(null)

const id = computed(() => Number(route.params.id))
const status = computed(() => (row.value ? String(row.value.status ?? '') : ''))
const outFlag = computed(() => (row.value ? Number(row.value.outFlag ?? 0) : 0))
const canReturn = computed(
  () => status.value === 'APPROVED' && outFlag.value === 1 && !row.value?.returnAt,
)

onMounted(async () => {
  try {
    row.value = await getSeal(id.value)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/oa/seals')
  } finally {
    loading.value = false
  }
})

async function reload() {
  try {
    row.value = await getSeal(id.value)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  }
}

const { onSubmit, onWithdraw, onCancel } = useOaActions(reload)

function goEdit() {
  router.push(`/oa/seals/${id.value}/edit`)
}

function handleSubmit() { onSubmit(() => submitSeal(id.value)) }
function handleWithdraw() { onWithdraw(() => withdrawSeal(id.value)) }
function handleCancel() { onCancel(() => cancelSeal(id.value)) }

async function onReturn() {
  try {
    await ElMessageBox.confirm('确认登记外带归还？', '归还', { type: 'warning' })
    await returnSeal(id.value)
    ElMessage.success('已登记归还')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head" v-if="row">
      <div>
        <h2 class="oa-page__title">用章详情 #{{ row.id }}</h2>
        <p class="muted"><el-tag type="info">{{ statusLabel(status) }}</el-tag></p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/oa/seals')">返回列表</el-button>
        <el-button v-if="status === 'DRAFT'" type="primary" @click="goEdit">编辑</el-button>
        <el-button v-if="status === 'DRAFT'" type="success" @click="handleSubmit">提交审批</el-button>
        <el-button v-if="status === 'APPROVING'" @click="handleWithdraw">撤回</el-button>
        <el-button v-if="status === 'DRAFT' || status === 'APPROVING'" type="danger" plain @click="handleCancel">作废</el-button>
        <el-button v-if="canReturn" type="warning" @click="onReturn">登记归还</el-button>
      </div>
    </div>

    <el-card v-if="row" shadow="never">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="印章类型">{{ row.sealType }}</el-descriptions-item>
        <el-descriptions-item label="印章名称">{{ row.sealName }}</el-descriptions-item>
        <el-descriptions-item label="文件标题" :span="2">{{ row.fileTitle }}</el-descriptions-item>
        <el-descriptions-item label="使用事由" :span="2">{{ row.useReason ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="使用时间">{{ formatDisplayDateTime(row.useAt) }}</el-descriptions-item>
        <el-descriptions-item label="外带">{{ outFlag === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="归还时间">{{ formatDisplayDateTime(row.returnAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>
