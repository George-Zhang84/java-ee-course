<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>商品审核</title>
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
        <template #header><span style="font-weight: bold; font-size: 16px;">待审核商品列表</span></template>
        <el-empty v-if="tableData.length === 0" description="暂无待审核商品"></el-empty>
        <el-table v-else :data="tableData" stripe style="width: 100%">
            <el-table-column prop="name" label="商品名称"></el-table-column>
            <el-table-column prop="sellerName" label="卖家" width="120"></el-table-column>
            <el-table-column prop="category" label="分类" width="100"></el-table-column>
            <el-table-column prop="price" label="价格" width="100">
                <template #default="scope"><span style="color: #f56c6c;">¥[[ scope.row.price ]]</span></template>
            </el-table-column>
            <el-table-column prop="description" label="描述" show-overflow-tooltip></el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
                <template #default="scope">
                    <el-button type="success" size="small" @click="handleAudit(scope.row.id, 'ON_SALE')">通过</el-button>
                    <el-button type="danger" size="small" @click="handleAudit(scope.row.id, 'OFFLINE')">拒绝</el-button>
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

            const loadData = async () => {
                try {
                    const data = await api.get('/product?action=pendingList');
                    tableData.value = data || [];
                } catch (e) {
                    // 错误提示已由拦截器处理
                }
            };

            const handleAudit = async (productId, status) => {
                try {
                    await api.post('/product?action=audit', { productId, status });
                    ElMessage.success(status === 'ON_SALE' ? '已通过审核' : '已拒绝并下架');
                    loadData();
                } catch (e) {}
            };

            onMounted(() => { loadData(); });
            return { tableData, handleAudit };
        }
    });
    app.config.compilerOptions.delimiters = ['[[', ']]'];
    app.use(ElementPlus);
    app.mount('#app');
</script>
</body>
</html>