<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { createMeetingRoom, listMeetingRooms, updateMeetingRoom } from '../../api/meetings'
import type { JsonObject } from '../../api/types'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const canManage = computed(() => {
  const p = authStore.user?.permissions ?? []
  return p.includes('*') || p.includes('org:create')
})

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  roomName: '',
  location: '',
  capacity: 10,
  equipment: '',
  remark: '',
  status: 'ENABLED',
})

async function load() {
  loading.value = true
  try {
    const res = await listMeetingRooms(page.value, size.value)
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
  editingId.value = null
  form.roomName = ''
  form.location = ''
  form.capacity = 10
  form.equipment = ''
  form.remark = ''
  form.status = 'ENABLED'
  dialogVisible.value = true
}

function openEdit(row: JsonObject) {
  editingId.value = Number(row.id)
  form.roomName = String(row.roomName ?? '')
  form.location = String(row.location ?? '')
  form.capacity = Number(row.capacity ?? 0)
  form.equipment = String(row.equipment ?? '')
  form.remark = String(row.remark ?? '')
  form.status = String(row.status ?? 'ENABLED')
  dialogVisible.value = true
}

async function submitForm() {
  const body = {
    roomName: form.roomName,
    location: form.location || null,
    capacity: form.capacity,
    equipment: form.equipment || null,
    remark: form.remark || null,
    status: form.status,
  }
  try {
    if (editingId.value != null) {
      await updateMeetingRoom(editingId.value, body)
      ElMessage.success('已保存')
    } else {
      await createMeetingRoom(body)
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

function roomStatusLabel(s: string) {
  if (s === 'ENABLED') return '可用'
  if (s === 'DISABLED') return '停用'
  return s || '—'
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">会议室</h2>
        <p class="muted">
          维护会议室档案。
          <RouterLink to="/meetings/bookings">会议预约</RouterLink>
        </p>
      </div>
      <div class="oa-page__actions">
        <el-button v-if="canManage" type="primary" @click="openCreate">新增会议室</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="roomName" label="名称" min-width="140" />
        <el-table-column prop="location" label="位置" min-width="120" />
        <el-table-column prop="capacity" label="容量" width="88" />
        <el-table-column prop="equipment" label="设备" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ roomStatusLabel(String(row.status ?? '')) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canManage" label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId != null ? '编辑会议室' : '新增会议室'" width="520px" destroy-on-close>
      <el-form label-width="88px">
        <el-form-item label="名称" required>
          <el-input v-model="form.roomName" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" maxlength="255" />
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="form.capacity" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="设备">
          <el-input v-model="form.equipment" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="可用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
