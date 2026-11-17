<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>收到的评价</title>
    <script src="https://cdn.jsdelivr.net/npm/vue@3.4.38/dist/vue.global.prod.js"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.css">
    <script src="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.full.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/axios@1.7.7/dist/axios.min.js"></script>
    <script src="../static/js/common.js"></script>
    <style>body { margin: 0; padding: 24px; background-color: #f0f2f5; font-family: 'Segoe UI', sans-serif; }
    .el-card { border-radius: 12px; border: none; box-shadow: 0 4px 16px rgba(0,0,0,0.04); }</style>
</head>
<body>
<div id="app">
    <el-card>
        <template #header><span style="font-weight: bold; font-size: 16px;">我收到的评价</span></template>
        <el-table :data="tableData" stripe style="width: 100%">
            <el-table-column prop="orderId" label="订单号" width="180"></el-table-column>
            <el-table-column prop="rating" label="评分" width="150">
                <template #default="scope"><el-rate v-model="scope.row.rating" disabled></el-rate></template>
            </el-table-column>
            <el-table-column prop="content" label="评价内容"></el-table-column>
            <el-table-column prop="createTime" label="评价时间" width="180"></el-table-column>
        </el-table>
    </el-card>
</div>
<script>
    const { createApp, ref, onMounted } = Vue;
    const app = createApp({
        setup() {
            const tableData = ref([]);
            const user = JSON.parse(localStorage.getItem('user') || 'null') || { id: '' };
            onMounted(async () => {
                try {
                    const data = await api.get('/review', { params: { action: 'list', sellerId: user.id } });
                    tableData.value = data || [];
                } catch (e) {
                    // 错误提示已由拦截器处理
                }
            });
            return { tableData };
        }
    });
    app.config.compilerOptions.delimiters = ['[[', ']]'];
    app.use(ElementPlus);
    app.mount('#app');
</script>
</body>
</html>