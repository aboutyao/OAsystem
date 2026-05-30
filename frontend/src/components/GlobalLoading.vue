<script setup lang="ts">
import { isLoading } from '../composables/useLoading'
</script>

<template>
  <Transition name="fade">
    <div v-if="isLoading" class="global-loading">
      <div class="global-loading__spinner">
        <el-icon class="is-loading" :size="24"><Refresh /></el-icon>
        <span class="global-loading__text">加载中...</span>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.global-loading {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.global-loading__spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.global-loading__text {
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
