<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  countWorkdays,
  deleteWorkCalendar,
  listWorkCalendar,
  upsertWorkCalendar,
} from '../../api/system'
import type { JsonObject } from '../../api/types'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(50)

const filters = reactive({
  from: '',
  to: '',
})

const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  calDate: '',
  dayType: 'HOLIDAY',
  description: '',
})

const countResult = ref<{ from: string; to: string; workdays: number } | null>(null)
const countLoading = ref(false)

const canManage = computed(() => {
  const p = auth.user?.permissions ?? []
  return p.includes('*') || p.includes('permission:role:assign')
})

async function load() {
  loading.value = true
  try {
    const res = await listWorkCalendar({
      from: filters.from || undefined,
      to: filters.to || undefined,
      page: page.value,
      size: size.value,
    })
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

void load()

function openCreate() {
  form.calDate = ''
  form.dayType = 'HOLIDAY'
  form.description = ''
  dialogVisible.value = true
}

async function onSubmit() {
  if (!form.calDate) {
    ElMessage.warning('请选择日期')
    return
  }
  submitting.value = true
  try {
    await upsertWorkCalendar({
      calDate: form.calDate,
      dayType: form.dayType,
      description: form.description || null,
    })
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    submitting.value = false
  }
}

async function onDelete(row: JsonObject) {
  try {
    await ElMessageBox.confirm(`确认删除 ${row.calDate} 的日历记录？`, '删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteWorkCalendar(String(row.calDate))
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function doCount() {
  if (!filters.from || !filters.to) {
    ElMessage.warning('请选择起止日期')
    return
  }
  countLoading.value = true
  try {
    const res = await countWorkdays(filters.from, filters.to)
    countResult.value = res as { from: string; to: string; workdays: number }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '计算失败')
  } finally {
    countLoading.value = false
  }
}

const DAY_TAG: Record<string, '' | 'success' | 'info' | 'warning' | 'danger'> = {
  WORKDAY: '',
  WEEKEND: 'info',
  HOLIDAY: 'danger',
  ADJUSTED_WORKDAY: 'warning',
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">工作日历</h2>
        <p class="muted">配置节假日与调休工作日；用于请假/审批时长计算等场景。</p>
      </div>
      <div class="oa-page__actions">
        <el-button v-if="canManage" type="primary" @click="openCreate">新增/覆盖</el-button>
      </div>
    </div>

    <el-card shadow="never" style="margin-bottom: 12px">
      <el-form inline>
        <el-form-item label="起始日期">
          <el-date-picker v-model="filters.from" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="filters.to" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button :loading="countLoading" @click="doCount">统计工作日</el-button>
        </el-form-item>
        <el-form-item v-if="countResult">
          <el-tag size="large" type="success">
            {{ countResult.from }} ~ {{ countResult.to }}：{{ countResult.workdays }} 个工作日
          </el-tag>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="calDate" label="日期" min-width="150" />
        <el-table-column label="类型" width="160">
          <template #default="{ row }">
            <el-tag :type="DAY_TAG[String(row.dayType ?? '')] ?? 'info'" size="small">
              {{ row.dayType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="200" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canManage" link type="danger" @click="onDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" title="新增/覆盖工作日历" width="480px">
      <el-form label-width="100px">
        <el-form-item label="日期" required>
          <el-date-picker v-model="form.calDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.dayType" style="width: 220px">
            <el-option label="WORKDAY 工作日" value="WORKDAY" />
            <el-option label="WEEKEND 周末" value="WEEKEND" />
            <el-option label="HOLIDAY 节假日" value="HOLIDAY" />
            <el-option label="ADJUSTED_WORKDAY 调休工作日" value="ADJUSTED_WORKDAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
