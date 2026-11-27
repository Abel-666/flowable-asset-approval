<template>
  <div>
    <el-card>
      <div style="font-size: 16px; font-weight: bold; margin-bottom: 16px;">
        待我审批的资产任务（当前角色：{{ user.roleName }}）
      </div>

      <el-table :data="list" border stripe>
        <el-table-column prop="apply.applyNo" label="申请单号" width="160" />
        <el-table-column prop="taskName" label="当前审批节点" width="140" />
        <el-table-column prop="apply.applicantName" label="申请人" width="120" />
        <el-table-column prop="apply.applyType" label="类型" width="100" />
        <el-table-column prop="apply.assetName" label="资产名称" min-width="140" />
        <el-table-column prop="apply.amount" label="金额(元)" width="100" />
        <el-table-column prop="apply.reason" label="申请原因" min-width="150" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="openAction(row, 'approve')">同意</el-button>
            <el-button size="small" type="danger" @click="openAction(row, 'reject')">驳回</el-button>
            <el-button size="small" type="primary" link @click="viewProgress(row.apply.processInstanceId)">流程图</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="actionVisible" append-to-body :title="actionType === 'approve' ? '审批通过' : '审批驳回'" width="400px">
        <el-input v-model="comment" type="textarea" :rows="3" placeholder="请输入审批意见" />
        <template #footer>
          <el-button @click="actionVisible = false">取消</el-button>
          <el-button :type="actionType === 'approve' ? 'success' : 'danger'" @click="confirmAction">确认提交</el-button>
        </template>
      </el-dialog>

      <BpmnHighlightDialog ref="progressRef" />
    </el-card>

    <!-- 直系上级 / 管理员：忘记密码重置审批 -->
    <el-card v-if="user.role === 'DEPT_MANAGER' || user.role === 'ASSET_ADMIN'" style="margin-top: 18px;">
      <div style="font-size: 16px; font-weight: bold; margin-bottom: 14px;">
        直系上级审批：员工密码重置申请
      </div>
      <el-table :data="pwdResets" border stripe>
        <el-table-column prop="username" label="申请账号" width="140" />
        <el-table-column prop="realName" label="员工姓名" width="150" />
        <el-table-column label="申请内容" min-width="200">
          <template #default>申请将登录密码重置为初始密码 123456</template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="doApprovePwd(row.id)">同意重置</el-button>
            <el-button size="small" type="danger" @click="doRejectPwd(row.id)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import BpmnHighlightDialog from '../components/BpmnHighlightDialog.vue'

const user = JSON.parse(localStorage.getItem('user') || '{}')
const list = ref([])
const pwdResets = ref([])
const actionVisible = ref(false)
const actionType = ref('approve')
const comment = ref('同意')
const currentRow = ref(null)
const progressRef = ref(null)

const loadData = async () => {
  list.value = await api.getTodoList()
  if (user.role === 'DEPT_MANAGER' || user.role === 'ASSET_ADMIN') {
    pwdResets.value = await api.getPwdResets()
  }
}

const openAction = (row, type) => {
  currentRow.value = row
  actionType.value = type
  comment.value = type === 'approve' ? '同意' : '不符合要求，退回'
  actionVisible.value = true
}

const confirmAction = async () => {
  const payload = {
    taskId: currentRow.value.taskId,
    applyId: currentRow.value.apply.id,
    approverName: user.realName,
    comment: comment.value
  }
  if (actionType.value === 'approve') {
    await api.approveTask(payload)
    ElMessage.success('已审批通过')
  } else {
    await api.rejectTask(payload)
    ElMessage.warning('已驳回申请')
  }
  actionVisible.value = false
  loadData()
}

const doApprovePwd = async (id) => {
  await api.approvePwdReset(id)
  ElMessage.success('已同意，该员工密码已重置为 123456')
  loadData()
}

const doRejectPwd = async (id) => {
  await api.rejectPwdReset(id)
  ElMessage.warning('已驳回该密码重置申请')
  loadData()
}

const viewProgress = (piId) => {
  progressRef.value.open(piId)
}

onMounted(loadData)
</script>
