import { useUserStore } from '@/store/user'

/**
 * 按钮级权限指令：v-permission="'product:publish'"。
 *
 * 注意：前端隐藏按钮只是体验优化，真正的拦截在后端 @PreAuthorize，
 * 不能因为"按钮看不见"就认为接口安全了。
 */
export const permissionDirective = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const required = binding.value
    if (!required) return
    const codes = Array.isArray(required) ? required : [required]
    const allowed = codes.some((code) => userStore.permissions.includes(code))
    if (!allowed) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}
