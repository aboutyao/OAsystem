export const OA_STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  APPROVING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回',
  CANCELLED: '已作废',
  SIGNED: '已签署',
  TERMINATED: '已终止',
  ARCHIVED: '已归档',
  UNPAID: '未付款',
  PAID: '已付款',
}

export function statusLabel(code: string | undefined) {
  if (!code) return '—'
  return OA_STATUS_LABEL[code] ?? code
}

function parseToDate(v: unknown): Date | null {
  if (v == null || v === '') return null
  if (v instanceof Date) return v
  if (typeof v === 'number') {
    const d = new Date(v)
    return Number.isNaN(d.getTime()) ? null : d
  }
  if (typeof v === 'string') {
    const d = new Date(v)
    return Number.isNaN(d.getTime()) ? null : d
  }
  if (Array.isArray(v) && v.length >= 3) {
    const y = Number(v[0])
    const m = Number(v[1])
    const day = Number(v[2])
    const h = v.length > 3 ? Number(v[3]) : 0
    const min = v.length > 4 ? Number(v[4]) : 0
    const s = v.length > 5 ? Number(v[5]) : 0
    return new Date(y, m - 1, day, h, min, s)
  }
  return null
}

export function formatDisplayDateTime(v: unknown): string {
  const d = parseToDate(v)
  return d ? d.toLocaleString('zh-CN', { hour12: false }) : '—'
}

export function formatDisplayDate(v: unknown): string {
  const d = parseToDate(v)
  return d ? d.toLocaleDateString('zh-CN') : '—'
}

/** For `<el-date-picker value-format>` / Java `LocalDateTime` */
export function toInputDateTime(v: unknown): string {
  const d = parseToDate(v)
  if (!d) return ''
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

export function toInputDate(v: unknown): string {
  const d = parseToDate(v)
  if (!d) return ''
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

export function computeLeaveSpan(startIso: string, endIso: string): { durationHours: number; durationDays: number } {
  const a = Date.parse(startIso)
  const b = Date.parse(endIso)
  if (Number.isNaN(a) || Number.isNaN(b) || b <= a) {
    return { durationHours: 0, durationDays: 0 }
  }
  const ms = b - a
  const durationHours = Math.round((ms / 3600000) * 100) / 100
  const durationDays = Math.round((ms / 86400000) * 100) / 100
  return { durationHours, durationDays }
}
