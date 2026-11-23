import { defineStore } from 'pinia'
import { unreadCount } from '@/api/wallet'
import { useUserStore } from '@/store/user'

/**
 * 未读消息数仓库。
 *
 * 为什么单独抽一个 store：铃铛在布局层、消息在页面层，两边都要读同一个数字。
 * 之前把计数放在 MainLayout 的 ref 里、只在 60 秒轮询时刷新，导致
 * "点开消息已读了、红点还在"和"来了新消息红点不涨"两个问题。
 *
 * 现在统一：组件只读 store，任何会改变未读数的动作（读一条、全部已读、
 * 打开消息页、下单产生通知）都调用 refresh()。
 */
export const useNotifyStore = defineStore('notify', {
  state: () => ({
    unread: 0,
    timer: null
  }),

  actions: {
    /** 重新拉取未读数；未登录直接归零 */
    async refresh() {
      const userStore = useUserStore()
      if (!userStore.isLogin) {
        this.unread = 0
        return
      }
      try {
        const res = await unreadCount()
        this.unread = Number(res.data?.count || 0)
      } catch (e) {
        // 网络异常时保留旧值，避免红点闪烁
      }
    },

    /** 本地减一（读单条后立即反馈，无需等接口） */
    decrease(n = 1) {
      this.unread = Math.max(0, this.unread - n)
    },

    /** 全部已读 */
    clear() {
      this.unread = 0
    },

    /** 开启轮询（布局挂载时调用），默认 20 秒 */
    startPolling(intervalMs = 20000) {
      this.stopPolling()
      this.timer = setInterval(() => this.refresh(), intervalMs)
    },

    stopPolling() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    }
  }
})
