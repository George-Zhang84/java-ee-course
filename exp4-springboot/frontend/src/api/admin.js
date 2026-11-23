import request from '@/api/request'

/** 待审核 / 全部商品 */
export const listAdminProducts = (params) => request.get('/admin/products', { params })
/** 审核 */
export const auditProduct = (id, data) => request.post(`/admin/products/${id}/audit`, data)
/** 强制下架 */
export const forceOffline = (id) => request.post(`/admin/products/${id}/force-offline`)

/** 用户管理 */
export const listAdminUsers = (params) => request.get('/admin/users', { params })
export const changeUserStatus = (id, status) => request.put(`/admin/users/${id}/status`, { status })
export const resetUserPassword = (id) => request.put(`/admin/users/${id}/reset-password`)

/** 角色与权限 */
export const listRoles = () => request.get('/admin/roles')
export const listPermissions = () => request.get('/admin/permissions')
export const updateRolePermissions = (id, permissionIds) =>
  request.put(`/admin/roles/${id}/permissions`, { permissionIds })

/** 看板 */
export const dashboardSummary = () => request.get('/admin/dashboard/summary')
export const dashboardTrend = (days = 7) => request.get('/admin/dashboard/trend', { params: { days } })
export const dashboardCategories = () => request.get('/admin/dashboard/categories')
export const dashboardHotProducts = (limit = 5) =>
  request.get('/admin/dashboard/hot-products', { params: { limit } })

/** 日志 */
export const listAuditLogs = (params) => request.get('/admin/audit-logs', { params })
export const listLoginLogs = (params) => request.get('/admin/login-logs', { params })
