<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import {
  cancelMeetingBooking,
  createMeetingBooking,
  listMeetingBookings,
  listMeetingRooms,
  meetingRoomAvailability,
} from '../../api/meetings'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'
import { ElMessage, ElMessageBox } from 'element-plus'

const authStore = useAuthStore()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const filterRoomId = ref<number | undefined>(undefined)

const allRooms = ref<JsonObject[]>([])
const enabledRooms = computed(() => allRooms.value.filter((r) => String(r.status) === 'ENABLED'))
const availability = ref<JsonObject[]>([])
const availLoading = ref(false)

const canBook = computed(() => {
  const p = authStore.user?.permissions ?? []
  return p.includes('*') || p.includes('org:create')
})

const dialogVisible = ref(false)
const form = reactive({
  roomId: undefined as number | undefined,
  title: '',
  startAt: '',
  endAt: '',
  participantCount: 1,
})

async function loadRooms() {
  const res = await listMeetingRooms(1, 500)
  allRooms.value = res.items
}

async function loadAvailability() {
  if (form.roomId == null) {
    availability.value = []
    return
  }
  availLoading.value = true
  try {
    availability.value = await meetingRoomAvailability(form.roomId)
  } catch {
    availability.value = []
  } finally {
    availLoading.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const res = await listMeetingBookings(page.value, size.value, filterRoomId.value)
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void loadRooms().then(() => void load())

function handleSizeChange() {
  page.value = 1
  load()
}

function openCreate() {
  form.roomId = enabledRooms.value[0] ? Number(enabledRooms.value[0].id) : undefined
  form.title = ''
  form.startAt = ''
  form.endAt = ''
  form.participantCount = 1
  dialogVisible.value = true
  void loadAvailability()
}

watch(
  () => form.roomId,
  () => void loadAvailability(),
)

async function submitBooking() {
  if (form.roomId == null || !form.title.trim() || !form.startAt || !form.endAt) {
    ElMessage.warning('请填写会议室、主题与起止时间')
    return
  }
  try {
    await createMeetingBooking({
      roomId: form.roomId,
      title: form.title.trim(),
      startAt: form.startAt,
      endAt: form.endAt,
      participantCount: form.participantCount,
    })
    ElMessage.success('预约已创建')
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '预约失败')
  }
}

async function onCancel(row: JsonObject) {
  try {
    await ElMessageBox.confirm('确定取消该预约？', '取消预约', { type: 'warning' })
  } catch {
    return
  }
  try {
    await cancelMeetingBooking(Number(row.id), {})
    ElMessage.success('已取消')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '取消失败')
  }
}

function bookingStatusLabel(s: string) {
  if (s === 'BOOKED') return '已预约'
  if (s === 'CANCELLED') return '已取消'
  if (s === 'FINISHED') return '已结束'
  return s || '—'
}

watch([page, size], () => void load())
watch(filterRoomId, () => {
  page.value = 1
  void load()
})
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">会议预约</h2>
        <p class="muted">
          新建预约前请确认时段不与已有预约重叠；超级管理员可查看全员预约。
          <RouterLink to="/meetings/rooms">会议室档案</RouterLink>
        </p>
      </div>
      <div class="oa-page__actions">
        <el-select
          v-model="filterRoomId"
          clearable
          placeholder="按会议室筛选"
          style="width: 200px; margin-right: 12px"
        >
          <el-option v-for="r in allRooms" :key="Number(r.id)" :label="String(r.roomName)" :value="Number(r.id)" />
        </el-select>
        <el-button v-if="canBook" type="primary" @click="openCreate">新建预约</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="roomName" label="会议室" width="140" />
        <el-table-column prop="title" label="主题" min-width="160" />
        <el-table-column label="开始" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.startAt) }}</template>
        </el-table-column>
        <el-table-column label="结束" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.endAt) }}</template>
        </el-table-column>
        <el-table-column prop="participantCount" label="人数" width="72" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ bookingStatusLabel(String(row.status ?? '')) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              v-if="String(row.status) === 'BOOKED' && canBook"
              link
              type="danger"
              @click="onCancel(row)"
            >
              取消
            </el-button>
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

    <el-dialog v-model="dialogVisible" title="新建预约" width="560px" destroy-on-close @opened="void loadRooms()">
      <el-form label-width="96px">
        <el-form-item label="会议室" required>
          <el-select v-model="form.roomId" placeholder="选择会议室" style="width: 100%" filterable>
            <el-option v-for="r in enabledRooms" :key="Number(r.id)" :label="String(r.roomName)" :value="Number(r.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="近7天占用">
          <div v-loading="availLoading" class="muted" style="font-size: 13px">
            <template v-if="availability.length === 0">暂无已确认预约或尚未选择会议室</template>
            <ul v-else class="meeting-avail">
              <li v-for="(s, i) in availability" :key="i">
                {{ formatDisplayDateTime(s.startAt) }} — {{ formatDisplayDateTime(s.endAt) }} · {{ s.title }}
              </li>
            </ul>
          </div>
        </el-form-item>
        <el-form-item label="主题" required>
          <el-input v-model="form.title" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker
            v-model="form.startAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            style="width: 100%"
            placeholder="开始"
          />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker
            v-model="form.endAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            style="width: 100%"
            placeholder="结束"
          />
        </el-form-item>
        <el-form-item label="参会人数">
          <el-input-number v-model="form.participantCount" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="submitBooking">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.meeting-avail {
  margin: 0;
  padding-left: 1.2em;
  max-height: 160px;
  overflow: auto;
}
</style>
