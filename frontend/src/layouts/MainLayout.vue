<template>
  <el-container class="layout-container">
    <!-- 顶部栏 -->
    <el-header class="layout-header">
      <div class="header-left">
        <h2>审批流系统</h2>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="user-dropdown">
            {{ userStore.userInfo?.name || userStore.userInfo?.username }}
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container>
      <!-- 侧边菜单 -->
      <el-aside class="layout-aside" width="200px">
        <el-menu :default-active="activeMenu" router class="menu">
          <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 内容区域 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, Document, Edit, Clock, Check, Setting, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 当前激活菜单
const activeMenu = computed(() => route.path)

// 根据角色返回菜单
const menus = computed(() => {
  const roleCode = userStore.userInfo?.roleCode

  // 管理员菜单
  if (roleCode === 'ADMIN') {
    return [
      { path: '/definitions', title: '流程定义', icon: Setting },
      { path: '/users', title: '用户管理', icon: User },
    ]
  }

  // 普通用户菜单
  return [
    { path: '/instances', title: '我的申请', icon: Document },
    { path: '/apply', title: '发起申请', icon: Edit },
    { path: '/pending', title: '待办任务', icon: Clock },
    { path: '/completed', title: '已办任务', icon: Check },
  ]
})

// 下拉菜单命令
const handleCommand = async (command) => {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-header {
  background-color: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
}

.header-left h2 {
  margin: 0;
  font-weight: 500;
  font-size: 20px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-dropdown {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
}

.layout-aside {
  background-color: #ffffff;
  border-right: 1px solid #e4e7ed;
}

.menu {
  border-right: none;
  height: 100%;
}

.layout-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
