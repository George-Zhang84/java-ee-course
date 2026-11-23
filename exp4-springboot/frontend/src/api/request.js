import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

/**
 * 全局 axios 实例。
 *
 * 统一处理三件事，避免每个页面重复写：
 *  1. 请求：自动携带 JWT；
 *  2. 响应：业务码非 0 时统一提示（401/1002 跳登录）；
 *  3. 异常：网络错误给出可读提示。
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 后端统一响应：{ code, message, data, traceId }
    if (body && typeof body.code === 'number') {
      if (body.code === 0) {
        return body
      }
      if (body.code === 1002) {
        handleUnauthorized(body.message)
        return Promise.reject(body)
      }
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(body)
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      handleUnauthorized('登录已过期，请重新登录')
    } else if (status === 403) {
      ElMessage.error('没有权限执行该操作')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络')
    } else {
      ElMessage.error(error.response?.data?.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized(message) {
  const userStore = useUserStore()
  userStore.clearAuth()
  ElMessage.warning(message || '请重新登录')
  const current = router.currentRoute.value
  if (current.path !== '/login') {
    router.push({ path: '/login', query: { redirect: current.fullPath } })
  }
}

export default request
