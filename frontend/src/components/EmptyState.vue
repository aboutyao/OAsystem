<script setup lang="ts">
import { Document, Plus, Delete, View, Warning, UploadFilled } from '@element-plus/icons-vue'

interface Props {
  /** 描述文字 */
  description?: string
  /** 图标 */
  icon?: object
  /** 按钮文字 */
  actionText?: string
  /** 按钮路径 */
  actionPath?: string
}

withDefaults(defineProps<Props>(), {
  description: '暂无数据',
  icon: Document,
  actionText: '',
  actionPath: '',
})

const emit = defineEmits<{
  action: []
}>()
</script>

<template>
  <div class="empty-state">
    <div class="empty-state__icon">
      <el-icon :size="64" color="var(--el-color-info-light-3)"><component :is="icon" /></el-icon>
    </div>
    <p class="empty-state__description">{{ description }}</p>
    <el-button v-if="actionText" type="primary" @click="actionPath ? $router.push(actionPath) : emit('action')">
      {{ actionText }}
    </el-button>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-state__icon {
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-state__description {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 20px;
}
</style>
