<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索商品标题或描述"
        clearable
        class="search"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
        <template #append>
          <el-button @click="search">搜索</el-button>
        </template>
      </el-input>

      <el-select
        v-if="categories.length"
        v-model="query.categoryId"
        placeholder="全部分类"
        clearable
        class="cate"
        @change="search"
      >
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-else placeholder="全部分类" class="cate" disabled />

      <el-select v-model="query.sort" class="sort" @change="search">
        <el-option label="最新发布" value="newest" />
        <el-option label="价格从低到高" value="price_asc" />
        <el-option label="价格从高到低" value="price_desc" />
        <el-option label="最多浏览" value="views" />
      </el-select>

      <div class="price-range">
        <el-input-number v-model="query.minPrice" :min="0" :controls="false" placeholder="最低价" />
        <span class="dash">-</span>
        <el-input-number v-model="query.maxPrice" :min="0" :controls="false" placeholder="最高价" />
        <el-button @click="search">确定</el-button>
      </div>

      <el-button text @click="reset">重置</el-button>
    </div>

    <div v-loading="loading" class="list-wrap">
      <div v-if="products.length" class="product-grid">
        <div v-for="p in products" :key="p.id" class="product-card" @click="router.push(`/products/${p.id}`)">
          <img class="cover-img" :src="p.coverUrl || placeholder" :alt="p.title" @error="onImgError" />
          <div class="product-body">
            <div class="product-title">{{ p.title }}</div>
            <div class="product-meta">
              <span class="price">{{ money(p.price) }}</span>
              <el-tag v-if="p.allowBargain === 1" size="small" type="warning" effect="plain">可议价</el-tag>
            </div>
            <div class="product-sub">
              <span>{{ p.categoryName }}</span>
              <span><el-icon><View /></el-icon> {{ p.views }}</span>
            </div>
            <div class="product-sub">
              <span>{{ p.conditionLevel }} · {{ p.location || '校内自提' }}</span>
              <span>{{ p.sellerNickname }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="没有找到符合条件的商品" />
    </div>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        background
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listCategories, listProducts } from '@/api/product'
import { money } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const categories = ref([])
const products = ref([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  keyword: '',
  // 初始为 null：等分类字典加载完再赋值，否则 el-select 匹配不到 option 会直接显示数字 id
  categoryId: null,
  minPrice: null,
  maxPrice: null,
  sort: 'newest',
  page: 1,
  size: 12
})

const placeholder =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300"><rect width="100%" height="100%" fill="%23eef1f6"/><text x="50%" y="50%" font-size="16" fill="%239aa4b2" text-anchor="middle">暂无图片</text></svg>'

async function load() {
  loading.value = true
  try {
    // 空值不传给后端，避免把 null 当成筛选条件
    const params = { page: query.page, size: query.size, sort: query.sort }
    if (query.keyword) params.keyword = query.keyword
    if (query.categoryId) params.categoryId = query.categoryId
    if (query.minPrice !== null && query.minPrice !== undefined) params.minPrice = query.minPrice
    if (query.maxPrice !== null && query.maxPrice !== undefined) params.maxPrice = query.maxPrice

    const res = await listProducts(params)
    products.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

/** 图片 404 回退占位图 */
function onImgError(event) {
  if (event.target.dataset.fallback) return
  event.target.dataset.fallback = '1'
  event.target.src = placeholder
}

function reset() {
  query.keyword = ''
  query.categoryId = null
  query.minPrice = null
  query.maxPrice = null
  query.sort = 'newest'
  search()
}

// 首页分类跳转过来时，同步筛选条件
watch(
  () => route.query.categoryId,
  (val) => {
    query.categoryId = val ? Number(val) : null
    query.page = 1
    load()
  }
)

onMounted(async () => {
  const res = await listCategories()
  categories.value = res.data || []
  // 分类字典到位后再恢复 URL 上的分类筛选，保证下拉框显示的是分类名而不是 id
  if (route.query.categoryId) {
    query.categoryId = Number(route.query.categoryId)
  }
  load()
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eef0f4;
  margin-bottom: 18px;
}

.search {
  width: 300px;
}

.cate {
  width: 140px;
}

.sort {
  width: 150px;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 6px;
}

.price-range :deep(.el-input-number) {
  width: 96px;
}

.dash {
  color: var(--text-sub);
}

.list-wrap {
  min-height: 320px;
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
  margin-top: 2px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
