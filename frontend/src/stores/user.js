import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useUserStore = defineStore('user', () => {
  // 从 localStorage 读取 token
  const token = ref(localStorage.getItem('token') || '')
  // 从 localStorage 读取用户信息
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  // 登录方法
  async function login(username, password) {
    const res = await request.post('/auth/login', { username, password })
    if (res.code === 200) {
      token.value = res.data.token
      userInfo.value = res.data.userInfo
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))
      return true
    }
    return false
  }

  // 退出登录方法
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, login, logout }
})
