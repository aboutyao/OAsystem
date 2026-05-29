import { ElMessage, ElMessageBox } from 'element-plus'

export interface Attachment {
  name: string
  url: string
  size: number
}

export interface AttachmentRule {
  /** 业务类型 */
  type: string
  /** 必需附件名称列表（模糊匹配） */
  required: string[]
  /** 可选附件提示 */
  optional?: string[]
}

/** 采购类型 → 必需附件映射 */
const purchaseRules: Record<string, AttachmentRule> = {
  '固定资产': {
    type: '固定资产',
    required: ['报价单'],
    optional: ['采购申请单', '合同', '验收单'],
  },
  '服务采购': {
    type: '服务采购',
    required: ['合同'],
    optional: ['服务方案', '报价单'],
  },
  '办公用品': {
    type: '办公用品',
    required: [],
    optional: ['采购清单'],
  },
  '原材料': {
    type: '原材料',
    required: ['报价单'],
    optional: ['质检报告', '合同'],
  },
}

/** 印章类型 → 必需附件映射 */
const sealRules: Record<string, AttachmentRule> = {
  '合同章': {
    type: '合同章',
    required: ['合同'],
    optional: ['审批单'],
  },
  '公章': {
    type: '公章',
    required: ['盖章文件'],
    optional: ['审批单', '授权书'],
  },
  '财务章': {
    type: '财务章',
    required: ['财务文件'],
    optional: ['审批单'],
  },
  '法人章': {
    type: '法人章',
    required: ['盖章文件'],
    optional: ['授权书'],
  },
}

/**
 * 检查附件完整性
 * @param attachments 已上传的附件
 * @param businessType 业务类型（采购类型/印章类型）
 * @param module 模块类型
 * @returns 是否通过检查
 */
export async function checkAttachmentCompleteness(
  attachments: Attachment[],
  businessType: string,
  module: 'purchase' | 'seal' = 'purchase'
): Promise<boolean> {
  const rules = module === 'purchase' ? purchaseRules : sealRules
  const rule = rules[businessType]

  // 没有配置规则，跳过检查
  if (!rule || rule.required.length === 0) {
    return true
  }

  const uploadedNames = attachments.map(a => a.name.toLowerCase())
  const missing: string[] = []

  for (const required of rule.required) {
    const found = uploadedNames.some(name => name.includes(required.toLowerCase()))
    if (!found) {
      missing.push(required)
    }
  }

  if (missing.length === 0) {
    return true
  }

  // 显示警告，允许用户选择继续或取消
  try {
    await ElMessageBox.confirm(
      `建议上传以下附件：\n${missing.map(m => `• ${m}`).join('\n')}\n\n是否继续提交？`,
      '附件完整性提示',
      {
        confirmButtonText: '继续提交',
        cancelButtonText: '去上传',
        type: 'warning',
      }
    )
    return true
  } catch {
    return false
  }
}

/**
 * 获取附件建议列表
 */
export function getAttachmentSuggestions(
  businessType: string,
  module: 'purchase' | 'seal' = 'purchase'
): { required: string[]; optional: string[] } {
  const rules = module === 'purchase' ? purchaseRules : sealRules
  const rule = rules[businessType]
  return {
    required: rule?.required || [],
    optional: rule?.optional || [],
  }
}
