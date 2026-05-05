<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addLibraryFileVersion,
  createLibraryFile,
  createLibraryFolder,
  deleteLibraryFile,
  listLibraryDownloadLogs,
  listLibraryFiles,
  listLibraryFolders,
  moveLibraryFile,
} from '../../api/file-library'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const loading = ref(false)
const folders = ref<JsonObject[]>([])
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const selectedFolderId = ref<number | undefined>(undefined)
const keyword = ref('')
const downloadLogs = ref<JsonObject[]>([])

const folderOptions = computed(() => {
  const out: { id: number; name: string }[] = []
  const walk = (nodes: JsonObject[], prefix = '') => {
    for (const n of nodes) {
      const id = Number(n.id)
      const name = prefix + String(n.folderName ?? '')
      out.push({ id, name })
      const children = (n.children as JsonObject[] | undefined) ?? []
      walk(children, prefix + ' / ')
    }
  }
  walk(folders.value)
  return out
})

const newFolder = reactive({
  parentId: undefined as number | undefined,
  folderName: '',
})

const newFile = reactive({
  folderId: undefined as number | undefined,
  fileName: '',
  mimeType: '',
  fileSize: undefined as number | undefined,
})

async function loadFolders() {
  folders.value = await listLibraryFolders()
}

async function loadFiles() {
  loading.value = true
  try {
    const res = await listLibraryFiles(page.value, size.value, selectedFolderId.value, keyword.value || undefined)
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function reload() {
  await loadFolders()
  await loadFiles()
}

void reload()

async function onCreateFolder() {
  if (!newFolder.folderName.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  try {
    await createLibraryFolder({
      parentId: newFolder.parentId ?? null,
      folderName: newFolder.folderName.trim(),
    })
    newFolder.folderName = ''
    ElMessage.success('文件夹已创建')
    await loadFolders()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建文件夹失败')
  }
}

async function onCreateFile() {
  if (!newFile.fileName.trim()) {
    ElMessage.warning('请输入文件名')
    return
  }
  try {
    await createLibraryFile({
      folderId: newFile.folderId ?? null,
      fileName: newFile.fileName.trim(),
      mimeType: newFile.mimeType || null,
      fileSize: newFile.fileSize ?? null,
    })
    newFile.fileName = ''
    ElMessage.success('文件已创建')
    await loadFiles()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建文件失败')
  }
}

async function onMove(row: JsonObject) {
  let value = ''
  try {
    const result = await ElMessageBox.prompt('输入目标文件夹 ID', '移动文件', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    value = String(result.value ?? '')
  } catch {
    return
  }
  if (!value) return
  const folderId = Number(value)
  if (!Number.isFinite(folderId) || folderId <= 0) {
    ElMessage.warning('请输入有效文件夹 ID')
    return
  }
  try {
    await moveLibraryFile(Number(row.id), { folderId })
    ElMessage.success('已移动')
    await loadFiles()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '移动失败')
  }
}

async function onAddVersion(row: JsonObject) {
  let value = ''
  try {
    const result = await ElMessageBox.prompt('输入新版本文件名', '上传新版本', {
      inputValue: String(row.fileName ?? ''),
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    value = String(result.value ?? '')
  } catch {
    return
  }
  if (!value) return
  try {
    await addLibraryFileVersion(Number(row.id), { fileName: value })
    ElMessage.success('版本已更新')
    await loadFiles()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '更新版本失败')
  }
}

async function onDelete(row: JsonObject) {
  try {
    await ElMessageBox.confirm('确认删除该文件？', '删除文件', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteLibraryFile(Number(row.id))
    ElMessage.success('已删除')
    await loadFiles()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function onLogs(row: JsonObject) {
  downloadLogs.value = await listLibraryDownloadLogs(Number(row.id))
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">文件资料库</h2>
        <p class="muted">维护文件夹与资料，支持版本更新、移动、删除与下载日志查看。</p>
      </div>
    </div>

    <el-row :gutter="12">
      <el-col :span="8">
        <el-card shadow="never" style="margin-bottom: 12px">
          <template #header>新建文件夹</template>
          <el-form label-width="88px">
            <el-form-item label="上级">
              <el-select v-model="newFolder.parentId" clearable style="width: 100%">
                <el-option v-for="f in folderOptions" :key="f.id" :label="f.name" :value="f.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="名称">
              <el-input v-model="newFolder.folderName" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onCreateFolder">创建</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>文件夹树</template>
          <el-tree
            node-key="id"
            :data="folders"
            :props="{ label: 'folderName', children: 'children' }"
            @node-click="(n:any) => { selectedFolderId = Number(n.id); loadFiles() }"
          />
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="never" class="oa-page__filters">
          <el-form inline>
            <el-form-item label="关键字">
              <el-input v-model="keyword" placeholder="文件名" clearable @change="loadFiles" />
            </el-form-item>
            <el-form-item>
              <el-button @click="selectedFolderId = undefined; loadFiles()">全部</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" style="margin-bottom: 12px">
          <template #header>新增资料</template>
          <el-form inline>
            <el-form-item label="文件夹">
              <el-select v-model="newFile.folderId" clearable style="width: 220px">
                <el-option v-for="f in folderOptions" :key="f.id" :label="f.name" :value="f.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="文件名">
              <el-input v-model="newFile.fileName" style="width: 220px" />
            </el-form-item>
            <el-form-item label="MIME">
              <el-input v-model="newFile.mimeType" style="width: 140px" />
            </el-form-item>
            <el-form-item label="大小">
              <el-input-number v-model="newFile.fileSize" :min="0" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onCreateFile">创建</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <el-table v-loading="loading" :data="rows" stripe>
            <el-table-column prop="id" label="#" width="72" />
            <el-table-column prop="fileName" label="文件名" min-width="220" />
            <el-table-column prop="folderName" label="文件夹" width="160" />
            <el-table-column prop="fileSize" label="大小" width="100" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ formatDisplayDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260">
              <template #default="{ row }">
                <el-button link type="primary" @click="onAddVersion(row)">新版本</el-button>
                <el-button link @click="onMove(row)">移动</el-button>
                <el-button link type="warning" @click="onLogs(row)">下载日志</el-button>
                <el-button link type="danger" @click="onDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="oa-page__pager">
            <el-pagination
              layout="total, prev, pager, next"
              :total="total"
              v-model:current-page="page"
              v-model:page-size="size"
              @current-change="loadFiles"
              @size-change="loadFiles"
            />
          </div>
        </el-card>

        <el-card shadow="never" style="margin-top: 12px">
          <template #header>下载日志</template>
          <el-table :data="downloadLogs" stripe>
            <el-table-column prop="userName" label="下载人" width="120" />
            <el-table-column prop="ipAddress" label="IP" width="140" />
            <el-table-column label="时间" width="180">
              <template #default="{ row }">{{ formatDisplayDateTime(row.downloadedAt) }}</template>
            </el-table-column>
            <el-table-column prop="userAgent" label="User-Agent" min-width="260" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
