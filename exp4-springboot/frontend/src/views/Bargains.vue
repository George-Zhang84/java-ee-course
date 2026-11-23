<template>
  <div class="page-container">
    <div class="section-title">议价消息</div>

    <el-card shadow="never">
      <el-tabs v-model="tab" @tab-change="load">
        <el-tab-pane label="我收到的" name="received" />
        <el-tab-pane label="我发出的" name="sent" />
      </el-tabs>

      <div v-loading="loading">
        <div v-for="o in list" :key="o.id" class="offer-item">
          <img :src="o.productCover || placeholder" class="offer-cover" @error="onImgError" @click="router.push(`/products/${o.productId}`)" />
          <div class="offer-main">
            <div class="offer-title" @click="router.push(`/products/${o.productId}`)">{{ o.productTitle }}</div>
            <div class="offer-line">
              <span class="text-sub">商品原价</span>
              <span class="strike">¥{{ money(o.productPrice) }}</span>
              <span class="text-sub ml">出价</span>
              <span class="price">{{ money(o.offerPrice) }}</span>
            </div>
            <div v-if="o.message" class="offer-msg">买家留言：{{ o.message }}</div>
            <div class="text-sub">
              {{ tab === 'received' ? '买家：' + o.buyerNickname : '卖家：' + o.sellerNickname }}
              · {{ fromNow(o.createTime) }}
              · 有效期至 {{ dateTime(o.expireTime) }}
            </div>
          </div>
          <div class="offer-side">
            <el-tag :type="OFFER_TAG[o.status] || 'info'" size="small">{{ o.statusText }}</el-tag>
            <div class="offer-actions">
              <template v-if="o.canHandle">
                <el-button type="primary" size="small" @click="accept(o)">接受</el-button>
                <el-button size="small" @click="reject(o)">拒绝</el-button>
              </template>
              <el-button v-if="o.canCancel" size="small" text type="danger" @click="cancel(o)">撤回</el-button>
            </div>
          </div>
        </div>
        <el-empty v-if="!list.length" :description="tab === 'received' ? '暂时没有人向你出价' : '你还没有发起过还价'" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { acceptOffer, cancelOffer, listReceivedOffers, listSentOffers, rejectOffer } from '@/api/bargain'
import { OFFER_TAG, dateTime, fromNow, money } from '@/utils/format'

const router = useRouter()
const tab = ref('received')
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const placeholder =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="96" height="72"><rect width="100%" height="100%" fill="%23eef1f6"/></svg>'

function onImgError(event) {
  if (event.target.dataset.fallback) return
  event.target.dataset.fallback = '1'
  event.target.src = placeholder
}

async function load() {
  loading.value = true
  try {
    const api = tab.value === 'received' ? listReceivedOffers : listSentOffers
    const res = await api({ page: page.value, size: size.value })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function accept(o) {
  try {
    await ElMessageBox.confirm(
      `接受后将以 ¥${money(o.offerPrice)} 成交，商品立即锁定并生成待支付订单。`,
      '接受该报价？',
      { type: 'warning', confirmButtonText: '接受并生成订单' }
    )
  } catch (e) {
    return
  }
  try {
    await acceptOffer(o.id)
    ElMessage.success('已接受报价，等待买家支付')
    load()
  } catch (e) {
    // 商品已售出等提示由拦截器处理
  }
}

async function reject(o) {
  try {
    await ElMessageBox.confirm('拒绝后买家可以重新出价，确定拒绝吗？', '拒绝报价', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await rejectOffer(o.id)
    ElMessage.success('已拒绝')
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

async function cancel(o) {
  try {
    await cancelOffer(o.id)
    ElMessage.success('已撤回')
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

onMounted(load)
</script>

<style scoped>
.offer-item {
  display: flex;
  gap: 14px;
  padding: 16px 0;
  border-bottom: 1px dashed #eef0f4;
}

.offer-item:last-child {
  border-bottom: none;
}

.offer-cover {
  width: 96px;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
  background: #eef1f6;
  cursor: pointer;
  flex-shrink: 0;
}

.offer-main {
  flex: 1;
  min-width: 0;
}

.offer-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.offer-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 14px;
}

.strike {
  text-decoration: line-through;
  color: var(--text-sub);
}

.ml {
  margin-left: 8px;
}

.offer-msg {
  font-size: 13px;
  color: #4b5563;
  margin-bottom: 4px;
}

.offer-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  flex-shrink: 0;
}

.offer-actions {
  display: flex;
  gap: 6px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
