<template>
  <div class="bento-grid">
    <!-- 第一行：左宽右窄 Bento 网格 -->
    <el-row :gutter="16">
      <!-- 左侧大便当盒：欢迎横幅 + 4项核心指标 -->
      <el-col :span="14">
        <el-card class="bento-hero">
          <div class="hero-top">
            <div>
              <div class="hero-badge">Flowable 7.0 · BPMN 2.0 引擎运行中</div>
              <h2 class="hero-title">你好，{{ user.realName }}</h2>
              <p class="hero-sub">企业固定资产全生命周期审批与可视化管控平台</p>
            </div>
            <div class="hero-actions">
              <el-button type="primary" @click="router.push('/apply')">
                <el-icon style="margin-right: 4px;"><DocumentAdd /></el-icon>发起申请
              </el-button>
              <el-button @click="router.push('/assets')">
                <el-icon style="margin-right: 4px;"><List /></el-icon>资产台账
              </el-button>
              <el-button v-if="user.role !== 'EMPLOYEE'" type="warning" plain @click="router.push('/todo')">
                <el-icon style="margin-right: 4px;"><Bell /></el-icon>待办审批
              </el-button>
            </div>
          </div>

          <el-row :gutter="12" style="margin-top: 18px;">
            <el-col :span="6">
              <div class="kpi-tile">
                <div class="kpi-label">固定资产总数</div>
                <div class="kpi-value">{{ stats.totalCount }} <span class="unit">台</span></div>
              </div>
            </el-col>
            <el-col :span="6">
              <div v-if="stats.canViewAmount" class="kpi-tile">
                <div class="kpi-label">公司资产总价值</div>
                <div class="kpi-value" style="color: #0071e3;">¥{{ stats.totalAmount }}</div>
                <div style="font-size: 11px; color: #64748b; margin-top: 2px;">本人名下：{{ stats.myAssetCount }} 台 · ¥{{ stats.myAssetAmount }}</div>
              </div>
              <div v-else class="kpi-tile" style="cursor: pointer;" @click="router.push('/assets')">
                <div class="kpi-label">我的名下设备</div>
                <div class="kpi-value" style="color: #0071e3;">{{ stats.myAssetCount }} <span class="unit">台</span></div>
                <div style="font-size: 11px; color: #64748b; margin-top: 2px;">名下资产价值：¥{{ stats.myAssetAmount }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="kpi-tile">
                <div class="kpi-label">在库闲置</div>
                <div class="kpi-value" style="color: #16a34a;">{{ stats.inStockCount }} <span class="unit">台</span></div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="kpi-tile">
                <div class="kpi-label">审批中单据</div>
                <div class="kpi-value" style="color: #f97316;">{{ stats.pendingCount }} <span class="unit">单</span></div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <!-- 右侧便当盒：最新审批动态 -->
      <el-col :span="10">
        <el-card class="bento-recent">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span style="font-weight: 600;">最新审批动态</span>
              <el-button type="primary" link size="small" @click="router.push('/apply')">查看全部</el-button>
            </div>
          </template>
          <div v-if="recentApplies.length" class="recent-list">
            <div v-for="item in recentApplies" :key="item.id" class="recent-item">
              <div>
                <div style="font-weight: 600; font-size: 13px;">{{ item.assetName }}</div>
                <div style="font-size: 12px; color: #64748b; margin-top: 2px;">
                  {{ item.applicantName }} · {{ typeText(item.applyType) }} · ¥{{ item.amount }}
                </div>
              </div>
              <el-tag size="small" round :type="statusTag(item.status)">{{ statusText(item.status) }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无审批动态，点击左侧“发起申请”体验" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行：左饼图右柱状图 Bento 网格 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="10">
        <el-card>
          <template #header><span style="font-weight: 600;">资产状态分布</span></template>
          <div ref="pieRef" style="height: 310px;"></div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card>
          <template #header><span style="font-weight: 600;">各部门占用资产数量</span></template>
          <div ref="barRef" style="height: 310px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import api from '../api'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const stats = ref({ totalCount: 0, canViewAmount: false, totalAmount: 0, myAssetCount: 0, myAssetAmount: 0, inStockCount: 0, pendingCount: 0 })
const recentApplies = ref([])
const pieRef = ref(null)
const barRef = ref(null)

const typeText = (t) => ({ PURCHASE: '采购', USE: '领用', BORROW: '借用', SCRAP: '报废' }[t] || t)
const statusText = (s) => ({ PENDING: '审批中', FINISHED: '已办结', REJECTED: '已驳回' }[s] || s)
const statusTag = (s) => ({ PENDING: 'warning', FINISHED: 'success', REJECTED: 'danger' }[s] || 'info')

onMounted(async () => {
  const [data, applies] = await Promise.all([
    api.getDashboardStats(),
    api.getApplyList({})
  ])
  stats.value = data
  recentApplies.value = (applies || []).slice(0, 4)

  const pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    color: ['#34c759', '#0071e3', '#ff9500', '#ff3b30', '#94a3b8'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['46%', '72%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      data: data.statusChart
    }]
  })

  const barChart = echarts.init(barRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: data.deptNames },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      barWidth: 42,
      data: data.deptValues,
      itemStyle: {
        borderRadius: [8, 8, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#0071e3' },
          { offset: 1, color: '#60a5fa' }
        ])
      }
    }]
  })
})
</script>

<style scoped>
.bento-hero {
  height: 100%;
}
.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 12px;
}
.hero-badge {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  color: #0071e3;
  background: rgba(0, 113, 227, 0.1);
  padding: 4px 10px;
  border-radius: 20px;
  margin-bottom: 8px;
}
.hero-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}
.hero-sub {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
}
.hero-actions {
  display: flex;
  gap: 8px;
}
.kpi-tile {
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 14px;
  padding: 14px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
}
.kpi-label {
  font-size: 12px;
  color: #64748b;
}
.kpi-value {
  font-size: 22px;
  font-weight: 700;
  margin-top: 6px;
  color: #0f172a;
}
.unit {
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
}
.bento-recent {
  height: 100%;
}
.recent-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.recent-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.8);
}
</style>
