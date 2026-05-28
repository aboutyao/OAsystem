<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()

function handlePrint() {
  window.print()
}
</script>

<template>
  <div class="printable">
    <div class="print-actions no-print">
      <el-button type="primary" @click="handlePrint">打印</el-button>
    </div>

    <!-- Print header (only visible when printing) -->
    <div class="print-header print-only">
      <div class="print-logo">企业 OA 系统</div>
      <h1>{{ title }}</h1>
      <p v-if="subtitle">{{ subtitle }}</p>
      <div class="print-meta">
        <span>打印时间: {{ new Date().toLocaleString('zh-CN') }}</span>
      </div>
    </div>

    <slot />

    <!-- Print footer -->
    <div class="print-footer print-only">
      <div class="print-signatures">
        <div class="print-sig-line">
          <span>申请人签字: _______________</span>
          <span>日期: _______________</span>
        </div>
        <div class="print-sig-line">
          <span>审批人签字: _______________</span>
          <span>日期: _______________</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
/* Print-specific styles (global, not scoped) */
@media print {
  .no-print {
    display: none !important;
  }
  .print-only {
    display: block !important;
  }
  body {
    background: white !important;
    margin: 0;
    padding: 20mm;
  }
  .app-shell__aside,
  .app-shell__header {
    display: none !important;
  }
  .app-shell__main {
    margin: 0 !important;
    padding: 0 !important;
  }
  .el-card {
    border: 1px solid #ddd !important;
    box-shadow: none !important;
    break-inside: avoid;
  }
  .el-table {
    font-size: 12px;
  }
}

/* Screen: hide print-only elements */
.print-only {
  display: none;
}
</style>

<style scoped>
.print-header {
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #333;
}

.print-logo {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.print-header h1 {
  font-size: 22px;
  margin: 8px 0;
  color: #000;
}

.print-header p {
  font-size: 14px;
  color: #666;
}

.print-meta {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.print-footer {
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid #ddd;
}

.print-signatures {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.print-sig-line {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #333;
}

.print-actions {
  margin-bottom: 16px;
  text-align: right;
}
</style>
