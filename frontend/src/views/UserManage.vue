<template>
  <el-card>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <span style="font-size: 16px; font-weight: bold;">系统用户管理</span>
      <el-button type="primary" @click="openAdd">+ 新增用户</el-button>
    </div>

    <el-table :data="users" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="登录账号" width="130" />
      <el-table-column prop="realName" label="真实姓名" width="140" />
      <el-table-column label="所属部门" width="130">
        <template #default="{ row }">{{ deptName(row.deptId) }}</template>
      </el-table-column>
      <el-table-column prop="roleName" label="角色" width="120" />
      <el-table-column prop="role" label="Flowable组标识" min-width="150" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="warning" link @click="doResetPwd(row.id)">重置密码</el-button>
          <el-button type="danger" link @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" append-to-body :title="form.id ? '编辑用户' : '新增用户'" width="420px">
      <el-form :model="form" label-width="85px">
        <el-form-item label="登录账号"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="form.deptId" style="width: 100%;">
            <el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户角色">
          <el-select v-model="form.role" style="width: 100%;" @change="syncRoleName">
            <el-option label="普通员工" value="EMPLOYEE" />
            <el-option label="部门主管" value="DEPT_MANAGER" />
            <el-option label="资产管理员" value="ASSET_ADMIN" />
            <el-option label="财务主管" value="FINANCE_MANAGER" />
          </el-select>
        </el-form-item>
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

const users = ref([])
const depts = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, username: '', realName: '', deptId: 1, role: 'EMPLOYEE', roleName: '普通员工' })

const roleMap = {
  EMPLOYEE: '普通员工',
  DEPT_MANAGER: '部门主管',
  ASSET_ADMIN: '资产管理员',
  FINANCE_MANAGER: '财务主管'
}

const syncRoleName = (val) => {
  form.roleName = roleMap[val] || '普通员工'
}

const deptName = (id) => depts.value.find((d) => d.id === id)?.deptName || '未分配'

const loadData = async () => {
  users.value = await api.getUsers()
  depts.value = await api.getDepts()
}

const openAdd = () => {
  Object.assign(form, { id: null, username: '', realName: '', deptId: 1, role: 'EMPLOYEE', roleName: '普通员工' })
  dialogVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const submit = async () => {
  if (form.id) {
    await api.updateUser(form)
    ElMessage.success('修改成功')
  } else {
    await api.addUser(form)
    ElMessage.success('新增成功（默认密码 123456）')
  }
  dialogVisible.value = false
  loadData()
}

const doResetPwd = async (id) => {
  await api.resetPwd(id)
  ElMessage.success('密码已重置为 123456')
}

const remove = async (id) => {
  await ElMessageBox.confirm('确定删除该用户吗？', '提示', { type: 'warning' })
  await api.deleteUser(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
