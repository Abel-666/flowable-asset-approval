<template>
  <el-card>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <span style="font-size: 16px; font-weight: bold;">Flowable 已部署流程定义列表</span>
      <el-button type="primary" @click="router.push('/process')">去设计新流程</el-button>
    </div>

    <el-table :data="list" border stripe>
      <el-table-column prop="id" label="流程定义 ID" min-width="220" />
      <el-table-column prop="key" label="流程标识 (Key)" width="180" />
      <el-table-column prop="name" label="流程名称" min-width="160" />
      <el-table-column prop="version" label="版本号" width="90">
        <template #default="{ row }">
          <el-tag type="success">v{{ row.version }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deploymentId" label="部署 ID" min-width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openPreview(row.id)">查看流程图</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="previewVisible" append-to-body title="流程图预览" width="850px" @opened="renderPreview">
      <div ref="viewerRef" style="height: 360px; border: 1px solid #e4e7ed; border-radius: 6px;"></div>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer'
import api from '../api'

const router = useRouter()
const list = ref([])
const previewVisible = ref(false)
const viewerRef = ref(null)
let viewer = null
let currentDefId = null

onMounted(async () => {
  list.value = await api.getProcessList()
})

const openPreview = (id) => {
  currentDefId = id
  previewVisible.value = true
}

const renderPreview = async () => {
  if (viewer) viewer.destroy()
  viewer = new BpmnViewer({ container: viewerRef.value })
  const xml = await api.getProcessXml(currentDefId)
  await viewer.importXML(xml)
  viewer.get('canvas').zoom('fit-viewport')
}
</script>
