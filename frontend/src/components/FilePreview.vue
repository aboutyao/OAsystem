<script setup lang="ts">
import { computed, ref } from 'vue'
import { Document, Picture, VideoPlay } from '@element-plus/icons-vue'

interface Props {
  url: string
  name: string
  visible: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
})

const fileType = computed(() => {
  const ext = props.name.split('.').pop()?.toLowerCase() || ''
  if (['pdf'].includes(ext)) return 'pdf'
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)) return 'image'
  if (['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(ext)) return 'office'
  if (['mp4', 'webm', 'ogg'].includes(ext)) return 'video'
  return 'other'
})

const previewUrl = computed(() => {
  if (fileType.value === 'pdf') {
    return props.url
  }
  if (fileType.value === 'image') {
    return props.url
  }
  // Office 文件使用在线预览服务
  if (fileType.value === 'office') {
    return `https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(props.url)}`
  }
  return ''
})

const canPreview = computed(() => ['pdf', 'image', 'office'].includes(fileType.value))
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="name"
    width="80%"
    destroy-on-close
    @close="dialogVisible = false"
  >
    <div class="preview-container">
      <!-- PDF 预览 -->
      <iframe
        v-if="fileType === 'pdf'"
        :src="previewUrl"
        class="preview-iframe"
      />

      <!-- 图片预览 -->
      <el-image
        v-else-if="fileType === 'image'"
        :src="previewUrl"
        fit="contain"
        class="preview-image"
        :preview-src-list="[previewUrl]"
      />

      <!-- Office 预览 -->
      <iframe
        v-else-if="fileType === 'office'"
        :src="previewUrl"
        class="preview-iframe"
      />

      <!-- 不支持预览 -->
      <div v-else class="preview-unsupported">
        <el-icon :size="64"><Document /></el-icon>
        <p>该文件类型不支持在线预览</p>
        <el-button type="primary" :href="url" download>
          下载文件
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.preview-container {
  width: 100%;
  height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
}

.preview-unsupported {
  text-align: center;
  color: var(--el-text-color-secondary);
}

.preview-unsupported p {
  margin: 16px 0;
}
</style>
