import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'UserLogin',
      component: () => import('@/views/UserLogin.vue'),
    },
    {
      path: '/register',
      name: 'UserRegister',
      component: () => import('@/views/UserRegister.vue'),
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        // 普通用户页面
        { path: 'instances', component: () => import('@/views/MyInstances.vue') },
        { path: 'apply', component: () => import('@/views/ApplyForm.vue') },
        { path: 'pending', component: () => import('@/views/PendingTasks.vue') },
        { path: 'completed', component: () => import('@/views/CompletedTasks.vue') },
        { path: 'notifications', component: () => import('@/views/NotificationList.vue') },

        // 申请相关页面（详情/草稿编辑/修改重提/审批）
        { path: 'instance/:id', component: () => import('@/views/InstanceDetail.vue') },
        { path: 'draft-edit/:id', component: () => import('@/views/DraftEdit.vue') },
        { path: 'resubmit-edit/:id', component: () => import('@/views/ResubmitEdit.vue') },
        { path: 'task/:taskId', component: () => import('@/views/TaskDetail.vue') },

        // 管理员页面
        { path: 'definitions', component: () => import('@/views/admin/DefinitionList.vue') },
        { path: 'users', component: () => import('@/views/admin/UserList.vue') },
        { path: 'admin/mq-outbox', component: () => import('@/views/admin/MqOutboxAdmin.vue') },
        {
          path: 'admin/definition/create',
          component: () => import('@/views/admin/DefinitionDetail.vue'),
        },
        {
          path: 'admin/definition/:id',
          component: () => import('@/views/admin/DefinitionDetail.vue'),
        },

        // 个人中心
        { path: 'profile', component: () => import('@/views/UserProfile.vue') },
      ],
    },
  ],
})

// 路由守卫
router.beforeEach((to) => {
  const token = localStorage.getItem('token')

  if (token) {
    // 已登录用户访问登录/注册页 或 根路径 -> 跳转到对应首页
    if (to.path === '/login' || to.path === '/register' || to.path === '/') {
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
      const roleCode = userInfo?.roleCode
      return roleCode === 'ADMIN' ? '/definitions' : '/instances'
    }
    return true
  }

  // 未登录用户
  if (to.path === '/login' || to.path === '/register') {
    return true
  }
  return '/login'
})

export default router
