<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { leaveSummary } from '../../../api/reports'
import type { JsonObject } from '../../../api/types'

const loading = ref(false)
const data = ref<JsonObject | null>(null)
const filter = reactive({ from: '', to: '' })

async function load() {
  loading.value = true
  try {
    data.value = await leaveSummary({
      from: filter.from || undefined,
      to: filter.to || undefined,
    })
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
        <h2 class="oa-page__title">请假报表</h2>
        <p class="muted">请假数据统计与分析。</p>
      </div>
    </div>

    <el-card shadow="never" style="margin-bottom: 12px">
      <el-form inline>
        <el-form-item label="起始">
          <el-date-picker v-model="filter.from" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束">
          <el-date-picker v-model="filter.to" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <template v-if="data">
      <div class="report-cards">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric__label">请假总数</div>
            <div class="metric__value">{{ data.totalCount ?? 0 }}</div>
          </div>
        </el-card>
        <el-card shadow="never">
          <div class="metric">
            <div class="metric__label">已批准</div>
            <div class="metric__value">{{ data.approvedCount ?? 0 }}</div>
          </div>
        </el-card>
        <el-card shadow="never">
          <div class="metric">
            <div class="metric__label">已驳回</div>
            <div class="metric__value">{{ data.rejectedCount ?? 0 }}</div>
          </div>
        </el-card>
        <el-card shadow="never">
          <div class="metric">
            <div class="metric__label">已批准总天数</div>
            <div class="metric__value">{{ data.totalDays ?? 0 }}</div>
          </div>
        </el-card>
      </div>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>按请假类型分布</template>
        <el-table :data="(data.byLeaveType as JsonObject[]) ?? []" stripe>
          <el-table-column prop="leaveType" label="类型" min-width="160" />
          <el-table-column prop="count" label="次数" width="120" />
          <el-table-column prop="totalDays" label="总天数" width="120" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

