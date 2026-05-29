<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Brush, Check } from '@element-plus/icons-vue'

interface ThemeConfig {
  primary: string
  sidebar: string
  header: string
  borderRadius: string
  title: string
}

const loading = ref(false)
const isEditing = ref(false)
const config = ref<ThemeConfig>({
  primary: '#4f46e5',
  sidebar: '#0f172a',
  header: '#ffffff',
  borderRadius: '8px',
  title: '企业级 OA 系统',
})

const presetThemes = [
  { name: '默认蓝', primary: '#409EFF', sidebar: '#0f172a' },
  { name: '活力橙', primary: '#E6A23C', sidebar: '#1a1a2e' },
  { name: '商务灰', primary: '#909399', sidebar: '#2d3436' },
  { name: '自然绿', primary: '#67C23A', sidebar: '#0d3b2e' },
  { name: '热情红', primary: '#F56C6C', sidebar: '#2d132c' },
]

async function loadConfig() {
  loading.value = true
  try {
    const response = await fetch('/api/theme/config')
    const data = await response.json()
    if (data.data) {
      config.value = { ...config.value, ...data.data }
      applyTheme(config.value)
    }
  } catch (e) {
    console.error('Failed to load theme config:', e)
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  try {
    await fetch('/api/theme/config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(config.value),
    })
    ElMessage.success('保存成功')
    isEditing.value = false
    applyTheme(config.value)
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

function applyTheme(theme: ThemeConfig) {
  document.documentElement.style.setProperty('--oa-primary', theme.primary)
  document.documentElement.style.setProperty('--oa-bg-sidebar', theme.sidebar)
  document.documentElement.style.setProperty('--oa-bg-header', theme.header)
  document.documentElement.style.setProperty('--oa-radius-sm', theme.borderRadius)
}

function applyPresetTheme(preset: { primary: string; sidebar: string }) {
  config.value.primary = preset.primary
  config.value.sidebar = preset.sidebar
  applyTheme(config.value)
}

onMounted(loadConfig)
</script>

<template>
  <div class="theme-customizer">
    <div class="customizer-header">
      <h3>主题定制</h3>
      <el-button v-if="!isEditing" type="primary" :icon="Brush" @click="isEditing = true">自定义</el-button>
      <template v-else>
        <el-button @click="isEditing = false">取消</el-button>
        <el-button type="primary" @click="saveConfig">保存</el-button>
      </template>
    </div>

    <div v-loading="loading">
      <!-- 预设主题 -->
      <div class="preset-themes">
        <h4>预设主题</h4>
        <div class="theme-list">
          <div
            v-for="preset in presetThemes"
            :key="preset.name"
            class="theme-item"
            :class="{ active: config.primary === preset.primary }"
            @click="applyPresetTheme(preset)"
          >
            <div class="theme-color" :style="{ backgroundColor: preset.primary }"></div>
            <span class="theme-name">{{ preset.name }}</span>
            <el-icon v-if="config.primary === preset.primary" class="check-icon"><Check /></el-icon>
          </div>
        </div>
      </div>

      <!-- 自定义配置 -->
      <div v-if="isEditing" class="custom-config">
        <h4>自定义配置</h4>
        <el-form label-width="100px">
          <el-form-item label="主题色">
            <el-color-picker v-model="config.primary" />
          </el-form-item>
          <el-form-item label="侧边栏色">
            <el-color-picker v-model="config.sidebar" />
          </el-form-item>
          <el-form-item label="圆角大小">
            <el-select v-model="config.borderRadius">
              <el-option value="4px" label="小" />
              <el-option value="8px" label="中" />
              <el-option value="12px" label="大" />
              <el-option value="16px" label="特大" />
            </el-select>
          </el-form-item>
          <el-form-item label="系统标题">
            <el-input v-model="config.title" />
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.theme-customizer {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.customizer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.customizer-header h3 {
  margin: 0;
}

.preset-themes {
  margin-bottom: 24px;
}

.preset-themes h4,
.custom-config h4 {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
}

.theme-list {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.theme-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.theme-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.theme-item.active {
  border: 2px solid var(--oa-primary);
}

.theme-color {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.theme-name {
  font-size: 13px;
}

.check-icon {
  position: absolute;
  top: 8px;
  right: 8px;
  color: var(--oa-primary);
}
</style>
