<template>
  <div class="page-container">
    <div class="section-title">用户管理</div>

    <el-card shadow="never">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="搜索用户名或昵称" clearable style="width: 220px" @keyup.enter="reload" />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="reload">
          <el-option label="正常" :value="1" />
          <el-option label="已禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="reload">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="28">{{ (row.nickname || 'U').slice(0, 1) }}</el-avatar>
              <div>
                <div>{{ row.nickname }}</div>
                <div class="text-sub">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="170">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" size="small" class="role-tag" effect="plain">
              {{ r }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="110">
          <template #default="{ row }">¥{{ money(row.balance) }}</template>
        </el-table-column>
        <el-table-column prop="creditScore" label="信用分" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.status === 1 ? 'danger' : 'success'"
              text
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" text type="warning" @click="resetPwd(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>

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
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changeUserStatus, listAdminUsers, resetUserPassword } from '@/api/admin'
import { dateTime, money } from '@/utils/format'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', status: null, page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== null && query.status !== undefined) params.status = query.status
    const res = await listAdminUsers(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  query.page = 1
  load()
}

async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      target === 0
        ? `禁用后「${row.nickname}」将立即无法登录，确定吗？`
        : `确定恢复「${row.nickname}」的账号吗？`,
      '修改账号状态',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await changeUserStatus(row.id, target)
    ElMessage.success(target === 1 ? '已启用' : '已禁用')
    load()
  } catch (e) {
    // 拦截器已提示（不能禁自己/超管）
  }
}

async function resetPwd(row) {
  try {
    await ElMessageBox.confirm(`确定重置「${row.nickname}」的密码吗？`, '重置密码', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    const res = await resetUserPassword(row.id)
    // 明文密码只在此刻返回一次，用弹窗展示给管理员
    ElMessageBox.alert(`新密码：${res.data.password}`, '重置成功', {
      confirmButtonText: '我已记录',
      type: 'success'
    })
  } catch (e) {
    // 拦截器已提示
  }
}

onMounted(load)
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.user-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}

.role-tag {
  margin-right: 4px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
