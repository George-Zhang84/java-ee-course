/**
 * 展示层格式化工具：金额、时间、状态色。
 * 集中在一处，保证全站展示一致。
 */

/** 金额：保留两位小数，null 显示- */
export function money(value) {
  if (value === null || value === undefined || value === '') return '-'
  return Number(value).toFixed(2)
}

/** 日期时间：后端返回 yyyy-MM-dd HH:mm:ss，这里只截取到分钟 */
export function dateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

/** 相对时间：用于订单/通知列表，超过 7 天显示日期 */
export function fromNow(value) {
  if (!value) return '-'
  const time = new Date(String(value).replace(' ', 'T')).getTime()
  const diff = Date.now() - time
  if (Number.isNaN(time)) return value
  const minute = 60 * 1000
  if (diff < minute) return '刚刚'
  if (diff < 60 * minute) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < 24 * 60 * minute) return `${Math.floor(diff / (60 * minute))} 小时前`
  if (diff < 7 * 24 * 60 * minute) return `${Math.floor(diff / (24 * 60 * minute))} 天前`
  return dateTime(value)
}

/** 订单状态 -> Element Plus 标签类型 */
export const ORDER_TAG = {
  UNPAID: 'warning',
  PAID: 'primary',
  SHIPPED: 'info',
  COMPLETED: 'success',
  CANCELLED: 'danger'
}

/** 商品状态 -> 标签类型 */
export const PRODUCT_TAG = {
  PENDING: 'warning',
  ON_SALE: 'success',
  LOCKED: 'primary',
  SOLD: 'info',
  REJECTED: 'danger',
  OFF_SHELF: 'info'
}

/** 议价状态 -> 标签类型 */
export const OFFER_TAG = {
  PENDING: 'warning',
  ACCEPTED: 'success',
  REJECTED: 'danger',
  CANCELLED: 'info',
  EXPIRED: 'info'
}
