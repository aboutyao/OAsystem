<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listFormFieldRules, listFormTemplates, upsertFormFieldRule } from '../../api/forms'
import type { JsonObject } from '../../api/types'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const filterTemplateId = ref<number | null>(null)
const templates = ref<JsonObject[]>([])

const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  templateId: null as number | null,
  fieldCode: '',
  ruleType: 'VISIBILITY',
  ruleExpression: '',
  description: '',
})

const canManage = computed(() => {
  const p = auth.user?.permissions ?? []
  return p.includes('*') || p.includes('permission:role:assign')
})

async function loadTemplates() {
  try {
    const res = await listFormTemplates(1, 100)
    templates.value = res.items
  } catch {
    /* ignore */
  }
}

async function load() {
  loading.value = true
  try {
    const res = await listFormFieldRules({
      page: page.value,
      size: size.value,
      templateId: filterTemplateId.value || undefined,
    })
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

void loadTemplates()
void load()

function openCreate() {
  form.templateId = filterTemplateId.value
  form.fieldCode = ''
  form.ruleType = 'VISIBILITY'
  form.ruleExpression = ''
  form.description = ''
  dialogVisible.value = true
}

async function onSubmit() {
  if (!form.templateId || !form.fieldCode || !form.ruleExpression) {
    ElMessage.warning('请填写完整')
    return
  }
  submitting.value = true
  try {
    await upsertFormFieldRule(form.templateId, {
      fieldCode: form.fieldCode,
      ruleType: form.ruleType,
      ruleExpression: form.ruleExpression,
      description: form.description || null,
    })
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">字段权限规则</h2>
        <p class="muted">配置表单字段的可见 / 可编辑 / 校验规则。</p>
      </div>
      <div class="oa-page__actions">
        <el-button v-if="canManage" type="primary" @click="openCreate">新建规则</el-button>
      </div>
    </div>

    <el-card shadow="never" style="margin-bottom: 12px">
      <el-form inline>
        <el-form-item label="按模板筛选">
          <el-select v-model="filterTemplateId" placeholder="全部" clearable style="width: 240px">
            <el-option v-for="t in templates" :key="t.id as number" :value="t.id as number"
              :label="`${t.templateCode} - ${t.templateName}`" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="templateCode" label="模板" min-width="180" />
        <el-table-column prop="fieldCode" label="字段编码" min-width="140" />
        <el-table-column prop="ruleType" label="规则类型" width="120" />
        <el-table-column prop="ruleExpression" label="表达式" min-width="200" show-overflow-tooltip />
        <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" />
      </el-table>
      <div class="oa-page__pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          v-model:current-page="page"
          v-model:page-size="size"
          @current-change="load"
          @size-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新建/覆盖字段规则" width="560px">
      <el-form label-width="110px">
        <el-form-item label="模板" required>
          <el-select v-model="form.templateId" placeholder="选择模板" style="width: 280px">
            <el-option v-for="t in templates" :key="t.id as number" :value="t.id as number"
              :label="`${t.templateCode} - ${t.templateName}`" />
          </el-select>
        </el-form-item>
        <el-form-item label="字段编码" required>
          <el-input v-model="form.fieldCode" />
        </el-form-item>
        <el-form-item label="规则类型" required>
          <el-select v-model="form.ruleType" style="width: 200px">
            <el-option label="VISIBILITY 可见性" value="VISIBILITY" />
            <el-option label="EDITABLE 可编辑" value="EDITABLE" />
            <el-option label="VALIDATION 校验" value="VALIDATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="表达式" required>
          <el-input v-model="form.ruleExpression" type="textarea" :rows="3"
            placeholder="role == 'EMPLOYEE' && status == 'DRAFT'" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
