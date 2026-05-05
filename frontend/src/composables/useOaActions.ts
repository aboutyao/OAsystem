import { ElMessage, ElMessageBox } from 'element-plus'

export function useOaActions() {
  async function confirmAction(message: string, title: string, type: 'warning' | 'danger' = 'warning'): Promise<boolean> {
    try {
      await ElMessageBox.confirm(message, title, { type })
      return true
    } catch {
      return false
    }
  }

  async function confirmWithdraw(): Promise<boolean> {
    return confirmAction('确定要撤回此申请吗？撤回后不可恢复。', '确认撤回', 'warning')
  }

  async function confirmTerminate(): Promise<boolean> {
    return confirmAction('确定要终止此流程吗？终止后不可恢复。', '确认终止', 'danger')
  }

  async function confirmDelete(): Promise<boolean> {
    return confirmAction('确定要删除此记录吗？删除后不可恢复。', '确认删除', 'danger')
  }

  async function confirmSubmit(): Promise<boolean> {
    return confirmAction('确定要提交审批吗？提交后不可修改。', '确认提交', 'warning')
  }

  async function confirmCancel(): Promise<boolean> {
    return confirmAction('确定要作废此申请吗？', '确认作废', 'warning')
  }

  function success(msg: string) {
    ElMessage.success(msg)
  }

  function error(e: unknown, fallback = '操作失败') {
    ElMessage.error(e instanceof Error ? e.message : fallback)
  }

  return {
    confirmAction,
    confirmWithdraw,
    confirmTerminate,
    confirmDelete,
    confirmSubmit,
    confirmCancel,
    success,
    error,
  }
}
