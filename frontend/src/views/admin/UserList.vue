<template>
  <div class="user-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div class="header-actions">
            <el-input
              v-model="keyword"
              placeholder="用户名/姓名/邮箱"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>

            <el-select
              v-model="filterRole"
              placeholder="全部角色"
              clearable
              style="width: 120px; margin-left: 12px"
              @change="handleSearch"
            >
              <el-option label="员工" value="STAFF" />
              <el-option label="主管" value="MANAGER" />
              <el-option label="经理" value="DIRECTOR" />
              <el-option label="总监" value="PRESIDENT" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>

            <el-select
              v-model="filterStatus"
              placeholder="全部状态"
              clearable
              style="width: 120px; margin-left: 12px"
              @change="handleSearch"
            >
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>

            <el-button type="primary" style="margin-left: 12px" @click="handleCreate">
              新增用户
            </el-button>
            <el-button @click="refreshList">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="roleCode" label="角色" width="100">
          <template #default="{ row }">
            {{ getRoleText(row.roleCode) }}
          </template>
        </el-table-column>
        <el-table-column prop="managerId" label="上级ID" width="100">
          <template #default="{ row }">
            {{ row.managerId ? row.managerId : '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button link type="primary" @click="handleChangeManager(row)">修改上级</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
        class="pagination"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="resetForm">
      <el-form
        :model="formData"
        :rules="formRules"
        ref="formRef"
        label-width="100px"
        @submit.prevent
      >
        <el-form-item label="用户名" prop="username" v-if="isCreate">
          <el-input v-model="formData.username" placeholder="4-20位，字母开头" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="isCreate">
          <el-input type="password" v-model="formData.password" placeholder="6-20位，字母和数字" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" placeholder="2-20位" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="邮箱格式" />
        </el-form-item>
        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="formData.roleCode" placeholder="请选择角色" style="width: 100%">
            <el-option label="员工" value="STAFF" />
            <el-option label="主管" value="MANAGER" />
            <el-option label="经理" value="DIRECTOR" />
            <el-option label="总监" value="PRESIDENT" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status" v-if="!isCreate">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="重置密码"
      width="400px"
      @close="resetPasswordForm"
    >
      <el-form
        :model="passwordForm"
        :rules="passwordRules"
        ref="passwordFormRef"
        label-width="100px"
        @submit.prevent
      >
        <el-form-item label="新密码" prop="password">
          <el-input
            type="password"
            v-model="passwordForm.password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            type="password"
            v-model="passwordForm.confirmPassword"
            placeholder="请再次输入密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitPassword" :loading="submittingPassword"
          >确定</el-button
        >
      </template>
    </el-dialog>

    <!-- 修改上级弹窗 -->
    <el-dialog
      v-model="managerDialogVisible"
      title="修改上级"
      width="400px"
      @close="resetManagerForm"
    >
      <el-form :model="managerForm" label-width="100px" @submit.prevent>
        <el-form-item label="当前上级ID">
          <span>{{ currentManagerId ? currentManagerId : '无' }}</span>
        </el-form-item>
        <el-form-item label="新上级ID">
          <el-input-number
            v-model="managerForm.managerId"
            :min="1"
            placeholder="上级用户ID"
            style="width: 100%"
            :controls="false"
            @keyup.enter.prevent
          />
        </el-form-item>
        <div class="form-tip">输入上级用户ID（可以为空，表示取消上级）</div>
      </el-form>
      <template #footer>
        <el-button @click="managerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitManager" :loading="submittingManager"
          >确定</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const filterRole = ref('')
const filterStatus = ref(null)

// 新增/编辑弹窗
const dialogVisible = ref(false)
const isCreate = ref(true)
const formRef = ref()
const submitting = ref(false)
const formData = ref({
  username: '',
  password: '',
  name: '',
  email: '',
  roleCode: '',
  status: 1,
})
const currentUserId = ref(null)

// 重置密码弹窗
const passwordDialogVisible = ref(false)
const submittingPassword = ref(false)
const passwordFormRef = ref()
const passwordForm = ref({ password: '', confirmPassword: '' })
const currentPasswordUserId = ref(null)

// 修改上级弹窗
const managerDialogVisible = ref(false)
const submittingManager = ref(false)
const managerForm = ref({ managerId: null })
const managerTargetUser = ref(null)
const currentManagerId = ref('')

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

const dialogTitle = computed(() => (isCreate.value ? '新增用户' : '编辑用户'))

// 新增/编辑表单校验规则
const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z][a-zA-Z0-9_]{3,19}$/,
      message: '4-20位，字母开头，仅字母/数字/下划线',
      trigger: 'blur',
    },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-zA-Z])(?=.*\d)[a-zA-Z0-9_]{6,20}$/,
      message: '6-20位，必须包含字母和数字',
      trigger: 'blur',
    },
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度2-20位', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
      message: '邮箱格式不正确',
      trigger: 'blur',
    },
  ],
  roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

