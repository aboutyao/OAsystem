import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

/**
 * 全局键盘快捷键
 * Ctrl+S: 保存当前表单
 * Esc: 关闭对话框/抽屉
 * Ctrl+K: 命令面板（由 CommandPalette 自己处理）
 * Ctrl+/: 快捷键帮助
 */
export function useKeyboardShortcuts() {
  const router = useRouter()

  function handleKeydown(e: KeyboardEvent) {
    const isInput = e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement

    // Ctrl+S — 保存
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
      e.preventDefault()
      // 触发自定义事件，表单组件监听此事件执行保存
      window.dispatchEvent(new CustomEvent('oa:shortcut-save'))
    }

    // Esc — 关闭对话框/返回
    if (e.key === 'Escape' && !isInput) {
      window.dispatchEvent(new CustomEvent('oa:shortcut-escape'))
    }

    // Ctrl+/ — 快捷键帮助
    if ((e.ctrlKey || e.metaKey) && e.key === '/') {
      e.preventDefault()
      window.dispatchEvent(new CustomEvent('oa:shortcut-help'))
    }

    // Ctrl+Shift+D — 仪表盘
    if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key === 'D') {
      e.preventDefault()
      router.push('/dashboard')
    }

    // Ctrl+Shift+T — 待办
    if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key === 'T') {
      e.preventDefault()
      router.push('/todos')
    }

    // Ctrl+Shift+M — 消息
    if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key === 'M') {
      e.preventDefault()
      router.push('/messages')
    }
  }

  onMounted(() => {
    document.addEventListener('keydown', handleKeydown)
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeydown)
  })
}
