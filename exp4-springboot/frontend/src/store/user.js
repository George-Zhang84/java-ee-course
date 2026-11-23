import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, logout as logoutApi, me as meApi } from '@/api/auth'

const TOKEN_KEY = 'exp4_token'

/**
 * 用户状态仓库。
 *
 * 持久化策略：token 放 localStorage（刷新页面仍在），
 * 用户信息与权限码每次启动通过 /auth/me 重新拉取——
 * 这样管理员改了权限，用户刷新页面就生效，不会用旧权限渲染出无法点击的菜单。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: null,
    permissions: [],
    roles: []
  }),

  getters: {
    isLogin: (state) => Boolean(state.token),
    nickname: (state) => state.userInfo?.nickname || '',
    isAdmin: (state) => state.roles.some((r) => ['ADMIN', 'SUPER_ADMIN'].includes(r))
  },

  actions: {
    setAuth({ token, user, permissions, roles }) {
      this.token = token
      this.userInfo = user
      this.permissions = permissions || []
      this.roles = roles || []
      localStorage.setItem(TOKEN_KEY, token)
    },

    clearAuth() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      this.roles = []
      localStorage.removeItem(TOKEN_KEY)
    },

    async login(form) {
      const res = await loginApi(form)
      this.setAuth({
        token: res.data.token,
        user: res.data.user,
        permissions: res.data.permissions,
        roles: res.data.roles
      })
      return res
    },

    async register(form) {
      const res = await registerApi(form)
      this.setAuth({
        token: res.data.token,
        user: res.data.user,
        permissions: res.data.permissions,
        roles: res.data.roles
      })
      return res
    },

    /** 拉取当前用户信息（路由守卫在首次进入时调用） */
    async fetchMe() {
      const res = await meApi()
      this.userInfo = res.data
      this.permissions = res.data.permissions || []
      this.roles = res.data.roles || []
      return res.data
    },

    /** 是否拥有某权限码 */
    has(permission) {
      if (!permission) return true
      return this.permissions.includes(permission)
    },

    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 令牌可能已过期，忽略服务端错误，本地状态一定要清
      }
      this.clearAuth()
    }
  }
})
