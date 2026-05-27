<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { setupTwoFactor, enableTwoFactor, disableTwoFactor } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const step = ref<'initial' | 'verify' | 'done' | 'disable'>('initial')
const secret = ref('')
const qrCodeImage = ref('')
const verificationCode = ref('')
const is2FAEnabled = ref(false)

onMounted(async () => {
  await checkTwoFactorStatus()
})

async function checkTwoFactorStatus() {
  // Check if 2FA is already enabled by looking at user data
  // For now, we'll always show setup
  try {
    const result = await setupTwoFactor()
    secret.value = result.secret
    qrCodeImage.value = `data:image/png;base64,${result.qrCode}`
    step.value = 'verify'
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '获取配置信息失败')
  }
}

async function verifyAndEnable() {
  if (!verificationCode.value || verificationCode.value.length !== 6) {
    ElMessage.warning('请输入6位验证码')
    return
  }

  loading.value = true
  try {
    await enableTwoFactor(secret.value, verificationCode.value)
    is2FAEnabled.value = true
    step.value = 'done'
    ElMessage.success('二步验证已启用')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '验证失败')
  } finally {
    loading.value = false
  }
}

async function handleDisable() {
  if (!verificationCode.value || verificationCode.value.length !== 6) {
    ElMessage.warning('请输入6位验证码')
    return
  }

  loading.value = true
  try {
    await disableTwoFactor(verificationCode.value)
    is2FAEnabled.value = false
    step.value = 'initial'
    verificationCode.value = ''
    ElMessage.success('二步验证已禁用')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '禁用失败')
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/dashboard')
}
</script>

<template>
  <div class="twofa-setup-page">
    <div class="twofa-setup-card">
      <!-- Step: Initial / Enable -->
      <div v-if="step === 'verify'" class="twofa-setup-content">
        <h2>设置二步验证</h2>
        <p class="twofa-setup-description">
          二步验证为您的账户增加一层额外的安全保护。启用后，登录时除了密码外，还需要输入身份验证器应用中的验证码。
        </p>

        <div class="twofa-setup-steps">
          <div class="twofa-step">
            <div class="twofa-step-number">1</div>
            <div class="twofa-step-content">
              <h4>安装身份验证器应用</h4>
              <p>在手机上安装 Google Authenticator、Microsoft Authenticator 或其他 TOTP 身份验证器应用。</p>
            </div>
          </div>

          <div class="twofa-step">
            <div class="twofa-step-number">2</div>
            <div class="twofa-step-content">
              <h4>扫描二维码</h4>
              <p>使用身份验证器应用扫描下方的二维码：</p>
              <div v-if="qrCodeImage" class="twofa-qr-code">
                <img :src="qrCodeImage" alt="2FA QR Code" />
              </div>
              <div v-else class="twofa-qr-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>正在生成二维码...</span>
              </div>
              <div class="twofa-secret">
                <p>如果无法扫描二维码，可以手动输入密钥：</p>
                <code>{{ secret }}</code>
              </div>
            </div>
          </div>

          <div class="twofa-step">
            <div class="twofa-step-number">3</div>
            <div class="twofa-step-content">
              <h4>输入验证码</h4>
              <p>输入身份验证器应用中显示的6位验证码：</p>
              <el-input
                v-model="verificationCode"
                placeholder="请输入6位验证码"
                maxlength="6"
                size="large"
                class="twofa-code-input"
              />
            </div>
          </div>
        </div>

        <div class="twofa-actions">
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            :disabled="!verificationCode || verificationCode.length !== 6"
            @click="verifyAndEnable"
          >
            启用二步验证
          </el-button>
          <el-button size="large" @click="goBack">取消</el-button>
        </div>
      </div>

      <!-- Step: Done -->
      <div v-else-if="step === 'done'" class="twofa-setup-content twofa-success">
        <el-icon :size="64" color="#67c23a"><CircleCheck /></el-icon>
        <h2>二步验证已启用</h2>
        <p>您的账户现在受到二步验证的保护。下次登录时，除了密码外，您还需要输入身份验证器应用中的验证码。</p>
        <p class="twofa-warning">
          <strong>重要提示：</strong>请确保妥善保管您的身份验证器应用。如果丢失手机，您将无法登录账户。
        </p>
        <el-button type="primary" size="large" @click="goBack">返回工作台</el-button>
      </div>

      <!-- Step: Disable -->
      <div v-else-if="step === 'disable'" class="twofa-setup-content">
        <h2>禁用二步验证</h2>
        <p class="twofa-setup-description">
          请输入当前的二步验证码以确认禁用操作。
        </p>
        <el-input
          v-model="verificationCode"
          placeholder="请输入6位验证码"
          maxlength="6"
          size="large"
          class="twofa-code-input"
        />
        <div class="twofa-actions">
          <el-button
            type="danger"
            size="large"
            :loading="loading"
            :disabled="!verificationCode || verificationCode.length !== 6"
            @click="handleDisable"
          >
            禁用二步验证
          </el-button>
          <el-button size="large" @click="step = 'initial'">取消</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.twofa-setup-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--oa-bg-page, #f5f7fa);
}

.twofa-setup-card {
  width: 100%;
  max-width: 520px;
  background: var(--oa-bg-card, #fff);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  padding: 32px;
}

.twofa-setup-content h2 {
  margin: 0 0 16px;
  font-size: 22px;
  font-weight: 600;
  color: var(--oa-text-primary);
  text-align: center;
}

.twofa-setup-description {
  margin: 0 0 24px;
  font-size: 14px;
  color: var(--oa-text-secondary);
  text-align: center;
  line-height: 1.6;
}

.twofa-setup-steps {
  margin-bottom: 24px;
}

.twofa-step {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.twofa-step-number {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--oa-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.twofa-step-content h4 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--oa-text-primary);
}

.twofa-step-content p {
  margin: 0;
  font-size: 13px;
  color: var(--oa-text-secondary);
  line-height: 1.6;
}

.twofa-qr-code {
  margin: 16px 0;
  text-align: center;
}

.twofa-qr-code img {
  width: 180px;
  height: 180px;
  border: 1px solid var(--oa-border, #e4e7ed);
  border-radius: 8px;
}

.twofa-qr-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: var(--oa-text-secondary);
}

.twofa-secret {
  margin-top: 12px;
}

.twofa-secret p {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--oa-text-muted);
}

.twofa-secret code {
  display: block;
  padding: 8px 12px;
  background: var(--oa-fill-tertiary, #f5f7fa);
  border-radius: 6px;
  font-family: ui-monospace, monospace;
  font-size: 13px;
  color: var(--oa-primary);
  word-break: break-all;
}

.twofa-code-input {
  margin-top: 12px;
}

.twofa-code-input :deep(.el-input__inner) {
  font-size: 18px;
  letter-spacing: 4px;
  text-align: center;
}

.twofa-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
}

.twofa-success {
  text-align: center;
}

.twofa-success h2 {
  margin-top: 16px;
}

.twofa-warning {
  margin: 16px 0 24px;
  padding: 12px 16px;
  background: #fdf6ec;
  border: 1px solid #e6a23c;
  border-radius: 8px;
  font-size: 13px;
  color: #e6a23c;
  text-align: left;
}
</style>
