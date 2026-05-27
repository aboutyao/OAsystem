<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createContract, getContract, updateContract } from '../../api/contracts'
import { toInputDate } from '../oa/oa-shared'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)

const isEdit = computed(() => route.name === 'contract-edit')
const id = computed(() => (isEdit.value ? Number(route.params.id) : 0))

const form = reactive({
  contractName: '',
  contractType: '销售合同',
  counterparty: '',
  amount: 0,
  startDate: '',
  endDate: '',
})

onMounted(async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const row = await getContract(id.value)
    if (String(row.status) !== 'DRAFT') {
      ElMessage.warning('仅草稿可编辑')
      router.replace(`/contracts/${id.value}`)
      return
    }
    form.contractName = String(row.contractName ?? '')
    form.contractType = String(row.contractType ?? '')
    form.counterparty = String(row.counterparty ?? '')
    form.amount = Number(row.amount ?? 0)
    form.startDate = toInputDate(row.startDate)
    form.endDate = toInputDate(row.endDate)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/contracts')
  } finally {
    loading.value = false
  }
})

async function onSave() {
  if (!form.contractName || !form.counterparty) {
    ElMessage.warning('请填写名称与相对方')
    return
  }
  const body = {
    contractName: form.contractName,
    contractType: form.contractType,
    counterparty: form.counterparty,
    amount: form.amount,
    startDate: form.startDate || null,
    endDate: form.endDate || null,
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateContract(id.value, body)
      ElMessage.success('已保存')
      router.push(`/contracts/${id.value}`)
    } else {
      const created = await createContract(body)
      ElMessage.success('已创建')
      router.push(`/contracts/${Number(created.id)}`)
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">{{ isEdit ? '编辑合同' : '新建合同' }}</h2>
        <p class="muted">保存为草稿后可提交审批。</p>
      </div>
      <el-button @click="router.push(isEdit ? `/contracts/${id}` : '/contracts')">取消</el-button>
    </div>

    <el-card shadow="never">
      <el-form label-width="100px" style="max-width: 640px">
        <el-form-item label="合同名称" required>
          <el-input v-model="form.contractName" />
        </el-form-item>
        <el-form-item label="合同类型" required>
          <el-select v-model="form.contractType" style="width: 100%">
            <el-option label="销售合同" value="销售合同" />
            <el-option label="采购合同" value="采购合同" />
            <el-option label="服务合同" value="服务合同" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="相对方" required>
          <el-input v-model="form.counterparty" />
        </el-form-item>
        <el-form-item label="金额" required>
          <el-input-number v-model="form.amount" :min="0" :step="0.01" :controls="true" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始日">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled-date="(time: Date) => time.getTime() < Date.now() - 86400000" />
        </el-form-item>
        <el-form-item label="结束日">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled-date="(time: Date) => form.startDate && time.getTime() < new Date(form.startDate).getTime() - 86400000" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
