<template>
  <div class="page-container">
    <div class="section-title">我的钱包</div>

    <div class="stat-grid">
      <div class="stat-card primary">
        <div class="stat-label">可用余额</div>
        <div class="stat-value">¥{{ money(wallet.balance) }}</div>
        <el-button type="primary" size="small" class="recharge-btn" @click="rechargeVisible = true">充值</el-button>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计收入</div>
        <div class="stat-value income">¥{{ money(wallet.totalIncome) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计支出</div>
        <div class="stat-value expense">¥{{ money(wallet.totalExpense) }}</div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="section-title" style="margin-top: 0">资金流水</div>
      <el-table v-loading="loading" :data="logs" stripe>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag
              :type="['PAY'].includes(row.type) ? 'danger' : 'success'"
              size="small"
              effect="plain"
            >
              {{ row.typeText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140">
          <template #default="{ row }">
            <span :class="row.amount > 0 ? 'amount-in' : 'amount-out'">
              {{ row.amount > 0 ? '+' : '' }}{{ money(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动后余额" width="140">
          <template #default="{ row }">¥{{ money(row.balanceAfter) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="220" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="loadLogs"
        />
      </div>
    </el-card>

    <el-dialog v-model="rechargeVisible" title="钱包充值" width="400px">
      <div class="text-sub mb12">演示环境为模拟充值，不接入真实支付渠道。</div>
      <el-input-number v-model="amount" :min="0.01" :max="10000" :precision="2" :step="50" style="width: 100%" />
      <div class="quick-amounts">
        <el-button v-for="a in [100, 200, 500, 1000]" :key="a" size="small" @click="amount = a">¥{{ a }}</el-button>
      </div>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="doRecharge">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getWallet, listWalletLogs, recharge } from '@/api/wallet'
import { dateTime, money } from '@/utils/format'

const wallet = reactive({ balance: 0, totalIncome: 0, totalExpense: 0 })
const logs = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const rechargeVisible = ref(false)
const recharging = ref(false)
const amount = ref(100)

async function loadWallet() {
  const res = await getWallet()
  Object.assign(wallet, res.data)
}

async function loadLogs() {
  loading.value = true
  try {
    const res = await listWalletLogs({ page: page.value, size: size.value })
    logs.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function doRecharge() {
  recharging.value = true
  try {
    await recharge(amount.value)
    ElMessage.success('充值成功')
    rechargeVisible.value = false
    page.value = 1
    await Promise.all([loadWallet(), loadLogs()])
  } catch (e) {
    // 拦截器已提示
  } finally {
    recharging.value = false
  }
}

onMounted(() => {
  loadWallet()
  loadLogs()
})
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #eef0f4;
  position: relative;
}

.stat-card.primary {
  background: linear-gradient(135deg, #2f6bff, #5b93ff);
  color: #fff;
  border: none;
}

.stat-label {
  font-size: 13px;
  opacity: 0.85;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  margin-top: 8px;
}

.stat-card.primary .stat-value {
  color: #fff;
}

.income {
  color: #16a34a;
}

.expense {
  color: #dc2626;
}

.recharge-btn {
  position: absolute;
  right: 18px;
  bottom: 18px;
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.6);
}

.amount-in {
  color: #16a34a;
  font-weight: 600;
}

.amount-out {
  color: #dc2626;
  font-weight: 600;
}

.mb12 {
  margin-bottom: 12px;
}

.quick-amounts {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
