<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { UploadFilled, Warning, View } from '@element-plus/icons-vue'
import FilePreview from '../../../components/FilePreview.vue'
import { createPurchase, getPurchase, updatePurchase } from '../../../api/oa-purchases'
import { uploadSealAttachment } from '../../../api/oa-seals'
import { useAutoSave } from '../../../composables/useAutoSave'
import { checkAttachmentCompleteness, getAttachmentSuggestions } from '../../../composables/useAttachmentCheck'
import type { JsonObject } from '../../../api/types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const isEdit = computed(() => route.name === 'purchase-edit')
const id = computed(() => (isEdit.value ? Number(route.params.id) : 0))

const form = reactive({
  purchaseType: '固定资产',
  supplierName: '',
  budgetSubject: '',
  reason: '',
  attachments: [] as Array<{ name: string; url: string; size: number }>,
})

type ItemRow = {
  itemName: string
  specification: string
  quantity: number
  unit: string
  unitPrice: number
  amount: number
  sortOrder: number
}

const items = ref<ItemRow[]>([
  { itemName: '', specification: '', quantity: 1, unit: '件', unitPrice: 0, amount: 0, sortOrder: 0 },
])

const sumAmount = computed(() => items.value.reduce((s, it) => s + Number(it.amount || 0), 0))

const rules: FormRules = {
  purchaseType: [{ required: true, message: '请选择采购类型', trigger: 'change' }],
  reason: [{ required: true, message: '请输入采购事由', trigger: 'blur' }],
}

// 自动保存草稿
const { lastSaved, save, restore, clear } = useAutoSave(
  { form, items: items.value } as any,
  `purchase_draft_${isEdit.value ? id.value : 'new'}`,
  { interval: 30000 }
)

// 脏数据检测
let isDirty = false
let isSubmitting = false

watch(
  () => ({ ...form, items: [...items.value] }),
  () => {
    if (!isSubmitting) isDirty = true
  },
  { deep: true }
)

onBeforeRouteLeave((to, from, next) => {
  if (isDirty && !isSubmitting) {
    if (window.confirm('有未保存的修改，确定离开吗？')) {
      clear()
      next()
    } else {
      next(false)
    }
  } else {
    next()
  }
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (isDirty && !isSubmitting) {
    e.preventDefault()
    e.returnValue = ''
  }
}

window.addEventListener('beforeunload', handleBeforeUnload)

watch(
  items,
  () => {
    for (const it of items.value) {
      const q = Number(it.quantity) || 0
      const p = Number(it.unitPrice) || 0
      it.amount = Math.round(q * p * 100) / 100
    }
  },
  { deep: true },
)

function addItem() {
  items.value.push({
    itemName: '',
    specification: '',
    quantity: 1,
    unit: '件',
    unitPrice: 0,
    amount: 0,
    sortOrder: items.value.length,
  })
}

function removeItem(i: number) {
  if (items.value.length <= 1) return
  items.value.splice(i, 1)
  items.value.forEach((r, idx) => (r.sortOrder = idx))
}

onMounted(async () => {
  // 尝试恢复草稿
  const draft = restore()
  if (!isEdit.value && draft) {
    if (draft.form) Object.assign(form, draft.form)
    if (draft.items && Array.isArray(draft.items) && draft.items.length) {
      items.value = draft.items
    }
    ElMessage.info('已恢复草稿')
  }

  if (!isEdit.value) return
  loading.value = true
  try {
    const data = await getPurchase(id.value)
    if (String(data.status) !== 'DRAFT') {
      ElMessage.warning('仅草稿可编辑')
      router.replace(`/oa/purchases/${id.value}`)
      return
    }
    form.purchaseType = String(data.purchaseType ?? '')
    form.supplierName = String(data.supplierName ?? '')
    form.budgetSubject = String(data.budgetSubject ?? '')
    form.reason = String(data.reason ?? '')
    form.attachments = (data as any).attachments ?? []
    const raw = (data as { items?: JsonObject[] }).items
    if (Array.isArray(raw) && raw.length) {
      items.value = raw.map((r, idx) => ({
        itemName: String(r.itemName ?? ''),
        specification: String(r.specification ?? ''),
        quantity: Number(r.quantity ?? 0),
        unit: String(r.unit ?? ''),
        unitPrice: Number(r.unitPrice ?? 0),
        amount: Number(r.amount ?? 0),
        sortOrder: Number(r.sortOrder ?? idx),
      }))
    }
    clear() // 编辑模式加载成功后清除草稿
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/oa/purchases')
  } finally {
    loading.value = false
  }
})

// 附件上传
const uploadLoading = ref(false)
async function handleUpload(options: { file: File }) {
  const file = options.file
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 20MB')
    return
  }
  uploadLoading.value = true
  try {
    const result = await uploadSealAttachment(file)
    form.attachments.push({ name: result.name, url: result.url, size: result.size })
    ElMessage.success('上传成功')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '上传失败')
  } finally {
    uploadLoading.value = false
  }
}

function removeAttachment(index: number) {
  form.attachments.splice(index, 1)
}

