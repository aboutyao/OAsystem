<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

interface Props {
  /** Data items */
  items: unknown[]
  /** Item height in pixels */
  itemHeight: number
  /** Buffer items above and below viewport */
  buffer?: number
  /** Container height */
  height?: number | string
}

const props = withDefaults(defineProps<Props>(), {
  buffer: 5,
  height: '100%',
})

const emit = defineEmits<{
  'scroll-to-bottom': []
}>()

const containerRef = ref<HTMLElement | null>(null)
const scrollTop = ref(0)
const containerHeight = ref(0)

const totalHeight = computed(() => props.items.length * props.itemHeight)

const startIndex = computed(() => {
  const index = Math.floor(scrollTop.value / props.itemHeight) - props.buffer
  return Math.max(0, index)
})

const endIndex = computed(() => {
  const visibleCount = Math.ceil(containerHeight.value / props.itemHeight)
  const index = startIndex.value + visibleCount + props.buffer * 2
  return Math.min(props.items.length, index)
})

const visibleItems = computed(() => {
  return props.items.slice(startIndex.value, endIndex.value).map((item, index) => ({
    item,
    index: startIndex.value + index,
  }))
})

const offsetY = computed(() => startIndex.value * props.itemHeight)

function handleScroll(e: Event) {
  const target = e.target as HTMLElement
  scrollTop.value = target.scrollTop

  // Check if scrolled to bottom
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - 10) {
    emit('scroll-to-bottom')
  }
}

function updateContainerHeight() {
  if (containerRef.value) {
    containerHeight.value = containerRef.value.clientHeight
  }
}

onMounted(() => {
  updateContainerHeight()
  window.addEventListener('resize', updateContainerHeight)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateContainerHeight)
})

watch(() => props.height, updateContainerHeight)

defineExpose({
  scrollTo: (index: number) => {
    if (containerRef.value) {
      containerRef.value.scrollTop = index * props.itemHeight
    }
  },
  scrollToTop: () => {
    if (containerRef.value) {
      containerRef.value.scrollTop = 0
    }
  },
})
</script>

<template>
  <div
    ref="containerRef"
    class="virtual-list"
    :style="{ height: typeof height === 'number' ? `${height}px` : height }"
    @scroll="handleScroll"
  >
    <div class="virtual-list__phantom" :style="{ height: `${totalHeight}px` }">
      <div class="virtual-list__content" :style="{ transform: `translateY(${offsetY}px)` }">
        <div
          v-for="{ item, index } in visibleItems"
          :key="index"
          class="virtual-list__item"
          :style="{ height: `${itemHeight}px` }"
        >
          <slot :item="item" :index="index" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.virtual-list {
  overflow-y: auto;
  position: relative;
}

.virtual-list__phantom {
  position: relative;
  width: 100%;
}

.virtual-list__content {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
}

.virtual-list__item {
  box-sizing: border-box;
}
</style>
