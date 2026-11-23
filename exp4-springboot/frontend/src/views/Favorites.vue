<template>
  <div class="page-container">
    <div class="section-title">我的收藏</div>

    <div v-loading="loading">
      <div v-if="list.length" class="product-grid">
        <div v-for="p in list" :key="p.id" class="product-card">
          <img class="cover-img" :src="p.coverUrl || placeholder" :alt="p.title" @click="router.push(`/products/${p.id}`)" />
          <div class="product-body">
            <div class="product-title" @click="router.push(`/products/${p.id}`)">{{ p.title }}</div>
            <div class="product-meta">
              <span class="price">{{ money(p.price) }}</span>
              <el-tag :type="PRODUCT_TAG[p.status] || 'info'" size="small">{{ p.statusText }}</el-tag>
            </div>
            <div class="product-sub">
              <span>{{ p.sellerNickname }}</span>
              <el-button size="small" text type="danger" @click="onRemove(p)">取消收藏</el-button>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="还没有收藏任何商品" />
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listFavorites, removeFavorite } from '@/api/product'
import { PRODUCT_TAG, money } from '@/utils/format'

const router = useRouter()
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)
const loading = ref(false)

const placeholder =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300"><rect width="100%" height="100%" fill="%23eef1f6"/></svg>'

async function load() {
  loading.value = true
  try {
    const res = await listFavorites({ page: page.value, size: size.value })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function onRemove(p) {
  try {
    await removeFavorite(p.id)
    ElMessage.success('已取消收藏')
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

onMounted(load)
</script>

<style scoped>
.product-card {
  background: #fff;
  border-radius: var(--card-radius);
  overflow: hidden;
  border: 1px solid #eef0f4;
}

.product-card img {
  cursor: pointer;
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
  cursor: pointer;
}

.product-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 4px;
}

.product-sub {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-sub);
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
