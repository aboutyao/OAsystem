<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'
import { Document, Plus, Delete, View, Warning, UploadFilled, Refresh } from '@element-plus/icons-vue'

interface Props {
  /** Fallback UI title */
  title?: string
  /** Show technical details */
  showDetails?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '页面出现错误',
  showDetails: true,
})

const error = ref<Error | null>(null)
const errorInfo = ref('')

onErrorCaptured((err, instance, info) => {
  error.value = err
  errorInfo.value = info
  // Prevent error from propagating further
  return false
})

function retry() {
  error.value = null
  errorInfo.value = ''
}

function reload() {
  window.location.reload()
}
</script>

<template>
  <div v-if="error" class="error-boundary">
    <div class="error-boundary__content">
      <div class="error-boundary__icon">
        <el-icon :size="48"><Warning /></el-icon>
      </div>
      <h2 class="error-boundary__title">{{ title }}</h2>
      <p class="error-boundary__message">
        抱歉，页面遇到了一个错误。您可以尝试刷新页面或联系管理员。
      </p>
      <div v-if="showDetails && error" class="error-boundary__details">
        <el-collapse>
          <el-collapse-item title="技术详情">
            <pre class="error-boundary__stack">{{ error.message }}</pre>
            <pre v-if="errorInfo" class="error-boundary__info">{{ errorInfo }}</pre>
          </el-collapse-item>
        </el-collapse>
      </div>
      <div class="error-boundary__actions">
        <el-button type="primary" @click="retry">
          <el-icon><Refresh /></el-icon>
          重试
        </el-button>
        <el-button @click="reload">刷新页面</el-button>
      </div>
    </div>
  </div>
  <slot v-else />
</template>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 40px;
}

.error-boundary__content {
  text-align: center;
  max-width: 480px;
}

.error-boundary__icon {
  color: var(--el-color-danger);
  margin-bottom: 24px;
}

.error-boundary__title {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 12px;
}

.error-boundary__message {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 24px;
  line-height: 1.6;
}

.error-boundary__details {
  text-align: left;
  margin-bottom: 24px;
}

.error-boundary__stack,
.error-boundary__info {
  font-family: monospace;
  font-size: 12px;
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.error-boundary__actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
