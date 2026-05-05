<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { updateUser } from '../../api/org'

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

function openEdit() {
  if (!user.value) return
  form.realName = user.value.realName ?? ''
  form.mobile = (user.value as Record<string, unknown>).mobile as string ?? ''
  form.email = (user.value as Record<string, unknown>).email as string ?? ''
  dialogVisible.value = true
}

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
