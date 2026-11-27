<template>
  <el-card>
    <div class="toolbar">
      <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
        <el-radio-group v-model="onlyMine" @change="page = 1; loadData()">
          <el-radio-button :value="false">全部资产</el-radio-button>
          <el-radio-button :value="true">我的名下设备</el-radio-button>
        </el-radio-group>
        <el-input v-model="keyword" placeholder="搜资产名称或编码" clearable style="width: 190px;" @keyup.enter="page = 1; loadData()" />
        <el-select v-model="status" placeholder="资产状态" clearable style="width: 120px;" @change="page = 1; loadData()">
          <el-option label="在库闲置" :value="1" />
          <el-option label="在用" :value="2" />
          <el-option label="借出" :value="3" />
          <el-option label="维修中" :value="4" />
          <el-option label="已报废" :value="5" />
        </el-select>
        <el-button type="primary" @click="page = 1; loadData()">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
      <div style="display: flex; gap: 10px;">
        <el-button v-if="user.role === 'ASSET_ADMIN' && selectedIds.length" type="danger" plain @click="doBatchDelete">
          批量删除 ({{ selectedIds.length }})
        </el-button>
        <el-button type="warning" plain @click="exportCsv">导出 CSV</el-button>
        <el-button v-if="user.role === 'ASSET_ADMIN'" type="success" @click="openAdd">+ 新增入库</el-button>
      </div>
    </div>

    <el-table :data="list" border stripe style="margin-top: 16px;" @selection-change="onSelect">
      <el-table-column type="selection" width="44" />
      <el-table-column prop="assetCode" label="资产编码" width="165" />
      <el-table-column prop="assetName" label="资产名称" min-width="135" />
      <el-table-column prop="category" label="分类" width="90" />
      <el-table-column prop="price" label="单价(元)" width="95" />
      <el-table-column label="状态" width="95">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="当前使用人" width="115">
        <template #default="{ row }">
          <el-tag v-if="row.userId === user.id" type="primary" effect="plain" size="small">我 ({{ row.userName }})</el-tag>
          <span v-else>{{ row.userName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="deptName" label="所属部门" width="105" />
      <el-table-column prop="remark" label="备注" min-width="120" />
      <el-table-column label="操作" width="290" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openBarcode(row)">条码</el-button>
          <el-button
            v-if="row.status === 2 && row.userId !== user.id"
            size="small"
            type="warning"
            link
            @click="quickBorrowFromPeer(row)"
          >向TA借用</el-button>
          <el-button
            v-if="(row.status === 1 || row.status === 2 || row.status === 3) && (row.userId === user.id || user.role === 'ASSET_ADMIN')"
            size="small"
            type="danger"
            link
            @click="quickRepairApply(row)"
          >申请报修</el-button>
          <el-button v-if="(row.status === 2 || row.status === 3) && row.userId === user.id" size="small" type="success" link @click="doReturn(row.id)">归还</el-button>
          <el-button
            v-if="(row.status === 2 || row.status === 3) && row.userId !== user.id && (user.role === 'ASSET_ADMIN' || user.role === 'DEPT_MANAGER')"
            size="small"
            type="warning"
            link
            @click="doRemind(row)"
          >催还</el-button>
          <el-button v-if="user.role === 'ASSET_ADMIN'" size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button v-if="user.role === 'ASSET_ADMIN'" size="small" type="danger" link @click="doDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>

    <!-- 新增/编辑资产弹窗 -->
    <el-dialog v-model="addVisible" append-to-body :title="form.id ? '编辑资产信息' : '登记新资产入库'" width="460px">
      <el-form :model="form" label-width="85px">
        <el-form-item label="资产名称"><el-input v-model="form.assetName" /></el-form-item>
        <el-form-item label="资产分类">
          <el-select v-model="form.category" style="width: 100%;">
            <el-option label="办公设备" value="办公设备" />
            <el-option label="服务器" value="服务器" />
            <el-option label="测试仪器" value="测试仪器" />
            <el-option label="网络设备" value="网络设备" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格型号"><el-input v-model="form.spec" /></el-form-item>
        <el-form-item label="单价(元)"><el-input-number v-model="form.price" :min="0" style="width: 100%;" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 条形码与二维码标签弹窗 -->
    <el-dialog v-model="codeVisible" append-to-body title="固定资产电子标签" width="460px" @opened="renderCodes">
      <div class="label-box">
        <div style="font-size: 16px; font-weight: bold; margin-bottom: 6px;">{{ currentAsset.assetName }}</div>
        <div style="font-size: 13px; color: #666; margin-bottom: 12px;">规格：{{ currentAsset.spec || '无' }}</div>
        <svg ref="barcodeRef" style="max-width: 100%; height: auto;"></svg>
        <div style="margin-top: 12px;">
          <canvas ref="qrRef"></canvas>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="printLabel">打印标签</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import JsBarcode from 'jsbarcode'
import QRCode from 'qrcode'
import api from '../api'

const user = JSON.parse(localStorage.getItem('user') || '{}')
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const onlyMine = ref(false)
const keyword = ref('')
const status = ref(null)
const selectedIds = ref([])
const addVisible = ref(false)
const codeVisible = ref(false)
const currentAsset = ref({})
const barcodeRef = ref(null)
const qrRef = ref(null)

const form = reactive({ id: null, assetName: '', category: '办公设备', spec: '', price: 3000, deptId: 2, remark: '' })

const statusText = (s) => ({ 1: '在库闲置', 2: '在用', 3: '借出', 4: '维修中', 5: '已报废' }[s] || '未知')
const statusTag = (s) => ({ 1: 'success', 2: 'primary', 3: 'warning', 4: 'danger', 5: 'info' }[s] || 'info')

const loadData = async () => {
  const res = await api.getAssetPage({
    page: page.value,
    size: size.value,
    status: status.value,
    keyword: keyword.value,
    onlyMine: onlyMine.value
  })
  list.value = res.records || []
  total.value = res.total || 0
}

const resetQuery = () => {
  keyword.value = ''
  status.value = null
  onlyMine.value = false
  page.value = 1
  loadData()
}

const quickRepairApply = async (row) => {
  const { value } = await ElMessageBox.prompt(`请填写「${row.assetName}」的故障原因（提交后由资产管理员审核）`, '发起设备报修申请', {
    confirmButtonText: '提交报修',
    cancelButtonText: '取消',
    inputValue: '设备故障需要维修'
  })
  await api.submitApply({
    applyType: 'REPAIR',
    assetId: row.id,
    assetName: row.assetName,
    amount: row.price,
    quantity: 1,
    reason: value || '设备故障需要维修'
  })
  ElMessage.success('报修申请已提交，等待资产管理员审核')
}

const quickBorrowFromPeer = async (row) => {
  const tip = Number(row.price) > 1000
    ? `该设备当前由「${row.userName}」使用，且价值超过 1000 元（需本人同意 + 主管审批）`
    : `该设备当前由「${row.userName}」使用，价值不超过 1000 元（只需本人同意即可借用）`
  const { value } = await ElMessageBox.prompt(tip, `向 ${row.userName} 借用「${row.assetName}」`, {
    confirmButtonText: '发送借用申请',
    cancelButtonText: '取消',
    inputValue: '项目临时急用，用完归还'
  })
  await api.submitApply({
    applyType: 'BORROW',
    assetId: row.id,
    assetName: row.assetName,
    amount: row.price,
    quantity: 1,
    reason: value || '临时借用'
  })
  ElMessage.success(`借用申请已发送给 ${row.userName}`)
}

const onSelect = (rows) => {
  selectedIds.value = rows.map((r) => r.id)
}

const doBatchDelete = async () => {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selectedIds.value.length} 条资产记录吗？`, '提示', { type: 'warning' })
  await api.batchDeleteAssets(selectedIds.value)
  ElMessage.success('批量删除成功')
  loadData()
}

const openAdd = () => {
  Object.assign(form, { id: null, assetName: '', category: '办公设备', spec: '', price: 3000, deptId: 2, remark: '' })
  addVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  addVisible.value = true
}

const submitForm = async () => {
  if (form.id) {
    await api.updateAsset(form)
    ElMessage.success('修改成功')
  } else {
    await api.addAsset(form)
    ElMessage.success('资产入库成功，已自动生成编码')
  }
  addVisible.value = false
  loadData()
}

const doDelete = async (id) => {
  await ElMessageBox.confirm('确定删除该资产记录吗？', '提示', { type: 'warning' })
  await api.deleteAsset(id)
  ElMessage.success('删除成功')
  loadData()
}

const doReturn = async (id) => {
  await api.returnAsset(id)
  ElMessage.success('已归还入库')
  loadData()
}

const doRemind = (row) => {
  ElMessage.success(`已向「${row.userName}」发送归还「${row.assetName}」的通知提醒`)
}

const exportCsv = () => {
  const header = '资产编码,资产名称,分类,单价,状态,当前使用人,所属部门,备注\n'
  const rows = list.value.map((r) => `${r.assetCode},${r.assetName},${r.category},${r.price},${statusText(r.status)},${r.userName || '-'},${r.deptName || '-'},${r.remark || ''}`).join('\n')
  const blob = new Blob(['\ufeff' + header + rows], { type: 'text/csv;charset=utf-8;' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '固定资产台账.csv'
  a.click()
}

const openBarcode = (row) => {
  currentAsset.value = row
  codeVisible.value = true
}

const renderCodes = () => {
  if (barcodeRef.value) {
    JsBarcode(barcodeRef.value, currentAsset.value.assetCode, { format: 'CODE128', width: 1.5, height: 55, displayValue: true })
  }
  if (qrRef.value) {
    const content = `编码:${currentAsset.value.assetCode} | 名称:${currentAsset.value.assetName}`
    QRCode.toCanvas(qrRef.value, content, { width: 130 })
  }
}

const printLabel = () => window.print()

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.label-box {
  text-align: center;
  padding: 16px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
}
</style>
