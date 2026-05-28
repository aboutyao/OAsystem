<script setup lang="ts">
import { useDragDrop } from '../composables/useDragDrop'

const props = withDefaults(
  defineProps<{
    accept?: string
    multiple?: boolean
  }>(),
  {
    multiple: true,
  },
)

const emit = defineEmits<{
  drop: [files: File[]]
}>()

const { isDragging, dragHandlers } = useDragDrop((files) => {
  const result = props.multiple ? files : files.slice(0, 1)
  emit('drop', result)
}, { accept: props.accept })
</script>

<template>
  <div
    v-on="dragHandlers"
    class="drop-zone"
    :class="{ 'drop-zone--active': isDragging }"
  >
    <slot v-if="!isDragging" />
    <div v-else class="drop-zone__overlay">
      <div class="drop-zone__icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
          <polyline points="17 8 12 3 7 8" />
          <line x1="12" y1="3" x2="12" y2="15" />
        </svg>
      </div>
      <p class="drop-zone__text">
        松开鼠标以上传文件
      </p>
      <p v-if="accept" class="drop-zone__hint">
        支持格式：{{ accept }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.drop-zone {
  position: relative;
  min-height: 120px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  transition: border-color 0.2s, background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.drop-zone--active {
  border-color: #409eff;
  background-color: rgba(64, 158, 255, 0.06);
}

.drop-zone__overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
}

.drop-zone__icon {
  color: #409eff;
  margin-bottom: 12px;
}

.drop-zone__text {
  font-size: 16px;
  color: #303133;
  margin: 0 0 4px;
}

.drop-zone__hint {
  font-size: 12px;
  color: #909399;
  margin: 0;
}
</style>