// 文件预览
const previewVisible = ref(false)
const previewFile = ref({ url: '', name: '' })
function openPreview(file: { url: string; name: string }) {
  previewFile.value = file
  previewVisible.value = true
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function onSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    if (!items.value.length) {
      ElMessage.warning('请至少一行明细')
      return
    }
    for (const it of items.value) {
      if (!it.itemName) {
        ElMessage.warning('请填写每行品名')
        return
      }
    }

    // 附件完整性检查
    const passed = await checkAttachmentCompleteness(form.attachments, form.purchaseType, 'purchase')
    if (!passed) return

    isSubmitting = true
    const totalAmount = sumAmount.value
    const body = {
      purchaseType: form.purchaseType,
      totalAmount,
      supplierName: form.supplierName || null,
      budgetSubject: form.budgetSubject || null,
      reason: form.reason || null,
      attachments: form.attachments,
      items: items.value.map((it, idx) => ({
        itemName: it.itemName,
        specification: it.specification || null,
        quantity: it.quantity,
        unit: it.unit || null,
        unitPrice: it.unitPrice,
        amount: it.amount,
        sortOrder: idx,
      })),
    }
    saving.value = true
    try {
      if (isEdit.value) {
        await updatePurchase(id.value, body)
        ElMessage.success('已保存')
        clear()
        router.push(`/oa/purchases/${id.value}`)
      } else {
        const created = await createPurchase(body)
        ElMessage.success('已创建')
        clear()
        router.push(`/oa/purchases/${Number(created.id)}`)
      }
    } catch (e) {
      ElMessage.error(e instanceof Error ? e.message : '保存失败')
      isSubmitting = false
    } finally {
      saving.value = false
    }
  })
}
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">{{ isEdit ? '编辑采购' : '新建采购' }}</h2>
        <p class="muted">合计须与明细金额之和一致（由明细自动汇总）。</p>
      </div>
      <div class="head-actions">
        <span v-if="lastSaved" class="draft-hint">草稿已保存 {{ lastSaved }}</span>
        <el-button @click="router.push(isEdit ? `/oa/purchases/${id}` : '/oa/purchases')">取消</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 900px">
        <el-form-item label="采购类型" prop="purchaseType">
          <el-select v-model="form.purchaseType" style="width: 100%">
            <el-option label="固定资产" value="固定资产" />
            <el-option label="低值易耗" value="低值易耗" />
            <el-option label="服务采购" value="服务采购" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="合计金额">
          <el-input :model-value="String(sumAmount)" disabled />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="form.supplierName" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="预算科目">
          <el-input v-model="form.budgetSubject" placeholder="请输入预算科目" />
        </el-form-item>
        <el-form-item label="事由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入采购事由" />
        </el-form-item>
        <el-form-item label="附件上传">
          <el-upload
            :http-request="handleUpload"
            :show-file-list="false"
            :disabled="uploadLoading"
          >
            <el-button :icon="UploadFilled" :loading="uploadLoading">{{ uploadLoading ? '上传中...' : '选择文件' }}</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、Word、Excel 格式，单个文件不超过 20MB</div>
            </template>
          </el-upload>
          <div v-if="getAttachmentSuggestions(form.purchaseType, 'purchase').required.length" class="attachment-suggestions">
            <el-text type="warning" size="small">
              <el-icon><Warning /></el-icon>
              建议上传：{{ getAttachmentSuggestions(form.purchaseType, 'purchase').required.join('、') }}
            </el-text>
          </div>
          <div v-if="form.attachments.length" class="attachment-list">
            <div v-for="(file, index) in form.attachments" :key="index" class="attachment-item">
              <el-icon><Document /></el-icon>
              <span class="attachment-name">{{ file.name }}</span>
              <span class="attachment-size">{{ formatFileSize(file.size) }}</span>
              <el-button type="primary" link :icon="View" @click="openPreview(file)" />
              <el-button type="danger" link :icon="Delete" @click="removeAttachment(index)" />
            </div>
          </div>
        </el-form-item>

        <el-form-item label="采购明细">
          <el-table :data="items" border size="small">
            <el-table-column label="品名" min-width="120">
              <template #default="{ row }">
                <el-input v-model="row.itemName" />
              </template>
            </el-table-column>
            <el-table-column label="规格" width="120">
              <template #default="{ row }">
                <el-input v-model="row.specification" />
              </template>
            </el-table-column>
            <el-table-column label="数量" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="0" :step="0.01" :controls="false" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="单位" width="88">
              <template #default="{ row }">
                <el-input v-model="row.unit" />
              </template>
            </el-table-column>
            <el-table-column label="单价" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.unitPrice" :min="0" :step="0.01" :controls="false" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="金额" width="100">
              <template #default="{ row }">
                <span>{{ row.amount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="" width="88">
              <template #default="{ $index }">
                <el-button link type="danger" @click="removeItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button class="oa-mt" @click="addItem">添加一行</el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">{{ saving ? '保存中...' : '保存' }}</el-button>
          <el-button @click="router.push(isEdit ? `/oa/purchases/${id}` : '/oa/purchases')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>

  <FilePreview
    v-model:visible="previewVisible"
    :url="previewFile.url"
    :name="previewFile.name"
  />
</template>

<style scoped>
.head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.draft-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.attachment-list {
  margin-top: 12px;
  width: 100%;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  margin-bottom: 8px;
}

.attachment-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-size {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.attachment-suggestions {
  margin-top: 8px;
}
</style>
