import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'UserLogin',
      component: () => import('@/views/UserLogin.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      redirect: '/login',
    },
  ],
})

export default router
