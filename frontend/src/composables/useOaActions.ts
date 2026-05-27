import { ElMessage, ElMessageBox } from 'element-plus'

export function useOaActions(reloadFn: () => Promise<void>) {
  async function doAction(
    confirmMsg: string,
    apiFn: () => Promise<unknown>,
    successMsg: string,
    errorMsg: string,
  ) {
    try {
      await ElMessageBox.confirm(confirmMsg, '操作确认', { type: 'warning' })
      await apiFn()
      ElMessage.success(successMsg)
      await reloadFn()
    } catch (e: unknown) {
      if (e !== 'cancel' && e !== 'close') {
        ElMessage.error(e instanceof Error ? e.message : errorMsg)
      }
    }
  }

  return {
    onSubmit: (apiFn: () => Promise<unknown>) =>
      doAction('确认提交审批？', apiFn, '已提交', '提交失败'),
    onWithdraw: (apiFn: () => Promise<unknown>) =>
      doAction('确认撤回？', apiFn, '已撤回', '撤回失败'),
    onCancel: (apiFn: () => Promise<unknown>) =>
      doAction('确认作废？', apiFn, '已作废', '操作失败'),
  }
}
