<template>
  <div v-loading="loading" class="page-container">
    <template v-if="product">
      <el-breadcrumb separator="/" class="crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/products' }">商品市场</el-breadcrumb-item>
        <el-breadcrumb-item>{{ product.title }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="detail-card">
        <div class="gallery">
          <img class="main-img" :src="currentImage" :alt="product.title" />
          <div v-if="images.length > 1" class="thumbs">
            <img
              v-for="(img, idx) in images"
              :key="idx"
              :src="img"
              :class="['thumb', { active: idx === currentIndex }]"
              @click="currentIndex = idx"
            />
          </div>
        </div>

        <div class="info">
          <h1>{{ product.title }}</h1>
          <div class="tag-row">
            <el-tag size="small" type="info" effect="plain">{{ product.conditionLevel }}</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ product.categoryName }}</el-tag>
            <el-tag v-if="product.allowBargain === 1" size="small" type="warning" effect="plain">支持议价</el-tag>
            <el-tag size="small" :type="PRODUCT_TAG[product.status] || 'info'" effect="dark">
              {{ product.statusText }}
            </el-tag>
          </div>

          <div class="price-box">
            <span class="price big">{{ money(product.price) }}</span>
            <span class="text-sub">浏览 {{ product.views }} 次</span>
          </div>

          <el-descriptions :column="2" class="desc">
            <el-descriptions-item label="交易地点">{{ product.location || '校内自提' }}</el-descriptions-item>
            <el-descriptions-item label="发布时间">{{ dateTime(product.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="卖家">{{ product.sellerNickname }}</el-descriptions-item>
            <el-descriptions-item label="信用分">
              <el-tooltip
                content="初始 100 分：好评（4~5 星）+1，差评（1~2 星）-2，区间 0~150。由订单评价自动计算"
                placement="top"
              >
                <span class="credit">{{ product.sellerCreditScore }}<el-icon class="q"><QuestionFilled /></el-icon></span>
              </el-tooltip>
            </el-descriptions-item>
            <!-- 底价只对卖家本人可见：买家看不到，避免被压到底价成交 -->
            <el-descriptions-item v-if="product.owner && product.minPrice" label="我的底价">
              <span class="price">{{ money(product.minPrice) }}</span>
              <el-tag size="small" type="warning" effect="plain" class="ml6">仅你可见</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <div class="actions">
            <el-button
              type="primary"
              size="large"
              :disabled="product.status !== 'ON_SALE'"
              :loading="buying"
              @click="onBuy"
            >
              立即购买
            </el-button>
            <el-button
              v-if="product.allowBargain === 1"
              type="warning"
              size="large"
              plain
              :disabled="product.status !== 'ON_SALE'"
              @click="openOffer"
            >
              我要砍价
            </el-button>
            <el-button size="large" @click="toggleFavorite">
              <el-icon><StarFilled v-if="product.favorited" color="#ffb400" /><Star v-else /></el-icon>
              <span class="ml6">{{ product.favorited ? '已收藏' : '收藏' }}</span>
            </el-button>
          </div>

          <el-alert
            v-if="product.status === 'SOLD'"
            type="info"
            :closable="false"
            title="该商品已售出"
            class="mt12"
          />
          <el-alert
            v-if="product.owner"
            type="warning"
            :closable="false"
            title="这是你发布的商品，可前往「我的商品」管理"
            class="mt12"
          />
          <el-alert
            v-if="product.auditRemark"
            type="error"
            :closable="false"
            :title="'审核意见：' + product.auditRemark"
            class="mt12"
          />
        </div>
      </div>

      <div class="section-title">商品描述</div>
      <div class="block">
        <p class="desc-text">{{ product.description || '卖家没有填写描述' }}</p>
      </div>

      <div class="section-title">买家评价（{{ reviewTotal }}）</div>
      <div class="block">
        <div v-for="r in reviews" :key="r.id" class="review-item">
          <div class="review-head">
            <span class="review-user">{{ r.reviewerNickname }}</span>
            <el-rate :model-value="r.score" disabled size="small" />
            <span class="text-sub">{{ dateTime(r.createTime) }}</span>
          </div>
          <p class="review-content">{{ r.content || '（无文字评价）' }}</p>
        </div>
        <el-empty v-if="!reviews.length" description="还没有评价" :image-size="80" />
      </div>
    </template>

    <el-dialog v-model="offerVisible" title="发起还价" width="440px">
      <div class="offer-tip">
        <div>
          商品原价：<span class="price">{{ money(product?.price) }}</span>
        </div>
        <div class="text-sub">
          你的出价必须低于原价。若卖家设置过底价且你的出价达到底价，系统会<strong>立即自动成交</strong>并生成待支付订单；否则需要卖家手动确认。
        </div>
      </div>
      <el-form label-width="96px">
        <el-form-item label="我的出价">
          <el-input-number v-model="offerForm.offerPrice" :min="0.01" :precision="2" :step="5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="留言（选填）">
          <el-input v-model="offerForm.message" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offerVisible = false">取消</el-button>
        <el-button type="primary" :loading="offering" @click="submitOffer">发送报价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addFavorite, getProduct, listProductReviews, removeFavorite } from '@/api/product'
import { createOrder } from '@/api/order'
import { createOffer } from '@/api/bargain'
import { useUserStore } from '@/store/user'
import { PRODUCT_TAG, dateTime, money } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const product = ref(null)
const reviews = ref([])
const reviewTotal = ref(0)
const loading = ref(false)
const buying = ref(false)
const offering = ref(false)
const offerVisible = ref(false)
const currentIndex = ref(0)
const offerForm = reactive({ offerPrice: 1, message: '' })

const images = computed(() => {
  if (!product.value) return []
  const list = product.value.images?.length ? [...product.value.images] : []
  if (product.value.coverUrl && !list.includes(product.value.coverUrl)) list.unshift(product.value.coverUrl)
  return list
})

const currentImage = computed(() => images.value[currentIndex.value] || product.value?.coverUrl || placeholder)

const placeholder =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="600" height="450"><rect width="100%" height="100%" fill="%23eef1f6"/><text x="50%" y="50%" font-size="18" fill="%239aa4b2" text-anchor="middle">暂无图片</text></svg>'

function requireLogin() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return false
  }
  return true
}

