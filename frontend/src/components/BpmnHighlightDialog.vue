<template>
  <el-dialog v-model="visible" append-to-body title="审批流程进度追踪" width="900px" @opened="renderDiagram">
    <div class="legend">
      <span class="dot done"></span> 已完成节点
      <span class="dot active" style="margin-left: 16px;"></span> 当前审批节点
    </div>
    <div ref="canvasRef" class="bpmn-canvas"></div>

    <h4 style="margin: 16px 0 8px;">审批流转记录</h4>
    <el-timeline v-if="history.length">
      <el-timeline-item
        v-for="(item, idx) in history"
        :key="idx"
        :type="item.endTime ? 'success' : 'warning'"
        :timestamp="item.endTime || '处理中'"
      >
        <strong>{{ item.taskName }}</strong> — {{ item.comment }}
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else description="暂无流转记录" :image-size="60" />
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer'
import api from '../api'

const visible = ref(false)
const canvasRef = ref(null)
const history = ref([])
let viewer = null
let currentPiId = null

const open = async (processInstanceId) => {
  currentPiId = processInstanceId
  visible.value = true
  history.value = await api.getApplyHistory(processInstanceId)
}

const renderDiagram = async () => {
  if (!currentPiId || !canvasRef.value) return
  if (viewer) {
    viewer.destroy()
  }
  viewer = new BpmnViewer({ container: canvasRef.value })
  const data = await api.getProcessHighlight(currentPiId)
  await viewer.importXML(data.xml)
  const canvas = viewer.get('canvas')
  canvas.zoom('fit-viewport')

  // 给已完成和正在进行的节点涂色
  ;(data.finishedIds || []).forEach((id) => canvas.addMarker(id, 'highlight-done'))
  ;(data.activeIds || []).forEach((id) => canvas.addMarker(id, 'highlight-active'))
}

defineExpose({ open })
</script>

<style scoped>
.bpmn-canvas {
  width: 100%;
  height: 300px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
}
.legend {
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}
.dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 3px;
  vertical-align: middle;
  margin-right: 4px;
}
.dot.done {
  background: #e8f8f0;
  border: 2px solid #10b981;
}
.dot.active {
  background: #fff7ed;
  border: 2px solid #f97316;
}
</style>
