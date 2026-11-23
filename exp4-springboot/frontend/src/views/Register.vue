<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>注册新账号</h2>
      <p class="text-sub">注册后即可买也可卖：浏览、下单、议价，也能发布商品（发布后需管理员审核上架）</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="字母开头，4~20 位字母/数字/下划线" size="large" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="展示给其他用户的名字" size="large" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password size="large" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password size="large" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="手机号（选填）" prop="phone">
              <el-input v-model="form.phone" size="large" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱（选填）" prop="email">
              <el-input v-model="form.email" size="large" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-button type="primary" size="large" class="submit" :loading="loading" @click="onSubmit">
          注册并登录
        </el-button>
      </el-form>
      <div class="auth-footer">
        <span class="text-sub">已有账号？</span>
        <el-link type="primary" @click="router.push('/login')">返回登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]{3,19}$/, message: '字母开头，4~20 位字母/数字/下划线', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6~32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.register({
        username: form.username,
        nickname: form.nickname,
        password: form.password,
        phone: form.phone || undefined,
        email: form.email || undefined
      })
      ElMessage.success('注册成功，已自动登录')
      router.replace('/')
    } catch (e) {
      // 拦截器已提示
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
  width: 560px;
  max-width: 100%;
  padding: 36px 40px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 24px 60px rgba(47, 107, 255, 0.12);
}

.auth-card h2 {
  margin: 0 0 6px;
}

.submit {
  width: 100%;
  margin-top: 8px;
}

.auth-footer {
  margin-top: 16px;
  display: flex;
  gap: 6px;
  justify-content: center;
}
</style>
