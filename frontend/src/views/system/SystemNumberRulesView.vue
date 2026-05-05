<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createNumberRule, listNumberRules, previewNumber } from '../../api/system'
import type { JsonObject } from '../../api/types'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  ruleCode: '',
  businessType: 'GENERIC',
  prefix: '',
  datePattern: 'yyyyMMdd',
  seqLength: 4,
  seqReset: 'DAILY',
  description: '',
})

const canManage = computed(() => {
  const p = auth.user?.permissions ?? []
  return p.includes('*') || p.includes('permission:role:assign')
})

async function load() {
  loading.value = true
  try {
    const res = await listNumberRules(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

void load()

function openCreate() {
  form.ruleCode = ''
  form.businessType = 'GENERIC'
  form.prefix = ''
  form.datePattern = 'yyyyMMdd'
  form.seqLength = 4
  form.seqReset = 'DAILY'
  form.description = ''
  dialogVisible.value = true
}

async function onCreate() {
  if (!form.ruleCode || !form.prefix) {
    ElMessage.warning('请填写编码与前缀')
    return
  }
  submitting.value = true
  try {
    await createNumberRule({
      ruleCode: form.ruleCode,
      businessType: form.businessType,
      prefix: form.prefix,
      datePattern: form.datePattern || null,
      seqLength: Number(form.seqLength),
      seqReset: form.seqReset,
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

async function onPreview(row: JsonObject) {
  try {
    const res = await previewNumber(String(row.ruleCode))
    ElMessage.success(`预览编号：${res.next}`)
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '预览失败')
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">编号规则</h2>
        <p class="muted">配置业务单据的编号生成规则；支持按日/月/年/永不重置流水。</p>
      </div>
      <div class="oa-page__actions">
        <el-button v-if="canManage" type="primary" @click="openCreate">新建规则</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="ruleCode" label="编码" min-width="150" />
        <el-table-column prop="businessType" label="业务" width="120" />
        <el-table-column prop="prefix" label="前缀" width="100" />
        <el-table-column prop="datePattern" label="日期格式" width="120" />
        <el-table-column prop="seqLength" label="流水长度" width="100" />
        <el-table-column prop="seqReset" label="重置周期" width="100" />
        <el-table-column prop="currentPeriod" label="当前周期" width="120" />
        <el-table-column prop="currentSeq" label="当前流水" width="100" />
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onPreview(row)">预览</el-button>
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

    <el-dialog v-model="dialogVisible" title="新建编号规则" width="560px">
      <el-form label-width="110px">
        <el-form-item label="编码" required>
          <el-input v-model="form.ruleCode" placeholder="EXPENSE_NO" />
        </el-form-item>
        <el-form-item label="业务类型" required>
          <el-input v-model="form.businessType" />
        </el-form-item>
        <el-form-item label="前缀" required>
          <el-input v-model="form.prefix" placeholder="EXP" />
        </el-form-item>
        <el-form-item label="日期格式">
          <el-input v-model="form.datePattern" placeholder="yyyyMMdd" />
        </el-form-item>
        <el-form-item label="流水长度">
          <el-input-number v-model="form.seqLength" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="重置周期">
          <el-select v-model="form.seqReset" style="width: 200px">
            <el-option label="DAILY 每日" value="DAILY" />
            <el-option label="MONTHLY 每月" value="MONTHLY" />
            <el-option label="YEARLY 每年" value="YEARLY" />
            <el-option label="NEVER 永不" value="NEVER" />
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
