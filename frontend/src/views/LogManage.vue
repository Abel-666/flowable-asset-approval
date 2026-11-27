<template>
  <el-card>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <div style="display: flex; gap: 10px; align-items: center;">
        <el-input v-model="keyword" placeholder="搜操作人、模块或详情" clearable style="width: 240px;" @keyup.enter="loadData" />
        <el-button type="primary" @click="page = 1; loadData()">查询</el-button>
        <el-button @click="keyword = ''; page = 1; loadData()">重置</el-button>
      </div>
      <el-tag type="success" effect="plain" round>只读审计日志（禁止删除与篡改）</el-tag>
    </div>

    <el-table :data="list" border stripe>
      <el-table-column prop="id" label="日志ID" width="80" />
      <el-table-column prop="userId" label="用户ID" width="85" />
      <el-table-column prop="operator" label="操作人" width="130" />
      <el-table-column prop="module" label="所属模块" width="115" />
      <el-table-column label="操作类型" width="115">
        <template #default="{ row }">
          <el-tag size="small" :type="row.action.includes('驳回') ? 'danger' : 'primary'">{{ row.action }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="操作详情" min-width="260" />
      <el-table-column prop="createTime" label="操作时间" width="175" />
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
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../api'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')

const loadData = async () => {
  const res = await api.getLogPage({ page: page.value, size: size.value, keyword: keyword.value })
  list.value = res.records || []
  total.value = res.total || 0
}

onMounted(loadData)
</script>
