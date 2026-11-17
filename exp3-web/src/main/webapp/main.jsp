<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>校园二手交易系统</title>
    <script src="https://cdn.jsdelivr.net/npm/vue@3.4.38/dist/vue.global.prod.js"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.css">
    <script src="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.full.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/axios@1.7.7/dist/axios.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@element-plus/icons-vue@2.3.1/dist/index.iife.min.js"></script>
    <script src="static/js/common.js"></script>
    <style>
        body { margin: 0; padding: 0; height: 100vh; overflow: hidden; }
        .el-container { height: 100vh; }
        .el-header {
            background-color: #fff;
            border-bottom: 1px solid #e6e6e6;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
            display: flex; justify-content: space-between; align-items: center;
            padding: 0 30px; z-index: 10;
        }
        .el-aside { background-color: #304156; color: #fff; box-shadow: 2px 0 6px rgba(0,0,0,0.1); z-index: 9;}
        .el-menu { border-right: none; background-color: #304156; padding: 10px 0; }
        .el-menu-item { color: #bfcbd9; margin: 4px 10px; border-radius: 6px; transition: all 0.3s; }
        .el-menu-item:hover { background-color: #263445; color: #fff; }
        .el-menu-item.is-active {
            color: #fff;
            background-color: #409EFF;
            box-shadow: 0 2px 8px rgba(64,158,255,0.4);
        }
        iframe { width: 100%; height: 100%; border: none; display: block; }
        .header-title { font-size: 22px; font-weight: 700; color: #303133; letter-spacing: 1px; }
    </style>
</head>
<body>
<div id="app">
    <el-container>
        <el-header>
            <div class="header-title">二手交易平台</div>
            <div style="display: flex; align-items: center;">
                <el-avatar size="small" style="background-color: #409EFF; margin-right: 10px;">[[ user.username.charAt(0).toUpperCase() ]]</el-avatar>
                <span style="font-size: 15px;">欢迎，<strong style="color: #409EFF;">[[ user.username ]]</strong> ([[ roleName ]])</span>
                <el-button type="danger" plain size="small" @click="handleLogout" style="margin-left: 20px;">退出登录</el-button>
            </div>
        </el-header>

        <el-container>
            <el-aside width="220px">
                <el-menu :default-active="activeMenu" @select="handleMenuSelect">
                    <el-menu-item index="views/welcome.jsp">
                        <el-icon><House /></el-icon><span>首页概览</span>
                    </el-menu-item>
                    <el-menu-item index="views/product-list.jsp" v-if="hasPerm('menu:product')">
                        <el-icon><Goods /></el-icon><span>商品广场</span>
                    </el-menu-item>
                    <el-menu-item index="views/my-products.jsp" v-if="user.roleCode === 'SELLER'">
                        <el-icon><Sell /></el-icon><span>我的发布</span>
                    </el-menu-item>
                    <el-menu-item index="views/order-list.jsp" v-if="hasPerm('menu:order')">
                        <el-icon><List /></el-icon><span>交易订单</span>
                    </el-menu-item>
                    <el-menu-item index="views/seller-reviews.jsp" v-if="user.roleCode === 'SELLER'">
                        <el-icon><Comment /></el-icon><span>收到评价</span>
                    </el-menu-item>
                    <el-menu-item index="views/admin-audit.jsp" v-if="hasPerm('menu:audit')">
                        <el-icon><Checked /></el-icon><span>商品审核</span>
                    </el-menu-item>
                    <el-menu-item index="views/admin-user.jsp" v-if="hasPerm('menu:user')">
                        <el-icon><User /></el-icon><span>用户管理</span>
                    </el-menu-item>
                    <el-menu-item index="views/admin-reviews.jsp" v-if="hasPerm('menu:user')">
                        <el-icon><Star /></el-icon><span>评价监管</span>
                    </el-menu-item>
                    <el-menu-item index="views/audit-log.jsp" v-if="hasPerm('menu:user')">
                        <el-icon><Document /></el-icon><span>日志审计</span>
                    </el-menu-item>
                </el-menu>
            </el-aside>

            <el-main style="padding: 0; background-color: #f0f2f5;">
                <iframe id="contentFrame" name="contentFrame" :src="activeMenu"></iframe>
            </el-main>
        </el-container>
    </el-container>
</div>
<script>
    const { createApp, ref, computed } = Vue;
    const { ElMessageBox } = ElementPlus;
    const { House, Goods, List, Sell, Checked, User, Star, Document, Comment } = ElementPlusIconsVue;

    const app = createApp({
        setup() {
            const userStr = localStorage.getItem('user');
            if (!userStr) {
                window.top.location.href = 'login.jsp';
            }
            const user = userStr
                ? JSON.parse(userStr)
                : { username: '', roleCode: '', balance: 0 };
            const permissions = JSON.parse(localStorage.getItem('permissions') || '[]');
            const roleMap = { 'SUPER_ADMIN': '超级管理员', 'ADMIN': '管理员', 'SELLER': '卖家', 'BUYER': '买家' };
            const roleName = computed(() => roleMap[user.roleCode] || user.roleCode || '未登录');

            const activeMenu = ref('views/welcome.jsp');
            const hasPerm = (permCode) => permissions.includes(permCode) || user.roleCode === 'SUPER_ADMIN';
            const handleMenuSelect = (index) => { activeMenu.value = index; };

            const handleLogout = () => {
                ElMessageBox.confirm('确定要退出当前账号吗?', '退出提示', { type: 'warning', confirmButtonText: '退出', cancelButtonText: '取消' }).then(async () => {
                    try {
                        await api.post('/auth?action=logout');
                    } catch (e) {
                        // 即使后端登出失败，也要清空本地状态
                    }
                    localStorage.clear();
                    window.top.location.href = 'login.jsp';
                }).catch(() => {});
            };
            return { user, roleName, activeMenu, hasPerm, handleMenuSelect, handleLogout };
        }
    });
    app.config.compilerOptions.delimiters = ['[[', ']]'];
    app.use(ElementPlus);
    for (const [key, component] of Object.entries(ElementPlusIconsVue)) { app.component(key, component); }
    app.mount('#app');
</script>
</body>
</html>