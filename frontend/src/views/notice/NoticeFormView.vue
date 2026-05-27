<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createNotice, getNotice, updateNotice } from '../../api/notices'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IEditorConfig } from '@wangeditor/editor'
import '@wangeditor/editor/dist/css/style.css'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)

const isEdit = computed(() => route.name === 'notice-edit')
const id = computed(() => (isEdit.value ? Number(route.params.id) : 0))

const form = reactive({
  title: '',
  content: '',
  category: 'GENERAL',
  publishScopeType: 'ALL',
  topFlag: 0,
  scheduledAt: null as Date | null,
})

const editorRef = shallowRef<IDomEditor>()
const toolbarConfig = {}
const editorConfig: Partial<IEditorConfig> = {
  placeholder: '请输入公告内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/notices/upload',
      fieldName: 'file',
      maxFileSize: 10 * 1024 * 1024,
      allowedFileTypes: ['image/*'],
      customInsert(res: { data: { url: string } }, insertFn: (url: string) => void) {
        insertFn(res.data.url, '', '')
      },
    },
    uploadVideo: {
      server: '/api/notices/upload',
      fieldName: 'file',
      maxFileSize: 50 * 1024 * 1024,
      allowedFileTypes: ['video/*'],
      customInsert(res: { data: { url: string } }, insertFn: (url: string) => void) {
        insertFn(res.data.url, '', '')
      },
    },
  },
}

function handleEditorCreated(editor: IDomEditor) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) {
    editor.destroy()
  }
})

onMounted(async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const row = await getNotice(id.value)
    if (String(row.status) !== 'DRAFT') {
      ElMessage.warning('仅草稿可编辑')
      router.replace(`/notices/${id.value}`)
      return
    }
    form.title = String(row.title ?? '')
    form.content = String(row.content ?? '')
    form.category = String(row.category ?? 'GENERAL')
    form.publishScopeType = String(row.publishScopeType ?? 'ALL')
    form.topFlag = Number(row.topFlag ?? 0)
    if (row.scheduledAt) {
      form.scheduledAt = new Date(String(row.scheduledAt))
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/notices')
  } finally {
    loading.value = false
  }
})

async function onSave() {
  const htmlContent = editorRef.value?.getHtml() ?? ''
  if (!form.title.trim() || !htmlContent.trim() || htmlContent === '<p><br></p>') {
    ElMessage.warning('请填写标题与正文')
    return
  }
  const body = {
    title: form.title.trim(),
    content: htmlContent,
    category: form.category,
    publishScopeType: form.publishScopeType,
    topFlag: form.topFlag,
    scheduledAt: form.scheduledAt || null,
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateNotice(id.value, body)
      ElMessage.success('已保存')
      router.push(`/notices/${id.value}`)
    } else {
      const created = await createNotice(body)
      ElMessage.success('已创建')
      router.push(`/notices/${Number(created.id)}`)
    }
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
        <h2 class="oa-page__title">{{ isEdit ? '编辑公告' : '新建公告' }}</h2>
        <p class="muted">保存为草稿后，在详情页发布。</p>
      </div>
      <el-button @click="router.push(isEdit ? `/notices/${id}` : '/notices')">取消</el-button>
    </div>

    <el-card shadow="never">
      <el-form label-width="100px" style="max-width: 720px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 200px">
            <el-option label="通用" value="GENERAL" />
            <el-option label="系统" value="SYSTEM" />
            <el-option label="人事" value="HR" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布范围">
          <el-select v-model="form.publishScopeType" style="width: 200px">
            <el-option label="全员" value="ALL" />
            <el-option label="按部门" value="DEPT" />
            <el-option label="按角色" value="ROLE" />
          </el-select>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.topFlag" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="定时发布">
          <el-date-picker
            v-model="form.scheduledAt"
            type="datetime"
            placeholder="留空则手动发布"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="(time: Date) => time.getTime() < Date.now() - 86400000"
          />
          <span class="muted" style="margin-left: 8px">留空则手动发布</span>
        </el-form-item>
        <el-form-item label="正文" required>
          <div style="border: 1px solid #dcdfe6; border-radius: 4px; width: 100%">
            <Toolbar
              style="border-bottom: 1px solid #dcdfe6"
              :editor="editorRef"
              :defaultConfig="toolbarConfig"
              mode="default"
            />
            <Editor
              style="height: 400px; overflow-y: hidden"
              v-model="form.content"
              :defaultConfig="editorConfig"
              mode="default"
              @onCreated="handleEditorCreated"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存草稿</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
