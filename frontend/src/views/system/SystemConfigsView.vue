<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listConfigs, updateConfig } from '../../api/system'
import type { JsonObject } from '../../api/types'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const editVisible = ref(false)
const current = ref<JsonObject | null>(null)
const editValue = ref('')
const saving = ref(false)

const canEdit = computed(() => {
  const p = auth.user?.permissions ?? []
  return p.includes('*') || p.includes('permission:role:assign')
})

async function load() {
  loading.value = true
  try {
    const res = await listConfigs(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()

function openEdit(row: JsonObject) {
  current.value = row
  editValue.value = String(row.configValue ?? '')
  editVisible.value = true
}

async function onSave() {
  if (!current.value) return
  saving.value = true
  try {
    await updateConfig(String(current.value.configKey), editValue.value)
    ElMessage.success('已保存')
    editVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">系统参数</h2>
        <p class="muted">管理 sys_config 中的运行时参数；不可编辑参数显示为只读。</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="configKey" label="键" min-width="220" />
        <el-table-column prop="configValue" label="值" min-width="220" show-overflow-tooltip />
        <el-table-column prop="configType" label="类型" width="100" />
        <el-table-column prop="configGroup" label="分组" width="120" />
        <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="可编辑" width="90">
          <template #default="{ row }">
            <el-tag :type="row.editable ? 'success' : 'info'" size="small">
              {{ row.editable ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.editable && canEdit" link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="editVisible" title="编辑参数" width="520px" destroy-on-close>
      <template v-if="current">
        <el-form label-width="100px">
          <el-form-item label="键">
            <code>{{ current.configKey }}</code>
          </el-form-item>
          <el-form-item label="说明">
            {{ current.description || '-' }}
          </el-form-item>
          <el-form-item label="值">
            <el-input v-model="editValue" type="textarea" :rows="3" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
