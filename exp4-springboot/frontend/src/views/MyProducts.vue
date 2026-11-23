<template>
  <div class="page-container">
    <div class="toolbar">
      <div class="section-title">我发布的商品</div>
      <el-button type="primary" @click="router.push('/publish')">
        <el-icon><Plus /></el-icon>
        <span class="ml4">发布商品</span>
      </el-button>
    </div>

    <el-card shadow="never">
      <el-tabs v-model="activeStatus" @tab-change="load">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待审核" name="PENDING" />
        <el-tab-pane label="在售" name="ON_SALE" />
        <el-tab-pane label="已下架" name="OFF_SHELF" />
        <el-tab-pane label="已售出" name="SOLD" />
        <el-tab-pane label="已驳回" name="REJECTED" />
      </el-tabs>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="cell-product" @click="router.push(`/products/${row.id}`)">
              <img :src="row.coverUrl || placeholder" class="cell-cover" @error="onImgError" />
              <div>
                <div class="cell-title">{{ row.title }}</div>
                <div class="text-sub">{{ row.categoryName }} · {{ row.conditionLevel }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="110">
          <template #default="{ row }"><span class="price">{{ money(row.price) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="PRODUCT_TAG[row.status] || 'info'" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="views" label="浏览" width="80" />
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'ON_SALE'" size="small" text type="warning" @click="changeStatus(row, 'OFF_SHELF')">
              下架
            </el-button>
            <el-button v-if="row.status === 'OFF_SHELF'" size="small" text type="primary" @click="changeStatus(row, 'ON_SALE')">
              重新上架
            </el-button>
            <el-button
              v-if="['PENDING', 'REJECTED', 'OFF_SHELF'].includes(row.status)"
              size="small"
              text
              @click="router.push({ path: '/publish', query: { id: row.id } })"
            >
              编辑
            </el-button>
            <el-button
              v-if="!['LOCKED', 'SOLD'].includes(row.status)"
              size="small"
              text
              type="danger"
              @click="onDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
import { changeProductStatus, deleteProduct, listMyProducts } from '@/api/product'
import { PRODUCT_TAG, dateTime, money } from '@/utils/format'

const router = useRouter()
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const activeStatus = ref('')

const placeholder =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80"><rect width="100%" height="100%" fill="%23eef1f6"/></svg>'

function onImgError(event) {
  if (event.target.dataset.fallback) return
  event.target.dataset.fallback = '1'
  event.target.src = placeholder
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (activeStatus.value) params.status = activeStatus.value
    const res = await listMyProducts(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function changeStatus(row, status) {
  try {
    await changeProductStatus(row.id, status)
    ElMessage.success(status === 'ON_SALE' ? '已重新上架' : '已下架')
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」吗？删除后不可恢复。`, '删除商品', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteProduct(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    // 拦截器已提示（交易中的商品不能删除）
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ml4 {
  margin-left: 4px;
}

.cell-product {
  display: flex;
  gap: 10px;
  align-items: center;
  cursor: pointer;
}

.cell-cover {
  width: 56px;
  height: 42px;
  object-fit: cover;
  border-radius: 6px;
  background: #eef1f6;
}

.cell-title {
  font-size: 14px;
  font-weight: 600;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
