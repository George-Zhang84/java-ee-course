<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>评价监管</title>
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
        <template #header><span style="font-weight: bold; font-size: 18px;">全站评价监管</span></template>
        <el-table :data="tableData" stripe style="width: 100%">
            <el-table-column prop="orderId" label="订单号" width="160"></el-table-column>
            <el-table-column prop="productName" label="商品名称"></el-table-column>
            <el-table-column prop="buyerName" label="买家" width="120"></el-table-column>
            <el-table-column prop="rating" label="评分" width="150">
                <template #default="scope"><el-rate v-model="scope.row.rating" disabled></el-rate></template>
            </el-table-column>
            <el-table-column prop="content" label="评价内容" show-overflow-tooltip></el-table-column>
            <el-table-column prop="createTime" label="时间" width="180"></el-table-column>
            <el-table-column label="操作" width="120" fixed="right" align="center">
                <template #default="scope">
                    <el-button type="danger" size="small" plain @click="handleDelete(scope.row.id)">删除违规</el-button>
                </template>
            </el-table-column>
        </el-table>
    </el-card>
</div>
<script>
    const { createApp, ref, onMounted } = Vue;
    const { ElMessage, ElMessageBox } = ElementPlus;
    const app = createApp({
        setup() {
            const tableData = ref([]);

            const loadData = async () => {
                try {
                    const data = await api.get('/review?action=list');
                    tableData.value = data || [];
                } catch (e) {
                    // 错误提示已由拦截器处理
                }
            };

            const handleDelete = async (reviewId) => {
                try {
                    await ElMessageBox.confirm('确认要删除这条评价吗？删除后买家可以重新评价。', '警告', { type: 'warning' });
                    await api.post('/review?action=delete', { reviewId });
                    ElMessage.success('评价已删除');
                    loadData();
                } catch (e) {}
            };

            onMounted(() => { loadData(); });
            return { tableData, handleDelete };
        }
    });
    app.config.compilerOptions.delimiters = ['[[', ']]'];
    app.use(ElementPlus);
    app.mount('#app');
</script>
</body>
</html>