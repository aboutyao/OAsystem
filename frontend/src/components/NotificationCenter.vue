<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '../api/http'

const router = useRouter()
const visible = ref(false)
const notifications = ref<Notification[]>([])
const loading = ref(false)
const activeTab = ref('all')

interface Notification {
  id: number
  title: string
  content: string
  messageType: string
  businessType?: string
  businessId?: number
  readAt?: string
  createdAt: string
  groupKey?: string
}

const groupedNotifications = computed(() => {
  const groups: Record<string, Notification[]> = {}
  for (const n of notifications.value) {
    const key = n.groupKey || n.businessType || n.messageType || 'OTHER'
    if (!groups[key]) groups[key] = []
    groups[key].push(n)
  }
  return Object.entries(groups)
    .map(([key, items]) => ({
      key,
      label: groupLabel(key),
      count: items.length,
      unread: items.filter((i) => !i.readAt).length,
      latest: items[0],
      items,
    }))
    .sort((a, b) => b.unread - a.unread || a.items[0]?.createdAt?.localeCompare(b.items[0]?.createdAt) ?? 0)
})

const filteredNotifications = computed(() => {
  if (activeTab.value === 'all') return notifications.value
  if (activeTab.value === 'unread') return notifications.value.filter((n) => !n.readAt)
  return notifications.value.filter((n) => n.messageType === activeTab.value)
})

const unreadCount = computed(() => notifications.value.filter((n) => !n.readAt).length)

function groupLabel(key: string): string {
  const labels: Record<string, string> = {
    WORKFLOW: '审批流程',
    LEAVE: '请假',
    EXPENSE: '报销',
    SEAL: '用章',
    PURCHASE: '采购',
    CONTRACT: '合同',
    MEETING: '会议',
    NOTICE: '公告',
    FILE: '文件',
    SYSTEM: '系统',
    OTHER: '其他',
  }
  return labels[key] || key
}

function groupIcon(key: string): string {
  const icons: Record<string, string> = {
    WORKFLOW: 'SetUp',
    LEAVE: 'Calendar',
    EXPENSE: 'Wallet',
    SEAL: 'Stamp',
    PURCHASE: 'ShoppingCart',
    CONTRACT: 'Document',
    MEETING: 'Calendar',
    NOTICE: 'Bell',
    FILE: 'FolderOpened',
    SYSTEM: 'Setting',
  }
  return icons[key] || 'ChatDotRound'
}

