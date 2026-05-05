<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { previewUserPermission } from '../../api/permission'
import { listUsers } from '../../api/org'
import type { JsonObject } from '../../api/types'

const userId = ref<number | undefined>(undefined)
const loading = ref(false)
const userOptions = ref<{ id: number; realName: string }[]>([])

async function loadUsers() {
  try {
    const res = await listUsers(1, 500)
    userOptions.value = res.items.map((u: Record<string, unknown>) => ({ id: Number(u.id), realName: String(u.realName ?? '') }))
  } catch { /* ignore */ }
}

onMounted(() => { void loadUsers() })
const result = ref<JsonObject | null>(null)

async function load() {
  if (!userId.value) return
  loading.value = true
  try {
    result.value = await previewUserPermission(userId.value)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">权限预览</h2>
        <p class="muted">查看指定用户最终菜单、按钮、数据权限。</p>
      </div>
    </div>

    <el-card shadow="never" style="margin-bottom: 12px">
      <el-form inline>
        <el-form-item label="用户ID">
          <el-select v-model="userId" filterable placeholder="选择用户" style="width: 200px">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-empty v-if="!result" description="请输入用户ID并查询" />
      <pre v-else class="permission-preview">{{ JSON.stringify(result, null, 2) }}</pre>
    </el-card>
  </div>
</template>

<style scoped>
.permission-preview {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.5;
}
</style>
