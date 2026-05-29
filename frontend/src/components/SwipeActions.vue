<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  leftAction?: string
  rightAction?: string
  leftColor?: string
  rightColor?: string
}

const props = withDefaults(defineProps<Props>(), {
  leftAction: '通过',
  rightAction: '驳回',
  leftColor: '#67C23A',
  rightColor: '#F56C6C',
})

const emit = defineEmits<{
  'swipe-left': []
  'swipe-right': []
}>()

const startX = ref(0)
const currentX = ref(0)
const isDragging = ref(false)
const threshold = 100

function onTouchStart(e: TouchEvent) {
  startX.value = e.touches[0].clientX
  isDragging.value = true
}

function onTouchMove(e: TouchEvent) {
  if (!isDragging.value) return
  currentX.value = e.touches[0].clientX - startX.value
}

function onTouchEnd() {
  isDragging.value = false

  if (currentX.value > threshold) {
    emit('swipe-left')
  } else if (currentX.value < -threshold) {
    emit('swipe-right')
  }

  currentX.value = 0
}

function getTransform() {
  return `translateX(${currentX.value}px)`
}

function getLeftOpacity() {
  return Math.min(currentX.value / threshold, 1)
}

function getRightOpacity() {
  return Math.min(-currentX.value / threshold, 1)
}
</script>

<template>
  <div class="swipe-actions-container">
    <!-- 左滑动作背景 -->
    <div
      class="action-bg left"
      :style="{ backgroundColor: leftColor, opacity: getLeftOpacity() }"
    >
      <span>{{ leftAction }}</span>
    </div>

    <!-- 右滑动作背景 -->
    <div
      class="action-bg right"
      :style="{ backgroundColor: rightColor, opacity: getRightOpacity() }"
    >
      <span>{{ rightAction }}</span>
    </div>

    <!-- 内容区域 -->
    <div
      class="swipe-content"
      :style="{ transform: getTransform() }"
      @touchstart="onTouchStart"
      @touchmove="onTouchMove"
      @touchend="onTouchEnd"
    >
      <slot />
    </div>
  </div>
</template>

<style scoped>
.swipe-actions-container {
  position: relative;
  overflow: hidden;
  border-radius: 8px;
}

.action-bg {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 500;
}

.action-bg.left {
  left: 0;
}

.action-bg.right {
  right: 0;
}

.swipe-content {
  position: relative;
  background: white;
  transition: transform 0.1s ease;
  z-index: 1;
}
</style>
