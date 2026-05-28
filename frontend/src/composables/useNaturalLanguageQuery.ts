import { ref } from 'vue'
import { http } from '../api/http'

interface QueryResult {
  type: 'info' | 'action' | 'navigation' | 'data'
  title: string
  content: string
  action?: { label: string; path: string }
}

/**
 * 自然语言查询
 * 用户输入自然语言问题，系统理解并回答
 */
export function useNaturalLanguageQuery() {
  const result = ref<QueryResult | null>(null)
  const loading = ref(false)

  // 本地意图识别（简单规则匹配）
  const INTENT_PATTERNS = [
    { pattern: /谁.*请假|请假.*谁|谁.*在|谁.*不在|请假.*日历/i, handler: 'leaveCalendar' },
    { pattern: /我.*请了.*假|剩余.*假|假期.*余额|还有.*天/i, handler: 'myLeaveBalance' },
    { pattern: /待办|审批|未处理/i, handler: 'myTodos' },
    { pattern: /报销.*多少|费用.*统计|花了.*钱/i, handler: 'expenseStats' },
    { pattern: /合同.*到期|到期.*合同/i, handler: 'contractExpiry' },
    { pattern: /团队|部门|人.*数|员工/i, handler: 'teamInfo' },
  ]

  async function query(input: string): Promise<QueryResult> {
    loading.value = true
    try {
      // 先尝试本地意图识别
      const intent = INTENT_PATTERNS.find(p => p.pattern.test(input))
      if (intent) {
        return await handleIntent(intent.handler, input)
      }

      // 降级到后端搜索
      const searchResult = await http.get('/search', { params: { q: input, limit: 3 } }) as any
      const items: QueryResult[] = []

      if (searchResult.users?.length) {
        items.push({
          type: 'info',
          title: `找到 ${searchResult.users.length} 个相关人员`,
          content: searchResult.users.map((u: any) => u.name).join('、'),
        })
      }
      if (searchResult.leaves?.length) {
        items.push({
          type: 'data',
          title: `找到 ${searchResult.leaves.length} 条请假记录`,
          content: searchResult.leaves.map((l: any) => `${l.createdName}的${l.leaveType}`).join('、'),
        })
      }

      if (items.length > 0) {
        result.value = items[0]
        return items[0]
      }

      return {
        type: 'info',
        title: '未找到匹配结果',
        content: `没有找到与"${input}"相关的信息`,
      }
    } catch {
      return { type: 'info', title: '查询失败', content: '请稍后重试' }
    } finally {
      loading.value = false
    }
  }

  async function handleIntent(handler: string, _input: string): Promise<QueryResult> {
    switch (handler) {
      case 'leaveCalendar': {
        const data = await http.get('/calendar/team-leaves', {
          params: { year: new Date().getFullYear(), month: new Date().getMonth() + 1 },
        }) as any[]
        const today = new Date().toISOString().split('T')[0]
        const onLeave = data.filter((l: any) => {
          const start = l.startAt?.split('T')[0] || ''
          const end = l.endAt?.split('T')[0] || ''
          return today >= start && today <= end
        })
        return {
          type: 'data',
          title: `今天有 ${onLeave.length} 人请假`,
          content: onLeave.length > 0
            ? onLeave.map((l: any) => `${l.userName}（${l.leaveType}）`).join('、')
            : '今天团队全员在岗',
        }
      }
      case 'myLeaveBalance': {
        const data = await http.get('/leave-balance/my') as any[]
        const summaries = data
          .filter((b: any) => b.remainingDays > 0)
          .map((b: any) => {
            const labels: Record<string, string> = { ANNUAL: '年假', SICK: '病假', PERSONAL: '事假' }
            return `${labels[b.leaveType] || b.leaveType} ${b.remainingDays}天`
          })
        return {
          type: 'data',
          title: '您的假期余额',
          content: summaries.join('、') || '暂无可用假期',
        }
      }
      case 'myTodos': {
        const data = await http.get('/dashboard/summary') as any
        return {
          type: 'data',
          title: `您有 ${data.todoCount} 条待办`,
          content: `待办 ${data.todoCount} 条，未读消息 ${data.messageCount} 条`,
          action: { label: '去处理', path: '/todos' },
        }
      }
      case 'expenseStats': {
        const data = await http.get('/reports/expense-summary', {
          params: { from: new Date(Date.now() - 90 * 86400000).toISOString().split('T')[0] },
        }) as any
        return {
          type: 'data',
          title: '近90天报销统计',
          content: `共 ${data.totalCount || 0} 笔，总额 ¥${data.totalAmount || 0}`,
          action: { label: '查看详情', path: '/oa/expenses' },
        }
      }
      case 'contractExpiry': {
        const data = await http.get('/contracts/expiring', { params: { days: 30 } }) as any[]
        return {
          type: 'data',
          title: `未来30天有 ${data.length} 个合同到期`,
          content: data.length > 0
            ? data.slice(0, 3).map((c: any) => c.contractName || c.contractNo).join('、')
            : '暂无即将到期的合同',
          action: { label: '查看合同', path: '/contracts' },
        }
      }
      case 'teamInfo': {
        const data = await http.get('/dashboard/summary') as any
        return {
          type: 'data',
          title: '团队概况',
          content: `审批中 ${data.startedCount} 条，待办 ${data.todoCount} 条`,
        }
      }
      default:
        return { type: 'info', title: '未理解', content: '请换个方式描述' }
    }
  }

  return { result, loading, query }
}
