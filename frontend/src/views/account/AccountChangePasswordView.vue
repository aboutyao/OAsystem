<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword } from '../../api/auth'

const submitting = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

async function onSubmit() {
  if (!form.oldPassword.trim()) {
    ElMessage.warning('请输入旧密码')
    return
  }
  if (!form.newPassword.trim()) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  submitting.value = true
  try {
    await changePassword(form.oldPassword.trim(), form.newPassword.trim())
    ElMessage.success('密码修改成功')
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '密码修改失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">修改密码</h2>
        <p class="muted">修改当前登录用户的密码。</p>
      </div>
    </div>

    <el-card shadow="never" style="max-width: 480px">
      <el-form label-width="96px">
        <el-form-item label="旧密码" required>
          <el-input v-model="form.oldPassword" type="password" show-password maxlength="128" />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="form.newPassword" type="password" show-password maxlength="128" />
        </el-form-item>
        <el-form-item label="确认新密码" required>
          <el-input v-model="form.confirmPassword" type="password" show-password maxlength="128" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">确认修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
