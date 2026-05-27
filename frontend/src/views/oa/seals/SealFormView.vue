<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createSeal, getSeal, updateSeal } from '../../../api/oa-seals'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)

const isEdit = computed(() => route.name === 'seal-edit')
const id = computed(() => (isEdit.value ? Number(route.params.id) : 0))

const form = reactive({
  sealType: '公章',
  sealName: '公司公章',
  fileTitle: '',
  useReason: '',
  useAt: '',
  outFlag: 0,
})

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
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/oa/seals')
  } finally {
    loading.value = false
  }
})

async function onSave() {
  if (!form.fileTitle || !form.useAt) {
    ElMessage.warning('请填写文件标题与使用时间')
    return
  }
  const body = {
    sealType: form.sealType,
    sealName: form.sealName,
    fileTitle: form.fileTitle,
    useReason: form.useReason || null,
    useAt: form.useAt,
    outFlag: form.outFlag,
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateSeal(id.value, body)
      ElMessage.success('已保存')
      router.push(`/oa/seals/${id.value}`)
    } else {
      const created = await createSeal(body)
      ElMessage.success('已创建')
      router.push(`/oa/seals/${Number(created.id)}`)
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">{{ isEdit ? '编辑用章' : '新建用章' }}</h2>
        <p class="muted">外带用章审批通过后可登记归还。</p>
      </div>
      <el-button @click="router.push(isEdit ? `/oa/seals/${id}` : '/oa/seals')">取消</el-button>
    </div>

    <el-card shadow="never">
      <el-form label-width="100px" style="max-width: 640px">
        <el-form-item label="印章类型" required>
          <el-select v-model="form.sealType" style="width: 100%">
            <el-option label="公章" value="公章" />
            <el-option label="合同章" value="合同章" />
            <el-option label="财务章" value="财务章" />
            <el-option label="法人章" value="法人章" />
          </el-select>
        </el-form-item>
        <el-form-item label="印章名称" required>
          <el-input v-model="form.sealName" />
        </el-form-item>
        <el-form-item label="文件标题" required>
          <el-input v-model="form.fileTitle" />
        </el-form-item>
        <el-form-item label="使用事由">
          <el-input v-model="form.useReason" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="使用时间" required>
          <el-date-picker v-model="form.useAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" :disabled-date="(time: Date) => time.getTime() < Date.now() - 86400000" />
        </el-form-item>
        <el-form-item label="是否外带">
          <el-radio-group v-model="form.outFlag">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
