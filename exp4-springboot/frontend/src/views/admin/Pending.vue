<template>
  <div class="page-container">
    <div class="section-title">商品审核</div>

    <el-card shadow="never">
      <el-radio-group v-model="status" class="status-filter" @change="reload">
        <el-radio-button label="PENDING">待审核</el-radio-button>
        <el-radio-button label="ON_SALE">在售</el-radio-button>
        <el-radio-button label="REJECTED">已驳回</el-radio-button>
        <el-radio-button label="">全部</el-radio-button>
      </el-radio-group>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="商品" min-width="280">
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
        <el-table-column label="卖家" width="120" prop="sellerNickname" />
        <el-table-column label="价格" width="110">
          <template #default="{ row }"><span class="price">{{ money(row.price) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="PRODUCT_TAG[row.status] || 'info'" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button size="small" type="primary" @click="openAudit(row, true)">通过</el-button>
              <el-button size="small" type="danger" plain @click="openAudit(row, false)">驳回</el-button>
            </template>
            <el-button
              v-if="row.status === 'ON_SALE'"
              size="small"
              text
              type="danger"
              @click="offline(row)"
            >
              强制下架
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

    <el-dialog v-model="auditVisible" :title="auditForm.pass ? '审核通过' : '驳回商品'" width="440px">
      <div class="audit-target">{{ currentTitle }}</div>
      <el-form label-width="80px">
        <el-form-item label="审核意见" :required="!auditForm.pass">
          <el-input
            v-model="auditForm.remark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            :placeholder="auditForm.pass ? '选填，例如：图片清晰，信息完整' : '请说明驳回原因，卖家会收到通知'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button :type="auditForm.pass ? 'primary' : 'danger'" :loading="auditing" @click="submitAudit">
          确认{{ auditForm.pass ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditProduct, forceOffline, listAdminProducts } from '@/api/admin'
import { PRODUCT_TAG, dateTime, money } from '@/utils/format'

const router = useRouter()
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('PENDING')
const loading = ref(false)
const auditVisible = ref(false)
const auditing = ref(false)
const currentId = ref(null)
const currentTitle = ref('')
const auditForm = reactive({ pass: true, remark: '' })

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
    if (status.value) params.status = status.value
    const res = await listAdminProducts(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

function openAudit(row, pass) {
  currentId.value = row.id
  currentTitle.value = row.title
  auditForm.pass = pass
  auditForm.remark = ''
  auditVisible.value = true
}

async function submitAudit() {
  if (!auditForm.pass && !auditForm.remark.trim()) {
    ElMessage.warning('驳回时请填写原因')
    return
  }
  auditing.value = true
  try {
    await auditProduct(currentId.value, { pass: auditForm.pass, remark: auditForm.remark || undefined })
    ElMessage.success(auditForm.pass ? '已通过，商品上架' : '已驳回并通知卖家')
    auditVisible.value = false
    load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    auditing.value = false
  }
}

async function offline(row) {
  try {
    await ElMessageBox.confirm(`确定强制下架「${row.title}」吗？卖家会收到通知。`, '强制下架', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await forceOffline(row.id)
    ElMessage.success('已强制下架')
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

onMounted(load)
</script>

<style scoped>
.status-filter {
  margin-bottom: 16px;
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
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-target {
  margin-bottom: 12px;
  font-weight: 600;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