// 重置密码校验规则
const passwordRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-zA-Z])(?=.*\d)[a-zA-Z0-9_]{6,20}$/,
      message: '6-20位，必须包含字母和数字',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    if (filterRole.value) params.roleCode = filterRole.value
    if (filterStatus.value !== null && filterStatus.value !== '') params.status = filterStatus.value
    const res = await request.get('/admin/users', { params })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('加载失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const refreshList = () => {
  keyword.value = ''
  filterRole.value = ''
  filterStatus.value = null
  pageNum.value = 1
  loadData()
}

const handleStatusChange = async (row) => {
  try {
    await request.patch(`/admin/users/${row.id}`, {
      name: row.name,
      email: row.email,
      roleCode: row.roleCode,
      status: row.status,
    })
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
    loadData()
  } catch {
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleCreate = () => {
  isCreate.value = true
  formData.value = { username: '', password: '', name: '', email: '', roleCode: '', status: 1 }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isCreate.value = false
  currentUserId.value = row.id
  formData.value = {
    name: row.name,
    email: row.email,
    roleCode: row.roleCode,
    status: row.status,
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isCreate.value) {
      await request.post('/admin/users', {
        username: formData.value.username,
        password: formData.value.password,
        name: formData.value.name,
        email: formData.value.email,
        roleCode: formData.value.roleCode,
      })
    } else {
      await request.patch(`/admin/users/${currentUserId.value}`, {
        name: formData.value.name,
        email: formData.value.email,
        roleCode: formData.value.roleCode,
        status: formData.value.status,
      })
    }
    ElMessage.success(isCreate.value ? '创建成功' : '保存成功')
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('保存失败', error)
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  formData.value = { username: '', password: '', name: '', email: '', roleCode: '', status: 1 }
  currentUserId.value = null
  formRef.value?.resetFields()
}

const handleResetPassword = (row) => {
  currentPasswordUserId.value = row.id
  passwordForm.value = { password: '', confirmPassword: '' }
  passwordDialogVisible.value = true
}

const resetPasswordForm = () => {
  passwordForm.value = { password: '', confirmPassword: '' }
  passwordFormRef.value?.resetFields()
}

const handleSubmitPassword = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submittingPassword.value = true
  try {
    await request.put(`/admin/users/${currentPasswordUserId.value}/password`, {
      password: passwordForm.value.password,
    })
    ElMessage.success('密码重置成功')
    passwordDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('重置密码失败', error)
  } finally {
    submittingPassword.value = false
  }
}

const handleChangeManager = (row) => {
  managerTargetUser.value = row
  currentManagerId.value = row.managerId ?? ''
  managerForm.value = { managerId: null }
  managerDialogVisible.value = true
}

const resetManagerForm = () => {
  managerForm.value = { managerId: null }
  managerTargetUser.value = null
  currentManagerId.value = ''
}

const handleSubmitManager = async () => {
  submittingManager.value = true
  try {
    await request.put(`/admin/users/${managerTargetUser.value.id}/manager`, {
      managerId: managerForm.value.managerId ?? null,
    })
    ElMessage.success('修改上级成功')
    managerDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('修改上级失败', error)
  } finally {
    submittingManager.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.user-list {
  height: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  align-items: center;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
