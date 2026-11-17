<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>用户管理</title>
    <script src="https://cdn.jsdelivr.net/npm/vue@3.4.38/dist/vue.global.prod.js"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.css">
    <script src="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.full.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/axios@1.7.7/dist/axios.min.js"></script>
    <script src="../static/js/common.js"></script>
    <style>
        body { margin: 0; padding: 24px; background-color: #f0f2f5; font-family: 'Segoe UI', sans-serif; }
        .el-card { border-radius: 12px; border: none; box-shadow: 0 4px 16px rgba(0,0,0,0.04); }
    </style>
</head>
<body>
<div id="app">
    <el-card>
        <template #header><span style="font-weight: bold; font-size: 18px;">全站用户管理</span></template>

        <!-- 搜索栏 -->
        <div style="margin-bottom: 20px; display: flex; gap: 15px;">
            <el-input v-model="searchKeyword" placeholder="搜索账号名..." style="width: 250px;" clearable @keyup.enter="loadData">
                <template #append><el-button icon="Search" @click="loadData"></el-button></template>
            </el-input>
            <el-select v-model="searchRole" placeholder="全部分角色" style="width: 150px;" clearable @change="loadData">
                <el-option label="超级管理员" value="SUPER_ADMIN"></el-option>
                <el-option label="管理员" value="ADMIN"></el-option>
                <el-option label="卖家" value="SELLER"></el-option>
                <el-option label="买家" value="BUYER"></el-option>
            </el-select>
        </div>

        <el-table :data="tableData" stripe style="width: 100%">
            <el-table-column prop="username" label="账号名"></el-table-column>
            <el-table-column prop="roleCode" label="角色" width="140">
                <template #default="scope">
                    <el-tag v-if="scope.row.roleCode === 'SUPER_ADMIN'" type="danger" effect="dark">超级管理员</el-tag>
                    <el-tag v-else-if="scope.row.roleCode === 'ADMIN'" type="warning">管理员</el-tag>
                    <el-tag v-else-if="scope.row.roleCode === 'SELLER'" type="success">卖家</el-tag>
                    <el-tag v-else>买家</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="balance" label="账户余额" width="120">
                <template #default="scope"><span style="color: #ff4d4f; font-weight: bold;">¥[[ scope.row.balance ]]</span></template>
            </el-table-column>
            <el-table-column prop="shopName" label="店铺名">
                <template #default="scope">[[ scope.row.shopName || '-' ]]</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
                <template #default="scope">
                    <el-tag v-if="scope.row.status === 1" type="success" effect="plain">正常</el-tag>
                    <el-tag v-else type="danger" effect="plain">已禁用</el-tag>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right" align="center">
                <template #default="scope">
                    <el-switch
                            v-model="scope.row.status"
                            :active-value="1"
                            :inactive-value="0"
                            :disabled="scope.row.username === currentUser.username"
                            active-text="启用"
                            inactive-text="禁用"
                            :before-change="() => handleStatusChange(scope.row)">
                    </el-switch>
                </template>
            </el-table-column>
        </el-table>
    </el-card>
</div>

<script>
    const { createApp, ref, onMounted } = Vue;
    const { ElMessage } = ElementPlus;
    const app = createApp({
        setup() {
            const tableData = ref([]);
            // 防止 localStorage 被清空时 JSON.parse(null) 得到 null 导致模板报错
            const currentUser = JSON.parse(localStorage.getItem('user') || 'null') || {};
            const searchKeyword = ref('');
            const searchRole = ref('');

            // 注意：@change 会把新选中的值作为第一个参数传进来。
            // 不能依赖 searchRole.value（Element Plus 的 change 事件可能在 v-model 更新前触发），
            // 也不要在 JSP 里写 ES 模板字符串的美元符号占位符——JSP 会把它当 EL 表达式在服务端吃掉。
            const loadData = async (roleArg) => {
                const roleCode = typeof roleArg === 'string' ? roleArg : searchRole.value;
                try {
                    const data = await api.get('/user', {
                        params: {
                            action: 'list',
                            keyword: searchKeyword.value,
                            roleCode: roleCode
                        }
                    });
                    tableData.value = data || [];
                } catch (e) {
                    // 错误提示已由 axios 拦截器统一处理
                }
            };

            const handleStatusChange = async (row) => {
                const newStatus = row.status === 1 ? 0 : 1;
                try {
                    await api.post('/user?action=updateStatus', { userId: row.id, status: newStatus });
                    ElMessage.success(newStatus === 1 ? '已启用该用户' : '已禁用该用户');
                    return true;
                } catch (e) {
                    // 后端拦截报错时，拒绝 UI 改变
                    return false;
                }
            };

            onMounted(() => { loadData(); });
            return { tableData, currentUser, searchKeyword, searchRole, loadData, handleStatusChange };
        }
    });
    app.config.compilerOptions.delimiters = ['[[', ']]'];
    app.use(ElementPlus);
    app.mount('#app');
</script>
</body>
</html>
