<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, UploadFilled, Document, Delete } from '@element-plus/icons-vue'
import { createSeal, getSeal, updateSeal, uploadSealAttachment } from '../../../api/oa-seals'
import { useAutoSave } from '../../../composables/useAutoSave'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const isEdit = computed(() => route.name === 'seal-edit')
const id = computed(() => (isEdit.value ? Number(route.params.id) : 0))

const form = reactive({
  sealType: '公章',
  sealName: '公司公章',
  fileTitle: '',
  useReason: '',
  useAt: '',
  outFlag: 0,
  attachments: [] as Array<{ name: string; url: string; size: number }>,
})

const rules: FormRules = {
  sealType: [{ required: true, message: '请选择印章类型', trigger: 'change' }],
  sealName: [{ required: true, message: '请输入印章名称', trigger: 'blur' }],
  fileTitle: [{ required: true, message: '请输入文件标题', trigger: 'blur' }],
  useReason: [{ required: true, message: '请输入使用事由', trigger: 'blur' }],
  useAt: [{ required: true, message: '请选择使用时间', trigger: 'change' }],
}

// 自动保存草稿
const { lastSaved, save, restore, clear } = useAutoSave(form, `seal_draft_${isEdit.value ? id.value : 'new'}`, { interval: 30000 })

// 脏数据检测
let isDirty = false
let isSubmitting = false

watch(
  () => ({ ...form }),
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

function normalizeDt(v: unknown): string {
  if (v == null) return ''
  if (typeof v === 'string') return v.includes('T') ? v.slice(0, 19) : v
  if (typeof v === 'number') {
    const d = new Date(v)
    const p = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
  }
  if (Array.isArray(v) && v.length >= 3) {
    const y = Number(v[0])
    const m = Number(v[1])
    const day = Number(v[2])
    const h = v.length > 3 ? Number(v[3]) : 0
    const min = v.length > 4 ? Number(v[4]) : 0
    const s = v.length > 5 ? Number(v[5]) : 0
    const p = (n: number) => String(n).padStart(2, '0')
    return `${y}-${p(m)}-${p(day)}T${p(h)}:${p(min)}:${p(s)}`
  }
  return ''
}

onMounted(async () => {
  // 尝试恢复草稿
  const draft = restore()
  if (!isEdit.value && draft) {
    Object.assign(form, draft)
    ElMessage.info('已恢复草稿')
  }

  if (!isEdit.value) return
  loading.value = true
  try {
    const row = await getSeal(id.value)
    if (String(row.status) !== 'DRAFT') {
      ElMessage.warning('仅草稿可编辑')
      router.replace(`/oa/seals/${id.value}`)
      return
    }
    form.sealType = String(row.sealType ?? '')
    form.sealName = String(row.sealName ?? '')
    form.fileTitle = String(row.fileTitle ?? '')
    form.useReason = String(row.useReason ?? '')
    form.useAt = normalizeDt(row.useAt)
    form.outFlag = Number(row.outFlag ?? 0)
    form.attachments = row.attachments ?? []
    clear() // 编辑模式加载成功后清除草稿
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/oa/seals')
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
  const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('仅支持 PDF、Word、JPG、PNG 格式')
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

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function onSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    isSubmitting = true
    const body = {
      sealType: form.sealType,
      sealName: form.sealName,
      fileTitle: form.fileTitle,
      useReason: form.useReason || null,
      useAt: form.useAt,
      outFlag: form.outFlag,
      attachments: form.attachments,
    }
    saving.value = true
    try {
      if (isEdit.value) {
        await updateSeal(id.value, body)
        ElMessage.success('已保存')
        clear()
        router.push(`/oa/seals/${id.value}`)
      } else {
        const created = await createSeal(body)
        ElMessage.success('已创建')
        clear()
        router.push(`/oa/seals/${Number(created.id)}`)
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
        <h2 class="oa-page__title">{{ isEdit ? '编辑用章' : '新建用章' }}</h2>
        <p class="muted">外带用章审批通过后可登记归还。</p>
      </div>
      <div class="head-actions">
        <span v-if="lastSaved" class="draft-hint">草稿已保存 {{ lastSaved }}</span>
        <el-button @click="router.push(isEdit ? `/oa/seals/${id}` : '/oa/seals')">取消</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 640px">
        <el-form-item label="印章类型" prop="sealType">
          <el-select v-model="form.sealType" style="width: 100%">
            <el-option label="公章" value="公章" />
            <el-option label="合同章" value="合同章" />
            <el-option label="财务章" value="财务章" />
            <el-option label="法人章" value="法人章" />
          </el-select>
        </el-form-item>
        <el-form-item label="印章名称" prop="sealName">
          <el-input v-model="form.sealName" />
        </el-form-item>
        <el-form-item label="文件标题" prop="fileTitle">
          <el-input v-model="form.fileTitle" />
        </el-form-item>
        <el-form-item label="使用事由" prop="useReason">
          <el-input v-model="form.useReason" type="textarea" :rows="3" placeholder="请输入使用事由" />
        </el-form-item>
        <el-form-item label="使用时间" prop="useAt">
          <el-date-picker v-model="form.useAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" :disabled-date="(time: Date) => time.getTime() < Date.now() - 86400000" />
        </el-form-item>
        <el-form-item label="是否外带">
          <el-radio-group v-model="form.outFlag">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="附件上传">
          <el-upload
            :http-request="handleUpload"
            :show-file-list="false"
            :disabled="uploadLoading"
          >
            <el-button :icon="UploadFilled" :loading="uploadLoading">{{ uploadLoading ? '上传中...' : '选择文件' }}</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、Word、JPG、PNG 格式，单个文件不超过 20MB</div>
            </template>
          </el-upload>
          <div v-if="form.attachments.length" class="attachment-list">
            <div v-for="(file, index) in form.attachments" :key="index" class="attachment-item">
              <el-icon><Document /></el-icon>
              <span class="attachment-name">{{ file.name }}</span>
              <span class="attachment-size">{{ formatFileSize(file.size) }}</span>
              <el-button type="danger" link :icon="Delete" @click="removeAttachment(index)" />
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">{{ saving ? '保存中...' : '保存' }}</el-button>
          <el-button @click="router.push(isEdit ? `/oa/seals/${id}` : '/oa/seals')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
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
</style>
