<template>
  <el-card>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <div style="display: flex; gap: 10px;">
        <el-input v-model="keyword" placeholder="搜盘点单号或任务名称" clearable style="width: 220px;" @keyup.enter="loadData" />
        <el-select v-model="resultFilter" placeholder="盘点结果" clearable style="width: 130px;" @change="loadData">
          <el-option label="账实相符" value="NORMAL" />
          <el-option label="存在差异" value="ABNORMAL" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="keyword = ''; resultFilter = ''; loadData()">重置</el-button>
      </div>
      <el-button type="primary" @click="openAdd">+ 新增盘点</el-button>
    </div>

    <el-table :data="list" border stripe>
      <el-table-column prop="inventoryNo" label="盘点单号" width="155" />
      <el-table-column prop="title" label="盘点任务名称" min-width="160" />
      <el-table-column prop="checkerName" label="盘点人" width="120" />
      <el-table-column prop="expectedCount" label="账面应盘(台)" width="120" />
      <el-table-column prop="actualCount" label="实际盘点(台)" width="120" />
      <el-table-column label="盘点结果" width="110">
        <template #default="{ row }">
          <el-tag :type="row.result === 'NORMAL' ? 'success' : 'danger'">
            {{ row.result === 'NORMAL' ? '账实相符' : '存在差异' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注说明" min-width="140" />
      <el-table-column prop="createTime" label="盘点时间" width="170" />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" append-to-body :title="form.id ? '编辑盘点记录' : '新增资产盘点'" width="420px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="盘点任务名称"><el-input v-model="form.title" placeholder="例如：2026年11月月度盘点" /></el-form-item>
        <el-form-item label="实盘资产数量"><el-input-number v-model="form.actualCount" :min="0" /></el-form-item>
        <el-form-item label="盘点备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const user = JSON.parse(localStorage.getItem('user') || '{}')
const list = ref([])
const keyword = ref('')
const resultFilter = ref('')
const dialogVisible = ref(false)
const form = reactive({ id: null, title: '', expectedCount: 0, actualCount: 5, remark: '' })

const loadData = async () => {
  list.value = await api.getInventoryList({ keyword: keyword.value, result: resultFilter.value })
}

const openAdd = () => {
  Object.assign(form, { id: null, title: '', expectedCount: 0, actualCount: 5, remark: '' })
  dialogVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const submit = async () => {
  if (form.id) {
    await api.updateInventory(form)
    ElMessage.success('盘点记录已更新')
  } else {
    await api.addInventory({ ...form, checkerId: user.id, checkerName: user.realName })
    ElMessage.success('盘点记录已保存')
  }
  dialogVisible.value = false
  loadData()
}

const remove = async (id) => {
  await ElMessageBox.confirm('确定删除该盘点记录吗？', '提示', { type: 'warning' })
  await api.deleteInventory(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
