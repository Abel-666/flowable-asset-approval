<template>
  <el-card>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
      <div>
        <span style="font-size: 16px; font-weight: bold;">BPMN 2.0 在线流程设计器</span>
        <span style="font-size: 13px; color: #6b7280; margin-left: 12px;">可拖动调整节点，点击右侧按钮直接部署到 Flowable 引擎</span>
      </div>
      <el-button type="primary" @click="deploy">部署发布新流程</el-button>
    </div>
    <div ref="modelerRef" style="height: 560px; border: 1px solid #e4e7ed; border-radius: 6px;"></div>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import { ElMessage } from 'element-plus'
import api from '../api'

const modelerRef = ref(null)
let modeler = null

onMounted(async () => {
  modeler = new BpmnModeler({ container: modelerRef.value })
  const xml = await api.getProcessXml()
  await modeler.importXML(xml)
  modeler.get('canvas').zoom('fit-viewport')
})

const deploy = async () => {
  const { xml } = await modeler.saveXML({ format: true })
  await api.deployProcess({ name: '固定资产审批流程（在线修改版）', xml })
  ElMessage.success('新流程已成功部署到 Flowable')
}
</script>
