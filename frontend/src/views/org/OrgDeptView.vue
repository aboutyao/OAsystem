<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { createDept, getDeptTree, listUsers, updateDept } from '../../api/org'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const tree = ref<JsonObject[]>([])
const users = ref<JsonObject[]>([])
const current = ref<JsonObject | null>(null)

const form = reactive({
  id: undefined as number | undefined,
  parentId: undefined as number | undefined,
  deptCode: '',
  deptName: '',
  sortOrder: 0,
  leaderUserId: undefined as number | undefined,
})

async function load() {
  loading.value = true
  try {
    tree.value = await getDeptTree()
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  try {
    const res = await listUsers(1, 500)
    // 过滤掉已离职的员工，仅展示在职人员作为负责人候选
    users.value = res.items.filter((u: JsonObject) => String(u.employeeStatus ?? '') !== 'RESIGNED')
  } catch {
    // ignore
  }
}

void load()
onMounted(loadUsers)

function onNodeClick(node: JsonObject) {
  current.value = node
  form.id = Number(node.id)
  form.parentId = node.parentId == null ? undefined : Number(node.parentId)
  form.deptCode = String(node.deptCode ?? '')
  form.deptName = String(node.deptName ?? '')
  form.sortOrder = Number(node.sortOrder ?? 0)
  form.leaderUserId = node.leaderUserId == null ? undefined : Number(node.leaderUserId)
}

function resetForm() {
  form.id = undefined
  form.parentId = undefined
  form.deptCode = ''
  form.deptName = ''
  form.sortOrder = 0
  form.leaderUserId = undefined
  current.value = null
}

async function submit() {
  if (!form.deptCode.trim() || !form.deptName.trim()) {
    ElMessage.warning('请填写部门编码与部门名称')
    return
  }
  const body = {
    deptCode: form.deptCode.trim(),
    deptName: form.deptName.trim(),
    parentId: form.parentId ?? null,
    leaderUserId: form.leaderUserId ?? null,
    sortOrder: form.sortOrder ?? 0,
  }
  try {
    if (form.id != null) {
      await updateDept(form.id, body)
      ElMessage.success('部门已更新')
    } else {
      await createDept(body)
      ElMessage.success('部门已创建')
    }
    await load()
    resetForm()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">组织架构</h2>
        <p class="muted">部门树与部门信息维护。</p>
      </div>
    </div>

    <el-row :gutter="12">
      <el-col :span="10">
        <el-card shadow="never" v-loading="loading">
          <template #header>部门树</template>
          <el-tree
            node-key="id"
            :data="tree"
            :props="{ label: 'deptName', children: 'children' }"
            @node-click="onNodeClick"
          />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>{{ form.id != null ? '编辑部门' : '新增部门' }}</template>
          <el-form label-width="96px">
            <el-form-item label="上级部门">
              <el-tree-select
                v-model="form.parentId"
                :data="tree"
                :props="{ label: 'deptName', value: 'id', children: 'children' }"
                check-strictly
                clearable
                placeholder="请选择上级部门"
              />
            </el-form-item>
            <el-form-item label="部门编码" required>
              <el-input v-model="form.deptCode" maxlength="64" />
            </el-form-item>
            <el-form-item label="部门名称" required>
              <el-input v-model="form.deptName" maxlength="128" />
            </el-form-item>
            <el-form-item label="负责人">
              <el-select v-model="form.leaderUserId" filterable clearable placeholder="请选择负责人">
                <el-option
                  v-for="u in users"
                  :key="u.id"
                  :label="u.realName || u.username"
                  :value="Number(u.id)"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" />
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
