import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', meta: { group: '首页', title: '数据看板' }, component: () => import('../views/Dashboard.vue') },
      { path: 'assets', meta: { group: '资产管理', title: '固定资产台账' }, component: () => import('../views/AssetList.vue') },
      { path: 'repair-scrap', meta: { group: '资产管理', title: '维修与报废台账' }, component: () => import('../views/RepairScrap.vue') },
      { path: 'inventory', meta: { group: '资产管理', title: '资产盘点记录' }, component: () => import('../views/InventoryPage.vue') },
      { path: 'apply', meta: { group: '审批与流程', title: '我的资产申请' }, component: () => import('../views/ApplyPage.vue') },
      { path: 'todo', meta: { group: '审批与流程', title: '待办审批任务' }, component: () => import('../views/TodoApprove.vue') },
      { path: 'done', meta: { group: '审批与流程', title: '已办审批历史' }, component: () => import('../views/DoneApprove.vue') },
      { path: 'process-list', meta: { group: '审批与流程', title: '流程定义列表' }, component: () => import('../views/ProcessList.vue') },
      { path: 'process', meta: { group: '审批与流程', title: 'BPMN 流程设计器' }, component: () => import('../views/ProcessDesign.vue') },
      { path: 'users', meta: { group: '系统管理', title: '用户管理' }, component: () => import('../views/UserManage.vue') },
      { path: 'depts', meta: { group: '系统管理', title: '部门管理' }, component: () => import('../views/DeptManage.vue') },
      { path: 'logs', meta: { group: '系统管理', title: '系统操作日志' }, component: () => import('../views/LogManage.vue') },
      { path: 'profile', meta: { group: '个人设置', title: '个人中心' }, component: () => import('../views/Profile.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const user = localStorage.getItem('user')
  if (to.path !== '/login' && !user) {
    next('/login')
  } else {
    next()
  }
})

export default router
