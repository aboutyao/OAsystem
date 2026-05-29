<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, Eye, FontSize, Monitor } from '@element-plus/icons-vue'

interface AccessibilityConfig {
  highContrast: boolean
  largeText: boolean
  reduceMotion: boolean
  screenReader: boolean
  keyboardNavigation: boolean
}

const loading = ref(false)
const config = ref<AccessibilityConfig>({
  highContrast: false,
  largeText: false,
  reduceMotion: false,
  screenReader: false,
  keyboardNavigation: true,
})

async function loadConfig() {
  loading.value = true
  try {
    const saved = localStorage.getItem('oa_accessibility_config')
    if (saved) {
      config.value = JSON.parse(saved)
      applyConfig(config.value)
    }
  } catch (e) {
    console.error('Failed to load accessibility config:', e)
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  try {
    localStorage.setItem('oa_accessibility_config', JSON.stringify(config.value))
    applyConfig(config.value)
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

function applyConfig(config: AccessibilityConfig) {
  const root = document.documentElement

  // 高对比度
  if (config.highContrast) {
    root.classList.add('high-contrast')
  } else {
    root.classList.remove('high-contrast')
  }

  // 大字体
  if (config.largeText) {
    root.classList.add('large-text')
  } else {
    root.classList.remove('large-text')
  }

  // 减少动画
  if (config.reduceMotion) {
    root.classList.add('reduce-motion')
  } else {
    root.classList.remove('reduce-motion')
  }
}

onMounted(loadConfig)
</script>

<template>
  <div class="accessibility-settings">
    <div class="settings-header">
      <h3>无障碍访问</h3>
      <el-button type="primary" @click="saveConfig">保存设置</el-button>
    </div>

    <div v-loading="loading" class="settings-list">
      <div class="setting-item">
        <div class="setting-info">
          <el-icon><Eye /></el-icon>
          <div>
            <div class="setting-name">高对比度模式</div>
            <div class="setting-desc">增强颜色对比度，便于视觉障碍用户阅读</div>
          </div>
        </div>
        <el-switch v-model="config.highContrast" />
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <el-icon><FontSize /></el-icon>
          <div>
            <div class="setting-name">大字体模式</div>
            <div class="setting-desc">放大所有文字，便于视力不佳用户阅读</div>
          </div>
        </div>
        <el-switch v-model="config.largeText" />
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <el-icon><Monitor /></el-icon>
          <div>
            <div class="setting-name">减少动画</div>
            <div class="setting-desc">减少页面动画效果，便于光敏性癫痫用户</div>
          </div>
        </div>
        <el-switch v-model="config.reduceMotion" />
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <el-icon><Setting /></el-icon>
          <div>
            <div class="setting-name">屏幕阅读器优化</div>
            <div class="setting-desc">优化界面元素，便于屏幕阅读器识别</div>
          </div>
        </div>
        <el-switch v-model="config.screenReader" />
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <el-icon><Setting /></el-icon>
          <div>
            <div class="setting-name">键盘导航</div>
            <div class="setting-desc">支持纯键盘操作，便于行动不便用户</div>
          </div>
        </div>
        <el-switch v-model="config.keyboardNavigation" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.accessibility-settings {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.settings-header h3 {
  margin: 0;
}

.settings-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: white;
  border-radius: 8px;
}

.setting-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.setting-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.setting-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>

<style>
/* 高对比度模式 */
.high-contrast {
  --el-color-primary: #0000ff;
  --el-color-success: #008000;
  --el-color-warning: #ff8c00;
  --el-color-danger: #ff0000;
  --el-text-color-primary: #000000;
  --el-text-color-regular: #333333;
  --el-bg-color: #ffffff;
  --el-border-color: #000000;
}

/* 大字体模式 */
.large-text {
  font-size: 18px;
}

.large-text * {
  font-size: inherit;
}

/* 减少动画 */
.reduce-motion *,
.reduce-motion *::before,
.reduce-motion *::after {
  animation-duration: 0.01ms !important;
  animation-iteration-count: 1 !important;
  transition-duration: 0.01ms !important;
}
</style>
