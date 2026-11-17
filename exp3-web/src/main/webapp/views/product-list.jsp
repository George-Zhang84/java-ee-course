<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>商品广场</title>
    <script src="https://cdn.jsdelivr.net/npm/vue@3.4.38/dist/vue.global.prod.js"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.css">
    <script src="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.full.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/axios@1.7.7/dist/axios.min.js"></script>
    <script src="../static/js/common.js"></script>
    <style>
        body { margin: 0; padding: 24px; background-color: #f0f2f5; font-family: 'Segoe UI', sans-serif; }
        .filter-bar { background: #fff; padding: 16px 24px; border-radius: 12px; margin-bottom: 24px; display: flex; gap: 20px; align-items: center; box-shadow: 0 2px 12px rgba(0,0,0,0.04); }
        .product-card { margin-bottom: 20px; transition: all 0.3s ease; cursor: pointer; border: none; border-radius: 12px; }
        .product-card:hover { transform: translateY(-6px); box-shadow: 0 12px 24px rgba(0,0,0,0.1); }
        .price { color: #ff4d4f; font-size: 22px; font-weight: 700; }
        .price::before { content: '¥'; font-size: 16px; margin-right: 2px; }
        .product-title { font-size: 18px; font-weight: bold; margin: 0 0 12px 0; color: #333; }
        .product-desc { color: #888; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    </style>
</head>
<body>
<div id="app">
    <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="搜索你想要的商品..." style="width: 300px;" clearable @keyup.enter="handleSearch">
            <template #append><el-button icon="Search" @click="handleSearch"></el-button></template>
        </el-input>
        <el-select v-model="query.category" placeholder="全部分类" style="width: 150px;" @change="handleSearch">
            <el-option label="全部" value="全部"></el-option>
            <el-option label="电子" value="电子"></el-option>
            <el-option label="教材" value="教材"></el-option>
            <el-option label="生活" value="生活"></el-option>
            <el-option label="运动" value="运动"></el-option>
            <el-option label="其他" value="其他"></el-option>
        </el-select>
        <el-select v-model="query.sortBy" placeholder="默认排序" style="width: 150px;" @change="handleSearch">
            <el-option label="最新发布" value="default"></el-option>
            <el-option label="价格从低到高" value="price_asc"></el-option>
            <el-option label="价格从高到低" value="price_desc"></el-option>
        </el-select>
        <div style="margin-left: auto; color: #1e3c72; font-weight: bold; font-size: 16px; background: #e6f7ff; padding: 8px 16px; border-radius: 20px;" v-if="user">
            可用余额：<span style="color: #ff4d4f;">¥ [[ Number(user.balance || 0).toFixed(2) ]]</span>
        </div>
    </div>

    <div style="min-height: 400px;">
        <el-empty v-if="tableData.length === 0" description="没有找到相关商品"></el-empty>
        <el-row :gutter="24">
            <el-col :span="12" v-for="item in tableData" :key="item.id">
                <el-card class="product-card" @click="showDetail(item)" shadow="hover">
                    <h3 class="product-title">[[ item.name ]]</h3>
                    <div style="margin-bottom: 12px;">
                        <el-tag size="small" type="info" effect="plain">[[ item.category ]]</el-tag>
                        <span style="color: #999; font-size: 13px; margin-left: 10px;">卖家：[[ item.sellerName ]]</span>
                    </div>
                    <p class="product-desc">[[ item.description || '卖家很懒，什么都没写...' ]]</p>
                    <div style="margin-top: 20px; display: flex; justify-content: space-between; align-items: flex-end;">
                        <span class="price">[[ item.price ]]</span>
                        <span style="color: #bbb; font-size: 12px;">[[ item.createTime ]]</span>
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>

    <div style="display: flex; justify-content: center; margin-top: 30px;">
        <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="query.pageSize" :current-page="query.page" @current-change="handlePageChange"></el-pagination>
    </div>

    <!-- 商品详情弹窗 -->
    <el-dialog v-model="dialogVisible" title="商品详情" width="550px" style="border-radius: 12px;">
        <div v-if="currentProduct" style="padding: 10px;">
            <h2 style="margin-top: 0; color: #1e3c72;">[[ currentProduct.name ]]</h2>
            <div style="color: #666; margin-bottom: 20px;">
                <el-tag size="small">[[ currentProduct.category ]]</el-tag>
                <span style="margin-left: 12px;">卖家：[[ currentProduct.sellerName ]]</span>
            </div>
            <el-divider></el-divider>
            <p style="font-size: 15px; line-height: 1.8; color: #444;">
                [[ currentProduct.description || '暂无详细描述' ]]
            </p>
            <div style="margin-top: 30px; background: #fdf6ec; padding: 20px; border-radius: 8px; text-align: center;">
                <span class="price">[[ currentProduct.price ]]</span>
            </div>
        </div>
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="dialogVisible = false" size="large">取消</el-button>
                <el-button type="primary" v-if="user.roleCode === 'BUYER'" :loading="buyLoading" size="large" style="padding: 12px 30px;" @click="handleBuy">
                    立即购买
                </el-button>
            </span>
        </template>
    </el-dialog>
</div>

<script>
    const { createApp, ref, reactive, onMounted } = Vue;
    const { ElMessage } = ElementPlus;

    const app = createApp({
        setup() {
            const tableData = ref([]);
            const total = ref(0);

            const userStr = localStorage.getItem('user');
            const user = Vue.reactive(userStr ? JSON.parse(userStr) : { balance: 0, roleCode: '' });

            const query = reactive({ keyword: '', category: '全部', sortBy: 'default', page: 1, pageSize: 6 });

            const dialogVisible = ref(false);
            const currentProduct = ref(null);
            const buyLoading = ref(false);

            const loadData = async () => {
                try {
                    const data = await api.get('/product', {
                        params: {
                            action: 'list',
                            keyword: query.keyword,
                            category: query.category,
                            sortBy: query.sortBy,
                            page: query.page,
                            pageSize: query.pageSize
                        }
                    });
                    tableData.value = data.list || [];
                    total.value = data.total || 0;
                } catch (e) {
                    // 错误提示已由拦截器处理
                }
            };

            const handleSearch = () => { query.page = 1; loadData(); };
            const handlePageChange = (newPage) => { query.page = newPage; loadData(); };
            const showDetail = (item) => { currentProduct.value = item; dialogVisible.value = true; };

            const handleBuy = async () => {
                buyLoading.value = true;
                try {
                    const data = await api.post('/order?action=create', { productId: currentProduct.value.id });
                    ElMessage.success('下单成功！请前往"交易订单"查看');
                    // 后端返回的是数据库中的最新用户信息，直接同步到页面与 localStorage
                    Object.assign(user, data);
                    localStorage.setItem('user', JSON.stringify(data));
                    dialogVisible.value = false;
                    loadData();
                } catch (e) {
                    // 错误提示已由拦截器处理
                } finally {
                    buyLoading.value = false;
                }
            };

            onMounted(() => { loadData(); });
            return { tableData, total, query, user, handleSearch, handlePageChange, showDetail, dialogVisible, currentProduct, buyLoading, handleBuy };
        }
    });
    app.config.compilerOptions.delimiters = ['[[', ']]'];
    app.use(ElementPlus);
    app.mount('#app');
</script>
</body>
</html>