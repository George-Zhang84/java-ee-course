<template>
  <div class="page-container">
    <div class="section-title">审计日志</div>

    <el-card shadow="never">
      <el-tabs v-model="tab" @tab-change="reload">
        <el-tab-pane label="操作日志" name="audit" />
        <el-tab-pane label="登录日志" name="login" />
      </el-tabs>

      <div class="filter-bar">
        <template v-if="tab === 'audit'">
          <el-input v-model="auditQuery.module" placeholder="模块，如 商品/订单" clearable style="width: 180px" />
          <el-input v-model="auditQuery.username" placeholder="操作人" clearable style="width: 150px" />
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </template>
        <el-input v-else v-model="loginQuery.username" placeholder="用户名" clearable style="width: 180px" />
        <el-button type="primary" @click="reload">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table v-if="tab === 'audit'" v-loading="loading" :data="auditList" stripe>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="110" />
        <el-table-column prop="module" label="模块" width="90" />
        <el-table-column prop="action" label="动作" width="130" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ row.result }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="80">
          <template #default="{ row }">{{ row.costMs }} ms</template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="请求" min-width="220">
          <template #default="{ row }">
            <div class="uri">{{ row.uri }}</div>
            <div class="params">{{ row.params }}</div>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else v-loading="loading" :data="loginList" stripe>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="msg" label="说明" width="180" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="userAgent" label="User-Agent" min-width="240" show-overflow-tooltip />
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
import { onMounted, reactive, ref } from 'vue'
import { listAuditLogs, listLoginLogs } from '@/api/admin'
import { dateTime } from '@/utils/format'

const tab = ref('audit')
const loading = ref(false)
const auditList = ref([])
const loginList = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const dateRange = ref(null)

const auditQuery = reactive({ module: '', username: '' })
const loginQuery = reactive({ username: '' })

async function load() {
  loading.value = true
  try {
    if (tab.value === 'audit') {
      const params = { page: page.value, size: size.value }
      if (auditQuery.module) params.module = auditQuery.module
      if (auditQuery.username) params.username = auditQuery.username
      if (dateRange.value?.length === 2) {
        params.startDate = dateRange.value[0]
        params.endDate = dateRange.value[1]
      }
      const res = await listAuditLogs(params)
      auditList.value = res.data?.records || []
      total.value = res.data?.total || 0
    } else {
      const params = { page: page.value, size: size.value }
      if (loginQuery.username) params.username = loginQuery.username
      const res = await listLoginLogs(params)
      loginList.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

function reset() {
  auditQuery.module = ''
  auditQuery.username = ''
  loginQuery.username = ''
  dateRange.value = null
  reload()
}

onMounted(load)
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.uri {
  font-size: 12px;
  color: #374151;
}

.params {
  font-size: 12px;
  color: #9aa4b2;
  word-break: break-all;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
