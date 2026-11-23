<template>
  <div class="page-container">
    <div class="section-title">个人中心</div>

    <div class="profile-grid">
      <el-card shadow="never" class="profile-card">
        <div class="avatar-row">
          <el-avatar :size="64" :src="user.avatar">{{ (user.nickname || 'U').slice(0, 1) }}</el-avatar>
          <div>
            <div class="name">{{ user.nickname }}</div>
            <div class="text-sub">@{{ user.username }}</div>
            <div class="role-row">
              <el-tag v-for="r in user.roles" :key="r" size="small" effect="plain">{{ roleText(r) }}</el-tag>
            </div>
          </div>
        </div>

        <el-divider />

        <el-descriptions :column="1">
          <el-descriptions-item label="钱包余额">¥{{ money(user.balance) }}</el-descriptions-item>
          <el-descriptions-item label="信用分">
            {{ user.creditScore }}
            <span class="text-sub credit-tip">（初始 100，好评 +1、差评 -2，区间 0~150）</span>
          </el-descriptions-item>
          <el-descriptions-item label="手机号">{{ user.phone || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ user.email || '未填写' }}</el-descriptions-item>
        </el-descriptions>

        <div class="quick-links">
          <el-button text type="primary" @click="router.push('/wallet')">查看钱包</el-button>
          <el-button text type="primary" @click="router.push('/my-products')">我的商品</el-button>
          <el-button text type="primary" @click="router.push('/orders')">我的订单</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="profile-card">
        <div class="section-title" style="margin-top: 0">修改密码</div>
        <div class="text-sub mb16">修改成功后当前登录状态会失效，需要重新登录。</div>
        <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="changePwd">保存并重新登录</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 收到过的好评/差评（含卖家对买家的评价），解决"评价只在数据库里"的问题 -->
    <el-card shadow="never" class="profile-card reviews-card">
      <div class="section-title" style="margin-top: 0">我收到的评价</div>
      <div v-if="received.length">
        <div v-for="r in received" :key="r.id" class="review-row">
          <div class="review-head">
            <el-rate :model-value="r.score" disabled size="small" />
            <span class="text-sub">
              {{ r.reviewerRole === 'BUYER' ? '买家' : '卖家' }}评价 · {{ dateTime(r.createTime) }}
            </span>
          </div>
          <div class="review-content">{{ r.content || '（无文字评价）' }}</div>
        </div>
      </div>
      <el-empty v-else description="还没有收到评价" :image-size="70" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/auth'
import { listReceivedReviews } from '@/api/wallet'
import { useUserStore } from '@/store/user'
import { dateTime, money } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()
const user = reactive({})
const received = ref([])
const pwdRef = ref()
const submitting = ref(false)

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6~32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

function roleText(code) {
  return { SUPER_ADMIN: '超级管理员', ADMIN: '管理员', USER: '普通用户', SELLER: '卖家' }[code] || code
}

async function changePwd() {
  await pwdRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
      ElMessage.success('密码已修改，请重新登录')
      userStore.clearAuth()
      router.push('/login')
    } catch (e) {
      // 拦截器已提示
    } finally {
      submitting.value = false
    }
  })
}

onMounted(async () => {
  const info = await userStore.fetchMe()
  Object.assign(user, info)
  try {
    const res = await listReceivedReviews({ page: 1, size: 10 })
    received.value = res.data?.records || []
  } catch (e) {
    // 评价加载失败不影响个人中心主体信息
  }
})
</script>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.profile-card {
  border-radius: 12px;
}

.avatar-row {
  display: flex;
  gap: 16px;
  align-items: center;
}

.name {
  font-size: 18px;
  font-weight: 700;
}

.role-row {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.quick-links {
  margin-top: 12px;
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.mb16 {
  margin-bottom: 16px;
}

.credit-tip {
  margin-left: 6px;
}

.reviews-card {
  margin-top: 16px;
}

.review-row {
  padding: 10px 0;
  border-bottom: 1px dashed #eef0f4;
}

.review-row:last-child {
  border-bottom: none;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.review-content {
  font-size: 13px;
  color: #374151;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
