<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Scan, Close } from '@element-plus/icons-vue'

const emit = defineEmits<{
  scan: [result: string]
  close: []
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const stream = ref<MediaStream | null>(null)
const isScanning = ref(false)

async function startScanner() {
  try {
    stream.value = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment' }
    })
    if (videoRef.value) {
      videoRef.value.srcObject = stream.value
      isScanning.value = true
      scanFrame()
    }
  } catch (e) {
    ElMessage.error('无法访问摄像头')
  }
}

function stopScanner() {
  if (stream.value) {
    stream.value.getTracks().forEach(track => track.stop())
    stream.value = null
  }
  isScanning.value = false
}

function scanFrame() {
  if (!isScanning.value || !videoRef.value) return

  // 这里应该使用 QR 扫描库（如 jsQR）
  // 暂时只演示视频流

  requestAnimationFrame(scanFrame)
}

function close() {
  stopScanner()
  emit('close')
}

onMounted(() => {
  startScanner()
})

onUnmounted(() => {
  stopScanner()
})
</script>

<template>
  <div class="qr-scanner">
    <div class="scanner-header">
      <span>扫描二维码</span>
      <el-button text :icon="Close" @click="close" />
    </div>

    <div class="scanner-container">
      <video ref="videoRef" autoplay playsinline class="scanner-preview" />
      <div class="scanner-overlay">
        <div class="scanner-frame"></div>
      </div>
    </div>

    <div class="scanner-tip">
      请将二维码放入框内
    </div>
  </div>
</template>

<style scoped>
.qr-scanner {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.scanner-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 500;
}

.scanner-container {
  position: relative;
  width: 100%;
  max-width: 300px;
  margin: 0 auto 12px;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
}

.scanner-preview {
  width: 100%;
  display: block;
}

.scanner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.scanner-frame {
  width: 200px;
  height: 200px;
  border: 2px solid rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.5);
}

.scanner-tip {
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
