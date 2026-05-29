<script setup lang="ts">
interface Props {
  /** 行数 */
  rows?: number
  /** 是否显示头像 */
  avatar?: boolean
  /** 是否显示标题 */
  title?: boolean
  /** 标题宽度 */
  titleWidth?: string
  /** 内容宽度比例 (0-1) */
  contentWidth?: number
  /** 动画 */
  animated?: boolean
  /** 是否显示操作按钮 */
  actions?: boolean
}

withDefaults(defineProps<Props>(), {
  rows: 3,
  avatar: false,
  title: true,
  titleWidth: '40%',
  contentWidth: 100,
  animated: true,
  actions: false,
})
</script>

<template>
  <div class="skeleton-loader" :class="{ 'skeleton-loader--animated': animated }">
    <!-- 头像 + 标题 + 内容 -->
    <div v-if="avatar || title" class="skeleton-loader__header">
      <el-skeleton-item v-if="avatar" variant="circle" style="width: 40px; height: 40px; flex-shrink: 0" />
      <div class="skeleton-loader__header-content">
        <el-skeleton-item v-if="title" variant="text" :style="{ width: titleWidth }" />
        <el-skeleton-item variant="text" style="width: 60%" />
      </div>
    </div>

    <!-- 内容行 -->
    <div class="skeleton-loader__content">
      <el-skeleton-item
        v-for="i in rows"
        :key="i"
        variant="text"
        :style="{ width: i === rows ? `${contentWidth * 0.6}%` : `${contentWidth}%` }"
      />
    </div>

    <!-- 操作按钮 -->
    <div v-if="actions" class="skeleton-loader__actions">
      <el-skeleton-item variant="button" style="width: 80px; height: 32px" />
      <el-skeleton-item variant="button" style="width: 80px; height: 32px" />
    </div>
  </div>
</template>

<style scoped>
.skeleton-loader {
  padding: 20px;
}

.skeleton-loader__header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.skeleton-loader__header-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-loader__content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.skeleton-loader__actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>
