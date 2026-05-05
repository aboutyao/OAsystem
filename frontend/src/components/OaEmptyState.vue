<script setup lang="ts">
defineProps<{
  description?: string
  imageSize?: number
}>()

defineEmits<{
  create: []
}>()
</script>

<template>
  <div class="oa-empty-state">
    <el-empty :description="description ?? '暂无数据'" :image-size="imageSize ?? 120">
      <template #default>
        <slot />
      </template>
    </el-empty>
  </div>
</template>

<style scoped>
.oa-empty-state {
  padding: 40px 0;
  text-align: center;
}
</style>
