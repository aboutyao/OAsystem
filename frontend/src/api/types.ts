export interface PageResponse<T> {
  page: number
  size: number
  total: number
  items: T[]
}

export type JsonObject = Record<string, unknown>

export type OaStatus = 'DRAFT' | 'APPROVING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN' | 'CANCELLED' | 'TERMINATED'

export interface BaseEntity {
  id: number
  createdAt: string
  updatedAt: string
}

export interface OaLeave extends BaseEntity {
  processInstanceId?: number
  wfInstanceId?: number
  leaveType: string
  startAt: string
  endAt: string
  durationHours: number
  durationDays: number
  reason: string
  handoverNote?: string
  status: OaStatus
  createdBy: number
  createdNameSnapshot: string
  createdDeptId?: number
  createdDeptNameSnapshot?: string
}

export interface OaExpense extends BaseEntity {
  processInstanceId?: number
  wfInstanceId?: number
  expenseType: string
  amount: number
  description: string
  status: OaStatus
  createdBy: number
  createdNameSnapshot: string
  paymentStatus?: string
}

export interface OaPurchase extends BaseEntity {
  processInstanceId?: number
  wfInstanceId?: number
  purchaseNo: string
  purchaseType: string
  totalAmount: number
  status: OaStatus
  createdBy: number
  createdNameSnapshot: string
  arrivalStatus?: string
  acceptanceStatus?: string
}

export interface OaSealApply extends BaseEntity {
  processInstanceId?: number
  wfInstanceId?: number
  sealType: string
  documentTitle: string
  status: OaStatus
  createdBy: number
  createdNameSnapshot: string
}

export interface ContractInfo extends BaseEntity {
  processInstanceId?: number
  wfInstanceId?: number
  contractNo: string
  contractType: string
  partyA: string
  partyB: string
  amount: number
  status: OaStatus
  createdBy: number
  createdNameSnapshot: string
}

export interface WfInstance extends BaseEntity {
  wfInstanceId: number
  title: string
  businessType: string
  businessId: number
  status: string
  currentNodeName?: string
  starterName: string
  startedAt: string
  endedAt?: string
}

export interface WfTimelineItem {
  action: string
  operatorName: string
  nodeName?: string
  comment?: string
  operatedAt: string
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  mainDeptId?: number
  mainDeptName?: string
  roles: string[]
  permissions: string[]
}

export interface LoginResponse {
  token: string
  expiresIn: number
  user: UserInfo
}

export interface NavGroup {
  id: number
  label: string
  icon: string
  children: { path: string; label: string }[]
}
