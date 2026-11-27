<template>
  <el-card>
    <div style="font-size: 16px; font-weight: bold; margin-bottom: 16px;">维修与报废资产台账</div>
    <el-table :data="list" border stripe>
      <el-table-column prop="assetCode" label="资产编码" width="160" />
      <el-table-column prop="assetName" label="资产名称" min-width="150" />
      <el-table-column prop="category" label="分类" width="110" />
      <el-table-column prop="spec" label="规格型号" width="140" />
      <el-table-column prop="price" label="原值(元)" width="110" />
      <el-table-column label="当前状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 4 ? 'warning' : 'info'">
            {{ row.status === 4 ? '维修中' : '已报废' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注说明" min-width="160" />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 4" type="success" size="small" link @click="finishRepair(row.id)">
            修好恢复在库
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const list = ref([])

const loadData = async () => {
  const all = await api.getAssets()
  list.value = all.filter((a) => a.status === 4 || a.status === 5)
}

const finishRepair = async (id) => {
  await api.repairAsset(id)
  ElMessage.success('设备已修好并恢复为在库状态')
  loadData()
}

onMounted(loadData)
</script>
