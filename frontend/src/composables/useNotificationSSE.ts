import { ref, onMounted, onUnmounted } from 'vue'
import { ElNotification } from 'element-plus'

export interface NotificationEvent {
  type: string
  title: string
  content: string
  userId: number
  timestamp: string
}

export function useNotificationSSE(
  onNotification?: (event: NotificationEvent) => void,
  options?: { autoConnect?: boolean }
) {
  const connected = ref(false)
  let abortController: AbortController | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectDelay = 1000

  function getToken(): string | null {
    return localStorage.getItem('oa_access_token')
  }

  async function connect() {
    const token = getToken()
    if (!token) return

    abortController = new AbortController()

    try {
      const response = await fetch('/api/notifications/stream', {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        signal: abortController.signal,
      })

      if (!response.ok) {
        throw new Error(`SSE connection failed: ${response.status}`)
      }

      connected.value = true
      reconnectDelay = 1000 // reset delay on successful connection

      const reader = response.body?.getReader()
      if (!reader) return

      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // Process SSE events from buffer
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // keep incomplete line in buffer

        let eventName = ''
        let eventData = ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventName = line.substring(6).trim()
          } else if (line.startsWith('data:')) {
            eventData = line.substring(5).trim()
          } else if (line === '' && eventName && eventData) {
            // End of event - dispatch it
            handleEvent(eventName, eventData)
            eventName = ''
            eventData = ''
          }
        }
      }
    } catch (err: unknown) {
      if (err instanceof Error && err.name === 'AbortError') {
        // Connection was intentionally closed
        return
      }
      console.error('SSE connection error:', err)
    } finally {
      connected.value = false
      scheduleReconnect()
    }
  }

  function handleEvent(eventName: string, data: string) {
    if (eventName === 'connected') {
      console.log('SSE connected:', JSON.parse(data))
      return
    }

    if (eventName === 'notification') {
      const event: NotificationEvent = JSON.parse(data)
      // Show browser notification
      ElNotification({
        title: event.title,
        message: event.content,
        type: 'info',
        duration: 5000,
      })
      // Call the optional callback
      onNotification?.(event)
    }
  }

  function scheduleReconnect() {
    if (reconnectTimer) return
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
    }, reconnectDelay)
    // Exponential backoff, max 30s
    reconnectDelay = Math.min(reconnectDelay * 2, 30000)
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    connected.value = false
  }

  if (options?.autoConnect !== false) {
    onMounted(() => connect())
  }
  onUnmounted(() => disconnect())

  return { connected, disconnect, connect }
}
