<script setup lang="ts">
import { ref, onMounted } from 'vue'

const props = defineProps<{
  name?: string
  duration?: number
}>()

const transitionName = ref(props.name || 'fade')
const transitionDuration = ref(props.duration || 300)
</script>

<template>
  <Transition
    :name="transitionName"
    :duration="transitionDuration"
    mode="out-in"
  >
    <slot />
  </Transition>
</template>

<style>
/* 淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 滑入滑出 */
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}

.slide-enter-from {
  transform: translateX(20px);
}

.slide-leave-to {
  transform: translateX(-20px);
}

/* 缩放 */
.zoom-enter-active,
.zoom-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.zoom-enter-from,
.zoom-leave-to {
  transform: scale(0.95);
  opacity: 0;
}

/* 从下方滑入 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.slide-up-enter-from {
  transform: translateY(20px);
  opacity: 0;
}

.slide-up-leave-to {
  transform: translateY(-20px);
  opacity: 0;
}

/* 旋转 */
.rotate-enter-active,
.rotate-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.rotate-enter-from {
  transform: rotate(-10deg) scale(0.95);
  opacity: 0;
}

.rotate-leave-to {
  transform: rotate(10deg) scale(0.95);
  opacity: 0;
}
</style>
