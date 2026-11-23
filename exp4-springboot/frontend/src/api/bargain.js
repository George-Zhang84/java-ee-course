import request from '@/api/request'

/** 买家出价 */
export const createOffer = (data) => request.post('/offers', data)
/** 我发出的报价 */
export const listSentOffers = (params) => request.get('/offers/sent', { params })
/** 我收到的报价 */
export const listReceivedOffers = (params) => request.get('/offers/received', { params })
/** 接受报价（生成订单） */
export const acceptOffer = (id) => request.post(`/offers/${id}/accept`)
/** 拒绝报价 */
export const rejectOffer = (id) => request.post(`/offers/${id}/reject`)
/** 撤回报价 */
export const cancelOffer = (id) => request.post(`/offers/${id}/cancel`)
