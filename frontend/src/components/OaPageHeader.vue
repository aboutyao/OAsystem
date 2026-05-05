<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<template>
  <div class="oa-page__head">
    <div>
      <h2 class="oa-page__title">{{ title }}</h2>
      <p v-if="subtitle" class="muted">{{ subtitle }}</p>
    </div>
    <div class="oa-page__head-actions">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.muted {
  color: var(--oa-text-secondary);
  font-size: 13px;
  margin-top: 4px;
}
.oa-page__head-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
