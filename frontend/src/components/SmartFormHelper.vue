<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, User, Document } from '@element-plus/icons-vue'

interface SupplierSuggestion {
  name: string
  usageCount: number
}

interface ExpenseTemplate {
  id: number
  title: string
  category: string
  amount: number
  createdAt: string
}

const props = defineProps<{
  userId: number
  type: 'supplier' | 'category' | 'template'
}>()

const emit = defineEmits<{
  select: [value: unknown]
}>()

const loading = ref(false)
const suggestions = ref<SupplierSuggestion[] | string[] | ExpenseTemplate[]>([])
const keyword = ref('')

async function loadSuggestions() {
  loading.value = true
  try {
    let url = ''
    switch (props.type) {
      case 'supplier':
        url = `/api/smart-form/suppliers?userId=${props.userId}&keyword=${keyword.value}`
        break
      case 'category':
        url = `/api/smart-form/categories?userId=${props.userId}&keyword=${keyword.value}`
        break
      case 'template':
        url = `/api/smart-form/templates?userId=${props.userId}`
        break
    }
    const response = await fetch(url)
    const data = await response.json()
    suggestions.value = data.data || []
  } catch (e) {
    console.error('Failed to load suggestions:', e)
  } finally {
    loading.value = false
  }
}

function selectItem(item: unknown) {
  emit('select', item)
}

watch(keyword, () => {
  if (props.type !== 'template') {
    loadSuggestions()
  }
})
</script>

<template>
  <div class="smart-form-helper">
    <div class="helper-header">
      <el-icon><MagicStick /></el-icon>
      <span>智能推荐</span>
    </div>

    <div v-if="type !== 'template'" class="search-box">
      <el-input
        v-model="keyword"
        placeholder="搜索..."
        size="small"
        clearable
      />
    </div>

    <div v-loading="loading" class="suggestion-list">
      <div v-if="suggestions.length === 0 && !loading" class="empty-hint">
        暂无推荐
      </div>

      <!-- 供应商推荐 -->
      <template v-if="type === 'supplier'">
        <div
          v-for="item in (suggestions as SupplierSuggestion[])"
          :key="item.name"
          class="suggestion-item"
          @click="selectItem(item.name)"
        >
          <el-icon><User /></el-icon>
          <span class="item-name">{{ item.name }}</span>
          <span class="item-count">使用 {{ item.usageCount }} 次</span>
        </div>
      </template>

      <!-- 费用科目推荐 -->
      <template v-if="type === 'category'">
        <div
          v-for="item in (suggestions as string[])"
          :key="item"
          class="suggestion-item"
          @click="selectItem(item)"
        >
          <el-icon><Document /></el-icon>
          <span class="item-name">{{ item }}</span>
        </div>
      </template>

      <!-- 模板推荐 -->
      <template v-if="type === 'template'">
        <div
          v-for="item in (suggestions as ExpenseTemplate[])"
          :key="item.id"
          class="suggestion-item template-item"
          @click="selectItem(item)"
        >
          <div class="template-title">{{ item.title }}</div>
          <div class="template-meta">
            <span>{{ item.category }}</span>
            <span>¥{{ item.amount }}</span>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.smart-form-helper {
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.helper-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--el-color-primary);
}

.search-box {
  margin-bottom: 8px;
}

.suggestion-list {
  max-height: 200px;
  overflow-y: auto;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
}

.suggestion-item:hover {
  background: white;
}

.item-name {
  flex: 1;
  font-size: 13px;
}

.item-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.template-item {
  flex-direction: column;
  align-items: flex-start;
}

.template-title {
  font-weight: 500;
}

.template-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.empty-hint {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 16px;
  font-size: 13px;
}
</style>
