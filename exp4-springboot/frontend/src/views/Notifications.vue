<template>
  <div class="page-container">
    <div class="toolbar">
      <div class="section-title">站内消息</div>
      <el-button text type="primary" @click="readAll">全部标记已读</el-button>
    </div>

    <el-card shadow="never">
      <div v-loading="loading">
        <div
          v-for="n in list"
          :key="n.id"
          :class="['notice', { unread: n.readFlag === 0 }]"
          @click="onClick(n)"
        >
          <el-tag size="small" :type="tagType(n.type)" effect="plain">{{ typeText(n.type) }}</el-tag>
          <div class="notice-main">
            <div class="notice-title">
              <span v-if="n.readFlag === 0" class="dot" />
              {{ n.title }}
            </div>
            <div class="notice-content">{{ n.content }}</div>
          </div>
          <div class="text-sub">{{ fromNow(n.createTime) }}</div>
        </div>
        <el-empty v-if="!list.length" description="暂无消息" />
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listNotifications, markAllNotificationsRead, markNotificationRead } from '@/api/wallet'
import { fromNow } from '@/utils/format'
import { useNotifyStore } from '@/store/notify'

const notifyStore = useNotifyStore()

const router = useRouter()
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

function typeText(type) {
  return { ORDER: '订单', BARGAIN: '议价', AUDIT: '审核', SYSTEM: '系统' }[type] || type
}

function tagType(type) {
  return { ORDER: 'primary', BARGAIN: 'warning', AUDIT: 'success', SYSTEM: 'info' }[type] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await listNotifications({ page: page.value, size: size.value })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function onClick(n) {
  if (n.readFlag === 0) {
    await markNotificationRead(n.id)
    n.readFlag = 1
    // 本地先减一，铃铛立即反馈；再拉一次保证与服务端一致
    notifyStore.decrease(1)
    notifyStore.refresh()
  }
  // 按消息类型跳转到对应页面，bizId 就是关联的业务 id
  if (n.type === 'ORDER' && n.bizId) {
    router.push('/orders')
  } else if (n.type === 'BARGAIN') {
    router.push('/bargains')
  } else if (n.type === 'AUDIT' && n.bizId) {
    router.push(`/products/${n.bizId}`)
  }
}

async function readAll() {
  await markAllNotificationsRead()
  notifyStore.clear()
  ElMessage.success('已全部标记为已读')
  load()
}

onMounted(() => {
  load()
  notifyStore.refresh()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.notice {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 12px;
  border-bottom: 1px dashed #eef0f4;
  cursor: pointer;
  border-radius: 8px;
}

.notice:hover {
  background: #f8faff;
}

.notice.unread {
  background: #f5f9ff;
}

.notice-main {
  flex: 1;
  min-width: 0;
}

.notice-title {
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ff4d4f;
  display: inline-block;
}

.notice-content {
  font-size: 13px;
  color: var(--text-sub);
  margin-top: 4px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
