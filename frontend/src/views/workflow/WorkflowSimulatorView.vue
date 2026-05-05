<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { startInstance } from '../../api/workflow'

const submitting = ref(false)
const result = ref<unknown>(null)

const form = reactive({
  businessType: 'GENERIC',
  businessId: 1,
  title: '模拟流程',
  managerId: '',
  variablesJson: '{}',
})

async function onSubmit() {
  let vars: Record<string, unknown> = {}
  if (form.variablesJson.trim()) {
    try {
      vars = JSON.parse(form.variablesJson)
    } catch {
      ElMessage.error('variables 不是合法 JSON')
      return
    }
  }
  if (form.managerId) {
    vars.managerId = Number(form.managerId)
  }
  submitting.value = true
  try {
    const res = await startInstance({
      businessType: form.businessType,
      businessId: Number(form.businessId),
      title: form.title,
      variables: vars,
    })
    result.value = res
    ElMessage.success('已发起模拟流程实例')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '发起失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">流程模拟</h2>
        <p class="muted">用真实 Flowable 引擎发起一个模拟实例，验证流程定义是否能正确推进。注意：会真实创建数据。</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-form label-width="120px" style="max-width: 720px">
        <el-form-item label="业务类型" required>
          <el-select v-model="form.businessType" style="width: 240px">
            <el-option label="GENERIC 通用" value="GENERIC" />
            <el-option label="LEAVE 请假" value="LEAVE" />
            <el-option label="EXPENSE 报销" value="EXPENSE" />
            <el-option label="SEAL 用章" value="SEAL" />
            <el-option label="PURCHASE 采购" value="PURCHASE" />
            <el-option label="CONTRACT 合同" value="CONTRACT" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务ID" required>
          <el-input-number v-model="form.businessId" :min="1" />
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="审批人ID">
          <el-input v-model="form.managerId" placeholder="可填写经理ID（覆盖默认上级）" />
          <div class="muted" style="margin-top: 4px">空则使用发起人的直属上级 manager_user_id</div>
        </el-form-item>
        <el-form-item label="variables JSON">
          <el-input v-model="form.variablesJson" type="textarea" :rows="6" placeholder='{"amount": 1000}' />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">发起模拟实例</el-button>
        </el-form-item>
      </el-form>

      <el-divider />
      <h4>结果</h4>
      <pre v-if="result" class="result-pre">{{ JSON.stringify(result, null, 2) }}</pre>
      <p v-else class="muted">尚未发起。</p>
    </el-card>
  </div>
</template>

<style scoped>
.result-pre {
  background: var(--el-bg-color-page);
  padding: 12px;
  border-radius: 6px;
  font-size: 12px;
  max-height: 360px;
  overflow: auto;
}
</style>
