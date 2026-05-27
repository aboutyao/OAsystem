<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { assetSummary } from '../../api/reports'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const data = ref<JsonObject | null>(null)

async function load() {
  loading.value = true
  try {
    data.value = await assetSummary()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">资产统计</h2>
      </div>
      <div class="oa-page__actions">
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <template v-if="data">
      <div class="report-cards">
        <el-card shadow="never"><div class="metric"><div class="metric__label">总数</div><div class="metric__value">{{ data.assetCount }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">闲置</div><div class="metric__value">{{ data.idleCount }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">在用</div><div class="metric__value">{{ data.inUseCount }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">维修中</div><div class="metric__value">{{ data.repairingCount }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">已报废</div><div class="metric__value">{{ data.scrappedCount }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">采购总价</div><div class="metric__value">¥{{ data.totalPurchasePrice }}</div></div></el-card>
      </div>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>按类别分布</template>
        <el-table :data="data.byCategory as JsonObject[]" stripe>
          <el-table-column prop="category" label="类别" min-width="160" />
          <el-table-column prop="count" label="数量" width="120" />
          <el-table-column prop="price" label="采购总价" width="160" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

