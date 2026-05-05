<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createRuleVersion, getRule, publishRuleVersion } from '../../api/rules'
import type { JsonObject } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { formatDisplayDateTime } from '../oa/oa-shared'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const detail = ref<JsonObject | null>(null)

const canManage = computed(() => {
  const p = authStore.user?.permissions ?? []
  return p.includes('*') || p.includes('rule:version:publish')
})

const VERSION_TAG: Record<string, '' | 'success' | 'info' | 'warning' | 'danger'> = {
  PUBLISHED: 'success',
  DRAFT: 'warning',
  DISABLED: 'info',
}

const versions = computed<JsonObject[]>(() => {
  const list = (detail.value?.versions as JsonObject[] | undefined) ?? []
  return list
})

const ruleId = computed(() => Number(route.params.id))

const versionDialog = ref(false)
const submitting = ref(false)
const versionForm = reactive({
  ruleContentJson: '{\n  "type": "AMOUNT",\n  "field": "amount",\n  "operator": ">=",\n  "value": 5000\n}',
  naturalLanguage: '',
  changeReason: '',
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

watch(ruleId, () => void load(), { immediate: true })

function openVersionDialog() {
  versionForm.ruleContentJson = '{\n  "type": "AMOUNT",\n  "field": "amount",\n  "operator": ">=",\n  "value": 5000\n}'
  versionForm.naturalLanguage = ''
  versionForm.changeReason = ''
  versionDialog.value = true
}

async function onCreateVersion() {
  try {
    JSON.parse(versionForm.ruleContentJson)
  } catch {
    ElMessage.error('规则内容必须是合法 JSON')
    return
  }
  submitting.value = true
  try {
    await createRuleVersion(ruleId.value, {
      ruleContentJson: versionForm.ruleContentJson,
      naturalLanguage: versionForm.naturalLanguage || undefined,
      changeReason: versionForm.changeReason || undefined,
    })
    ElMessage.success('版本已创建（草稿）')
    versionDialog.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    submitting.value = false
  }
}

async function onPublish(row: JsonObject) {
  try {
    await ElMessageBox.confirm(`确认发布版本 v${row.versionNo}？发布后会自动作废当前已发布版本。`, '发布版本', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await publishRuleVersion(Number(row.id))
    ElMessage.success('版本已发布')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '发布失败')
  }
}
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">规则详情</h2>
        <p class="muted">规则一旦发布，新建版本可替换当前已发布版本；旧版本会自动失效。</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/rules')">返回列表</el-button>
        <el-button v-if="canManage" type="primary" @click="openVersionDialog">新建版本</el-button>
      </div>
    </div>

    <el-card shadow="never" v-if="detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="编码">{{ detail.ruleCode }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ detail.ruleName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.ruleType }}</el-descriptions-item>
        <el-descriptions-item label="业务">{{ detail.businessType }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="分组ID">{{ detail.groupId }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <template #header>版本列表</template>
      <el-table :data="versions" stripe>
        <el-table-column prop="versionNo" label="版本" width="80" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="VERSION_TAG[String(row.status ?? '')] ?? 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="naturalLanguage" label="自然语言" min-width="220" show-overflow-tooltip />
        <el-table-column prop="changeReason" label="变更说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.publishedAt) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canManage && String(row.status) === 'DRAFT'"
              link
              type="primary"
              @click="onPublish(row)"
            >
              发布
            </el-button>
            <el-button
              link
              type="info"
              @click="router.push({ path: '/rules/simulator', query: { versionId: String(row.id) } })"
            >
              模拟
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="versionDialog" title="新建版本" width="640px">
      <el-form label-width="100px">
        <el-form-item label="规则内容 JSON" required>
          <el-input v-model="versionForm.ruleContentJson" type="textarea" :rows="10" spellcheck="false" />
        </el-form-item>
        <el-form-item label="自然语言">
          <el-input v-model="versionForm.naturalLanguage" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="变更原因">
          <el-input v-model="versionForm.changeReason" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="versionDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onCreateVersion">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>
