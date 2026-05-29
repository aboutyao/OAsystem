<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, At, Send } from '@element-plus/icons-vue'

interface Comment {
  id: number
  content: string
  authorId: number
  authorName: string
  parentId: number | null
  createdAt: string
  mentions: string | null
}

const props = defineProps<{
  entityType: string
  entityId: number
}>()

const comments = ref<Comment[]>([])
const newComment = ref('')
const loading = ref(false)
const submitting = ref(false)

async function loadComments() {
  loading.value = true
  try {
    const response = await fetch(`/api/discussions/${props.entityType}/${props.entityId}`)
    const data = await response.json()
    comments.value = data.data || []
  } catch (e) {
    console.error('Failed to load comments:', e)
  } finally {
    loading.value = false
  }
}

async function submitComment() {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  submitting.value = true
  try {
    await fetch(`/api/discussions/${props.entityType}/${props.entityId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        content: newComment.value,
        parentId: null,
      }),
    })
    ElMessage.success('评论成功')
    newComment.value = ''
    loadComments()
  } catch (e) {
    ElMessage.error('评论失败')
  } finally {
    submitting.value = false
  }
}

function insertMention() {
  newComment.value += '@'
}

function formatTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(loadComments)
</script>

<template>
  <div class="discussion-thread">
    <div class="thread-header">
      <el-icon><ChatDotRound /></el-icon>
      <span>讨论 ({{ comments.length }})</span>
    </div>

    <div v-loading="loading" class="comment-list">
      <div v-if="comments.length === 0 && !loading" class="empty-hint">
        暂无讨论，快来发表第一条评论吧
      </div>

      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-avatar">
          <el-avatar :size="32">{{ comment.authorName?.charAt(0) }}</el-avatar>
        </div>
        <div class="comment-content">
          <div class="comment-header">
            <span class="comment-author">{{ comment.authorName }}</span>
            <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
          </div>
          <div class="comment-body">{{ comment.content }}</div>
          <div v-if="comment.mentions" class="comment-mentions">
            提到了: {{ comment.mentions }}
          </div>
        </div>
      </div>
    </div>

    <div class="comment-input">
      <el-input
        v-model="newComment"
        type="textarea"
        :rows="2"
        placeholder="输入评论... 使用 @ 提及某人"
        @keydown.enter.ctrl="submitComment"
      />
      <div class="input-actions">
        <el-button :icon="At" text @click="insertMention">@提及</el-button>
        <el-button type="primary" :icon="Send" :loading="submitting" @click="submitComment">
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.discussion-thread {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.thread-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 16px;
}

.comment-list {
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  margin-bottom: 8px;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-author {
  font-weight: 500;
}

.comment-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.comment-body {
  font-size: 14px;
  line-height: 1.6;
}

.comment-mentions {
  font-size: 12px;
  color: var(--el-color-primary);
  margin-top: 4px;
}

.empty-hint {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 20px;
}

.comment-input {
  background: white;
  padding: 12px;
  border-radius: 8px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
}
</style>
