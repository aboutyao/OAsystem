<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { teamLeaveCalendar } from '../../../api/oa-leaves'
import type { JsonObject } from '../../../api/types'

const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth() + 1)

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']

const LEAVE_TYPE_COLORS: Record<string, string> = {
  ANNUAL: '#409EFF',
  SICK: '#E6A23C',
  PERSONAL: '#909399',
  MATERNITY: '#F56C6C',
  PATERNITY: '#67C23A',
  MARRIAGE: '#E040FB',
  BEREAVEMENT: '#78909C',
}

const LEAVE_TYPE_LABELS: Record<string, string> = {
  ANNUAL: '年假',
  SICK: '病假',
  PERSONAL: '事假',
  MATERNITY: '产假',
  PATERNITY: '陪产假',
  MARRIAGE: '婚假',
  BEREAVEMENT: '丧假',
}

const loading = ref(false)
const leaveData = ref<JsonObject[]>([])

const monthTitle = computed(() => `${currentYear.value} 年 ${currentMonth.value} 月`)

const daysInMonth = computed(() => {
  return new Date(currentYear.value, currentMonth.value, 0).getDate()
})

const firstDayOfWeek = computed(() => {
  return new Date(currentYear.value, currentMonth.value - 1, 1).getDay()
})

const calendarDays = computed(() => {
  const days: { day: number; isCurrentMonth: boolean }[] = []
  // Fill leading empty cells
  for (let i = 0; i < firstDayOfWeek.value; i++) {
    days.push({ day: 0, isCurrentMonth: false })
  }
  for (let d = 1; d <= daysInMonth.value; d++) {
    days.push({ day: d, isCurrentMonth: true })
  }
  return days
})

// Map: date string -> array of leave entries
const leaveMap = computed(() => {
  const map: Record<string, JsonObject[]> = {}
  for (const item of leaveData.value) {
    const start = parseDateOnly(String(item.startAt ?? ''))
    const end = parseDateOnly(String(item.endAt ?? ''))
    if (!start || !end) continue

    const startY = start.getFullYear()
    const startM = start.getMonth() + 1
    const startD = start.getDate()
    const endY = end.getFullYear()
    const endM = end.getMonth() + 1
    const endD = end.getDate()

    // Iterate over each day this leave covers
    const cur = new Date(startY, startM - 1, startD)
    const endDate = new Date(endY, endM - 1, endD)
    while (cur <= endDate) {
      if (cur.getFullYear() === currentYear.value && cur.getMonth() + 1 === currentMonth.value) {
        const key = `${cur.getFullYear()}-${String(cur.getMonth() + 1).padStart(2, '0')}-${String(cur.getDate()).padStart(2, '0')}`
        if (!map[key]) map[key] = []
        map[key].push(item)
      }
      cur.setDate(cur.getDate() + 1)
    }
  }
  return map
})

function parseDateOnly(s: string): Date | null {
  if (!s) return null
  // Handle ISO format like 2024-01-15T10:00:00 or 2024-01-15
  const d = new Date(s)
  return Number.isNaN(d.getTime()) ? null : d
}

function getLeavesForDay(day: number): JsonObject[] {
  if (day <= 0) return []
  const key = `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  return leaveMap.value[key] || []
}

function isToday(day: number): boolean {
  return (
    day === today.getDate() &&
    currentMonth.value === today.getMonth() + 1 &&
    currentYear.value === today.getFullYear()
  )
}

function prevMonth() {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

function nextMonth() {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

function goToday() {
  currentYear.value = today.getFullYear()
  currentMonth.value = today.getMonth() + 1
}

async function loadData() {
  loading.value = true
  try {
    const startDate = `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}-01`
    const lastDay = daysInMonth.value
    const endDate = `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
    leaveData.value = await teamLeaveCalendar(startDate, endDate)
  } catch {
    leaveData.value = []
  } finally {
    loading.value = false
  }
}

watch([currentYear, currentMonth], loadData)
onMounted(loadData)

function typeColor(type: string): string {
  return LEAVE_TYPE_COLORS[type] || '#909399'
}

