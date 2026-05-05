/** 流程实例 businessType + businessId → 前端业务详情路由 */
export function businessDocPath(businessType: string, businessId: number): string | null {
  switch (businessType) {
    case 'LEAVE':
      return `/oa/leaves/${businessId}`
    case 'EXPENSE':
      return `/oa/expenses/${businessId}`
    case 'SEAL':
      return `/oa/seals/${businessId}`
    case 'PURCHASE':
      return `/oa/purchases/${businessId}`
    case 'CONTRACT':
      return `/contracts/${businessId}`
    default:
      return null
  }
}
