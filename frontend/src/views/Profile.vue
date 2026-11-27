<template>
  <div>
    <!-- 顶部个人名片卡片 -->
    <el-card style="margin-bottom: 18px;">
      <div class="profile-banner">
        <div class="banner-left">
          <div class="avatar-circle">{{ (user.realName || 'U').slice(0, 1) }}</div>
          <div>
            <div style="font-size: 20px; font-weight: 700; color: #0f172a;">
              {{ user.realName }}
              <el-tag size="small" round style="margin-left: 8px;">{{ user.roleName }}</el-tag>
            </div>
            <div style="font-size: 13px; color: #64748b; margin-top: 4px;">
              账号：{{ user.username }} · 所属部门：{{ user.deptName }}
            </div>
          </div>
        </div>
        <div class="banner-stats">
          <div class="mini-stat">
            <div class="mini-num">{{ myCount }}</div>
            <div class="mini-label">我的申请单</div>
          </div>
          <div class="mini-stat">
            <div class="mini-num" style="color: #16a34a;">{{ finishedCount }}</div>
            <div class="mini-label">已办结</div>
          </div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card header="基本资料（部门与角色仅限管理员调整）">
          <el-form :model="profileForm" label-width="90px">
            <el-form-item label="登录账号"><el-input v-model="user.username" disabled /></el-form-item>
            <el-form-item label="当前角色"><el-input v-model="user.roleName" disabled /></el-form-item>
            <el-form-item label="所属部门"><el-input v-model="user.deptName" disabled /></el-form-item>
            <el-form-item label="真实姓名"><el-input v-model="profileForm.realName" /></el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile">保存姓名</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card header="修改登录密码">
          <el-form :model="pwdForm" label-width="100px">
            <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
            <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="5-16个字符" /></el-form-item>
            <el-form-item label="确认新密码"><el-input v-model="pwdForm.confirmPassword" type="password" show-password /></el-form-item>
            <el-form-item>
              <el-button type="warning" @click="savePassword">确认修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const profileForm = reactive({ realName: user.value.realName })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const myCount = ref(0)
const finishedCount = ref(0)

onMounted(async () => {
  const list = await api.getApplyList()
  const mine = (list || []).filter((i) => i.applicantId === user.value.id)
  myCount.value = mine.length
  finishedCount.value = mine.filter((i) => i.status === 'FINISHED').length
})

const saveProfile = async () => {
  const updated = await api.updateProfile({ realName: profileForm.realName })
  // 保留原有的登录 token
  const merged = { ...user.value, ...updated }
  localStorage.setItem('user', JSON.stringify(merged))
  user.value = merged
  ElMessage.success('个人资料已更新')
}

const savePassword = async () => {
  if (!pwdForm.oldPassword || pwdForm.newPassword.length < 5 || pwdForm.newPassword.length > 16) {
    ElMessage.warning('新密码长度应为 5-16 个字符')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  await api.updatePassword({
    oldPassword: pwdForm.oldPassword,
    newPassword: pwdForm.newPassword
  })
  ElMessage.success('密码修改成功')
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
}
</script>

<style scoped>
.profile-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}
.banner-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.avatar-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0071e3, #3b82f6);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(0, 113, 227, 0.25);
}
.banner-stats {
  display: flex;
  gap: 28px;
  padding-right: 12px;
}
.mini-stat {
  text-align: center;
}
.mini-num {
  font-size: 22px;
  font-weight: 700;
  color: #0071e3;
}
.mini-label {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}
</style>
