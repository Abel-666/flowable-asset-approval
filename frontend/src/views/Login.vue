<template>
  <div class="login-bg">
    <el-card class="login-card" shadow="always">
      <h2 class="title">企业固定资产审批系统</h2>
      <p class="subtitle">基于 Spring Boot + Flowable 工作流引擎</p>

      <el-form :model="form" label-position="top">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large" />
        </el-form-item>
        <el-form-item label="图形验证码">
          <div style="display: flex; gap: 10px; width: 100%; align-items: center;">
            <el-input
              v-model="form.code"
              placeholder="不区分大小写"
              size="large"
              style="flex: 1;"
              @keyup.enter="handleLogin"
            />
            <canvas
              ref="captchaRef"
              width="115"
              height="40"
              title="点击刷新验证码"
              style="cursor: pointer; border-radius: 6px; border: 1px solid #dcdfe6;"
              @click="drawCaptcha"
            ></canvas>
          </div>
        </el-form-item>

        <el-button type="primary" size="large" style="width: 100%; margin-top: 4px;" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
        <div style="text-align: right; margin-top: 8px;">
          <el-button type="primary" link size="small" @click="forgotVisible = true">忘记密码？</el-button>
        </div>
      </el-form>

      <el-dialog v-model="forgotVisible" append-to-body title="申请重置密码" width="380px">
        <p style="font-size: 13px; color: #64748b; margin-top: 0;">
          为保障企业内控安全，密码不可未经核验直接修改。请输入您的登录账号提交申请，由资产管理员在后台为您重置：
        </p>
        <el-input v-model="forgotUsername" placeholder="请输入您的登录账号（如 emp1）" />
        <template #footer>
          <el-button @click="forgotVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForgot">提交重置申请</el-button>
        </template>
      </el-dialog>

      <div class="quick-box">
        <div class="quick-title">答辩演示快捷切换账号（密码均为 123456）：</div>
        <div class="quick-btns">
          <el-button size="small" @click="fill('emp1')">普通员工 (emp1)</el-button>
          <el-button size="small" @click="fill('manager1')">部门主管 (manager1)</el-button>
          <el-button size="small" @click="fill('finance1')">财务主管 (finance1)</el-button>
          <el-button size="small" @click="fill('admin1')">资产管理员 (admin1)</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const loading = ref(false)
const forgotVisible = ref(false)
const forgotUsername = ref('')
const captchaRef = ref(null)
let realCode = ''

const form = reactive({ username: 'emp1', password: '123456', code: '' })

const drawCaptcha = () => {
  const chars = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789'
  let code = ''
  const canvas = captchaRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#f1f5f9'
  ctx.fillRect(0, 0, canvas.width, canvas.height)

  for (let i = 0; i < 4; i++) {
    const ch = chars[Math.floor(Math.random() * chars.length)]
    code += ch
    ctx.font = 'bold 22px Arial'
    ctx.fillStyle = `rgb(${40 + Math.random() * 120}, ${40 + Math.random() * 120}, ${80 + Math.random() * 140})`
    ctx.save()
    ctx.translate(18 + i * 24, 28)
    ctx.rotate((Math.random() - 0.5) * 0.4)
    ctx.fillText(ch, 0, 0)
    ctx.restore()
  }
  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = '#cbd5e1'
    ctx.beginPath()
    ctx.moveTo(Math.random() * canvas.width, Math.random() * canvas.height)
    ctx.lineTo(Math.random() * canvas.width, Math.random() * canvas.height)
    ctx.stroke()
  }
  realCode = code
}

const fill = (u) => {
  form.username = u
  form.password = '123456'
}

const submitForgot = async () => {
  if (!forgotUsername.value.trim()) {
    ElMessage.warning('请输入登录账号')
    return
  }
  await api.forgotPwd({ username: forgotUsername.value.trim() })
  ElMessage.success('已记录重置申请，请联系资产管理员（admin1）为您重置密码')
  forgotVisible.value = false
  forgotUsername.value = ''
}

const handleLogin = async () => {
  if (!form.code.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (form.code.trim().toUpperCase() !== realCode) {
    ElMessage.error('验证码错误，请重新输入')
    form.code = ''
    drawCaptcha()
    return
  }
  loading.value = true
  try {
    const user = await api.login({ username: form.username, password: form.password })
    localStorage.setItem('user', JSON.stringify(user))
    ElMessage.success(`欢迎回来，${user.realName}`)
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}

onMounted(drawCaptcha)
</script>

<style scoped>
.login-bg {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #f5f5f7 0%, #e8eef7 100%);
}
.login-card {
  width: 420px;
  border-radius: 20px !important;
  padding: 16px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(20px);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.08) !important;
}
.title {
  text-align: center;
  margin: 0 0 6px;
  color: #1f2937;
}
.subtitle {
  text-align: center;
  margin: 0 0 20px;
  font-size: 13px;
  color: #6b7280;
}
.quick-box {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed #e5e7eb;
}
.quick-title {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
}
.quick-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
