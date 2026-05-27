<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMyDelegations, createDelegation, cancelDelegation } from '../../api/workflow'
import { listUsers } from '../../api/org'
import { formatDisplayDateTime } from '../oa/oa-shared'
import type { JsonObject } from '../../api/types'

/* ---------- list state ---------- */
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

async function load() {
  loading.value = true
  try {
    const res = await listMyDelegations(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  page.value = 1
  load()
}

/* ---------- form state ---------- */
const formVisible = ref(false)
const formLoading = ref(false)
const form = reactive({
  delegateeId: null as number | null,
  startAt: '',
  endAt: '',
  reason: '',
})

/* ---------- user picker ---------- */
const userOptions = ref<JsonObject[]>([])
const userKeyword = ref('')
const userLoading = ref(false)

async function searchUsers() {
  userLoading.value = true
  try {
    const res = await listUsers(1, 50, userKeyword.value || undefined)
    userOptions.value = res.items
  } catch {
    userOptions.value = []
  } finally {
    userLoading.value = false
  }
}

function openForm() {
  form.delegateeId = null
  form.startAt = ''
  form.endAt = ''
  form.reason = ''
  formVisible.value = true
  searchUsers()
}

async function submitForm() {
  if (!form.delegateeId) {
    ElMessage.warning('请选择代理人')
    return
  }
  if (!form.startAt || !form.endAt) {
    ElMessage.warning('请选择生效时间段')
    return
  }
  formLoading.value = true
  try {
    await createDelegation({
      delegateeId: form.delegateeId,
      startAt: form.startAt,
      endAt: form.endAt,
      reason: form.reason || null,
    })
    ElMessage.success('委托创建成功')
    formVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    formLoading.value = false
  }
}

/* ---------- cancel ---------- */
async function handleCancel(row: JsonObject) {
  try {
    await ElMessageBox.confirm('确定要取消此委托吗？', '确认取消', { type: 'warning' })
  } catch {
    return
  }
  try {
    await cancelDelegation(Number(row.id))
    ElMessage.success('已取消')
    load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '取消失败')
  }
}

/* ---------- helpers ---------- */
function delegationStatusLabel(row: JsonObject): string {
  const now = new Date()
  const startAt = row.startAt ? new Date(String(row.startAt)) : null
  const endAt = row.endAt ? new Date(String(row.endAt)) : null
  if (row.cancelled) return '已取消'
  if (endAt && endAt < now) return '已过期'
  if (startAt && startAt > now) return '待生效'
  return '生效中'
}

function delegationStatusType(row: JsonObject): string {
  const label = delegationStatusLabel(row)
  if (label === '生效中') return 'success'
  if (label === '已取消') return 'info'
  if (label === '已过期') return 'warning'
  return ''
}

onMounted(() => load())
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">审批委托</h2>
        <p class="muted">管理我的审批委托，将审批权限临时交给他人。</p>
      </div>
      <el-button type="primary" @click="openForm">新建委托</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="delegateeName" label="代理人" width="120" />
        <el-table-column label="生效开始" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.startAt) }}</template>
        </el-table-column>
        <el-table-column label="生效结束" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.endAt) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="delegationStatusType(row)">{{ delegationStatusLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.cancelled && delegationStatusLabel(row) !== '已过期'"
              link
              type="danger"
              @click="handleCancel(row)"
            >取消</el-button>
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

    <!-- 新建委托弹窗 -->
    <el-dialog v-model="formVisible" title="新建审批委托" width="520px" destroy-on-close>
      <el-form label-width="100px" v-loading="formLoading">
        <el-form-item label="代理人" required>
          <el-select
            v-model="form.delegateeId"
            filterable
            remote
            reserve-keyword
            placeholder="搜索用户"
            :remote-method="(q: string) => { userKeyword = q; searchUsers() }"
            :loading="userLoading"
            style="width: 100%"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="u.realName || u.username"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生效开始" required>
          <el-date-picker
            v-model="form.startAt"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="生效结束" required>
          <el-date-picker
            v-model="form.endAt"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="委托原因（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="submitForm">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.muted {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-top: 4px;
}
</style>