async function onBuy() {
  if (!requireLogin()) return
  try {
    await ElMessageBox.confirm(
      `将以 ¥${money(product.value.price)} 购买「${product.value.title}」，订单创建后请在 15 分钟内完成支付。`,
      '确认购买',
      { type: 'warning', confirmButtonText: '创建订单' }
    )
  } catch (e) {
    return
  }
  buying.value = true
  try {
    const res = await createOrder(product.value.id)
    ElMessage.success('下单成功，请尽快支付')
    router.push({ path: '/orders', query: { highlight: res.data.id } })
  } catch (e) {
    // 拦截器已提示（如"商品刚被别人下单"）
    await load()
  } finally {
    buying.value = false
  }
}

function openOffer() {
  if (!requireLogin()) return
  offerForm.offerPrice = Math.max(Number((product.value.price * 0.9).toFixed(2)), 0.01)
  offerForm.message = ''
  offerVisible.value = true
}

async function submitOffer() {
  offering.value = true
  try {
    const res = await createOffer({
      productId: product.value.id,
      offerPrice: offerForm.offerPrice,
      message: offerForm.message || undefined
    })
    offerVisible.value = false
    const data = res.data || {}
    if (data.autoAccepted) {
      // 达到底价自动成交：直接引导去支付
      ElMessage.success(data.message || '已达到卖家底价，自动成交')
      router.push({ path: '/orders', query: { highlight: data.orderId } })
    } else {
      ElMessage.success(data.message || '报价已发送，等待卖家回复')
    }
    load()
  } catch (e) {
    // 拦截器已提示（如次数上限、还有待处理报价）
  } finally {
    offering.value = false
  }
}

async function toggleFavorite() {
  if (!requireLogin()) return
  try {
    if (product.value.favorited) {
      await removeFavorite(product.value.id)
      product.value.favorited = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(product.value.id)
      product.value.favorited = true
      ElMessage.success('已加入收藏')
    }
  } catch (e) {
    // 拦截器已提示
  }
}

async function load() {
  loading.value = true
  try {
    const res = await getProduct(route.params.id)
    product.value = res.data
    const reviewRes = await listProductReviews(route.params.id, { page: 1, size: 10 })
    reviews.value = reviewRes.data?.records || []
    reviewTotal.value = reviewRes.data?.total || 0
  } catch (e) {
    // 商品不存在或无权查看
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.crumb {
  margin-bottom: 14px;
}

.detail-card {
  display: grid;
  grid-template-columns: 460px 1fr;
  gap: 28px;
  padding: 22px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid #eef0f4;
}

.main-img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  border-radius: 10px;
  background: #eef1f6;
}

.thumbs {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.thumb {
  width: 66px;
  height: 50px;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
  border: 2px solid transparent;
  opacity: 0.7;
}

.thumb.active {
  border-color: var(--brand);
  opacity: 1;
}

.info h1 {
  margin: 0 0 10px;
  font-size: 22px;
  line-height: 1.4;
}

.tag-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.price-box {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 14px 16px;
  background: #fff7f0;
  border-radius: 10px;
  margin-bottom: 14px;
}

.price.big {
  font-size: 26px;
}

.desc {
  margin-bottom: 16px;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ml6 {
  margin-left: 6px;
}

.credit {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.q {
  color: #9aa4b2;
  font-size: 13px;
}

.mt12 {
  margin-top: 12px;
}

.block {
  background: #fff;
  border-radius: 12px;
  padding: 18px;
  border: 1px solid #eef0f4;
}

.desc-text {
  margin: 0;
  line-height: 1.9;
  white-space: pre-wrap;
}

.review-item {
  padding: 12px 0;
  border-bottom: 1px dashed #eef0f4;
}

.review-item:last-child {
  border-bottom: none;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.review-user {
  font-weight: 600;
  font-size: 14px;
}

.review-content {
  margin: 0;
  font-size: 14px;
  color: #333;
  line-height: 1.7;
}

.offer-tip {
  margin-bottom: 14px;
  font-size: 14px;
}

@media (max-width: 900px) {
  .detail-card {
    grid-template-columns: 1fr;
  }
}
</style>
