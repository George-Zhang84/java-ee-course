import request from '@/api/request'

/** 钱包概览 */
export const getWallet = () => request.get('/wallet')
/** 模拟充值 */
export const recharge = (amount) => request.post('/wallet/recharge', { amount })
/** 资金流水 */
export const listWalletLogs = (params) => request.get('/wallet/logs', { params })

/** 发表评价 */
export const createReview = (data) => request.post('/reviews', data)
/** 我发出的评价 */
export const listMyReviews = (params) => request.get('/reviews/mine', { params })
/** 我收到的评价（含卖家对我的评价） */
export const listReceivedReviews = (params) => request.get('/reviews/received', { params })

/** 通知 */
export const listNotifications = (params) => request.get('/notifications', { params })
export const unreadCount = () => request.get('/notifications/unread-count')
export const listUnread = (limit = 5) => request.get('/notifications/unread', { params: { limit } })
export const markNotificationRead = (id) => request.put(`/notifications/${id}/read`)
export const markAllNotificationsRead = () => request.put('/notifications/read-all')
