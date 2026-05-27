<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { changePassword } from '../../api/auth'

const router = useRouter()
const authStore = useAuthStore()
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
  if (form.newPassword.length < 8) {
    ElMessage.warning('密码长度不能少于8位')
    return
  }

  submitting.value = true
  try {
    await changePassword(form.oldPassword.trim(), form.newPassword.trim())
    ElMessage.success('密码修改成功，请重新登录')
    await authStore.signOut()
    router.push('/login')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '密码修改失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="force-password-page">
    <section class="force-password-panel">
      <div class="force-password-panel__topbar" />

      <div class="force-password-panel__brand">
        <div class="force-password-panel__icon">
          <svg
            width="48"
            height="48"
            viewBox="0 0 48 48"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true"
          >
            <circle cx="24" cy="24" r="22" stroke="var(--oa-primary)" stroke-width="2" fill="var(--oa-primary)" opacity="0.1" />
            <path
              d="M24 14a6 6 0 0 0-6 6v4h-1a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V26a2 2 0 0 0-2-2h-1v-4a6 6 0 0 0-6-6zm-3 6a3 3 0 1 1 6 0v4h-6v-4zm3 14a2 2 0 1 1 0-4 2 2 0 0 1 0 4z"
              fill="var(--oa-primary)"
            />
          </svg>
        </div>
        <h1 class="force-password-panel__title">密码已过期</h1>
        <p class="force-password-panel__subtitle">您的密码已超过有效期，请修改密码后继续使用</p>
      </div>

      <div class="force-password-panel__body">
        <el-form label-position="top" @submit.prevent="onSubmit">
          <el-form-item label="当前密码" required>
            <el-input
              v-model="form.oldPassword"
              type="password"
              placeholder="请输入当前密码"
              autocomplete="current-password"
              show-password
              size="large"
            />
          </el-form-item>
          <el-form-item label="新密码" required>
            <el-input
              v-model="form.newPassword"
              type="password"
              placeholder="请输入新密码（至少8位，包含大小写字母、数字和特殊字符）"
              autocomplete="new-password"
              show-password
              size="large"
            />
          </el-form-item>
          <el-form-item label="确认新密码" required>
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              autocomplete="new-password"
              show-password
              size="large"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="submitting"
            class="force-password-panel__button"
            @click="onSubmit"
          >
            确认修改
          </el-button>
        </el-form>
      </div>
    </section>
  </main>
</template>

<style scoped>
.force-password-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: var(--oa-bg-page, #f5f5f5);
}

.force-password-panel {
  width: 100%;
  max-width: 420px;
  background: var(--oa-bg-card, #fff);
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.force-password-panel__topbar {
  height: 4px;
  background: var(--oa-primary);
}

.force-password-panel__brand {
  padding: 32px 32px 0;
  text-align: center;
}

.force-password-panel__icon {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.force-password-panel__title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--oa-text-primary);
}

.force-password-panel__subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--oa-text-secondary);
  line-height: 1.5;
}

.force-password-panel__body {
  padding: 24px 32px 32px;
}

.force-password-panel__button {
  width: 100%;
  margin-top: 8px;
}
</style>
