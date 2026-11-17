<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>交易订单</title>
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
        <template #header><div style="font-weight: bold; font-size: 18px; color: #303133;">我的交易订单</div></template>
        <el-table :data="tableData" style="width: 100%" stripe>
            <el-table-column prop="id" label="订单号" width="220"></el-table-column>
            <el-table-column prop="productName" label="商品名称" min-width="150"></el-table-column>
            <el-table-column prop="amount" label="交易金额" width="120">
                <template #default="scope"><span style="color: #ff4d4f; font-weight: bold;">¥[[ scope.row.amount ]]</span></template>
            </el-table-column>
            <el-table-column prop="buyerName" label="买家" width="120"></el-table-column>
            <el-table-column prop="sellerName" label="卖家" width="120"></el-table-column>
            <el-table-column label="状态" width="130">
                <template #default="scope">
                    <el-tag v-if="scope.row.status === 'PENDING'" type="warning" effect="dark">待卖家确认</el-tag>
                    <el-tag v-else-if="scope.row.status === 'CONFIRMED'" type="primary" effect="plain">已确认(待收货)</el-tag>
                    <el-tag v-else-if="scope.row.status === 'RECEIVED'" type="success" effect="dark">已收货</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="createTime" label="下单时间" width="180"></el-table-column>
            <el-table-column label="操作" width="220" fixed="right" align="center">
                <template #default="scope">
                    <el-button v-if="user.roleCode === 'SELLER' && scope.row.status === 'PENDING'" type="primary" size="small" @click="handleUpdate(scope.row, 'CONFIRMED')">确认订单</el-button>
                    <el-button v-if="user.roleCode === 'BUYER' && scope.row.status === 'CONFIRMED'" type="success" size="small" @click="handleUpdate(scope.row, 'RECEIVED')">确认收货</el-button>
                    <el-button v-if="scope.row.reviewed" type="info" size="small" plain @click="openViewReview(scope.row.id)">查看评价</el-button>
                    <el-button v-else-if="user.roleCode === 'BUYER' && scope.row.status === 'RECEIVED'" type="warning" size="small" @click="openReview(scope.row)">去评价</el-button>
                </template>
            </el-table-column>
        </el-table>
    </el-card>

    <!-- 写评价弹窗 -->
    <el-dialog v-model="dialogVisible" title="评价本次交易" width="400px" style="border-radius: 12px;">
        <el-form label-position="top">
            <el-form-item label="商品评分">
                <el-rate v-model="reviewForm.rating" show-score text-color="#ff9900" score-template="{value} 星"></el-rate>
            </el-form-item>
            <el-form-item label="评价内容">
                <el-input type="textarea" v-model="reviewForm.content" :rows="4" maxlength="255" show-word-limit placeholder="描述一下商品的成色或卖家的服务态度吧..."></el-input>
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitReview" :loading="submitLoading">提交评价</el-button>
        </template>
    </el-dialog>

    <!-- 查看评价弹窗 -->
    <el-dialog v-model="viewDialogVisible" title="我的评价" width="400px" style="border-radius: 12px;">
        <div v-if="myReview" style="padding: 10px;">
            <p><strong>商品评分：</strong> <el-rate v-model="myReview.rating" disabled style="display: inline-block; vertical-align: middle;"></el-rate></p>
            <el-divider></el-divider>
            <p><strong>评价内容：</strong></p>
            <p style="background: #f5f7fa; padding: 10px; border-radius: 6px; color: #333;">[[ myReview.content ]]</p>
            <p style="color: #999; font-size: 12px; text-align: right; margin-top: 15px;">评价时间：[[ myReview.createTime ]]</p>
        </div>
        <template #footer>
            <el-button type="primary" @click="viewDialogVisible = false">关闭</el-button>
        </template>
    </el-dialog>
</div>

<script>
    const { createApp, ref, reactive, onMounted } = Vue;
    const { ElMessage } = ElementPlus;
    const app = createApp({
        setup() {
            const tableData = ref([]);
            const user = JSON.parse(localStorage.getItem('user') || 'null') || { roleCode: '' };

            const dialogVisible = ref(false);
            const submitLoading = ref(false);
            const reviewForm = reactive({ orderId: '', rating: 5, content: '' });

            // 查看评价相关状态
            const viewDialogVisible = ref(false);
            const myReview = ref(null);

            const loadData = async () => {
                try {
                    const data = await api.get('/order?action=list');
                    tableData.value = data || [];
                } catch (e) {
                    // 错误提示已由拦截器处理
                }
            };

            const handleUpdate = async (row, newStatus) => {
                try {
                    // 订单的真实状态由后端根据数据库判断，前端只传目标状态
                    await api.post('/order?action=updateStatus', { orderId: row.id, newStatus: newStatus });
                    ElMessage.success('操作成功');
                    loadData();
                } catch (e) {
                    // 错误提示已由拦截器处理
                }
            };

            const openReview = (row) => {
                reviewForm.orderId = row.id;
                reviewForm.rating = 5;
                reviewForm.content = '';
                dialogVisible.value = true;
            };

            const submitReview = async () => {
                if (!reviewForm.content || !reviewForm.content.trim()) return ElMessage.warning('请填写评价内容');
                submitLoading.value = true;
                try {
                    // sellerId 由后端根据订单反查，前端不再传，避免伪造评价对象
                    await api.post('/review?action=submit', {
                        orderId: reviewForm.orderId,
                        rating: reviewForm.rating,
                        content: reviewForm.content
                    });
                    ElMessage.success('感谢您的反馈！');
                    dialogVisible.value = false;
                    loadData(); // 刷新列表，自动变为已评价
                } catch (e) {
                    // 错误提示已由拦截器处理
                } finally {
                    submitLoading.value = false;
                }
            };

            // 查看评价逻辑
            const openViewReview = async (orderId) => {
                try {
                    const data = await api.get('/review', { params: { action: 'getByOrder', orderId: orderId } });
                    if (data) {
                        myReview.value = data;
                        viewDialogVisible.value = true;
                    } else {
                        ElMessage.error('未找到该评价记录');
                    }
                } catch (e) {
                    // 错误提示已由拦截器处理
                }
            };

            onMounted(() => { loadData(); });
            return {
                tableData, user, handleUpdate,
                dialogVisible, reviewForm, submitLoading, openReview, submitReview,
                viewDialogVisible, myReview, openViewReview
            };
        }
    });
    app.config.compilerOptions.delimiters = ['[[', ']]'];
    app.use(ElementPlus);
    app.mount('#app');
</script>
</body>
</html>
