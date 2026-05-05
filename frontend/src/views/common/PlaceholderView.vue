<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getModules } from '../../api/modules'

const route = useRoute()
const title = computed(() => route.meta.title ?? '模块页面')
const moduleName = computed(() => route.meta.module ?? 'module')
const available = ref(false)

onMounted(async () => {
  const result = await getModules()
  available.value = result.modules.includes(String(moduleName.value))
})
</script>

<template>
  <el-card shadow="never" class="placeholder-page">
    <template #header>
      <div class="placeholder-page__header">
        <span>{{ title }}</span>
        <el-tag type="info">开发中</el-tag>
      </div>
    </template>
    <p class="muted">
      {{ moduleName }} 模块已建立路由占位。实现前请先查阅 `docs/oa-design/` 中对应设计文档，
      并按权限、流程、规则、表单、消息、审计统一接入。
    </p>
    <el-alert
      :title="available ? '后端模块入口已登记' : '后端模块入口待登记'"
      :type="available ? 'success' : 'warning'"
      show-icon
      :closable="false"
    />
  </el-card>
</template>
