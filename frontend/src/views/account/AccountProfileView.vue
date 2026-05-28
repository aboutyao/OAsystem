<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { updateUser } from '../../api/org'
import { http } from '../../api/http'

const router = useRouter()
const auth = useAuthStore()

const user = computed(() => auth.user)

// --- Edit dialog ---
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({
  realName: '',
  mobile: '',
  email: '',
})

// --- Security info ---
const twoFactorEnabled = ref(false)
const notificationSettings = ref({ enableEmail: true, enableSse: true, enableDnd: false })

function openEdit() {
  if (!user.value) return
  form.realName = user.value.realName ?? ''
  form.mobile = (user.value as Record<string, unknown>).mobile as string ?? ''
  form.email = (user.value as Record<string, unknown>).email as string ?? ''
  dialogVisible.value = true
}

async function loadSecurityInfo() {
  try {
    const me = await http.get('/auth/me') as any
    twoFactorEnabled.value = me.totpEnabled === 1 || me.totpEnabled === true
  } catch {}
  try {
    const settings = await http.get('/messages/settings') as any
    notificationSettings.value = settings
  } catch {}
}

onMounted(loadSecurityInfo)

async function saveProfile() {
  if (!user.value) return
  if (!form.realName.trim()) {
    ElMessage.warning('姓名不能为空')
    return
  }
  saving.value = true
  try {
    await updateUser(user.value.id, {
      realName: form.realName,
      mobile: form.mobile || null,
      email: form.email || null,
      employeeNo: (user.value as Record<string, unknown>).employeeNo as string ?? '',
      mainDeptId: user.value.mainDeptId,
    })
    ElMessage.success('个人信息已更新')
    dialogVisible.value = false
    await auth.loadCurrentUser()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '更新失败')
  } finally {
    saving.value = false
  }
}

function goChangePassword() {
  router.push('/account/change-password')
}

function goTwoFactor() {
  router.push('/2fa/setup')
}

async function toggleNotificationEmail() {
  try {
    await http.put('/messages/settings', {
      ...notificationSettings.value,
      enableEmail: !notificationSettings.value.enableEmail,
    })
    notificationSettings.value.enableEmail = !notificationSettings.value.enableEmail
    ElMessage.success('已更新')
  } catch {}
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">个人信息</h2>
        <p class="muted">查看并编辑当前登录用户的基本信息。</p>
      </div>
      <div class="oa-page__actions">
        <el-button type="primary" @click="openEdit">编辑信息</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <template v-if="user">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">{{ user.realName }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
          <el-descriptions-item label="所属部门">{{ user.mainDeptName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ (user as Record<string, unknown>).mobile ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ (user as Record<string, unknown>).email ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag
              v-for="role in user.roles"
              :key="role"
              size="small"
              style="margin-right: 4px"
            >
              {{ role }}
            </el-tag>
            <span v-if="!user.roles || user.roles.length === 0">-</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="未获取到用户信息" />
    </el-card>

    <!-- Security Section -->
    <div class="profile-grid">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span>账户安全</span>
          </div>
        </template>
        <div class="security-item">
          <div class="security-item__info">
            <span class="security-item__label">登录密码</span>
            <span class="security-item__desc">定期修改密码以保障账户安全</span>
          </div>
          <el-button size="small" @click="goChangePassword">修改密码</el-button>
        </div>
        <div class="security-item">
          <div class="security-item__info">
            <span class="security-item__label">两步验证</span>
            <span class="security-item__desc">{{ twoFactorEnabled ? '已启用' : '未启用' }}</span>
          </div>
          <el-tag :type="twoFactorEnabled ? 'success' : 'info'" size="small">
            {{ twoFactorEnabled ? '已开启' : '未开启' }}
          </el-tag>
          <el-button size="small" link type="primary" @click="goTwoFactor" style="margin-left: 8px">
            {{ twoFactorEnabled ? '管理' : '开启' }}
          </el-button>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span>通知设置</span>
          </div>
        </template>
        <div class="security-item">
          <div class="security-item__info">
            <span class="security-item__label">邮件通知</span>
            <span class="security-item__desc">审批结果、催办等邮件提醒</span>
          </div>
          <el-switch
            :model-value="notificationSettings.enableEmail"
            @change="toggleNotificationEmail"
          />
        </div>
        <div class="security-item">
          <div class="security-item__info">
            <span class="security-item__label">站内通知</span>
            <span class="security-item__desc">系统内消息推送</span>
          </div>
          <el-tag type="success" size="small">已开启</el-tag>
        </div>
        <div class="security-item">
          <div class="security-item__info">
            <span class="security-item__label">免打扰</span>
            <span class="security-item__desc">开启后不接收通知</span>
          </div>
          <el-switch v-model="notificationSettings.enableDnd" />
        </div>
      </el-card>
    </div>

    <!-- Edit Profile Dialog -->
    <el-dialog v-model="dialogVisible" title="编辑个人信息" width="480px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.mobile" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.security-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--oa-border-light, #ebeef5);
}

.security-item:last-child {
  border-bottom: none;
}

.security-item__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.security-item__label {
  font-size: 14px;
  font-weight: 500;
  color: var(--oa-text-primary, #303133);
}

.security-item__desc {
  font-size: 12px;
  color: var(--oa-text-muted, #909399);
}
</style>
