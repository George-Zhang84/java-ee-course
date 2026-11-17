<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>我的发布</title>
    <script src="https://cdn.jsdelivr.net/npm/vue@3.4.38/dist/vue.global.prod.js"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.css">
    <script src="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.full.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/axios@1.7.7/dist/axios.min.js"></script>
    <script src="../static/js/common.js"></script>
    <style>body { margin: 0; padding: 20px; background-color: #f0f2f5; font-family: sans-serif; }</style>
</head>
<body>
<div id="app">
    <el-card>
        <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: bold; font-size: 16px;">我的发布</span>
                <el-button type="primary" @click="dialogVisible = true">发布新商品</el-button>
            </div>
        </template>
        <el-table :data="tableData" stripe style="width: 100%">
            <el-table-column prop="name" label="商品名称"></el-table-column>
            <el-table-column prop="category" label="分类" width="100"></el-table-column>
            <el-table-column prop="price" label="价格" width="100">
                <template #default="scope"><span style="color: #f56c6c;">¥[[ scope.row.price ]]</span></template>
            </el-table-column>
            <el-table-column label="状态" width="120">
                <template #default="scope">
                    <el-tag v-if="scope.row.status === 'PENDING'" type="warning">待审核</el-tag>
                    <el-tag v-else-if="scope.row.status === 'ON_SALE'" type="success">在售中</el-tag>
                    <el-tag v-else-if="scope.row.status === 'SOLD'" type="info">已售出</el-tag>
                    <el-tag v-else-if="scope.row.status === 'OFFLINE'" type="danger">已下架</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="createTime" label="发布时间" width="180"></el-table-column>
        </el-table>
    </el-card>

    <!-- 发布商品弹窗 -->
    <el-dialog v-model="dialogVisible" title="发布新商品" width="500px">
        <el-form :model="form" label-width="80px">
            <el-form-item label="商品名称"><el-input v-model="form.name" placeholder="请输入名称"></el-input></el-form-item>
            <el-form-item label="商品分类">
                <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%;">
                    <el-option label="电子" value="电子"></el-option>
                    <el-option label="教材" value="教材"></el-option>
                    <el-option label="生活" value="生活"></el-option>
                    <el-option label="运动" value="运动"></el-option>
                    <el-option label="其他" value="其他"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item label="价格(元)"><el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%;"></el-input-number></el-form-item>
            <el-form-item label="商品描述"><el-input type="textarea" v-model="form.description" :rows="3" placeholder="描述一下成色等"></el-input></el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handlePublish" :loading="loading">确认发布</el-button>
        </template>
    </el-dialog>
</div>

<script>
    const { createApp, ref, reactive, onMounted } = Vue;
    const { ElMessage } = ElementPlus;
    const app = createApp({
        setup() {
            const tableData = ref([]);
            const dialogVisible = ref(false);
            const loading = ref(false);
            const form = reactive({ name: '', category: '', price: 0, description: '' });

            const loadData = async () => {
                try {
                    const data = await api.get('/product?action=myList');
                    tableData.value = data || [];
                } catch (e) {
                    // 错误提示已由拦截器处理
                }
            };

            const handlePublish = async () => {
                if (!form.name || !form.category || form.price <= 0) return ElMessage.warning('请填写完整商品信息');
                loading.value = true;
                try {
                    await api.post('/product?action=create', form);
                    ElMessage.success('发布成功，等待管理员审核');
                    dialogVisible.value = false;
                    Object.assign(form, { name: '', category: '', price: 0, description: '' });
                    loadData();
                } catch (e) {} finally { loading.value = false; }
            };

            onMounted(() => { loadData(); });
            return { tableData, dialogVisible, form, loading, handlePublish };
        }
    });
    app.config.compilerOptions.delimiters = ['[[', ']]'];
    app.use(ElementPlus);
    app.mount('#app');
</script>
</body>
</html>