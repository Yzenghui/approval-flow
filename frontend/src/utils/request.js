import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data

    // 业务成功
    if (res.code === 200) {
      return res
    }

    // 业务失败
    // 业务码 400：登录失败等业务错误，只提示，不跳转
    if (res.code === 400) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }

    // 业务码 401：Token 失效或未登录，提示并跳转登录页
    if (res.code === 401) {
      ElMessage.error(res.message || '登录已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
      return Promise.reject(new Error(res.message))
    }

    // 其他业务码（500 等）
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    // 网络错误、超时等
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  },
)

export default request
