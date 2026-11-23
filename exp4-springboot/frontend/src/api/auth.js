import request from '@/api/request'

/** 登录 */
export const login = (data) => request.post('/auth/login', data)
/** 注册（默认普通用户） */
export const register = (data) => request.post('/auth/register', data)
/** 退出登录 */
export const logout = () => request.post('/auth/logout')
/** 当前用户信息（含权限码） */
export const me = () => request.get('/auth/me')
/** 修改密码 */
export const changePassword = (data) => request.put('/auth/password', data)