function typeLabel(type: string): string {
  return LEAVE_TYPE_LABELS[type] || type
}
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">团队请假日历</h2>
        <p class="muted">查看团队成员的请假安排</p>
      </div>
    </div>

    <el-card shadow="never">
      <!-- Calendar Header -->
      <div class="calendar-header">
        <el-button @click="prevMonth" :icon="'ArrowLeft'" />
        <span class="calendar-header__title">{{ monthTitle }}</span>
        <el-button @click="nextMonth" :icon="'ArrowRight'" />
        <el-button size="small" @click="goToday" style="margin-left: 12px">今天</el-button>
      </div>

      <!-- Weekday Headers -->
      <div class="calendar-grid">
        <div
          v-for="wd in WEEKDAYS"
          :key="wd"
          class="calendar-grid__weekday"
        >
          {{ wd }}
        </div>

        <!-- Day Cells -->
        <div
          v-for="(cell, idx) in calendarDays"
          :key="idx"
          class="calendar-grid__cell"
          :class="{
            'calendar-grid__cell--empty': !cell.isCurrentMonth,
            'calendar-grid__cell--today': cell.isCurrentMonth && isToday(cell.day),
          }"
        >
          <template v-if="cell.isCurrentMonth">
            <div class="calendar-grid__day" :class="{ 'calendar-grid__day--today': isToday(cell.day) }">
              {{ cell.day }}
            </div>
            <div class="calendar-grid__leaves">
              <div
                v-for="(leave, li) in getLeavesForDay(cell.day).slice(0, 3)"
                :key="li"
                class="calendar-grid__leave-tag"
                :style="{ backgroundColor: typeColor(String(leave.leaveType)) }"
                :title="`${leave.userName} - ${typeLabel(String(leave.leaveType))}`"
              >
                <span class="calendar-grid__leave-name">{{ leave.userName }}</span>
                <span class="calendar-grid__leave-type">{{ typeLabel(String(leave.leaveType)) }}</span>
              </div>
              <div
                v-if="getLeavesForDay(cell.day).length > 3"
                class="calendar-grid__more"
              >
                +{{ getLeavesForDay(cell.day).length - 3 }} 人
              </div>
            </div>
          </template>
        </div>
      </div>

      <!-- Legend -->
      <div class="calendar-legend">
        <span class="calendar-legend__label">图例：</span>
        <div
          v-for="(label, code) in LEAVE_TYPE_LABELS"
          :key="code"
          class="calendar-legend__item"
        >
          <span
            class="calendar-legend__dot"
            :style="{ backgroundColor: LEAVE_TYPE_COLORS[code] }"
          />
          {{ label }}
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.calendar-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 20px;
}

.calendar-header__title {
  font-size: 18px;
  font-weight: 700;
  color: var(--oa-text-primary);
  min-width: 120px;
  text-align: center;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 1px;
  background: var(--oa-border-light);
  border: 1px solid var(--oa-border-light);
  border-radius: var(--oa-radius-md);
  overflow: hidden;
}

.calendar-grid__weekday {
  padding: 10px 4px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--oa-text-secondary);
  background: var(--oa-bg);
}

.calendar-grid__cell {
  min-height: 100px;
  padding: 6px;
  background: var(--oa-bg-white);
  vertical-align: top;
}

.calendar-grid__cell--empty {
  background: var(--oa-bg);
  opacity: 0.5;
}

.calendar-grid__cell--today {
  background: #ecf5ff;
}

.calendar-grid__day {
  font-size: 13px;
  font-weight: 500;
  color: var(--oa-text-primary);
  margin-bottom: 4px;
}

.calendar-grid__day--today {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--oa-primary);
  color: #fff;
  font-weight: 700;
}

.calendar-grid__leaves {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.calendar-grid__leave-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 11px;
  color: #fff;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.calendar-grid__leave-name {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
}

.calendar-grid__leave-type {
  opacity: 0.85;
  flex-shrink: 0;
}

.calendar-grid__more {
  font-size: 11px;
  color: var(--oa-text-muted);
  padding: 1px 4px;
}

.calendar-legend {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--oa-border-light);
  flex-wrap: wrap;
}

.calendar-legend__label {
  font-size: 13px;
  color: var(--oa-text-secondary);
  font-weight: 600;
}

.calendar-legend__item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--oa-text-secondary);
}

.calendar-legend__dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  flex-shrink: 0;
}
</style>
