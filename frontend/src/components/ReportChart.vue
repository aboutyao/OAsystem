<script setup lang="ts">
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
  option: echarts.EChartsOption
  height?: string
  autoResize?: boolean
}>()

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  chart.setOption(props.option)
}

function handleResize() {
  chart?.resize()
}

onMounted(() => {
  initChart()
  if (props.autoResize !== false) {
    window.addEventListener('resize', handleResize)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})

watch(() => props.option, (newOption) => {
  chart?.setOption(newOption, true)
}, { deep: true })
</script>

<template>
  <div ref="chartRef" :style="{ width: '100%', height: height || '320px' }" />
</template>
