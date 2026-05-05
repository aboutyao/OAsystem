<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createMenu, getMenuTree, updateMenu } from '../../api/permission'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const tree = ref<JsonObject[]>([])
const editingId = ref<number | null>(null)
const form = reactive({
  parentId: undefined as number | undefined,
  menuCode: '',
  menuName: '',
  routePath: '',
  component: 'common/PlaceholderView',
  icon: '',
  sortOrder: 0,
  visible: 1,
  status: 'ENABLED',
})

async function load() {
  loading.value = true
  try {
    tree.value = await getMenuTree()
  } finally {
    loading.value = false
  }
}
void load()

function onSelect(node: JsonObject) {
  editingId.value = Number(node.id)
  form.parentId = node.parentId == null ? undefined : Number(node.parentId)
  form.menuCode = String(node.menuCode ?? '')
  form.menuName = String(node.menuName ?? '')
  form.routePath = String(node.routePath ?? '')
  form.component = String(node.component ?? 'common/PlaceholderView')
  form.icon = String(node.icon ?? '')
  form.sortOrder = Number(node.sortOrder ?? 0)
  form.visible = Number(node.visible ?? 1)
  form.status = String(node.status ?? 'ENABLED')
}

function resetForm() {
  editingId.value = null
  form.parentId = undefined
  form.menuCode = ''
  form.menuName = ''
  form.routePath = ''
  form.component = 'common/PlaceholderView'
  form.icon = ''
  form.sortOrder = 0
  form.visible = 1
  form.status = 'ENABLED'
}

async function submit() {
  if (!form.menuCode.trim() || !form.menuName.trim()) {
    ElMessage.warning('请填写菜单编码与名称')
    return
  }
  const body = {
    parentId: form.parentId ?? null,
    menuCode: form.menuCode.trim(),
    menuName: form.menuName.trim(),
    routePath: form.routePath || null,
    component: form.component || null,
    icon: form.icon || null,
    sortOrder: form.sortOrder,
    visible: form.visible,
    status: form.status,
  }
  if (editingId.value == null) {
    await createMenu(body)
    ElMessage.success('菜单已创建')
  } else {
    await updateMenu(editingId.value, body)
    ElMessage.success('菜单已更新')
  }
  await load()
  resetForm()
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">菜单管理</h2>
        <p class="muted">维护菜单树结构与路由配置。</p>
      </div>
    </div>
    <el-row :gutter="12">
      <el-col :span="10">
        <el-card shadow="never" v-loading="loading">
          <template #header>菜单树</template>
          <el-tree node-key="id" :data="tree" :props="{ label: 'menuName', children: 'children' }" @node-click="onSelect" />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>{{ editingId == null ? '新增菜单' : '编辑菜单' }}</template>
          <el-form label-width="96px">
            <el-form-item label="上级菜单">
              <el-tree-select
                v-model="form.parentId"
                :data="tree"
                :props="{ label: 'menuName', value: 'id', children: 'children' }"
                check-strictly
                clearable
                placeholder="请选择上级菜单"
              />
            </el-form-item>
            <el-form-item label="编码" required><el-input v-model="form.menuCode" /></el-form-item>
            <el-form-item label="名称" required><el-input v-model="form.menuName" /></el-form-item>
            <el-form-item label="路由"><el-input v-model="form.routePath" /></el-form-item>
            <el-form-item label="组件"><el-input v-model="form.component" /></el-form-item>
            <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
            <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
            <el-form-item label="可见">
              <el-select v-model="form.visible"><el-option label="显示" :value="1" /><el-option label="隐藏" :value="0" /></el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="form.status"><el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" /></el-select>
            </el-form-item>
            <el-form-item>
              <el-button @click="resetForm">清空</el-button>
              <el-button type="primary" @click="submit">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
