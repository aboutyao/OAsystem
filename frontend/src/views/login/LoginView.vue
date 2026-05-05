<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  remember: false,
})

async function submit() {
  loading.value = true
  try {
    await authStore.signIn(form.username, form.password)
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <!-- Colored top bar -->
      <div class="login-panel__topbar" />

      <!-- Brand area with logo -->
      <div class="login-panel__brand">
        <div class="login-panel__logo">
          <svg
            width="48"
            height="48"
            viewBox="0 0 48 48"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true"
          >
            <!-- Shield / document shape -->
            <path
              d="M24 4L6 12v12c0 11.11 7.67 21.48 18 24 10.33-2.52 18-12.89 18-24V12L24 4z"
              fill="var(--oa-primary)"
              opacity="0.12"
            />
            <path
              d="M24 4L6 12v12c0 11.11 7.67 21.48 18 24 10.33-2.52 18-12.89 18-24V12L24 4z"
              stroke="var(--oa-primary)"
              stroke-width="2"
              stroke-linejoin="round"
              fill="none"
            />
            <!-- OA text inside shield -->
            <text
              x="24"
              y="30"
              text-anchor="middle"
              fill="var(--oa-primary)"
              font-size="16"
              font-weight="800"
              font-family="Inter, system-ui, sans-serif"
            >
              OA
            </text>
          </svg>
        </div>
        <h1 class="login-panel__title">企业级 OA 系统</h1>
        <p class="login-panel__subtitle">统一组织、权限、流程、规则、消息、审计与业务协同</p>
      </div>

      <!-- Form body -->
      <div class="login-panel__body">
        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="账号">
            <el-input
              v-model="form.username"
              placeholder="请输入账号"
              autocomplete="username"
              size="large"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              show-password
              size="large"
            />
          </el-form-item>
          <div class="login-panel__actions">
            <el-checkbox v-model="form.remember">记住我</el-checkbox>
          </div>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-panel__button"
            @click="submit"
          >
            登录
          </el-button>
        </el-form>
      </div>

      <!-- Footer with copyright -->
      <div class="login-panel__footer">
        <span class="login-panel__copyright">&copy; 2026 企业级 OA 系统</span>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-panel__topbar {
  height: 4px;
  background: var(--oa-primary);
}

.login-panel__logo {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.login-panel__title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--oa-text-primary);
}

.login-panel__subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--oa-text-secondary);
  line-height: 1.5;
}

.login-panel__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.login-panel__copyright {
  font-size: 12px;
  color: var(--oa-text-muted);
}
</style>
