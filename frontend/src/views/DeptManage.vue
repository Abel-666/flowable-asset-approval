<template>
  <el-card>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <span style="font-size: 16px; font-weight: bold;">部门组织架构管理</span>
      <el-button type="primary" @click="openAdd">+ 新增部门</el-button>
    </div>

    <el-table :data="depts" border stripe>
      <el-table-column prop="id" label="部门 ID" width="90" />
      <el-table-column prop="deptName" label="部门名称" min-width="160" />
      <el-table-column prop="leaderName" label="部门负责人" min-width="150" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" append-to-body :title="form.id ? '编辑部门' : '新增部门'" width="400px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="部门名称"><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leaderName" /></el-form-item>
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

const depts = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, deptName: '', leaderName: '' })

const loadData = async () => {
  depts.value = await api.getDepts()
}

const openAdd = () => {
  Object.assign(form, { id: null, deptName: '', leaderName: '' })
  dialogVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const submit = async () => {
  if (form.id) {
    await api.updateDept(form)
  } else {
    await api.addDept(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const remove = async (id) => {
  await ElMessageBox.confirm('确定删除该部门吗？', '提示', { type: 'warning' })
  await api.deleteDept(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
