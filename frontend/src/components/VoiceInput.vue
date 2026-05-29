<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Microphone, Loading } from '@element-plus/icons-vue'

const emit = defineEmits<{
  result: [text: string]
}>()

const isRecording = ref(false)
const recognition = ref<any>(null)
const transcript = ref('')

function startRecording() {
  if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
    ElMessage.error('您的浏览器不支持语音识别')
    return
  }

  const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  recognition.value = new SpeechRecognition()
  recognition.value.continuous = false
  recognition.value.interimResults = true
  recognition.value.lang = 'zh-CN'

  recognition.value.onstart = () => {
    isRecording.value = true
    transcript.value = ''
  }

  recognition.value.onresult = (event: any) => {
    let finalTranscript = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      if (event.results[i].isFinal) {
        finalTranscript += event.results[i][0].transcript
      }
    }
    transcript.value = finalTranscript
  }

  recognition.value.onend = () => {
    isRecording.value = false
    if (transcript.value) {
      emit('result', transcript.value)
    }
  }

  recognition.value.onerror = (event: any) => {
    isRecording.value = false
    ElMessage.error('语音识别失败: ' + event.error)
  }

  recognition.value.start()
}

function stopRecording() {
  if (recognition.value) {
    recognition.value.stop()
  }
}

onUnmounted(() => {
  if (recognition.value) {
    recognition.value.stop()
  }
})
</script>

<template>
  <div class="voice-input">
    <el-button
      :type="isRecording ? 'danger' : 'primary'"
      :icon="isRecording ? Loading : Microphone"
      :loading="isRecording"
      @click="isRecording ? stopRecording() : startRecording()"
    >
      {{ isRecording ? '停止录音' : '语音输入' }}
    </el-button>

    <div v-if="transcript" class="transcript">
      <span class="label">识别结果：</span>
      <span class="text">{{ transcript }}</span>
    </div>
  </div>
</template>

<style scoped>
.voice-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.transcript {
  padding: 8px 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  font-size: 13px;
}

.transcript .label {
  color: var(--el-text-color-secondary);
}

.transcript .text {
  color: var(--el-text-color-primary);
}
</style>
