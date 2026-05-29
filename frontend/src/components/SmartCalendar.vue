<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { http } from '../api/http'

const visible = ref(false)
const currentDate = ref(new Date())
const teamLeaves = ref<TeamLeave[]>([])
const meetingBookings = ref<MeetingBooking[]>([])
const loading = ref(false)

interface TeamLeave {
  id: number
  userName: string
  leaveType: string
  startAt: string
  endAt: string
  status: string
}

interface MeetingBooking {
  id: number
  title: string
  roomName: string
  startAt: string
  endAt: string
  organizerName: string
}

const weekDays = ['一', '二', '三', '四', '五', '六', '日']

const monthLabel = computed(() => {
  const d = currentDate.value
  return `${d.getFullYear()}年${d.getMonth() + 1}月`
})

const calendarDays = computed(() => {
  const d = currentDate.value
  const year = d.getFullYear()
  const month = d.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startOffset = (firstDay.getDay() + 6) % 7
  const days: CalendarDay[] = []

  for (let i = 0; i < startOffset; i++) {
    const prev = new Date(year, month, -startOffset + i + 1)
    days.push({ date: prev, isCurrentMonth: false, isToday: false })
  }

  const today = new Date()
  for (let i = 1; i <= lastDay.getDate(); i++) {
    const date = new Date(year, month, i)
    const isToday = date.toDateString() === today.toDateString()
    days.push({ date, isCurrentMonth: true, isToday })
  }

  const remaining = 42 - days.length
  for (let i = 1; i <= remaining; i++) {
    const next = new Date(year, month + 1, i)
    days.push({ date: next, isCurrentMonth: false, isToday: false })
  }

  return days
})

interface CalendarDay {
  date: Date
  isCurrentMonth: boolean
  isToday: boolean
}

function dateKey(d: Date): string {
  return d.toISOString().split('T')[0]
}

function leavesForDay(d: Date): TeamLeave[] {
  const key = dateKey(d)
  return teamLeaves.value.filter((l) => {
    const start = l.startAt.split('T')[0]
    const end = l.endAt.split('T')[0]
    return key >= start && key <= end
  })
}

function meetingsForDay(d: Date): MeetingBooking[] {
  const key = dateKey(d)
  return meetingBookings.value.filter((b) => {
    const start = b.startAt.split('T')[0]
    return start === key
  })
}

function leaveTypeColor(type: string): string {
  const colors: Record<string, string> = {
    ANNUAL: '#67C23A',
    SICK: '#E6A23C',
    PERSONAL: '#409EFF',
    MATERNITY: '#F56C6C',
    PATERNITY: '#909399',
    MARRIAGE: '#E6A23C',
    BEREAVEMENT: '#909399',
  }
  return colors[type] || '#409EFF'
}

function prevMonth() {
  const d = new Date(currentDate.value)
  d.setMonth(d.getMonth() - 1)
  currentDate.value = d
}

function nextMonth() {
  const d = new Date(currentDate.value)
  d.setMonth(d.getMonth() + 1)
  currentDate.value = d
}

function goToday() {
  currentDate.value = new Date()
}

async function fetchData() {
  loading.value = true
  try {
    const year = currentDate.value.getFullYear()
    const month = currentDate.value.getMonth() + 1
    const [leaves, bookings] = await Promise.all([
      http.get('/calendar/team-leaves', { params: { year, month } }).catch(() => []),
      http.get('/calendar/meetings', { params: { year, month } }).catch(() => []),
    ])
    teamLeaves.value = (leaves as any) || []
    meetingBookings.value = (bookings as any) || []
  } catch {
    teamLeaves.value = []
    meetingBookings.value = []
  } finally {
    loading.value = false
  }
}

function open() {
  visible.value = true
  currentDate.value = new Date()
  fetchData()
}

watch(currentDate, () => fetchData())

defineExpose({ open })
</script>

