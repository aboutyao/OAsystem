<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useNaturalLanguageQuery } from '../composables/useNaturalLanguageQuery'

const router = useRouter()
const { result, loading, query } = useNaturalLanguageQuery()
const visible = ref(false)
const input = ref('')

const EXAMPLES = [
  '下周一谁在？',
  '我还有几天年假？',
  '有合同快到期吗？',
  '这个月报销了多少？',
]

async function handleQuery() {
  if (!input.value.trim()) return
  await query(input.value.trim())
}

function go(path: string) {
  visible.value = false
  router.push(path)
}
</script>

<template>
  <div class="nl-query-trigger" @click="visible = true" title="智能提问">
    <el-icon :size="18"><ChatDotRound /></el-icon>
  </div>

  <el-dialog v-model="visible" title="智能问答" width="500px" destroy-on-close>
    <div class="nl-query">
      <el-input
        v-model="input"
        placeholder="用自然语言提问，如：谁在请假？我还有几天年假？"
        size="large"
        @keyup.enter="handleQuery"
      >
        <template #prefix>
          <el-icon><ChatDotRound /></el-icon>
        </template>
        <template #append>
          <el-button :loading="loading" @click="handleQuery">问一下</el-button>
        </template>
      </el-input>

      <div class="nl-examples">
        <span class="nl-examples__label">试试问：</span>
        <el-tag
          v-for="ex in EXAMPLES"
          :key="ex"
          size="small"
          effect="plain"
          style="cursor: pointer"
          @click="input = ex; handleQuery()"
        >
          {{ ex }}
        </el-tag>
      </div>

      <Transition name="fade">
        <div v-if="result" class="nl-result" :class="`nl-result--${result.type}`">
          <div class="nl-result__title">{{ result.title }}</div>
          <div class="nl-result__content">{{ result.content }}</div>
          <el-button
            v-if="result.action"
            type="primary"
            size="small"
            @click="go(result.action!.path)"
            style="margin-top: 12px"
          >
            {{ result.action.label }}
          </el-button>
        </div>
      </Transition>
    </div>
  </el-dialog>
</template>

<style scoped>
.nl-query-trigger {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--oa-radius-sm);
  cursor: pointer;
  transition: background var(--oa-transition);
  color: var(--oa-text-secondary);
}

.nl-query-trigger:hover {
  background: var(--oa-bg-page);
  color: var(--oa-primary);
}

.nl-query {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.nl-examples {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.nl-examples__label {
  font-size: 13px;
  color: var(--oa-text-muted, #909399);
}

.nl-result {
  padding: 16px;
  border-radius: 8px;
  border: 1px solid var(--oa-border-light, #e4e7ed);
  background: var(--oa-bg-gray, #f5f7fa);
}

.nl-result--data {
  border-color: #b3d8ff;
  background: #ecf5ff;
}

.nl-result--action {
  border-color: #c3e6cb;
  background: #f0f9eb;
}

.nl-result__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--oa-text-primary, #303133);
  margin-bottom: 8px;
}

.nl-result__content {
  font-size: 14px;
  color: var(--oa-text-secondary, #606266);
  line-height: 1.6;
}

.fade-enter-active, .fade-leave-active {
  transition: all 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
