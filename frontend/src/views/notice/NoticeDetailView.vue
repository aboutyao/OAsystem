<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  confirmNoticeRead,
  getNotice,
  markNoticeRead,
  noticeReadStats,
  publishNotice,
  withdrawNotice,
} from '../../api/notices'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime, statusLabel } from '../oa/oa-shared'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(true)
const row = ref<JsonObject | null>(null)
const stats = ref<JsonObject | null>(null)

const id = computed(() => Number(route.params.id))
const status = computed(() => (row.value ? String(row.value.status ?? '') : ''))
const canManage = computed(() => {
  const uid = authStore.user?.id
  const owner = row.value?.createdBy
  if (uid == null || owner == null) return false
  return Number(owner) === uid || authStore.isSuperUser
})

onMounted(async () => {
  try {
    row.value = await getNotice(id.value)
    try {
      await markNoticeRead(id.value)
    } catch {
      /* 忽略已读失败 */
    }
    try {
      stats.value = await noticeReadStats(id.value)
    } catch {
      stats.value = null
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/notices')
  } finally {
    loading.value = false
  }
})

async function reload() {
  try {
    row.value = await getNotice(id.value)
    try {
      stats.value = await noticeReadStats(id.value)
    } catch {
      stats.value = null
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  }
}

function goEdit() {
  router.push(`/notices/${id.value}/edit`)
}

async function onPublish() {
  try {
    await ElMessageBox.confirm('确认发布公告？', '发布', { type: 'warning' })
    await publishNotice(id.value)
    ElMessage.success('已发布')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onWithdraw() {
  try {
    await ElMessageBox.confirm('确认撤回该公告？', '撤回', { type: 'warning' })
    await withdrawNotice(id.value)
    ElMessage.success('已撤回')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onConfirm() {
  try {
    await confirmNoticeRead(id.value)
    ElMessage.success('已确认阅读')
    await reload()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head" v-if="row">
      <div>
        <h2 class="oa-page__title">{{ row.title }}</h2>
        <p class="muted">
          <el-tag size="small" type="info">{{ statusLabel(status) }}</el-tag>
          <span class="notice-meta">{{ formatDisplayDateTime(row.publishAt) }}</span>
        </p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/notices')">返回列表</el-button>
        <el-button v-if="canManage && status === 'DRAFT'" type="primary" @click="goEdit">编辑</el-button>
        <el-button v-if="canManage && status === 'DRAFT'" type="success" @click="onPublish">发布</el-button>
        <el-button v-if="canManage && status === 'PUBLISHED'" @click="onWithdraw">撤回</el-button>
        <el-button v-if="status === 'PUBLISHED'" type="primary" plain @click="onConfirm">确认阅读</el-button>
      </div>
    </div>

    <el-card v-if="row" shadow="never">
      <div class="notice-body" v-html="row.content"></div>
      <el-divider v-if="stats" />
      <p v-if="stats" class="muted notice-stats">
        已读 {{ stats.readCount }} · 未读约 {{ stats.unreadCount }} · 确认 {{ stats.confirmedCount }}
      </p>
    </el-card>
  </div>
</template>

<style scoped>
.notice-body {
  line-height: 1.65;
  font-size: 15px;
}
.notice-body :deep(img) {
  max-width: 100%;
  height: auto;
}
.notice-meta {
  margin-left: 12px;
}
.notice-stats {
  margin: 0;
  font-size: 13px;
}
</style>
