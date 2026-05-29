<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, Refresh, Check } from '@element-plus/icons-vue'

const emit = defineEmits<{
  capture: [file: File]
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const stream = ref<MediaStream | null>(null)
const capturedImage = ref<string | null>(null)
const isCapturing = ref(false)

async function startCamera() {
  try {
    stream.value = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment' }
    })
    if (videoRef.value) {
      videoRef.value.srcObject = stream.value
    }
  } catch (e) {
    ElMessage.error('无法访问摄像头')
  }
}

function stopCamera() {
  if (stream.value) {
    stream.value.getTracks().forEach(track => track.stop())
    stream.value = null
  }
}

function capture() {
  if (!videoRef.value || !canvasRef.value) return

  isCapturing.value = true

  const video = videoRef.value
  const canvas = canvasRef.value
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight

  const ctx = canvas.getContext('2d')
  if (ctx) {
    ctx.drawImage(video, 0, 0)
    capturedImage.value = canvas.toDataURL('image/jpeg', 0.8)
  }

  isCapturing.value = false
}

function retake() {
  capturedImage.value = null
}

function confirm() {
  if (!capturedImage.value || !canvasRef.value) return

  canvasRef.value.toBlob((blob) => {
    if (blob) {
      const file = new File([blob], `capture_${Date.now()}.jpg`, { type: 'image/jpeg' })
      emit('capture', file)
      capturedImage.value = null
    }
  }, 'image/jpeg', 0.8)
}

onMounted(() => {
  startCamera()
})

onUnmounted(() => {
  stopCamera()
})
</script>

<template>
  <div class="camera-capture">
    <div class="camera-container" v-show="!capturedImage">
      <video ref="videoRef" autoplay playsinline class="camera-preview" />
      <canvas ref="canvasRef" style="display: none" />
    </div>

    <div v-if="capturedImage" class="captured-preview">
      <img :src="capturedImage" alt="拍照结果" />
    </div>

    <div class="camera-actions">
      <template v-if="!capturedImage">
        <el-button type="primary" :icon="Camera" :loading="isCapturing" @click="capture">
          拍照
        </el-button>
      </template>
      <template v-else>
        <el-button :icon="Refresh" @click="retake">重拍</el-button>
        <el-button type="success" :icon="Check" @click="confirm">确认</el-button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.camera-capture {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.camera-container {
  position: relative;
  width: 100%;
  max-width: 400px;
  margin: 0 auto 16px;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
}

.camera-preview {
  width: 100%;
  display: block;
}

.captured-preview {
  max-width: 400px;
  margin: 0 auto 16px;
}

.captured-preview img {
  width: 100%;
  border-radius: 8px;
}

.camera-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
