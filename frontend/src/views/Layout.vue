<template>
  <el-container class="layout-container">
    <!-- 悬浮液态玻璃侧边栏 -->
    <el-aside :width="isCollapse ? '72px' : '236px'" class="aside">
      <div class="logo">
        <div class="logo-badge">
          <el-icon size="18" color="#fff"><Box /></el-icon>
        </div>
        <span v-if="!isCollapse" class="logo-text">固定资产审批</span>
      </div>

      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        :default-openeds="['asset', 'workflow', 'system']"
        router
        class="glass-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>首页数据看板</template>
        </el-menu-item>

        <el-sub-menu index="asset">
          <template #title>
            <el-icon><Monitor /></el-icon>
            <span>资产管理中心</span>
          </template>
          <el-menu-item index="/assets">
            <el-icon><List /></el-icon><span>固定资产台账</span>
          </el-menu-item>
          <el-menu-item index="/repair-scrap">
            <el-icon><Tools /></el-icon><span>维修与报废台账</span>
          </el-menu-item>
          <el-menu-item v-if="user.role === 'ASSET_ADMIN'" index="/inventory">
            <el-icon><Checked /></el-icon><span>资产盘点记录</span>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="workflow">
          <template #title>
            <el-icon><Connection /></el-icon>
            <span>审批与流程中心</span>
          </template>
          <el-menu-item index="/apply">
            <el-icon><DocumentAdd /></el-icon><span>我的资产申请</span>
          </el-menu-item>
          <el-menu-item index="/todo">
            <el-icon><Bell /></el-icon><span>待办审批任务</span>
          </el-menu-item>
          <el-menu-item index="/done">
            <el-icon><DocumentChecked /></el-icon><span>已办审批历史</span>
          </el-menu-item>
          <el-menu-item v-if="user.role === 'ASSET_ADMIN'" index="/process-list">
            <el-icon><Share /></el-icon><span>流程定义列表</span>
          </el-menu-item>
          <el-menu-item v-if="user.role === 'ASSET_ADMIN'" index="/process">
            <el-icon><EditPen /></el-icon><span>BPMN 流程设计器</span>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="user.role === 'ASSET_ADMIN'" index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/users">
            <el-icon><User /></el-icon><span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/depts">
            <el-icon><OfficeBuilding /></el-icon><span>部门管理</span>
          </el-menu-item>
          <el-menu-item index="/logs">
            <el-icon><Tickets /></el-icon><span>系统操作日志</span>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/profile">
          <el-icon><UserFilled /></el-icon>
          <template #title>个人中心</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="right-shell">
      <!-- 悬浮液态玻璃顶栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" size="18" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ route.meta.group || '首页' }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title || '数据看板' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tag size="small" type="info" effect="plain" round style="margin-right: 10px;">{{ user.deptName }}</el-tag>
          <span style="font-weight: 600;">{{ user.realName }}</span>
          <el-tag size="small" round style="margin-left: 8px;">{{ user.roleName }}</el-tag>
          <el-button type="danger" plain size="small" style="margin-left: 14px;" @click="logout">
            退出
          </el-button>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))

const logout = async () => {
  try {
    await api.logout()
  } catch (e) {}
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  padding: 12px;
  gap: 14px;
  box-sizing: border-box;
}
.aside {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.05), inset 0 1px 0 rgba(255, 255, 255, 0.95);
  transition: width 0.22s ease;
  overflow-x: hidden;
  padding: 0 8px 12px;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  margin-bottom: 10px;
}
.logo-badge {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: linear-gradient(135deg, #0071e3, #3b82f6);
  box-shadow: 0 4px 12px rgba(0, 113, 227, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-text {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}
.glass-menu {
  border-right: none !important;
  background: transparent !important;
}
:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  border-radius: 11px;
  margin-bottom: 3px;
  height: 42px;
  line-height: 42px;
  color: #334155;
  transition: all 0.18s ease;
}
:deep(.el-menu-item.is-active) {
  background: rgba(0, 113, 227, 0.12) !important;
  color: #0071e3 !important;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(0, 113, 227, 0.2);
}
:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.65) !important;
}
.right-shell {
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
}
.header {
  height: 58px;
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(22px) saturate(180%);
  -webkit-backdrop-filter: blur(22px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 16px;
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.04), inset 0 1px 0 rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.collapse-btn {
  cursor: pointer;
  color: #475569;
  padding: 6px;
  border-radius: 8px;
}
.collapse-btn:hover {
  background: rgba(0, 113, 227, 0.1);
  color: #0071e3;
}
.header-right {
  display: flex;
  align-items: center;
  font-size: 14px;
}
.main {
  padding: 2px;
  overflow-y: auto;
}
</style>