<template>
  <el-drawer
    v-model="visible"
    title="智能日历"
    direction="rtl"
    size="480px"
  >
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between; width: 100%">
        <div style="display: flex; align-items: center; gap: 12px">
          <el-button :icon="ArrowLeft" text @click="prevMonth" />
          <span style="font-weight: 600; font-size: 16px; min-width: 100px; text-align: center">{{ monthLabel }}</span>
          <el-button :icon="ArrowRight" text @click="nextMonth" />
        </div>
        <el-button size="small" @click="goToday">今天</el-button>
      </div>
    </template>

    <div class="smart-calendar">
      <!-- Week header -->
      <div class="cal-weekdays">
        <div v-for="day in weekDays" :key="day" class="cal-weekday">{{ day }}</div>
      </div>

      <!-- Calendar grid -->
      <div class="cal-grid">
        <div
          v-for="(day, idx) in calendarDays"
          :key="idx"
          class="cal-cell"
          :class="{
            'cal-cell--other': !day.isCurrentMonth,
            'cal-cell--today': day.isToday,
          }"
        >
          <div class="cal-cell__date" :class="{ 'cal-cell__date--today': day.isToday }">
            {{ day.date.getDate() }}
          </div>
          <div class="cal-cell__events">
            <div
              v-for="leave in leavesForDay(day.date).slice(0, 2)"
              :key="'l' + leave.id"
              class="cal-event cal-event--leave"
              :style="{ borderLeftColor: leaveTypeColor(leave.leaveType) }"
              :title="`${leave.userName} ${leave.leaveType}`"
            >
              {{ leave.userName }}
            </div>
            <div
              v-for="meeting in meetingsForDay(day.date).slice(0, 1)"
              :key="'m' + meeting.id"
              class="cal-event cal-event--meeting"
              :title="meeting.title"
            >
              {{ meeting.title }}
            </div>
            <div
              v-if="(leavesForDay(day.date).length + meetingsForDay(day.date).length) > 3"
              class="cal-more"
            >
              +{{ (leavesForDay(day.date).length + meetingsForDay(day.date).length) - 3 }}
            </div>
          </div>
        </div>
      </div>

      <!-- Legend -->
      <div class="cal-legend">
        <div class="cal-legend__item">
          <span class="cal-legend__dot" style="background: #67C23A" />
          <span>年假</span>
        </div>
        <div class="cal-legend__item">
          <span class="cal-legend__dot" style="background: #E6A23C" />
          <span>病假/婚假</span>
        </div>
        <div class="cal-legend__item">
          <span class="cal-legend__dot" style="background: #409EFF" />
          <span>事假</span>
        </div>
        <div class="cal-legend__item">
          <span class="cal-legend__dot" style="background: #F56C6C" />
          <span>产假</span>
        </div>
        <div class="cal-legend__item">
          <span class="cal-legend__dot" style="background: #909399" />
          <span>会议</span>
        </div>
      </div>

      <!-- Today summary -->
      <div class="cal-summary">
        <h4>今日概览</h4>
        <div class="cal-summary__items">
          <div class="cal-summary__item">
            <span class="cal-summary__label">团队请假</span>
            <span class="cal-summary__value">{{ leavesForDay(new Date()).length }} 人</span>
          </div>
          <div class="cal-summary__item">
            <span class="cal-summary__label">今日会议</span>
            <span class="cal-summary__value">{{ meetingsForDay(new Date()).length }} 场</span>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.smart-calendar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
}

.cal-weekday {
  font-size: 12px;
  font-weight: 600;
  color: var(--oa-text-muted, #909399);
  padding: 8px 0;
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.cal-cell {
  min-height: 72px;
  border: 1px solid var(--oa-border-light, #ebeef5);
  border-radius: 4px;
  padding: 4px;
  transition: background 0.15s;
}

.cal-cell:hover {
  background: var(--oa-bg-gray, #f5f7fa);
}

.cal-cell--other {
  opacity: 0.35;
}

.cal-cell--today {
  border-color: var(--oa-primary, #409eff);
  background: var(--oa-primary-bg, #ecf5ff);
}

.cal-cell__date {
  font-size: 12px;
  color: var(--oa-text-secondary, #606266);
  text-align: right;
  padding: 2px 4px;
}

.cal-cell__date--today {
  background: var(--oa-primary, #409eff);
  color: #fff;
  border-radius: 50%;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  float: right;
}

.cal-cell__events {
  display: flex;
  flex-direction: column;
  gap: 2px;
  clear: both;
}

.cal-event {
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.cal-event--leave {
  background: rgba(103, 194, 58, 0.1);
  border-left: 2px solid #67C23A;
  color: #67C23A;
}

.cal-event--meeting {
  background: rgba(144, 147, 153, 0.1);
  border-left: 2px solid #909399;
  color: #909399;
}

.cal-more {
  font-size: 10px;
  color: var(--oa-text-muted, #909399);
  text-align: center;
}

.cal-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 8px 0;
}

.cal-legend__item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--oa-text-secondary, #606266);
}

.cal-legend__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.cal-summary {
  background: var(--oa-bg-gray, #f5f7fa);
  border-radius: 8px;
  padding: 16px;
}

.cal-summary h4 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 12px 0;
  color: var(--oa-text-primary, #303133);
}

.cal-summary__items {
  display: flex;
  gap: 24px;
}

.cal-summary__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cal-summary__label {
  font-size: 12px;
  color: var(--oa-text-muted, #909399);
}

.cal-summary__value {
  font-size: 20px;
  font-weight: 700;
  color: var(--oa-primary, #409eff);
}
</style>
