<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createFormTemplate, listFormTemplates } from '../../api/forms'
import type { JsonObject } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { formatDisplayDateTime } from '../oa/oa-shared'

const auth = useAuthStore()
const router = useRouter()

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  templateCode: '',
  templateName: '',
  businessType: 'GENERIC',
  description: '',
})

const canManage = computed(() => {
  const p = auth.user?.permissions ?? []
  return p.includes('*') || p.includes('permission:role:assign')
})

async function load() {
  loading.value = true
  try {
    const res = await listFormTemplates(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

void load()

async function onCreate() {
  if (!form.templateCode || !form.templateName) {
    ElMessage.warning('请填写编码与名称')
    return
  }
  submitting.value = true
  try {
    await createFormTemplate({
      templateCode: form.templateCode,
      templateName: form.templateName,
      businessType: form.businessType,
      description: form.description || null,
    })
    ElMessage.success('已创建')
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">表单模板</h2>
        <p class="muted">维护业务表单模板及其版本；试用版的设计器为字段 JSON 文本框，正式版可替换为可视化设计器。</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/forms/field-rules')">字段权限规则</el-button>
        <el-button v-if="canManage" type="primary" @click="dialogVisible = true">新建模板</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="templateCode" label="编码" min-width="180" />
        <el-table-column prop="templateName" label="名称" min-width="180" />
        <el-table-column prop="businessType" label="业务类型" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/forms/templates/${row.id}/versions`)">版本</el-button>
            <el-button link @click="router.push(`/forms/templates/${row.id}/designer`)">设计器</el-button>
          </template>
        </el-table-column>
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

    <el-dialog v-model="dialogVisible" title="新建表单模板" width="520px">
      <el-form label-width="100px">
        <el-form-item label="编码" required>
          <el-input v-model="form.templateCode" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.templateName" />
        </el-form-item>
        <el-form-item label="业务类型" required>
          <el-input v-model="form.businessType" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onCreate">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>
