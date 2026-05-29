<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { Sunny, Moon } from '@element-plus/icons-vue'

const isDark = ref(false)

function toggleTheme() {
  isDark.value = !isDark.value
  applyTheme()
  localStorage.setItem('oa_theme', isDark.value ? 'dark' : 'light')
}

function applyTheme() {
  if (isDark.value) {
    document.documentElement.classList.add('dark')
    document.documentElement.classList.remove('light')
  } else {
    document.documentElement.classList.add('light')
    document.documentElement.classList.remove('dark')
  }
}

onMounted(() => {
  const savedTheme = localStorage.getItem('oa_theme')
  if (savedTheme) {
    isDark.value = savedTheme === 'dark'
  } else {
    // 检测系统偏好
    isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  applyTheme()
})
</script>

<template>
  <el-button text @click="toggleTheme" class="theme-toggle">
    <el-icon :size="18">
      <Sunny v-if="isDark" />
      <Moon v-else />
    </el-icon>
  </el-button>
</template>

<style scoped>
.theme-toggle {
  font-size: 18px;
}
</style>

<style>
/* 暗色主题变量 */
.dark {
  --el-bg-color: #1a1a2e;
  --el-bg-color-page: #0f0f23;
  --el-bg-color-overlay: #16213e;
  --el-text-color-primary: #e0e0e0;
  --el-text-color-regular: #cccccc;
  --el-text-color-secondary: #999999;
  --el-border-color: #333366;
  --el-border-color-light: #2a2a5a;
  --el-fill-color-blank: #1a1a2e;
  --el-fill-color: #2a2a5a;
  --el-fill-color-light: #222255;
  --el-fill-color-lighter: #1e1e4a;
  --el-color-primary: #6366f1;
  --el-color-primary-light-3: #818cf8;
  --el-color-primary-light-5: #a5b4fc;
  --el-color-primary-light-7: #c7d2fe;
  --el-color-primary-light-9: #e0e7ff;
  --el-color-primary-dark-2: #4f46e5;
  --el-mask-color: rgba(0, 0, 0, 0.6);

  /* OA 自定义变量 */
  --oa-bg: #1a1a2e;
  --oa-bg-white: #16213e;
  --oa-bg-page: #0f0f23;
  --oa-bg-card: #1a1a2e;
  --oa-bg-sidebar: #0a0a1a;
  --oa-bg-header: #16213e;
  --oa-text-primary: #e0e0e0;
  --oa-text-secondary: #999999;
  --oa-text-regular: #cccccc;
  --oa-text-muted: #666666;
  --oa-border: #333366;
  --oa-border-light: #2a2a5a;
}

/* 浅色主题变量（默认） */
.light {
  --oa-bg: #ffffff;
  --oa-bg-white: #ffffff;
  --oa-bg-page: #f8fafc;
  --oa-bg-card: #fff;
  --oa-bg-sidebar: #0f172a;
  --oa-bg-header: #fff;
  --oa-text-primary: #1e293b;
  --oa-text-secondary: #64748b;
  --oa-text-regular: #475569;
  --oa-text-muted: #94a3b8;
  --oa-border: #e2e8f0;
  --oa-border-light: #f1f5f9;
}
</style>
