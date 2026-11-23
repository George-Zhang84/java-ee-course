<template>
  <div class="layout">
    <header class="layout-header">
      <div class="header-inner">
        <router-link to="/" class="brand">
          <span class="brand-logo">校</span>
          <span class="brand-name">校园二手交易平台</span>
        </router-link>

        <el-menu
          class="nav-menu"
          mode="horizontal"
          :default-active="activePath"
          router
          :ellipsis="false"
        >
          <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>

          <el-sub-menu v-if="adminMenus.length" index="admin">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>后台管理</span>
            </template>
            <el-menu-item v-for="item in adminMenus" :key="item.path" :index="item.path">
              {{ item.title }}
            </el-menu-item>
          </el-sub-menu>
        </el-menu>

        <div class="header-right">
          <template v-if="userStore.isLogin">
            <!-- 未读为 0 时不渲染小红点；超过 99 显示 99+（el-badge 的 max 属性） -->
            <el-badge
              v-if="canSeeMessage"
              :value="unread"
              :hidden="unread === 0"
              :max="99"
              class="bell"
            >
              <el-button text circle @click="router.push('/notifications')">
                <el-icon :size="18"><Bell /></el-icon>
              </el-button>
            </el-badge>
            <el-dropdown @command="onUserCommand">
              <span class="user-entry">
                <el-avatar :size="28" :src="userStore.userInfo?.avatar">
                  {{ (userStore.nickname || 'U').slice(0, 1) }}
                </el-avatar>
                <span class="nickname">{{ userStore.nickname }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="item in dropdownItems"
                    :key="item.command"
                    :command="item.command"
                  >
                    {{ item.label }}
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="router.push('/login')">登录</el-button>
            <el-button type="primary" @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="layout-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <footer class="layout-footer">
      校园二手商品交易管理系统 · 20240648 张豪杰 · Java 企业级应用实验四
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useNotifyStore } from '@/store/notify'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 当前高亮菜单：详情页归属到商品市场
const activePath = computed(() => {
  if (route.path.startsWith('/products')) return '/products'
  return route.path
})

/**
 * 从路由表自动生成菜单并按权限过滤。
 * 用 computed 而非普通数组：登录/退出后 isLogin 与 permissions 变化时菜单立即重算，
 * 未登录不显示"我的订单/钱包"等必须登录的入口。
 */
const allMenus = computed(() =>
  router
    .getRoutes()
    .filter((r) => r.meta?.group && !r.meta?.hidden)
    .map((r) => ({
      path: r.path,
      title: r.meta.title,
      icon: r.meta.icon,
      group: r.meta.group,
      permission: r.meta.permission,
      requiresAuth: r.meta.requiresAuth
    }))
    .filter((m) => !m.requiresAuth || userStore.isLogin)
    .filter((m) => !m.permission || userStore.has(m.permission))
)

/**
 * 一级菜单白名单：只把最高频的入口放在顶栏。
 * 收藏/钱包/站内消息/个人中心等收进右上角头像下拉，
 * 否则 1440 宽度下菜单会溢出，后台管理入口会被挤出屏幕。
 */
const TOP_MENU_PATHS = ['/', '/products', '/publish', '/my-products', '/bargains', '/orders']

const menus = computed(() =>
  allMenus.value.filter((m) => m.group === 'main' && TOP_MENU_PATHS.includes(m.path))
)
const adminMenus = computed(() => allMenus.value.filter((m) => m.group === 'admin'))

/** 头像下拉里的入口同样按权限码过滤：管理员没有 menu:wallet 就不显示"我的钱包" */
const DROPDOWN_ITEMS = [
  { command: 'profile', label: '个人中心', permission: 'menu:profile' },
  { command: 'favorites', label: '我的收藏', permission: 'menu:favorites' },
  { command: 'wallet', label: '我的钱包', permission: 'menu:wallet' },
  { command: 'notifications', label: '站内消息', permission: 'menu:message' }
]
const dropdownItems = computed(() =>
  DROPDOWN_ITEMS.filter((i) => userStore.has(i.permission))
)

/** 未读数统一走 store，读消息/全部已读后立即同步，不必等轮询 */
const notifyStore = useNotifyStore()
const unread = computed(() => notifyStore.unread)
const canSeeMessage = computed(() => userStore.has('menu:message'))

function onUserCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        await userStore.logout()
        notifyStore.stopPolling()
        notifyStore.clear()
        ElMessage.success('已退出登录')
        router.push('/login')
      })
      .catch(() => {})
    return
  }
  router.push(`/${command}`)
}

// 路由变化（如从消息页返回）时刷新一次，保证红点与实际未读数一致
watch(
  () => route.fullPath,
  () => {
    if (userStore.isLogin) notifyStore.refresh()
  }
)

onMounted(() => {
  notifyStore.refresh()
  notifyStore.startPolling(20000)
})

onUnmounted(() => {
  notifyStore.stopPolling()
})
</script>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.layout-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 1px solid #eef0f4;
  box-shadow: 0 1px 6px rgba(15, 23, 42, 0.04);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 17px;
  white-space: nowrap;
}

.brand-logo {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--brand);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
}

.nav-menu {
  flex: 1;
  border-bottom: none;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
}

.nickname {
  font-size: 14px;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.layout-main {
  flex: 1;
}

.layout-footer {
  padding: 18px 0;
  text-align: center;
  color: var(--text-sub);
  font-size: 12px;
  border-top: 1px solid #eef0f4;
  background: #fff;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 820px) {
  .brand-name {
    display: none;
  }
}
</style>
