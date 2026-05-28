<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

const visible = ref(false)

function handleShortcutHelp() {
  visible.value = !visible.value
}

onMounted(() => {
  window.addEventListener('oa:shortcut-help', handleShortcutHelp)
})

onUnmounted(() => {
  window.removeEventListener('oa:shortcut-help', handleShortcutHelp)
})

const shortcuts = [
  { keys: 'Ctrl + K', action: '打开命令面板' },
  { keys: 'Ctrl + S', action: '保存当前表单' },
  { keys: 'Ctrl + /', action: '显示/隐藏快捷键帮助' },
  { keys: 'Ctrl + Shift + D', action: '跳转到工作台' },
  { keys: 'Ctrl + Shift + T', action: '跳转到我的待办' },
  { keys: 'Ctrl + Shift + M', action: '跳转到消息中心' },
  { keys: 'Esc', action: '关闭对话框/抽屉' },
]
</script>

<template>
  <el-dialog v-model="visible" title="键盘快捷键" width="400px" :show-close="true">
    <div class="shortcut-list">
      <div v-for="s in shortcuts" :key="s.keys" class="shortcut-item">
        <div class="shortcut-keys">
          <kbd v-for="key in s.keys.split(' + ')" :key="key">{{ key }}</kbd>
        </div>
        <span class="shortcut-action">{{ s.action }}</span>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.shortcut-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--oa-border-light, #ebeef5);
}

.shortcut-item:last-child {
  border-bottom: none;
}

.shortcut-keys {
  display: flex;
  gap: 4px;
}

.shortcut-keys kbd {
  display: inline-block;
  padding: 4px 8px;
  border: 1px solid var(--oa-border-light, #dcdfe6);
  border-radius: 6px;
  font-size: 12px;
  font-family: monospace;
  background: var(--oa-bg-gray, #f5f7fa);
  box-shadow: 0 1px 0 var(--oa-border-light, #dcdfe6);
  min-width: 24px;
  text-align: center;
}

.shortcut-action {
  font-size: 14px;
  color: var(--oa-text-secondary, #606266);
}
</style>
