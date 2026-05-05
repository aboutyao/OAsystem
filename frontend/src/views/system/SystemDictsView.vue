<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDictTypes, listDictItems, createDictType, updateDictType, deleteDictType, createDictItem, updateDictItem, deleteDictItem } from '../../api/system'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const itemsLoading = ref(false)
const items = ref<JsonObject[]>([])
const currentType = ref<JsonObject | null>(null)

// --- Dict Type dialog ---
const typeDialogVisible = ref(false)
const typeDialogMode = ref<'create' | 'edit'>('create')
const typeForm = reactive({ dictCode: '', dictName: '', status: 'ACTIVE', remark: '' })
const typeEditingId = ref<number | null>(null)

// --- Dict Item dialog ---
const itemDialogVisible = ref(false)
const itemDialogMode = ref<'create' | 'edit'>('create')
const itemForm = reactive({ itemLabel: '', itemValue: '', sortOrder: 0, remark: '' })
const itemEditingId = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    const res = await listDictTypes(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

void load()

async function selectType(row: JsonObject) {
  currentType.value = row
  itemsLoading.value = true
  try {
    items.value = await listDictItems(String(row.dictCode))
  } finally {
    itemsLoading.value = false
  }
}

// --- Dict Type CRUD ---
function openCreateType() {
  typeDialogMode.value = 'create'
  typeForm.dictCode = ''
  typeForm.dictName = ''
  typeForm.status = 'ACTIVE'
  typeForm.remark = ''
  typeEditingId.value = null
  typeDialogVisible.value = true
}

function openEditType(row: JsonObject) {
  typeDialogMode.value = 'edit'
  typeForm.dictCode = String(row.dictCode ?? '')
  typeForm.dictName = String(row.dictName ?? '')
  typeForm.status = String(row.status ?? 'ACTIVE')
  typeForm.remark = String(row.remark ?? '')
  typeEditingId.value = Number(row.id)
  typeDialogVisible.value = true
}

async function saveType() {
  if (!typeForm.dictCode.trim() || !typeForm.dictName.trim()) {
    ElMessage.warning('编码和名称不能为空')
    return
  }
  try {
    if (typeDialogMode.value === 'create') {
      await createDictType({ dictCode: typeForm.dictCode, dictName: typeForm.dictName, remark: typeForm.remark || null })
      ElMessage.success('字典类型已创建')
    } else {
      await updateDictType(typeEditingId.value!, { dictName: typeForm.dictName, remark: typeForm.remark || null })
      ElMessage.success('字典类型已更新')
    }
    typeDialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function handleDeleteType(row: JsonObject) {
  try {
    await ElMessageBox.confirm(`确认删除字典类型「${row.dictName}」？删除后其下所有字典项也将被删除。`, '删除确认', { type: 'warning' })
    await deleteDictType(Number(row.id))
    ElMessage.success('已删除')
    if (currentType.value && currentType.value.id === row.id) {
      currentType.value = null
      items.value = []
    }
    await load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

// --- Dict Item CRUD ---
function openCreateItem() {
  if (!currentType.value) {
    ElMessage.warning('请先选择一个字典类型')
    return
  }
  itemDialogMode.value = 'create'
  itemForm.itemLabel = ''
  itemForm.itemValue = ''
  itemForm.sortOrder = 0
  itemForm.remark = ''
  itemEditingId.value = null
  itemDialogVisible.value = true
}

function openEditItem(row: JsonObject) {
  itemDialogMode.value = 'edit'
  itemForm.itemLabel = String(row.itemLabel ?? '')
  itemForm.itemValue = String(row.itemValue ?? '')
  itemForm.sortOrder = Number(row.sortOrder ?? 0)
  itemForm.remark = String(row.remark ?? '')
  itemEditingId.value = Number(row.id)
  itemDialogVisible.value = true
}

async function saveItem() {
  if (!itemForm.itemLabel.trim() || !itemForm.itemValue.trim()) {
    ElMessage.warning('标签和值不能为空')
    return
  }
  try {
    if (itemDialogMode.value === 'create') {
      await createDictItem(Number(currentType.value!.id), {
        itemLabel: itemForm.itemLabel,
        itemValue: itemForm.itemValue,
        sortOrder: itemForm.sortOrder,
        remark: itemForm.remark || null,
      })
      ElMessage.success('字典项已创建')
    } else {
      await updateDictItem(itemEditingId.value!, {
        itemLabel: itemForm.itemLabel,
        itemValue: itemForm.itemValue,
        sortOrder: itemForm.sortOrder,
      })
      ElMessage.success('字典项已更新')
    }
    itemDialogVisible.value = false
    if (currentType.value) {
      items.value = await listDictItems(String(currentType.value.dictCode))
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function handleDeleteItem(row: JsonObject) {
  try {
    await ElMessageBox.confirm(`确认删除字典项「${row.itemLabel}」？`, '删除确认', { type: 'warning' })
    await deleteDictItem(Number(row.id))
    ElMessage.success('已删除')
    if (currentType.value) {
      items.value = await listDictItems(String(currentType.value.dictCode))
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">字典管理</h2>
        <p class="muted">管理字典类型与字典项；左侧选择类型，右侧显示字典项。</p>
      </div>
    </div>

    <div class="dict-grid">
      <el-card shadow="never" class="dict-types">
        <template #header>
          <div class="card-header-flex">
            <span>字典类型</span>
            <el-button type="primary" size="small" @click="openCreateType">新增类型</el-button>
          </div>
        </template>
        <el-table v-loading="loading" :data="rows" stripe :highlight-current-row="true" @row-click="selectType">
          <el-table-column prop="dictCode" label="编码" min-width="140" />
          <el-table-column prop="dictName" label="名称" min-width="140" />
          <el-table-column prop="status" label="状态" width="80" />
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click.stop="openEditType(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click.stop="handleDeleteType(row)">删除</el-button>
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

      <el-card shadow="never" class="dict-items">
        <template #header>
          <div class="card-header-flex">
            <span>
              字典项 <span v-if="currentType" class="muted">{{ currentType.dictCode }} - {{ currentType.dictName }}</span>
            </span>
            <el-button type="primary" size="small" :disabled="!currentType" @click="openCreateItem">新增字典项</el-button>
          </div>
        </template>
        <el-table v-if="currentType" v-loading="itemsLoading" :data="items" stripe>
          <el-table-column prop="itemValue" label="值" min-width="120" />
          <el-table-column prop="itemLabel" label="标签" min-width="140" />
          <el-table-column prop="sortOrder" label="排序" width="70" />
          <el-table-column prop="status" label="状态" width="80" />
          <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openEditItem(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDeleteItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <p v-else class="muted">请先在左侧选择一个字典类型。</p>
      </el-card>
    </div>

    <!-- Dict Type Dialog -->
    <el-dialog v-model="typeDialogVisible" :title="typeDialogMode === 'create' ? '新增字典类型' : '编辑字典类型'" width="480px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="编码">
          <el-input v-model="typeForm.dictCode" :disabled="typeDialogMode === 'edit'" placeholder="请输入字典编码" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="typeForm.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="typeForm.status">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveType">确定</el-button>
      </template>
    </el-dialog>

    <!-- Dict Item Dialog -->
    <el-dialog v-model="itemDialogVisible" :title="itemDialogMode === 'create' ? '新增字典项' : '编辑字典项'" width="480px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="标签">
          <el-input v-model="itemForm.itemLabel" placeholder="显示标签" />
        </el-form-item>
        <el-form-item label="值">
          <el-input v-model="itemForm.itemValue" placeholder="字典项值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveItem">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dict-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 1200px) {
  .dict-grid {
    grid-template-columns: 1fr;
  }
}
.card-header-flex {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
