<template>
  <div class="page-container">
    <div class="section-title">
      我的订单
      <span class="text-sub total-tip">共 {{ total }} 单</span>
    </div>

    <el-card shadow="never">
      <el-tabs v-model="role" @tab-change="reload">
        <el-tab-pane label="我买到的" name="buyer" />
        <el-tab-pane label="我卖出的" name="seller" />
      </el-tabs>

      <el-radio-group v-model="status" class="status-filter" @change="reload">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="UNPAID">待支付</el-radio-button>
        <el-radio-button label="PAID">已支付</el-radio-button>
        <el-radio-button label="SHIPPED">已发货</el-radio-button>
        <el-radio-button label="COMPLETED">已完成</el-radio-button>
        <el-radio-button label="CANCELLED">已取消</el-radio-button>
      </el-radio-group>

      <div v-loading="loading" class="order-list">
        <!-- 用卡片式列表而非 el-table：订单信息较多，卡片能同时展示商品、金额、
             时间线与操作按钮，移动端也能自适应 -->
        <div v-for="o in list" :key="o.id" class="order-item">
          <img
            :src="o.productCover || placeholder"
            class="order-cover"
            alt=""
            @click="router.push(`/products/${o.productId}`)"
            @error="onImgError"
          />
          <div class="order-main">
            <div class="order-title" @click="router.push(`/products/${o.productId}`)">
              {{ o.productTitle }}
            </div>
            <div class="order-meta">
              <span class="mono">{{ o.orderNo }}</span>
              <el-tag size="small" effect="plain" type="info">{{ o.originText }}</el-tag>
            </div>
            <div class="order-meta text-sub">
              {{ role === 'buyer' ? '卖家：' + (o.sellerNickname || '-') : '买家：' + (o.buyerNickname || '-') }}
              <span class="divider">|</span>
              下单于 {{ dateTime(o.createTime) }}
            </div>
            <div v-if="o.cancelReason" class="order-cancel">取消原因：{{ o.cancelReason }}</div>
          </div>

          <div class="order-side">
            <div class="price big">{{ money(o.amount) }}</div>
            <el-tag :type="ORDER_TAG[o.status] || 'info'" size="small">{{ o.statusText }}</el-tag>
            <div class="order-actions">
              <el-button v-if="o.canPay" type="primary" size="small" @click="pay(o)">去支付</el-button>
              <el-button v-if="o.canShip" type="primary" size="small" @click="ship(o)">发货</el-button>
              <el-button v-if="o.canConfirm" type="success" size="small" @click="confirm(o)">确认收货</el-button>
              <el-button v-if="o.canCancel" size="small" text type="danger" @click="cancel(o)">取消订单</el-button>
              <el-button
                v-if="o.status === 'COMPLETED' && !o.reviewed"
                size="small"
                text
                type="warning"
                @click="openReview(o)"
              >
                评价
              </el-button>
              <span v-if="o.status === 'COMPLETED' && o.reviewed" class="text-sub">已评价</span>
            </div>
          </div>
        </div>

        <el-empty v-if="!list.length" description="暂无订单" />
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

    <el-dialog v-model="reviewVisible" title="评价订单" width="460px">
      <el-form label-width="72px">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.score" />
        </el-form-item>
        <el-form-item label="评价">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="说说交易体验，帮助其他同学判断"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, confirmOrder, listOrders, payOrder, shipOrder } from '@/api/order'
import { createReview } from '@/api/wallet'
import { ORDER_TAG, dateTime, money } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const role = ref('buyer')
const status = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const reviewVisible = ref(false)
const reviewing = ref(false)
const reviewForm = reactive({ orderId: null, score: 5, content: '' })

const placeholder =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="96" height="72"><rect width="100%" height="100%" fill="%23eef1f6"/></svg>'

/** 图片 404 回退占位图，避免出现浏览器默认裂图 */
function onImgError(event) {
  if (event.target.dataset.fallback) return
  event.target.dataset.fallback = '1'
  event.target.src = placeholder
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value, role: role.value }
    if (status.value) params.status = status.value
    const res = await listOrders(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

async function pay(row) {
  try {
    await ElMessageBox.confirm(`确认支付 ¥${money(row.amount)}？将从钱包余额扣除。`, '确认支付', {
      type: 'warning',
      confirmButtonText: '立即支付'
    })
  } catch (e) {
    return
  }
  try {
    await payOrder(row.id)
    ElMessage.success('支付成功')
    load()
  } catch (e) {
    // 余额不足等提示由拦截器统一处理
  }
}

async function ship(row) {
  try {
    await shipOrder(row.id)
    ElMessage.success('已发货，等待买家确认')
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

async function confirm(row) {
  try {
    await confirmOrder(row.id)
    ElMessage.success('已确认收货，交易完成')
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

async function cancel(row) {
  try {
    await ElMessageBox.confirm('取消后商品会重新上架，确定取消该订单吗？', '取消订单', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await cancelOrder(row.id)
    ElMessage.success('订单已取消')
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

function openReview(row) {
  reviewForm.orderId = row.id
  reviewForm.score = 5
  reviewForm.content = ''
  reviewVisible.value = true
}

async function submitReview() {
  reviewing.value = true
  try {
    await createReview(reviewForm)
    reviewVisible.value = false
    ElMessage.success('评价成功')
    load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    reviewing.value = false
  }
}

onMounted(() => {
  if (route.query.highlight) {
    ElMessage.info('订单已创建，请在 15 分钟内支付，超时将自动取消')
  }
  load()
})
</script>

<style scoped>
.status-filter {
  margin-bottom: 8px;
}

.total-tip {
  margin-left: 10px;
  font-weight: 400;
}

.order-list {
  min-height: 120px;
}

.order-item {
  display: flex;
  gap: 14px;
  padding: 16px 4px;
  border-bottom: 1px dashed #eef0f4;
}

.order-item:last-child {
  border-bottom: none;
}

.order-cover {
  width: 96px;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
  background: #eef1f6;
  cursor: pointer;
  flex-shrink: 0;
}

.order-main {
  flex: 1;
  min-width: 0;
}

.order-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-meta {
  font-size: 12px;
  color: var(--text-sub);
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.order-cancel {
  font-size: 12px;
  color: #dc2626;
}

.order-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
  min-width: 150px;
}

.price.big {
  font-size: 18px;
}

.order-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.mono {
  font-family: Consolas, monospace;
}

.divider {
  color: #dfe3ea;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
