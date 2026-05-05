<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { createButton, getMenuTree, listButtons } from '../../api/permission'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const menuIdFilter = ref<number | undefined>(undefined)
const menus = ref<JsonObject[]>([])

function flattenTree(nodes: JsonObject[]): JsonObject[] {
  const result: JsonObject[] = []
  for (const node of nodes) {
    result.push(node)
    if (Array.isArray(node.children) && node.children.length > 0) {
      result.push(...flattenTree(node.children as JsonObject[]))
    }
  }
  return result
}

const form = reactive({
  menuId: undefined as number | undefined,
  buttonCode: '',
  buttonName: '',
  permissionCode: '',
  status: 'ENABLED',
})

async function load() {
  loading.value = true
  try {
    const res = await listButtons(page.value, size.value, menuIdFilter.value)
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}
async function loadMenus() {
  try {
    const tree = await getMenuTree()
    menus.value = flattenTree(tree)
  } catch {
    // ignore
  }
}

void load()
onMounted(loadMenus)

async function submit() {
  if (!form.menuId || !form.buttonCode.trim() || !form.buttonName.trim() || !form.permissionCode.trim()) {
    ElMessage.warning('请填写菜单ID、按钮编码、按钮名、权限标识')
    return
  }
  await createButton({
    menuId: form.menuId,
    buttonCode: form.buttonCode.trim(),
    buttonName: form.buttonName.trim(),
    permissionCode: form.permissionCode.trim(),
    status: form.status,
  })
  ElMessage.success('按钮已创建')
  form.buttonCode = ''
  form.buttonName = ''
  form.permissionCode = ''
  await load()
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">按钮权限</h2>
        <p class="muted">维护按钮与权限标识。</p>
      </div>
    </div>

    <el-card shadow="never" class="oa-page__filters">
      <el-form inline>
        <el-form-item label="按菜单筛选">
          <el-select v-model="menuIdFilter" filterable clearable placeholder="请选择菜单">
            <el-option
              v-for="m in menus"
              :key="m.id"
              :label="m.menuName"
              :value="Number(m.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item><el-button @click="load">查询</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-bottom: 12px">
      <template #header>新增按钮</template>
      <el-form inline>
        <el-form-item label="菜单">
          <el-select v-model="form.menuId" filterable clearable placeholder="请选择菜单">
            <el-option
              v-for="m in menus"
              :key="m.id"
              :label="m.menuName"
              :value="Number(m.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="按钮编码"><el-input v-model="form.buttonCode" /></el-form-item>
        <el-form-item label="按钮名称"><el-input v-model="form.buttonName" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.permissionCode" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status"><el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" /></el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="submit">创建</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="menuId" label="菜单ID" width="90" />
        <el-table-column prop="buttonCode" label="按钮编码" width="140" />
        <el-table-column prop="buttonName" label="按钮名称" width="140" />
        <el-table-column prop="permissionCode" label="权限标识" min-width="180" />
        <el-table-column prop="status" label="状态" width="100" />
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
