<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { formTemplateDetail, getFormVersion } from '../../api/forms'
import type { JsonObject } from '../../api/types'

const route = useRoute()
const templateId = computed(() => Number(route.params.id))
const detail = ref<JsonObject | null>(null)
const fields = ref<Array<{ fieldCode: string; label: string; type?: string; required?: boolean }>>([])

async function load() {
  if (!templateId.value) return
  try {
    detail.value = await formTemplateDetail(templateId.value)
    const versionId = detail.value.currentVersionId as number | null
    if (versionId) {
      const v = await getFormVersion(versionId)
      try {
        fields.value = JSON.parse(String(v.fieldsJson))
      } catch {
        fields.value = []
      }
    } else {
      fields.value = []
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  }
}

watch(() => route.params.id, () => void load(), { immediate: true })
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">表单预览</h2>
        <p class="muted">展示当前发布版本的字段；试用版仅渲染只读预览，正式版替换为可视化设计器。</p>
      </div>
    </div>

    <el-card v-if="detail" shadow="never">
      <el-descriptions :column="3" size="small" border>
        <el-descriptions-item label="模板">{{ detail.templateCode }} - {{ detail.templateName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="当前版本">{{ detail.currentVersionId || '未发布' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider />
      <h4>表单字段预览</h4>
      <el-form v-if="fields.length" label-width="120px" disabled>
        <el-form-item v-for="f in fields" :key="f.fieldCode" :label="f.label">
          <el-input v-if="!f.type || f.type === 'text'" placeholder="文本输入" />
          <el-input v-else-if="f.type === 'textarea'" type="textarea" :rows="2" placeholder="多行文本" />
          <el-input-number v-else-if="f.type === 'number'" :disabled="true" :value="0" />
          <el-date-picker v-else-if="f.type === 'date'" :model-value="null" type="date" />
          <el-select v-else-if="f.type === 'select'" placeholder="下拉选择" :model-value="null">
            <el-option label="选项1" value="1" />
          </el-select>
          <el-input v-else placeholder="未知字段类型" />
          <span v-if="f.required" class="muted" style="margin-left: 8px">必填</span>
        </el-form-item>
      </el-form>
      <p v-else class="muted">该模板尚无已发布版本，请前往「版本」页面创建并发布。</p>
    </el-card>
  </div>
</template>