function timeAgo(dateStr: string): string {
  const now = new Date()
  const date = new Date(dateStr)
  const diffMs = now.getTime() - date.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}小时前`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return `${diffDay}天前`
  return date.toLocaleDateString('zh-CN')
}

async function fetchNotifications() {
  loading.value = true
  try {
    const data = await http.get('/messages', { params: { page: 1, size: 50 } }) as any
    notifications.value = data.items || data || []
  } catch {
    notifications.value = []
  } finally {
    loading.value = false
  }
}

async function markAsRead(id: number) {
  try {
    await http.post(`/messages/${id}/read`)
    const n = notifications.value.find((item) => item.id === id)
    if (n) n.readAt = new Date().toISOString()
  } catch {}
}

async function markAllAsRead() {
  try {
    await http.post('/messages/read-all')
    for (const n of notifications.value) {
      if (!n.readAt) n.readAt = new Date().toISOString()
    }
  } catch {}
}

function handleClick(notification: Notification) {
  markAsRead(notification.id)
  if (notification.businessType && notification.businessId) {
    const routeMap: Record<string, string> = {
      LEAVE: '/oa/leaves',
      EXPENSE: '/oa/expenses',
      SEAL: '/oa/seals',
      PURCHASE: '/oa/purchases',
      CONTRACT: '/contracts',
    }
    const base = routeMap[notification.businessType]
    if (base) {
      router.push(`${base}/${notification.businessId}`)
      visible.value = false
    }
  }
}

function open() {
  visible.value = true
  fetchNotifications()
}

defineExpose({ open })
</script>

<template>
  <el-drawer
    v-model="visible"
    title="通知中心"
    direction="rtl"
    size="420px"
    :before-close="() => visible = false"
  >
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between; width: 100%">
        <div style="display: flex; align-items: center; gap: 8px">
          <span style="font-weight: 600; font-size: 16px">通知中心</span>
          <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" type="danger" />
        </div>
        <el-button
          v-if="unreadCount > 0"
          text
          type="primary"
          size="small"
          @click="markAllAsRead"
        >全部已读</el-button>
      </div>
    </template>

    <el-tabs v-model="activeTab" class="nc-tabs">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane name="unread">
        <template #label>
          未读
          <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" type="danger" style="margin-left: 4px" />
        </template>
      </el-tab-pane>
      <el-tab-pane label="审批" name="WORKFLOW" />
      <el-tab-pane label="系统" name="SYSTEM" />
    </el-tabs>

    <el-loading v-if="loading" />
    <el-empty v-else-if="filteredNotifications.length === 0" description="暂无通知" :image-size="80" />

    <div v-else class="nc-list">
      <!-- Grouped view when on 'all' tab -->
      <template v-if="activeTab === 'all'">
        <div
          v-for="group in groupedNotifications"
          :key="group.key"
          class="nc-group"
        >
          <div class="nc-group__header">
            <el-icon :size="14"><component :is="groupIcon(group.key)" /></el-icon>
            <span class="nc-group__label">{{ group.label }}</span>
            <el-badge v-if="group.unread > 0" :value="group.unread" type="danger" size="small" />
            <span class="nc-group__count">{{ group.count }} 条</span>
          </div>
          <div
            v-for="item in group.items.slice(0, 3)"
            :key="item.id"
            class="nc-item"
            :class="{ 'nc-item--unread': !item.readAt }"
            @click="handleClick(item)"
          >
            <div class="nc-item__dot" :class="{ 'nc-item__dot--unread': !item.readAt }" />
            <div class="nc-item__content">
              <div class="nc-item__title">{{ item.title }}</div>
              <div class="nc-item__time">{{ timeAgo(item.createdAt) }}</div>
            </div>
          </div>
          <div v-if="group.items.length > 3" class="nc-group__more">
            查看全部 {{ group.count }} 条
          </div>
        </div>
      </template>

      <!-- Flat list for other tabs -->
      <template v-else>
        <div
          v-for="item in filteredNotifications"
          :key="item.id"
          class="nc-item"
          :class="{ 'nc-item--unread': !item.readAt }"
          @click="handleClick(item)"
        >
          <div class="nc-item__dot" :class="{ 'nc-item__dot--unread': !item.readAt }" />
          <div class="nc-item__icon">
            <el-icon :size="16"><component :is="groupIcon(item.messageType)" /></el-icon>
          </div>
          <div class="nc-item__content">
            <div class="nc-item__title">{{ item.title }}</div>
            <div class="nc-item__subtitle">{{ item.content }}</div>
            <div class="nc-item__time">{{ timeAgo(item.createdAt) }}</div>
          </div>
        </div>
      </template>
    </div>
  </el-drawer>
</template>

<style scoped>
.nc-tabs {
  margin-bottom: 12px;
}

.nc-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nc-group {
  border: 1px solid var(--oa-border-light, #e4e7ed);
  border-radius: 8px;
  margin-bottom: 8px;
  overflow: hidden;
}

.nc-group__header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: var(--oa-bg-gray, #f5f7fa);
  font-size: 13px;
  font-weight: 500;
  color: var(--oa-text-primary, #303133);
}

.nc-group__label {
  flex: 1;
}

.nc-group__count {
  font-size: 12px;
  color: var(--oa-text-muted, #909399);
  font-weight: 400;
}

.nc-group__more {
  text-align: center;
  padding: 8px;
  font-size: 12px;
  color: var(--oa-primary, #409eff);
  cursor: pointer;
  border-top: 1px solid var(--oa-border-light, #e4e7ed);
}

.nc-group__more:hover {
  background: var(--oa-primary-bg, #ecf5ff);
}

.nc-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}

.nc-item:hover {
  background: var(--oa-bg-gray, #f5f7fa);
}

.nc-item--unread {
  background: var(--oa-primary-bg, #ecf5ff);
}

.nc-item__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: transparent;
  flex-shrink: 0;
  margin-top: 6px;
}

.nc-item__dot--unread {
  background: var(--oa-danger, #f56c6c);
}

.nc-item__icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--oa-primary-bg, #ecf5ff);
  color: var(--oa-primary, #409eff);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.nc-item__content {
  flex: 1;
  min-width: 0;
}

.nc-item__title {
  font-size: 13px;
  font-weight: 500;
  color: var(--oa-text-primary, #303133);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nc-item__subtitle {
  font-size: 12px;
  color: var(--oa-text-muted, #909399);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nc-item__time {
  font-size: 11px;
  color: var(--oa-text-muted, #909399);
  margin-top: 4px;
}
</style>
