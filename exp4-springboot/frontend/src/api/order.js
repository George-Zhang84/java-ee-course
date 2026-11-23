import request from '@/api/request'

/** 直接下单 */
export const createOrder = (productId) => request.post('/orders', { productId })
/** 我的订单（role=buyer/seller） */
export const listOrders = (params) => request.get('/orders', { params })
/** 订单详情 */
export const getOrder = (id) => request.get(`/orders/${id}`)
/** 支付 */
export const payOrder = (id) => request.post(`/orders/${id}/pay`)
/** 发货 */
export const shipOrder = (id) => request.post(`/orders/${id}/ship`)
/** 确认收货 */
export const confirmOrder = (id) => request.post(`/orders/${id}/confirm`)
/** 取消订单 */
export const cancelOrder = (id) => request.post(`/orders/${id}/cancel`)
