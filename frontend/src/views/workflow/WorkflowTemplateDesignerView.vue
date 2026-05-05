<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { templateDetail } from '../../api/workflow'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const route = useRoute()
const loading = ref(false)
const detail = ref<JsonObject | null>(null)

async function load() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    detail.value = await templateDetail(id)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载模板失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">流程模板设计器</h2>
        <p class="muted">设计和编辑 BPMN 流程模板。</p>
      </div>
    </div>

    <el-card v-loading="loading" shadow="never">
      <template v-if="detail">
        <el-descriptions :column="2" border style="margin-bottom: 16px">
          <el-descriptions-item label="模板编码">{{ detail.templateCode }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ detail.templateName }}</el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ detail.businessType }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDisplayDateTime(detail.updatedAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-card shadow="never" style="min-height: 400px">
          <el-empty description="BPMN 流程设计器加载中，敬请期待。" />
        </el-card>
      </template>
    </el-card>
  </div>
</template>
