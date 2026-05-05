<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRule } from '../../api/rules'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<JsonObject | null>(null)

const ruleId = computed(() => Number(route.params.id))

async function load() {
  if (!ruleId.value) return
  loading.value = true
  try {
    detail.value = await getRule(ruleId.value)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">规则编辑</h2>
        <p class="muted">查看规则基本信息；如需变更规则内容请新建版本。</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/rules')">返回列表</el-button>
        <el-button type="primary" @click="router.push(`/rules/${ruleId}`)">查看详情</el-button>
      </div>
    </div>

    <el-card shadow="never" v-if="detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="规则编码">{{ detail.ruleCode }}</el-descriptions-item>
        <el-descriptions-item label="规则名称">{{ detail.ruleName }}</el-descriptions-item>
        <el-descriptions-item label="规则类型">{{ detail.ruleType }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ detail.businessType }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="分组ID">{{ detail.groupId }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDisplayDateTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDisplayDateTime(detail.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" v-else-if="!loading">
      <el-empty description="未找到规则数据" />
    </el-card>
  </div>
</template>
