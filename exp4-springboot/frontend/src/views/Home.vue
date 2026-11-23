<template>
  <div>
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-text">
          <h1>校园闲置，遇见它的下一位主人</h1>
          <p>教材、数码、运动装备——同校交易，见面自提，价格好商量。</p>
          <div class="hero-actions">
            <el-button type="primary" size="large" @click="router.push('/products')">去逛逛</el-button>
            <el-button size="large" plain @click="goPublish">我要卖东西</el-button>
          </div>
        </div>
        <div class="hero-stats">
          <div v-for="item in stats" :key="item.label" class="stat">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
        </div>
      </div>
    </section>

    <div class="page-container">
      <div class="section-title">按分类逛逛</div>
      <div class="category-row">
        <div
          v-for="c in categories"
          :key="c.id"
          class="category-chip"
          @click="router.push({ path: '/products', query: { categoryId: c.id } })"
        >
          <el-icon :size="22"><component :is="iconOf(c.icon)" /></el-icon>
          <span>{{ c.name }}</span>
        </div>
      </div>

      <div class="section-title">
        最新上架
        <el-link class="more" type="primary" :underline="false" @click="router.push('/products')">
          查看全部 →
        </el-link>
      </div>

      <div v-loading="loading">
        <div v-if="products.length" class="product-grid">
          <div v-for="p in products" :key="p.id" class="product-card" @click="router.push(`/products/${p.id}`)">
            <img class="cover-img" :src="p.coverUrl || placeholder" :alt="p.title" @error="onImgError" />
            <div class="product-body">
              <div class="product-title">{{ p.title }}</div>
              <div class="product-meta">
                <span class="price">{{ money(p.price) }}</span>
                <el-tag size="small" type="info" effect="plain">{{ p.conditionLevel }}</el-tag>
              </div>
              <div class="product-sub">
                <span>{{ p.categoryName }}</span>
                <span>{{ p.sellerNickname }}</span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="还没有在售商品" />
      </div>

      <div class="section-title">平台特色</div>
      <div class="feature-grid">
        <div v-for="f in features" :key="f.title" class="feature">
          <el-icon :size="26" color="#2f6bff"><component :is="f.icon" /></el-icon>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listCategories, listProducts } from '@/api/product'
import { money } from '@/utils/format'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const categories = ref([])
const products = ref([])
const loading = ref(false)

const placeholder =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300"><rect width="100%" height="100%" fill="%23eef1f6"/><text x="50%" y="50%" font-size="16" fill="%239aa4b2" text-anchor="middle">暂无图片</text></svg>'

const stats = [
  { label: '在售商品', value: '实时' },
  { label: '校内自提', value: '0 运费' },
  { label: '支持议价', value: '可小刀' }
]

const features = [
  { icon: 'ChatDotRound', title: '结构化议价', desc: '出价、接受、拒绝全程留痕，24 小时未处理自动过期' },
  { icon: 'Timer', title: '超时自动取消', desc: '未支付订单 15 分钟后自动释放商品，不占库存' },
  { icon: 'Lock', title: '资金安全', desc: '钱包余额变动均有流水，扣款与入账在同一事务内完成' }
]

function iconOf(name) {
  return name || 'Goods'
}

/** 图片 404 时回退到占位图，避免出现浏览器默认的裂图图标 */
function onImgError(event) {
  if (event.target.dataset.fallback) return
  event.target.dataset.fallback = '1'
  event.target.src = placeholder
}

function goPublish() {
  if (!userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: '/publish' } })
    return
  }
  router.push('/publish')
}

onMounted(async () => {
  loading.value = true
  try {
    const [cateRes, prodRes] = await Promise.all([
      listCategories(),
      listProducts({ page: 1, size: 8, sort: 'newest' })
    ])
    categories.value = cateRes.data || []
    products.value = prodRes.data?.records || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.hero {
  background: linear-gradient(135deg, #2f6bff 0%, #4b8bff 60%, #7fb0ff 100%);
  color: #fff;
}

.hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 46px 16px;
  display: flex;
  justify-content: space-between;
  gap: 28px;
  flex-wrap: wrap;
}

.hero-text h1 {
  margin: 0 0 12px;
  font-size: 30px;
}

.hero-text p {
  margin: 0 0 22px;
  opacity: 0.92;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.hero-stats {
  display: flex;
  gap: 28px;
  align-items: center;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat strong {
  font-size: 20px;
}

.stat span {
  font-size: 12px;
  opacity: 0.85;
}

.category-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.category-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid #eef0f4;
}

.category-chip:hover {
  color: var(--brand);
  border-color: #c9daff;
  transform: translateY(-2px);
}

.product-card {
  background: #fff;
  border-radius: var(--card-radius);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid #eef0f4;
}

.product-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.product-body {
  padding: 12px;
}

.product-title {
  font-size: 14px;
  font-weight: 600;
  height: 40px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 4px;
}

.product-sub {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-sub);
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.feature {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #eef0f4;
}

.feature h3 {
  margin: 10px 0 6px;
  font-size: 15px;
}

.feature p {
  margin: 0;
  font-size: 13px;
  color: var(--text-sub);
  line-height: 1.7;
}
</style>
