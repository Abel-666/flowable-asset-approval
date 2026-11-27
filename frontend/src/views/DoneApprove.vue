<template>
  <el-card>
    <div style="font-size: 16px; font-weight: bold; margin-bottom: 16px;">已办审批历史记录</div>
    <el-table :data="list" border stripe>
      <el-table-column prop="applyNo" label="申请单号" width="160" />
      <el-table-column prop="applicantName" label="申请人" width="120" />
      <el-table-column label="申请类型" width="100">
        <template #default="{ row }">
          <el-tag>{{ typeText(row.applyType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="assetName" label="资产名称" min-width="150" />
      <el-table-column prop="amount" label="金额(元)" width="110" />
      <el-table-column label="最终结果" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'FINISHED' ? 'success' : 'danger'">
            {{ row.status === 'FINISHED' ? '已办结' : '已驳回' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="办结时间" width="170" />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.processInstanceId" type="primary" link @click="progressRef.open(row.processInstanceId)">
            查看流程图
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <BpmnHighlightDialog ref="progressRef" />
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../api'
import BpmnHighlightDialog from '../components/BpmnHighlightDialog.vue'

const list = ref([])
const progressRef = ref(null)
const typeText = (t) => ({ PURCHASE: '采购申购', USE: '领用', BORROW: '借用', SCRAP: '报废' }[t] || t)

onMounted(async () => {
  list.value = await api.getDoneList()
})
</script>
