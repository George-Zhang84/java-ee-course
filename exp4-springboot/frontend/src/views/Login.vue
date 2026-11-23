<template>
  <div class="auth-page">
    <div class="auth-card">
      <section class="auth-intro">
        <h1>校园二手<br />交易平台</h1>
        <p>把闲置卖给需要的人。<br />发布、议价、支付、评价，一站完成。</p>
        <ul>
          <li>✔ 校内自提，见面交易更放心</li>
          <li>✔ 支持还价协商，价格透明</li>
          <li>✔ 全流程留痕，纠纷可追溯</li>
        </ul>
      </section>

      <section class="auth-form">
        <h2>登录</h2>
        <p class="text-sub">使用校园账号登录，演示密码均为 123456</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="onSubmit">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              @keyup.enter="onSubmit"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="onSubmit">
            登录
          </el-button>
        </el-form>

        <div class="auth-footer">
          <span class="text-sub">还没有账号？</span>
          <el-link type="primary" @click="router.push('/register')">立即注册</el-link>
        </div>

        <el-divider>演示账号</el-divider>
        <div class="demo-accounts">
          <el-tag v-for="acc in accounts" :key="acc.username" class="demo-tag" @click="fill(acc.username)">
            {{ acc.label }}：{{ acc.username }}
          </el-tag>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const accounts = [
  { label: '超管', username: 'admin' },
  { label: '管理员', username: 'manager' },
  { label: '买家', username: 'buyer1' },
  { label: '卖家', username: 'seller1' }
]

function fill(username) {
  form.username = username
  form.password = '123456'
}

async function onSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({ ...form })
      ElMessage.success('登录成功')
      router.replace(route.query.redirect || '/')
    } catch (e) {
      // 错误提示已由 axios 拦截器统一处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #eaf1ff 0%, #f7f9ff 45%, #fff 100%);
}

.auth-card {
  width: 900px;
  max-width: 100%;
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(47, 107, 255, 0.12);
}

.auth-intro {
  padding: 44px 38px;
  color: #fff;
  background: linear-gradient(160deg, #2f6bff 0%, #4b8bff 55%, #6ea6ff 100%);
  /* 内容整体垂直+水平居中，避免文字挤在左上角 */
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.auth-intro h1 {
  margin: 0 0 14px;
  font-size: 30px;
  line-height: 1.25;
}

.auth-intro p {
  margin: 0 0 24px;
  opacity: 0.92;
  line-height: 1.7;
  font-size: 14px;
  max-width: 300px;
}

.auth-intro ul {
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 13px;
  line-height: 2.1;
  opacity: 0.95;
  text-align: left;
}

.auth-form {
  padding: 44px 40px;
}

.auth-form h2 {
  margin: 0 0 6px;
  font-size: 24px;
}

.submit {
  width: 100%;
  margin-top: 6px;
}

.auth-footer {
  margin-top: 16px;
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
}

.demo-accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.demo-tag {
  cursor: pointer;
}

@media (max-width: 780px) {
  .auth-card {
    grid-template-columns: 1fr;
  }

  .auth-intro {
    padding: 28px;
  }
}
</style>
