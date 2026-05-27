<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotificationSettings, updateNotificationSettings } from '../../api/messages'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const saving = ref(false)
const settings = ref<JsonObject>({
  enableEmail: true,
  enableSse: true,
  enableDnd: false,
  dndStart: null,
  dndEnd: null,
})

onMounted(async () => {
  loading.value = true
  try {
    settings.value = await getNotificationSettings()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
})

async function onSave() {
  saving.value = true
  try {
    await updateNotificationSettings({
      enableEmail: Boolean(settings.value.enableEmail),
      enableSse: Boolean(settings.value.enableSse),
      enableDnd: Boolean(settings.value.enableDnd),
      dndStart: settings.value.dndStart as string | null,
      dndEnd: settings.value.dndEnd as string | null,
    })
    ElMessage.success('已保存')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">消息推送设置</h2>
        <p class="muted">配置消息通知方式和免打扰时段。</p>
      </div>
    </div>

    <el-card shadow="never" style="max-width: 600px">
      <el-form label-width="120px">
        <el-form-item label="邮件通知">
          <el-switch v-model="settings.enableEmail" />
          <span class="muted" style="margin-left: 12px">审批结果通过邮件通知</span>
        </el-form-item>
        <el-form-item label="站内消息">
          <el-switch v-model="settings.enableSse" />
          <span class="muted" style="margin-left: 12px">实时推送站内消息</span>
        </el-form-item>
        <el-form-item label="免打扰">
          <el-switch v-model="settings.enableDnd" />
        </el-form-item>
        <template v-if="settings.enableDnd">
          <el-form-item label="免打扰时段">
            <el-time-picker
              v-model="settings.dndStart"
              placeholder="开始时间"
              format="HH:mm"
              value-format="HH:mm"
              style="width: 140px"
            />
            <span style="margin: 0 8px">至</span>
            <el-time-picker
              v-model="settings.dndEnd"
              placeholder="结束时间"
              format="HH:mm"
              value-format="HH:mm"
              style="width: 140px"
            />
          </el-form-item>
          <el-form-item>
            <span class="muted">免打扰时段内不推送实时消息</span>
          </el-form-item>
        </template>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存设置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
