import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动携带服务端颁发的 Token
request.interceptors.request.use((config) => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  if (user.token) {
    config.headers.Authorization = `Bearer ${user.token}`
  }
  return config
})

// 响应拦截器：统一处理 401 未登录与业务错误
request.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data.code !== 200) {
      ElMessage.error(data.msg || '请求失败')
      return Promise.reject(new Error(data.msg))
    }
    return data.data
  },
  (err) => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('user')
      ElMessage.warning('登录已过期，请重新登录')
      window.location.href = '/login'
    } else {
      const msg = err.response?.data?.msg || err.response?.data?.message || '请求失败，请检查输入或后端日志'
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

export default {
  // 登录与用户部门
  login: (data) => request.post('/auth/login', data),
  logout: () => request.post('/auth/logout'),
  forgotPwd: (data) => request.post('/auth/forgot-pwd', data),
  getPwdResets: () => request.get('/auth/pwd-resets'),
  approvePwdReset: (id) => request.post(`/auth/pwd-resets/${id}/approve`),
  rejectPwdReset: (id) => request.post(`/auth/pwd-resets/${id}/reject`),
  getUsers: () => request.get('/auth/users'),
  addUser: (data) => request.post('/auth/users', data),
  updateUser: (data) => request.put('/auth/users', data),
  deleteUser: (id) => request.delete(`/auth/users/${id}`),
  resetPwd: (id) => request.put(`/auth/users/${id}/reset-pwd`),
  updateProfile: (data) => request.put('/auth/profile', data),
  updatePassword: (data) => request.put('/auth/password', data),
  getDepts: () => request.get('/auth/depts'),
  addDept: (data) => request.post('/auth/depts', data),
  updateDept: (data) => request.put('/auth/depts', data),
  deleteDept: (id) => request.delete(`/auth/depts/${id}`),

  // 首页看板与只读审计日志
  getDashboardStats: () => request.get('/dashboard/stats'),
  getLogPage: (params) => request.get('/log/page', { params }),

  // 资产台账
  getAssets: (params) => request.get('/asset/list', { params }),
  getAssetPage: (params) => request.get('/asset/page', { params }),
  addAsset: (data) => request.post('/asset/add', data),
  updateAsset: (data) => request.put('/asset/update', data),
  deleteAsset: (id) => request.delete(`/asset/${id}`),
  batchDeleteAssets: (ids) => request.post('/asset/batch-delete', ids),
  returnAsset: (id) => request.put(`/asset/return/${id}`),
  repairAsset: (id) => request.put(`/asset/repair/${id}`),

  // 申请与 Flowable 审批
  submitApply: (data) => request.post('/apply/submit', data),
  resubmitApply: (data) => request.put('/apply/resubmit', data),
  deleteApply: (id) => request.delete(`/apply/${id}`),
  getApplyList: (params) => request.get('/apply/list', { params }),
  getApplyPage: (params) => request.get('/apply/page', { params }),
  getTodoList: () => request.get('/apply/todo'),
  getDoneList: () => request.get('/apply/done'),
  approveTask: (data) => request.post('/apply/approve', data),
  rejectTask: (data) => request.post('/apply/reject', data),
  getApplyHistory: (piId) => request.get(`/apply/history/${piId}`),

  // Flowable 流程图与高亮
  getProcessList: () => request.get('/process/list'),
  getProcessXml: (id) => request.get('/process/xml', { params: { id } }),
  getProcessHighlight: (piId) => request.get(`/process/highlight/${piId}`),
  deployProcess: (data) => request.post('/process/deploy', data),

  // 资产盘点
  getInventoryList: (params) => request.get('/inventory/list', { params }),
  addInventory: (data) => request.post('/inventory/add', data),
  updateInventory: (data) => request.put('/inventory/update', data),
  deleteInventory: (id) => request.delete(`/inventory/${id}`)
}
