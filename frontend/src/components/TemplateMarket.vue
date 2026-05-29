<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Search, Filter } from '@element-plus/icons-vue'

interface Template {
  id: number
  name: string
  description: string
  category: string
  industry: string
  downloadCount: number
  authorName: string
  createdAt: string
}

const loading = ref(false)
const templates = ref<Template[]>([])
const categories = ref<{ category: string; count: number }[]>([])
const industries = ref<{ industry: string; count: number }[]>([])

const selectedCategory = ref('')
const selectedIndustry = ref('')
const searchKeyword = ref('')

async function loadTemplates() {
  loading.value = true
  try {
    let url = '/api/templates?'
    if (selectedCategory.value) url += `category=${selectedCategory.value}&`
    if (selectedIndustry.value) url += `industry=${selectedIndustry.value}&`

    const response = await fetch(url)
    const data = await response.json()
    templates.value = data.data || []
  } catch (e) {
    console.error('Failed to load templates:', e)
  } finally {
    loading.value = false
  }
}

async function loadFilters() {
  try {
    const [catRes, indRes] = await Promise.all([
      fetch('/api/templates/categories'),
      fetch('/api/templates/industries'),
    ])
    const catData = await catRes.json()
    const indData = await indRes.json()
    categories.value = catData.data || []
    industries.value = indData.data || []
  } catch (e) {
    console.error('Failed to load filters:', e)
  }
}

async function downloadTemplate(template: Template) {
  try {
    const response = await fetch(`/api/templates/${template.id}/download`, { method: 'POST' })
    const result = await response.json()

    if (result.data?.content) {
      // 下载模板文件
      const blob = new Blob([result.data.content], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${template.name}.json`
      a.click()
      URL.revokeObjectURL(url)

      ElMessage.success('下载成功')
      loadTemplates()
    }
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadTemplates()
  loadFilters()
})
</script>

<template>
  <div class="template-market">
    <div class="market-header">
      <h3>模板市场</h3>
    </div>

    <div class="filter-bar">
      <el-select v-model="selectedCategory" placeholder="分类" clearable @change="loadTemplates">
        <el-option v-for="cat in categories" :key="cat.category" :label="`${cat.category} (${cat.count})`" :value="cat.category" />
      </el-select>
      <el-select v-model="selectedIndustry" placeholder="行业" clearable @change="loadTemplates">
        <el-option v-for="ind in industries" :key="ind.industry" :label="`${ind.industry} (${ind.count})`" :value="ind.industry" />
      </el-select>
    </div>

    <div v-loading="loading" class="template-list">
      <div v-if="templates.length === 0 && !loading" class="empty-hint">
        暂无模板
      </div>

      <div v-for="template in templates" :key="template.id" class="template-card">
        <div class="template-info">
          <h4>{{ template.name }}</h4>
          <p>{{ template.description }}</p>
          <div class="template-meta">
            <el-tag size="small">{{ template.category }}</el-tag>
            <el-tag size="small" type="info">{{ template.industry }}</el-tag>
            <span class="download-count">
              <el-icon><Download /></el-icon>
              {{ template.downloadCount }}
            </span>
          </div>
        </div>
        <el-button type="primary" :icon="Download" @click="downloadTemplate(template)">
          下载
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.template-market {
  padding: 16px;
}

.market-header {
  margin-bottom: 16px;
}

.market-header h3 {
  margin: 0;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.template-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.template-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.template-info h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
}

.template-info p {
  margin: 0 0 8px 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.template-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.download-count {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--el-text-color-secondary);
}

.empty-hint {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 40px;
}
</style>
