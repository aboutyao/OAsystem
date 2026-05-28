<script setup lang="ts">
import { useOnboarding } from '../composables/useOnboarding'

const { isActive, step, currentStep, totalSteps, isFirst, isLast, next, prev, skip } = useOnboarding()
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="isActive" class="onboarding-overlay">
        <div class="onboarding-backdrop" @click="skip" />

        <div class="onboarding-tooltip" :style="{ top: '50%', left: '50%', transform: 'translate(-50%, -50%)' }">
          <div class="onboarding-tooltip__header">
            <span class="onboarding-step">{{ currentStep + 1 }} / {{ totalSteps }}</span>
            <button class="onboarding-close" @click="skip">跳过</button>
          </div>

          <h3 class="onboarding-title">{{ step?.title }}</h3>
          <p class="onboarding-content">{{ step?.content }}</p>

          <div class="onboarding-tooltip__footer">
            <el-button v-if="!isFirst" text @click="prev">上一步</el-button>
            <div v-else />
            <el-button v-if="isLast" type="primary" @click="next">开始使用</el-button>
            <el-button v-else type="primary" @click="next">下一步</el-button>
          </div>

          <div class="onboarding-progress">
            <div
              v-for="i in totalSteps"
              :key="i"
              class="onboarding-progress__dot"
              :class="{ 'active': i - 1 <= currentStep }"
            />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.onboarding-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
}

.onboarding-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
}

.onboarding-tooltip {
  position: absolute;
  background: white;
  border-radius: 12px;
  padding: 24px;
  width: 400px;
  max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.onboarding-tooltip__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.onboarding-step {
  font-size: 13px;
  color: var(--oa-text-muted, #909399);
}

.onboarding-close {
  background: none;
  border: none;
  color: var(--oa-text-muted, #909399);
  cursor: pointer;
  font-size: 13px;
}

.onboarding-close:hover {
  color: var(--oa-primary, #409eff);
}

.onboarding-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--oa-text-primary, #303133);
  margin: 0 0 8px 0;
}

.onboarding-content {
  font-size: 14px;
  color: var(--oa-text-secondary, #606266);
  line-height: 1.6;
  margin: 0 0 20px 0;
}

.onboarding-tooltip__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.onboarding-progress {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}

.onboarding-progress__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--oa-border-light, #dcdfe6);
  transition: background 0.3s;
}

.onboarding-progress__dot.active {
  background: var(--oa-primary, #409eff);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
