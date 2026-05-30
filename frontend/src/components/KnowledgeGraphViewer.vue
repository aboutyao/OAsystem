<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Plus, Delete, View, Warning, UploadFilled } from '@element-plus/icons-vue'

interface Relation {
  type: string
  id: number | string
  label: string
  [key: string]: any
}

const props = defineProps<{
  entityType: string
  entityId: number
}>()

const loading = ref(false)
const relations = ref<Relation[]>([])
const searchKeyword = ref('')
const searchResults = ref<any[]>([])

async function loadRelations() {
  loading.value = true
  try {
    const response = await fetch(`/api/knowledge-graph/relations/${props.entityType}/${props.entityId}`)
    const data = await response.json()
    relations.value = data.data?.relations || []
  } catch (e) {
    console.error('Failed to load relations:', e)
  } finally {
    loading.value = false
  }
}

async function searchEntities() {
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }

  try {
    const response = await fetch(`/api/knowledge-graph/search?keyword=${encodeURIComponent(searchKeyword.value)}`)
    const data = await response.json()
    searchResults.value = data.data || []
  } catch (e) {
    console.error('Failed to search:', e)
  }
}

function getRelationColor(type: string): string {
  const colors: Record<string, string> = {
    'PURCHASE': '#409EFF',
    'CONTRACT': '#67C23A',
    'EXPENSE': '#E6A23C',
    'USER': '#909399',
    'DEPARTMENT': '#F56C6C',
    'SUPPLIER': '#b37feb',
  }
  return colors[type] || '#909399'
}

function getRelationIcon(type: string): string {
  const icons: Record<string, string> = {
    'PURCHASE': 'ShoppingCart',
    'CONTRACT': 'Document',
    'EXPENSE': 'Wallet',
    'USER': 'User',
    'DEPARTMENT': 'OfficeBuilding',
    'SUPPLIER': 'Shop',
  }
  return icons[type] || 'Link'
}

onMounted(loadRelations)
</script>

<template>
  <div class="knowledge-graph-viewer">
    <div class="viewer-header">
      <el-icon><Document /></el-icon>
      <span>关联图谱</span>
    </div>

    <!-- 搜索 -->
    <div class="search-box">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索关联实体..."
        :prefix-icon="View"
        clearable
        @input="searchEntities"
      />
    </div>

    <div v-if="searchResults.length > 0" class="search-results">
      <div v-for="result in searchResults" :key="`${result.type}-${result.id}`" class="search-result-item">
        <el-tag :color="getRelationColor(result.type)" size="small" effect="dark">{{ result.type }}</el-tag>
        <span>{{ result.name }}</span>
      </div>
    </div>

    <!-- 关联列表 -->
    <div v-loading="loading" class="relations-list">
      <div v-if="relations.length === 0 && !loading" class="empty-hint">
        暂无关联数据
      </div>

      <div v-for="relation in relations" :key="`${relation.type}-${relation.id}`" class="relation-item">
        <div class="relation-badge" :style="{ backgroundColor: getRelationColor(relation.type) }">
          {{ relation.type.charAt(0) }}
        </div>
        <div class="relation-info">
          <div class="relation-label">{{ relation.label }}</div>
          <div class="relation-meta">
            <el-tag size="small" :color="getRelationColor(relation.type)" effect="plain">{{ relation.type }}</el-tag>
            <span v-if="relation.amount">¥{{ relation.amount }}</span>
            <span v-if="relation.status">{{ relation.status }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.knowledge-graph-viewer {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.viewer-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 12px;
}

.search-box {
  margin-bottom: 12px;
}

.search-results {
  max-height: 150px;
  overflow-y: auto;
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
}

.search-result-item:hover {
  background: white;
}

.relations-list {
  max-height: 300px;
  overflow-y: auto;
}

.relation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  background: white;
  border-radius: 8px;
  margin-bottom: 8px;
}

.relation-badge {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 14px;
}

.relation-info {
  flex: 1;
}

.relation-label {
  font-weight: 500;
  margin-bottom: 4px;
}

.relation-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.empty-hint {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 20px;
}
</style>
