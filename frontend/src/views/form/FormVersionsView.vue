<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createFormVersion, formTemplateDetail, publishFormVersion } from '../../api/forms'
import type { JsonObject } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { formatDisplayDateTime } from '../oa/oa-shared'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const templateId = computed(() => Number(route.params.id))
const loading = ref(false)
const detail = ref<JsonObject | null>(null)

const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  fieldsJson: '[\n  {"fieldCode":"title","label":"标题","type":"text","required":true}\n]',
  layoutJson: '',
  changeReason: '',
})

const canManage = computed(() => {
  const p = auth.user?.permissions ?? []
  return p.includes('*') || p.includes('permission:role:assign')
})

async function load() {
  if (!templateId.value) return
  loading.value = true
  try {
    detail.value = await formTemplateDetail(templateId.value)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, () => void load(), { immediate: true })

async function onCreateVersion() {
  submitting.value = true
  try {
    await createFormVersion(templateId.value, {
      fieldsJson: form.fieldsJson,
      layoutJson: form.layoutJson || null,
      changeReason: form.changeReason || null,
    })
    ElMessage.success('已创建版本')
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    submitting.value = false
  }
}

async function onPublish(version: JsonObject) {
  try {
    await publishFormVersion(Number(version.id))
    ElMessage.success('已发布')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '发布失败')
  }
}

const versions = computed(() => (detail.value?.versions as JsonObject[]) || [])
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">表单版本</h2>
        <p class="muted">
          <a href="javascript:;" @click="router.push('/forms/templates')">表单模板</a> /
          {{ detail?.templateCode }} {{ detail?.templateName }}
        </p>
      </div>
      <div class="oa-page__actions">
        <el-button v-if="canManage" type="primary" @click="dialogVisible = true">新建版本</el-button>
      </div>
    </div>

    <el-card v-if="detail" shadow="never" style="margin-bottom: 12px">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="编码">{{ detail.templateCode }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ detail.templateName }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ detail.businessType }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="当前版本">{{ detail.currentVersionId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ detail.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <el-table :data="versions" stripe>
        <el-table-column prop="versionNo" label="版本号" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small"
              :type="row.status === 'PUBLISHED' ? 'success' : row.status === 'ARCHIVED' ? 'info' : 'warning'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeReason" label="变更说明" min-width="180" />
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.publishedAt) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canManage && row.status === 'DRAFT'" link type="primary" @click="onPublish(row)">
              发布
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新建版本" width="640px">
      <el-form label-width="120px">
        <el-form-item label="字段定义 JSON" required>
          <el-input v-model="form.fieldsJson" type="textarea" :rows="10" />
          <div class="muted" style="margin-top: 4px">数组结构，每项至少包含 fieldCode 和 label。</div>
        </el-form-item>
        <el-form-item label="布局 JSON">
          <el-input v-model="form.layoutJson" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="变更说明">
          <el-input v-model="form.changeReason" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onCreateVersion">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>
