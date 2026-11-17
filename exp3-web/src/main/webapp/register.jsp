<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>注册 - 校园二手交易</title>
    <script src="https://cdn.jsdelivr.net/npm/vue@3.4.38/dist/vue.global.prod.js"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.css">
    <script src="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.full.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/axios@1.7.7/dist/axios.min.js"></script>
    <script src="static/js/common.js"></script>
    <style>
        body { margin: 0; padding: 0; background-color: #f0f2f5; height: 100vh; display: flex; justify-content: center; align-items: center; font-family: sans-serif; }
        .register-card { width: 450px; }
    </style>
</head>
<body>
<div id="app">
    <el-card class="register-card">
        <template #header>
            <div style="text-align: center; font-size: 20px; font-weight: bold; color: #409EFF;">
                注册新账号
            </div>
        </template>
        <el-form label-width="80px">
            <el-form-item label="账号">
                <el-input v-model="form.username" placeholder="请输入用户名"></el-input>
            </el-form-item>
            <el-form-item label="密码">
                <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password></el-input>
            </el-form-item>
            <el-form-item label="身份">
                <el-radio-group v-model="form.roleCode" @change="handleRoleChange">
                    <el-radio value="BUYER">我是买家 (送1000元测试金)</el-radio>
                    <el-radio value="SELLER">我是卖家</el-radio>
                </el-radio-group>
            </el-form-item>
            <el-form-item label="店铺名" v-if="form.roleCode === 'SELLER'">
                <el-input v-model="form.shopName" placeholder="请输入您的店铺名称"></el-input>
            </el-form-item>

            <el-form-item>
                <el-button type="primary" style="width: 100%;" :loading="loading" @click="handleRegister">注 册</el-button>
            </el-form-item>
            <div style="text-align: right; font-size: 14px;">
                已有账号？<a href="login.jsp" style="color: #409EFF; text-decoration: none;">去登录</a>
            </div>
        </el-form>
    </el-card>
</div>

<script>
    const { createApp, ref, reactive } = Vue;
    const { ElMessage } = ElementPlus;

    const app = createApp({
        setup() {
            const form = reactive({ username: '', password: '', roleCode: 'BUYER', shopName: '' });
            const loading = ref(false);

            const handleRoleChange = () => {
                if (form.roleCode === 'BUYER') form.shopName = '';
            };

            const handleRegister = async () => {
                if (!form.username || !form.password) return ElMessage.warning('请填写账号和密码');
                if (form.username.trim().length < 3) return ElMessage.warning('用户名至少 3 个字符');
                if (form.password.length < 6) return ElMessage.warning('密码至少 6 位');
                if (form.roleCode === 'SELLER' && !form.shopName) return ElMessage.warning('请填写店铺名称');

                loading.value = true;
                try {
                    await api.post('/auth?action=register', {
                        username: form.username.trim(),
                        password: form.password,
                        roleCode: form.roleCode,
                        shopName: form.shopName ? form.shopName.trim() : ''
                    });
                    ElMessage.success('注册成功，请登录');
                    setTimeout(() => { window.location.href = 'login.jsp'; }, 1000);
                } catch (e) {
                    // 错误提示已由拦截器处理
                } finally {
                    loading.value = false;
                }
            };

            return { form, loading, handleRegister, handleRoleChange };
        }
    });

    app.config.compilerOptions.delimiters = ['[[', ']]'];
    app.use(ElementPlus);
    app.mount('#app');
</script>
</body>
</html>