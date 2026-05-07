<template>
  <div class="user-profile">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
          <el-button @click="goBack">返回</el-button>
        </div>
      </template>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">
          {{ userInfo.username || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="姓名">
          {{ userInfo.name || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          {{ getRoleText(userInfo.roleCode) }}
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">
          <div v-if="!isEditingEmail" class="email-display">
            <span>{{ userInfo.email || '—' }}</span>
            <el-button link type="primary" @click="startEditEmail">编辑</el-button>
          </div>
          <div v-else class="email-edit">
            <el-input
              v-model="emailForm.email"
              placeholder="请输入邮箱"
              class="email-input"
              @keyup.enter="handleUpdateEmail"
            />
            <el-button type="primary" @click="handleUpdateEmail" :loading="submittingEmail"
              >保存</el-button
            >
            <el-button @click="cancelEditEmail">取消</el-button>
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 底部操作栏 -->
      <div class="action-bar">
        <el-button type="primary" plain @click="openPasswordDialog">修改密码</el-button>
      </div>
    </el-card>

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="400px"
      @close="resetPasswordForm"
    >
      <el-form
        :model="passwordForm"
        :rules="passwordRules"
        ref="passwordFormRef"
        label-width="100px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            type="password"
            v-model="passwordForm.oldPassword"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            type="password"
            v-model="passwordForm.newPassword"
            placeholder="6-20位，字母和数字"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            type="password"
            v-model="passwordForm.confirmPassword"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdatePassword" :loading="submittingPassword"
          >确定</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const userInfo = ref({})

// 邮箱编辑相关
const isEditingEmail = ref(false)
const submittingEmail = ref(false)
const emailForm = reactive({ email: '' })

// 密码弹窗相关
const passwordDialogVisible = ref(false)
const submittingPassword = ref(false)
const passwordFormRef = ref()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const getRoleText = (roleCode) => {
  const map = {
    STAFF: '员工',
    MANAGER: '主管',
    DIRECTOR: '经理',
    PRESIDENT: '总监',
    ADMIN: '管理员',
  }
  return map[roleCode] || roleCode
}

// 加载个人信息
const loadProfile = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/profile')
    if (res.code === 200) {
      userInfo.value = res.data
      emailForm.email = res.data.email || ''
    }
  } catch (error) {
    console.error('加载个人信息失败', error)
    ElMessage.error('加载个人信息失败')
  } finally {
    loading.value = false
  }
}

// 邮箱编辑
const startEditEmail = () => {
  isEditingEmail.value = true
  emailForm.email = userInfo.value.email || ''
}

const cancelEditEmail = () => {
  isEditingEmail.value = false
  emailForm.email = userInfo.value.email || ''
}

const handleUpdateEmail = async () => {
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  if (!emailForm.email) {
    ElMessage.warning('邮箱不能为空')
    return
  }
  if (!emailRegex.test(emailForm.email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }

  submittingEmail.value = true
  try {
    const res = await request.patch('/user/profile', { email: emailForm.email })
    if (res.code === 200) {
      ElMessage.success('修改成功')
      userInfo.value.email = emailForm.email
      userStore.userInfo.email = emailForm.email
      const storedUserInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
      storedUserInfo.email = emailForm.email
      localStorage.setItem('userInfo', JSON.stringify(storedUserInfo))
      isEditingEmail.value = false
    }
  } catch (error) {
    console.error('修改失败', error)
  } finally {
    submittingEmail.value = false
  }
}

// 修改密码
const openPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.resetFields()
}

// 自定义校验：确认密码
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 自定义校验：新旧密码不能相同
const validateNotSameAsOld = (rule, value, callback) => {
  if (value && passwordForm.oldPassword && value === passwordForm.oldPassword) {
    callback(new Error('新密码不能与原密码相同'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-zA-Z])(?=.*\d)[a-zA-Z0-9_]{6,20}$/,
      message: '6-20位，必须包含字母和数字',
      trigger: 'blur',
    },
    { validator: validateNotSameAsOld, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const handleUpdatePassword = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submittingPassword.value = true
  try {
    const res = await request.put('/user/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      passwordDialogVisible.value = false
      userStore.logout()
      router.push('/login')
    }
  } catch (error) {
    console.error('修改密码失败', error)
  } finally {
    submittingPassword.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.user-profile {
  height: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.email-display {
  display: flex;
  align-items: center;
  gap: 12px;
}
.email-edit {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.email-input {
  width: 250px;
}
.action-bar {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
</style>
