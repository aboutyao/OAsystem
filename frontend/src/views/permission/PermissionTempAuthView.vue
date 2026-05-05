<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createTempAuth, listTempAuths, revokeTempAuth } from '../../api/permission'
import { listUsers } from '../../api/org'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const form = reactive({
  userId: undefined as number | undefined,
  authType: 'ROLE',
  targetId: undefined as number | undefined,
  startAt: '',
  endAt: '',
  reason: '',
})

const userOptions = ref<{ id: number; realName: string }[]>([])

async function loadUsers() {
  try {
    const res = await listUsers(1, 500)
    userOptions.value = res.items.map((u: Record<string, unknown>) => ({ id: Number(u.id), realName: String(u.realName ?? '') }))
  } catch { /* ignore */ }
}

onMounted(() => { void loadUsers() })

async function load() {
  loading.value = true
  try {
    const res = await listTempAuths(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}
void load()

async function submit() {
  if (!form.userId || !form.targetId || !form.startAt || !form.endAt || !form.reason.trim()) {
    ElMessage.warning('请填写完整授权信息')
    return
  }
  await createTempAuth({
    userId: form.userId,
    authType: form.authType,
    targetId: form.targetId,
    startAt: form.startAt,
    endAt: form.endAt,
    reason: form.reason.trim(),
  })
  ElMessage.success('临时授权已创建')
  form.reason = ''
  await load()
}

async function onRevoke(row: JsonObject) {
  try {
    await ElMessageBox.confirm('确认撤销该临时授权？', '撤销授权', { type: 'warning' })
  } catch {
    return
  }
  await revokeTempAuth(Number(row.id))
  ElMessage.success('已撤销')
  await load()
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">临时授权</h2>
        <p class="muted">创建与撤销临时授权。</p>
      </div>
    </div>

    <el-card shadow="never" style="margin-bottom: 12px">
      <template #header>新增临时授权</template>
      <el-form inline>
        <el-form-item label="用户ID">
          <el-select v-model="form.userId" filterable placeholder="选择用户" style="width: 200px">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.authType">
            <el-option label="ROLE" value="ROLE" />
            <el-option label="MENU" value="MENU" />
            <el-option label="DATA_SCOPE" value="DATA_SCOPE" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标ID">
          <el-select v-model="form.targetId" filterable placeholder="选择用户" style="width: 200px">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始">
          <el-date-picker v-model="form.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束">
          <el-date-picker v-model="form.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="form.reason" style="width: 220px" /></el-form-item>
        <el-form-item><el-button type="primary" @click="submit">创建</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="authType" label="类型" width="120" />
        <el-table-column prop="targetId" label="目标ID" width="90" />
        <el-table-column label="开始" width="170"><template #default="{ row }">{{ formatDisplayDateTime(row.startAt) }}</template></el-table-column>
        <el-table-column label="结束" width="170"><template #default="{ row }">{{ formatDisplayDateTime(row.endAt) }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="100">
          <template #default="{ row }"><el-button link type="danger" @click="onRevoke(row)">撤销</el-button></template>
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
  </div>
</template>
