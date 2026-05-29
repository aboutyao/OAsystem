<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Clock, Check } from '@element-plus/icons-vue'

interface TimeSlot {
  startTime: string
  endTime: string
  availableCount: number
  totalUsers: number
  score: number
}

interface Conflict {
  userId: number
  userName: string
  meetingTitle: string
  meetingStart: string
  meetingEnd: string
}

const props = defineProps<{
  participantIds: number[]
  durationMinutes: number
}>()

const emit = defineEmits<{
  select: [slot: TimeSlot]
}>()

const loading = ref(false)
const recommendations = ref<TimeSlot[]>([])
const conflicts = ref<Conflict[]>([])
const selectedDate = ref(new Date().toISOString().split('T')[0])

async function loadRecommendations() {
  loading.value = true
  try {
    const response = await fetch('/api/meetings/smart-schedule/recommend', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userIds: props.participantIds,
        durationMinutes: props.durationMinutes,
        preferredDate: selectedDate.value,
      }),
    })
    const data = await response.json()
    recommendations.value = data.data || []
  } catch (e) {
    console.error('Failed to load recommendations:', e)
  } finally {
    loading.value = false
  }
}

async function checkConflicts() {
  try {
    const now = new Date()
    const response = await fetch('/api/meetings/smart-schedule/check-conflicts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userIds: props.participantIds,
        startTime: now.toISOString(),
        endTime: new Date(now.getTime() + props.durationMinutes * 60000).toISOString(),
      }),
    })
    const data = await response.json()
    conflicts.value = data.data?.conflicts || []
  } catch (e) {
    console.error('Failed to check conflicts:', e)
  }
}

function selectSlot(slot: TimeSlot) {
  emit('select', slot)
}

function formatTime(dateStr: string): string {
  const date = new Date(dateStr)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

function getScoreColor(score: number): string {
  if (score >= 80) return '#67C23A'
  if (score >= 60) return '#E6A23C'
  return '#F56C6C'
}

onMounted(loadRecommendations)
</script>

<template>
  <div class="smart-scheduler">
    <div class="scheduler-header">
      <el-icon><Calendar /></el-icon>
      <span>智能日程协调</span>
    </div>

    <div class="date-picker">
      <el-input v-model="selectedDate" type="date" @change="loadRecommendations" />
    </div>

    <div v-loading="loading" class="recommendations-list">
      <div v-if="recommendations.length === 0 && !loading" class="empty-hint">
        暂无可用时间段
      </div>

      <div v-for="(slot, index) in recommendations" :key="index" class="slot-item" @click="selectSlot(slot)">
        <div class="slot-time">
          <el-icon><Clock /></el-icon>
          <span>{{ formatTime(slot.startTime) }} - {{ formatTime(slot.endTime) }}</span>
        </div>
        <div class="slot-info">
          <span class="slot-availability">{{ slot.availableCount }}/{{ slot.totalUsers }} 可用</span>
          <div class="slot-score" :style="{ color: getScoreColor(slot.score) }">
            {{ slot.score.toFixed(0) }}分
          </div>
        </div>
        <el-icon class="slot-check"><Check /></el-icon>
      </div>
    </div>

    <div v-if="conflicts.length > 0" class="conflicts-section">
      <h4>冲突提醒</h4>
      <div v-for="conflict in conflicts" :key="conflict.userId" class="conflict-item">
        <span class="conflict-user">{{ conflict.userName }}</span>
        <span class="conflict-meeting">{{ conflict.meetingTitle }}</span>
        <span class="conflict-time">{{ formatTime(conflict.meetingStart) }} - {{ formatTime(conflict.meetingEnd) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.smart-scheduler {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.scheduler-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 12px;
}

.date-picker {
  margin-bottom: 12px;
}

.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.slot-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.slot-item:hover {
  border: 1px solid var(--el-color-primary);
}

.slot-time {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.slot-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.slot-availability {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.slot-score {
  font-weight: 600;
}

.slot-check {
  color: var(--el-color-success);
  opacity: 0;
  transition: opacity 0.2s;
}

.slot-item:hover .slot-check {
  opacity: 1;
}

.conflicts-section h4 {
  font-size: 14px;
  margin-bottom: 8px;
  color: var(--el-color-warning);
}

.conflict-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: var(--el-color-warning-light-9);
  border-radius: 6px;
  margin-bottom: 6px;
  font-size: 13px;
}

.conflict-user {
  font-weight: 500;
}

.conflict-meeting {
  color: var(--el-text-color-secondary);
}

.conflict-time {
  margin-left: auto;
  color: var(--el-text-color-secondary);
}

.empty-hint {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 20px;
}
</style>
