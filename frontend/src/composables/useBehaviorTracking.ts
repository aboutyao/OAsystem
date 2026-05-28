import { ref, onMounted } from 'vue'
import { http } from '../api/http'

interface UserAction {
  path: string
  count: number
  lastUsed: string
}

interface BehaviorProfile {
  topActions: UserAction[]
  peakHours: number[]
  avgSessionMinutes: number
 常用模块: string[]
}

const TRACKING_KEY = 'oa_behavior'

/**
 * 用户行为模式学习
 * 追踪用户操作频率，个性化快捷入口排序
 */
export function useBehaviorTracking() {
  const profile = ref<BehaviorProfile | null>(null)
  const loading = ref(false)

  // 本地追踪（不依赖后端）
  function trackAction(path: string) {
    try {
      const raw = localStorage.getItem(TRACKING_KEY)
      const actions: Record<string, { count: number; lastUsed: string }> = raw ? JSON.parse(raw) : {}
      const now = new Date().toISOString()

      if (actions[path]) {
        actions[path].count++
        actions[path].lastUsed = now
      } else {
        actions[path] = { count: 1, lastUsed: now }
      }

      localStorage.setItem(TRACKING_KEY, JSON.stringify(actions))
    } catch {
      // ignore
    }
  }

  function getLocalProfile(): UserAction[] {
    try {
      const raw = localStorage.getItem(TRACKING_KEY)
      if (!raw) return []
      const actions: Record<string, { count: number; lastUsed: string }> = JSON.parse(raw)
      return Object.entries(actions)
        .map(([path, data]) => ({ path, count: data.count, lastUsed: data.lastUsed }))
        .sort((a, b) => b.count - a.count)
        .slice(0, 10)
    } catch {
      return []
    }
  }

  // 后端追踪（更精确）
  async function loadProfile() {
    loading.value = true
    try {
      const data = await http.get('/dashboard/behavior-profile') as BehaviorProfile
      profile.value = data
    } catch {
      // 降级到本地数据
      profile.value = {
        topActions: getLocalProfile(),
        peakHours: [],
        avgSessionMinutes: 0,
        常用模块: [],
      }
    } finally {
      loading.value = false
    }
  }

  function getSuggestedActions(): UserAction[] {
    if (profile.value?.topActions) return profile.value.topActions
    return getLocalProfile()
  }

  onMounted(() => {
    loadProfile()
  })

  return { profile, loading, trackAction, getSuggestedActions, loadProfile }
}
