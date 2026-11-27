<template>
  <el-card>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <div style="display: flex; gap: 10px; align-items: center;">
        <el-input v-model="keyword" placeholder="搜单号或资产名称" clearable style="width: 210px;" />
        <el-select v-model="filterStatus" placeholder="审批状态" clearable style="width: 130px;">
          <el-option label="审批中" value="PENDING" />
          <el-option label="已办结" value="FINISHED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-button @click="keyword = ''; filterStatus = ''">重置</el-button>
      </div>
      <el-button type="primary" @click="openNew">+ 发起新申请</el-button>
    </div>

    <el-table :data="filteredList" border stripe>
      <el-table-column prop="applyNo" label="申请单号" width="155" />
      <el-table-column label="申请类型" width="95">
        <template #default="{ row }">
          <el-tag>{{ typeText(row.applyType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="assetName" label="资产名称" min-width="140" />
      <el-table-column prop="quantity" label="数量" width="65" />
      <el-table-column prop="amount" label="金额(元)" width="100" />
      <el-table-column prop="reason" label="申请原因" min-width="140" />
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" width="165" />
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.processInstanceId" type="primary" link @click="progressRef.open(row.processInstanceId)">
            进度图
          </el-button>
          <el-button v-if="row.status === 'REJECTED'" type="warning" link @click="openResubmit(row)">
            修改重提
          </el-button>
          <el-button v-if="row.status !== 'FINISHED'" type="danger" link @click="removeApply(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 发起 / 修改重提弹窗 -->
    <el-dialog v-model="dialogVisible" append-to-body :title="form.id ? '修改并重新提交申请' : '发起固定资产业务申请'" width="480px">
      <el-form :model="form" label-width="95px">
        <el-form-item label="申请类型">
          <el-radio-group v-model="form.applyType" :disabled="!!form.id" @change="onTypeChange">
            <el-radio-button value="PURCHASE">采购申购</el-radio-button>
            <el-radio-button value="USE">领用</el-radio-button>
            <el-radio-button value="BORROW">借用</el-radio-button>
            <el-radio-button value="REPAIR">报修</el-radio-button>
            <el-radio-button value="SCRAP">报废</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.applyType !== 'PURCHASE'" label="选择资产">
          <el-select v-model="form.assetId" placeholder="请选择现有资产" style="width: 100%;" @change="onPickAsset">
            <el-option
              v-for="a in selectableAssets"
              :key="a.id"
              :label="`${a.assetCode} - ${a.assetName}${a.status === 2 ? ' (占有人:' + a.userName + ')' : ''}`"
              :value="a.id"
            />
          </el-select>
        </el-form-item>

        <template v-else>
          <el-form-item label="资产名称"><el-input v-model="form.assetName" placeholder="例如：联想工作站" /></el-form-item>
          <el-form-item label="资产分类">
            <el-select v-model="form.category" style="width: 100%;">
              <el-option label="办公设备" value="办公设备" />
              <el-option label="服务器" value="服务器" />
              <el-option label="测试仪器" value="测试仪器" />
              <el-option label="网络设备" value="网络设备" />
            </el-select>
          </el-form-item>
          <el-form-item label="规格型号"><el-input v-model="form.spec" placeholder="例如：i7 / 32G / 1T" /></el-form-item>
          <el-form-item label="申购数量"><el-input-number v-model="form.quantity" :min="1" /></el-form-item>
        </template>

        <el-form-item label="预计金额(元)">
          <el-input-number v-model="form.amount" :min="0" style="width: 100%;" />
          <div style="font-size: 12px; color: #909399;">提示：金额 ≥ 5000 元会自动加签财务主管审批</div>
        </el-form-item>
        <el-form-item label="申请原因"><el-input v-model="form.reason" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">{{ form.id ? '重新提交审批' : '提交并启动流程' }}</el-button>
      </template>
    </el-dialog>

    <BpmnHighlightDialog ref="progressRef" />
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import BpmnHighlightDialog from '../components/BpmnHighlightDialog.vue'

const user = JSON.parse(localStorage.getItem('user') || '{}')
const list = ref([])
const allAssets = ref([])
const keyword = ref('')
const filterStatus = ref('')
const dialogVisible = ref(false)
const progressRef = ref(null)

const form = reactive({
  id: null,
  applyType: 'PURCHASE',
  assetId: null,
  assetName: '',
  category: '办公设备',
  spec: '',
  quantity: 1,
  amount: 6000,
  reason: ''
})

const typeText = (t) => ({ PURCHASE: '采购申购', USE: '领用', BORROW: '借用', REPAIR: '设备报修', SCRAP: '报废' }[t] || t)
const statusText = (s) => ({ PENDING: '审批中', FINISHED: '已办结', REJECTED: '已驳回' }[s] || s)
const statusTag = (s) => ({ PENDING: 'warning', FINISHED: 'success', REJECTED: 'danger' }[s] || 'info')

const filteredList = computed(() => {
  return list.value.filter((item) => {
    const matchKw = !keyword.value || item.applyNo.includes(keyword.value) || item.assetName.includes(keyword.value)
    const matchStatus = !filterStatus.value || item.status === filterStatus.value
    return matchKw && matchStatus
  })
})

const selectableAssets = computed(() => {
  if (form.applyType === 'SCRAP') {
    return allAssets.value.filter((a) => a.status !== 5)
  }
  if (form.applyType === 'REPAIR') {
    return allAssets.value.filter((a) => a.status !== 4 && a.status !== 5)
  }
  if (form.applyType === 'BORROW') {
    return allAssets.value.filter((a) => a.status === 1 || (a.status === 2 && a.userId !== user.id))
  }
  return allAssets.value.filter((a) => a.status === 1)
})

const loadData = async () => {
  const params = user.role === 'EMPLOYEE' ? { applicantId: user.id } : {}
  list.value = await api.getApplyList(params)
  allAssets.value = await api.getAssets()
}

const openNew = () => {
  Object.assign(form, {
    id: null,
    applyType: 'PURCHASE',
    assetId: null,
    assetName: '',
    category: '办公设备',
    spec: '',
    quantity: 1,
    amount: 6000,
    reason: ''
  })
  dialogVisible.value = true
}

const openResubmit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const onTypeChange = () => {
  form.assetId = null
  form.assetName = ''
  form.amount = 0
}

const onPickAsset = (id) => {
  const item = allAssets.value.find((a) => a.id === id)
  if (item) {
    form.assetName = item.assetName
    form.amount = Number(item.price || 0)
  }
}

const submit = async () => {
  if (form.id) {
    await api.resubmitApply(form)
    ElMessage.success('已重新提交并启动新审批流程')
  } else {
    await api.submitApply({
      ...form,
      applicantId: user.id,
      applicantName: user.realName,
      deptId: user.deptId
    })
    ElMessage.success('申请已提交，Flowable 流程已启动')
  }
  dialogVisible.value = false
  loadData()
}

const removeApply = async (id) => {
  await ElMessageBox.confirm('确定删除该申请记录吗？', '提示', { type: 'warning' })
  await api.deleteApply(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
