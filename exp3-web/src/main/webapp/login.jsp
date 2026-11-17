<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>系统登录 - 校园二手交易</title>
    <script src="https://cdn.jsdelivr.net/npm/vue@3.4.38/dist/vue.global.prod.js"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.css">
    <script src="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.full.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/axios@1.7.7/dist/axios.min.js"></script>
    <script src="static/js/common.js"></script>
    <style>
        body {
            margin: 0; padding: 0; height: 100vh;
            display: flex; justify-content: center; align-items: center;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
        }
        .login-card {
            width: 400px;
            border-radius: 12px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.2);
            border: none;
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
        }
        .login-btn {
            background: linear-gradient(90deg, #409EFF, #79bbff);
            border: none;
            transition: transform 0.2s;
        }
        .login-btn:hover { transform: scale(1.02); }
    </style>
</head>
<body>
<div id="app">
    <el-card class="login-card">
        <template #header>
            <div style="text-align: center; font-size: 24px; font-weight: bold; color: #303133; margin-top: 10px;">
                校园二手交易系统
            </div>
        </template>
        <el-form label-position="top" style="padding: 0 10px;">
            <el-form-item label="账号">
                <el-input v-model="form.username" placeholder="请输入用户名" size="large"></el-input>
            </el-form-item>
            <el-form-item label="密码">
                <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large" @keyup.enter="handleLogin"></el-input>
            </el-form-item>
            <el-form-item style="margin-top: 20px;">
                <el-button type="primary" class="login-btn" style="width: 100%; font-size: 16px;" size="large" :loading="loading" @click="handleLogin">登 录</el-button>
            </el-form-item>
            <div style="text-align: right; font-size: 14px; color: #909399;">
                没有账号？<a href="register.jsp" style="color: #409EFF; text-decoration: none; font-weight: bold;">去注册</a>
            </div>
        </el-form>
    </el-card>
</div>
<script>
    const { createApp, ref, reactive } = Vue;
    const { ElMessage } = ElementPlus;
    const app = createApp({
        setup() {
            const form = reactive({ username: '', password: '' });
            const loading = ref(false);
            const handleLogin = async () => {
                if (!form.username || !form.password) return ElMessage.warning('请输入账号和密码');
                loading.value = true;
                try {
                    const data = await api.post('/auth?action=login', {
                        username: form.username.trim(),
                        password: form.password
                    });
                    // 先清掉上一个账号可能残留的缓存，避免权限串号
                    localStorage.clear();
                    localStorage.setItem('user', JSON.stringify(data.user));
                    localStorage.setItem('permissions', JSON.stringify(data.permissions));
                    ElMessage.success('登录成功');
                    window.location.href = 'main.jsp';
                } catch (e) {
                    // 错误提示已由拦截器处理
                } finally {
                    loading.value = false;
                }
            };
            return { form, loading, handleLogin };
        }
    });
    app.config.compilerOptions.delimiters = ['[[', ']]'];
    app.use(ElementPlus);
    app.mount('#app');
</script>
</body>
</html>