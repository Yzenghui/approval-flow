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
      redirect: '/login',
    },
  ],
})

export default router
