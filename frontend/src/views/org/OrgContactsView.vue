<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listUsers } from '../../api/org'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    const res = await listUsers(page.value, size.value, keyword.value || undefined)
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">通讯录</h2>
        <p class="muted">员工通讯录查询。</p>
      </div>
      <div class="oa-page__actions">
        <el-input
          v-model="keyword"
          placeholder="姓名/用户名/工号"
          clearable
          style="width: 220px"
          @change="load"
        />
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="employeeNo" label="工号" width="120" />
        <el-table-column prop="mainDeptName" label="部门" min-width="160" />
        <el-table-column prop="mobile" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
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
