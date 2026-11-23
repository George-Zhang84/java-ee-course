import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

/**
 * 路由表。
 *
 * meta 约定：
 *  - requiresAuth：需要登录才可进入；
 *  - permission：需要指定权限码（与后端 RBAC 权限码一致）；
 *  - title / icon：用于侧边/顶部菜单渲染；
 *  - group：菜单分组（main=前台，admin=后台）。
 *
 * 全部页面使用路由懒加载，首屏只加载登录与首页需要的代码。
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', icon: 'HomeFilled', group: 'main' }
      },
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('@/views/ProductList.vue'),
        meta: { title: '商品市场', icon: 'Goods', group: 'main' }
      },
      {
        path: 'products/:id',
        name: 'ProductDetail',
        component: () => import('@/views/ProductDetail.vue'),
        meta: { title: '商品详情', hidden: true }
      },
      {
        path: 'publish',
        name: 'Publish',
        component: () => import('@/views/Publish.vue'),
        meta: { title: '发布商品', icon: 'Plus', permission: 'product:publish', group: 'main' }
      },
      {
        path: 'my-products',
        name: 'MyProducts',
        component: () => import('@/views/MyProducts.vue'),
        meta: { title: '我的商品', icon: 'Sell', permission: 'menu:mine', group: 'main' }
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: () => import('@/views/Favorites.vue'),
        meta: { title: '我的收藏', icon: 'Star', permission: 'menu:favorites', group: 'main' }
      },
      {
        path: 'bargains',
        name: 'Bargains',
        component: () => import('@/views/Bargains.vue'),
        meta: { title: '议价消息', icon: 'ChatDotRound', permission: 'menu:bargain', group: 'main' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/Orders.vue'),
        meta: { title: '我的订单', icon: 'List', permission: 'menu:order', group: 'main' }
      },
      {
        path: 'wallet',
        name: 'Wallet',
        component: () => import('@/views/Wallet.vue'),
        meta: { title: '我的钱包', icon: 'Wallet', permission: 'menu:wallet', group: 'main' }
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/Notifications.vue'),
        meta: { title: '站内消息', icon: 'Bell', permission: 'menu:message', group: 'main' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', icon: 'User', permission: 'menu:profile', group: 'main' }
      },
      {
        path: 'admin/pending',
        name: 'AdminPending',
        component: () => import('@/views/admin/Pending.vue'),
        meta: { title: '商品审核', icon: 'Checked', permission: 'product:audit', group: 'admin' }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理', icon: 'UserFilled', permission: 'user:manage', group: 'admin' }
      },
      {
        path: 'admin/roles',
        name: 'AdminRoles',
        component: () => import('@/views/admin/Roles.vue'),
        meta: { title: '角色权限', icon: 'Setting', permission: 'role:manage', group: 'admin' }
      },
      {
        path: 'admin/audit',
        name: 'AdminAudit',
        component: () => import('@/views/admin/AuditLogs.vue'),
        meta: { title: '审计日志', icon: 'Document', permission: 'audit:view', group: 'admin' }
      },
      {
        path: 'admin/dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据看板', icon: 'DataLine', permission: 'dashboard:view', group: 'admin' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/**
 * 全局前置守卫：
 *  1. 首次进入且本地有 token 时，先拉一次 /auth/me 恢复权限（刷新页面不丢状态）；
 *  2. 未登录访问受限页面 -> 跳登录并带 redirect；
 *  3. 权限不足 -> 提示并回到首页（后端仍会二次校验，这里只是体验优化）。
 */
router.beforeEach(async (to) => {
  const userStore = useUserStore()

  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.fetchMe()
    } catch (e) {
      userStore.clearAuth()
    }
  }

  const needAuth = to.meta.requiresAuth || Boolean(to.meta.permission)
  if (needAuth && !userStore.isLogin) {
    ElMessage.warning('请先登录')
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.permission && !userStore.has(to.meta.permission)) {
    ElMessage.error('没有访问该页面的权限')
    return { path: '/' }
  }
  document.title = to.meta.title ? `${to.meta.title} - 校园二手交易平台` : '校园二手交易平台'
  return true
})

export default router
