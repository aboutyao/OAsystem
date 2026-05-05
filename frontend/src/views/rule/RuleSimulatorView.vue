<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRule, listRules, simulateRuleVersion } from '../../api/rules'
import type { JsonObject } from '../../api/types'

const route = useRoute()
const router = useRouter()

const submitting = ref(false)
const result = ref<JsonObject | null>(null)

const rules = ref<JsonObject[]>([])
const selectedRuleId = ref<number | null>(null)
const versions = ref<JsonObject[]>([])
const versionsLoading = ref(false)

const form = reactive({
  versionId: 0,
  businessType: 'EXPENSE',
  contextJson: '{\n  "amount": 6000\n}',
})

async function loadRules() {
  try {
    const res = await listRules(1, 200)
    rules.value = res.items
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载规则列表失败')
  }
}

async function loadVersions(ruleId: number) {
  versionsLoading.value = true
  versions.value = []
  form.versionId = 0
  try {
    const detail = await getRule(ruleId)
    versions.value = (detail.versions as JsonObject[] | undefined) ?? []
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载版本列表失败')
  } finally {
    versionsLoading.value = false
  }
}

function onRuleChange(ruleId: number | null) {
  if (ruleId) {
    loadVersions(ruleId)
  } else {
    versions.value = []
    form.versionId = 0
  }
}

onMounted(loadRules)

watch(
  () => route.query.versionId,
  (v) => {
    const n = Number(v)
    if (!Number.isNaN(n) && n > 0) {
      form.versionId = n
    }
  },
  { immediate: true },
)

async function onSimulate() {
  if (!form.versionId) {
    ElMessage.warning('请选择规则版本')
    return
  }
  let context: Record<string, unknown>
  try {
    context = JSON.parse(form.contextJson)
    if (typeof context !== 'object' || Array.isArray(context) || context === null) {
      throw new Error('context 必须是对象')
    }
  } catch (e) {
    ElMessage.error(`上下文 JSON 不合法：${e instanceof Error ? e.message : 'parse error'}`)
    return
  }
  submitting.value = true
  result.value = null
  try {
    result.value = await simulateRuleVersion(form.versionId, {
      businessType: form.businessType,
      context,
    })
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '模拟失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">规则模拟</h2>
        <p class="muted">选择已发布版本并提供业务上下文，验证规则是否命中。仅模拟，不影响线上数据。</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/rules')">返回规则列表</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form label-width="120px">
        <el-form-item label="选择规则" required>
          <el-select
            v-model="selectedRuleId"
            filterable
            placeholder="搜索或选择规则"
            style="width: 100%"
            @change="onRuleChange"
          >
            <el-option
              v-for="r in rules"
              :key="r.id"
              :label="`${r.ruleName} (${r.ruleCode})`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="规则版本" required>
          <el-select
            v-model="form.versionId"
            filterable
            :disabled="!selectedRuleId"
            :loading="versionsLoading"
            placeholder="选择规则版本"
            style="width: 100%"
          >
            <el-option
              v-for="v in versions"
              :key="v.id"
              :label="`v${v.versionNo} - ${v.status}${v.naturalLanguage ? ' | ' + v.naturalLanguage : ''}`"
              :value="v.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型" required>
          <el-select v-model="form.businessType" style="width: 200px">
            <el-option label="EXPENSE 报销" value="EXPENSE" />
            <el-option label="LEAVE 请假" value="LEAVE" />
            <el-option label="SEAL 用章" value="SEAL" />
            <el-option label="PURCHASE 采购" value="PURCHASE" />
            <el-option label="GENERIC 通用" value="GENERIC" />
          </el-select>
        </el-form-item>
        <el-form-item label="上下文 JSON">
          <el-input v-model="form.contextJson" type="textarea" :rows="8" spellcheck="false" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSimulate">运行模拟</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result" shadow="never" style="margin-top: 12px">
      <template #header>
        <span>模拟结果：</span>
        <el-tag :type="result.matched ? 'success' : 'info'">
          {{ result.matched ? '命中' : '未命中' }}
        </el-tag>
      </template>
      <pre class="result-json">{{ JSON.stringify(result, null, 2) }}</pre>
    </el-card>
  </div>
</template>

<style scoped>
.result-json {
  margin: 0;
  font-family:
    ui-monospace,
    SFMono-Regular,
    Menlo,
    Monaco,
    Consolas,
    monospace;
  font-size: 12px;
  background: #fafbfc;
  padding: 12px;
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
