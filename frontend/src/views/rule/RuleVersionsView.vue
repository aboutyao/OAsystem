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

const VERSION_TAG: Record<string, '' | 'success' | 'info' | 'warning' | 'danger'> = {
  PUBLISHED: 'success',
  DRAFT: 'info',
  ARCHIVED: 'warning',
}

const versions = computed<JsonObject[]>(() => {
  return (detail.value?.versions as JsonObject[] | undefined) ?? []
})

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

function goDetail(row: JsonObject) {
  router.push(`/rules/${ruleId.value}`)
}
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">规则版本列表</h2>
        <p class="muted" v-if="detail">规则：{{ detail.ruleName }}（{{ detail.ruleCode }}）</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/rules')">返回列表</el-button>
        <el-button type="primary" @click="router.push(`/rules/${ruleId}`)">规则详情</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="versions" stripe>
        <el-table-column prop="versionNo" label="版本号" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="VERSION_TAG[String(row.status ?? '')] ?? 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="naturalLanguage" label="自然语言描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="changeReason" label="变更原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.publishedAt) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && versions.length === 0" description="暂无版本记录" />
    </el-card>
  </div>
</template>
