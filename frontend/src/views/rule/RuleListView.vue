<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createRule, listRules } from '../../api/rules'
import type { JsonObject } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { formatDisplayDateTime } from '../oa/oa-shared'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const canManage = computed(() => {
  const p = authStore.user?.permissions ?? []
  return p.includes('*') || p.includes('rule:version:publish')
})

const STATUS_TAG: Record<string, '' | 'success' | 'info' | 'warning' | 'danger'> = {
  ENABLED: 'success',
  DISABLED: 'info',
}

const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  groupCode: '',
  ruleCode: '',
  ruleName: '',
  ruleType: 'AMOUNT',
  businessType: 'EXPENSE',
  description: '',
})

async function load() {
  loading.value = true
  try {
    const res = await listRules(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()

function handleSizeChange() {
  page.value = 1
  load()
}

function openCreate() {
  form.groupCode = ''
  form.ruleCode = ''
  form.ruleName = ''
  form.ruleType = 'AMOUNT'
  form.businessType = 'EXPENSE'
  form.description = ''
  dialogVisible.value = true
}

async function onCreate() {
  if (!form.ruleCode || !form.ruleName) {
    ElMessage.warning('请填写规则编码与名称')
    return
  }
  submitting.value = true
  try {
    await createRule({
      groupCode: form.groupCode.trim(),
      ruleCode: form.ruleCode.trim(),
      ruleName: form.ruleName.trim(),
      ruleType: form.ruleType,
      businessType: form.businessType,
      description: form.description || undefined,
    })
    ElMessage.success('规则已创建')
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    submitting.value = false
  }
}

function openDetail(row: JsonObject) {
  void router.push(`/rules/${row.id}`)
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">规则中心</h2>
        <p class="muted">维护审批与路由相关业务规则；变更需通过新建版本并发布才能生效。</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/rules/simulator')">规则模拟</el-button>
        <el-button v-if="canManage" type="primary" @click="openCreate">新建规则</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe @row-dblclick="openDetail">
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="ruleCode" label="编码" min-width="200" />
        <el-table-column prop="ruleName" label="名称" min-width="200" />
        <el-table-column prop="ruleType" label="类型" width="100" />
        <el-table-column prop="businessType" label="业务" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="STATUS_TAG[String(row.status ?? '')] ?? 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新建规则" width="520px">
      <el-form label-width="100px">
        <el-form-item label="分组编码" required>
          <el-input v-model="form.groupCode" placeholder="输入分组编码" />
        </el-form-item>
        <el-form-item label="规则编码" required>
          <el-input v-model="form.ruleCode" placeholder="EXPENSE_AMOUNT_GTE_5000" />
        </el-form-item>
        <el-form-item label="规则名称" required>
          <el-input v-model="form.ruleName" />
        </el-form-item>
        <el-form-item label="规则类型" required>
          <el-select v-model="form.ruleType" style="width: 200px">
            <el-option label="AMOUNT 金额" value="AMOUNT" />
            <el-option label="TIME 时间" value="TIME" />
            <el-option label="ROUTING 路由" value="ROUTING" />
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
