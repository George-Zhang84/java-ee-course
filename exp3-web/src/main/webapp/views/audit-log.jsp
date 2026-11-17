<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>系统审计日志</title>
    <script src="https://cdn.jsdelivr.net/npm/vue@3.4.38/dist/vue.global.prod.js"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.css">
    <script src="https://cdn.jsdelivr.net/npm/element-plus@2.8.4/dist/index.full.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/axios@1.7.7/dist/axios.min.js"></script>
    <script src="../static/js/common.js"></script>
    <style>
        body { margin: 0; padding: 20px; background-color: #f0f2f5; font-family: sans-serif; }
        .log-box {
            background-color: #1e1e1e; color: #a9b7c6; font-family: Consolas, monospace;
            padding: 15px; border-radius: 8px; font-size: 13px; line-height: 1.5;
            height: 600px; overflow-y: auto;
        }
        .log-line { margin-bottom: 5px; white-space: pre-wrap; word-break: break-all; }
    </style>
</head>
<body>
<div id="app">
    <el-card>
        <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: bold; font-size: 16px;">系统审计日志 (最近 100 条)</span>
                <el-button type="primary" size="small" @click="loadLog">刷新日志</el-button>
            </div>
        </template>
        <div class="log-box" v-loading="loading">
            <el-empty v-if="logs.length === 0" description="暂无日志"></el-empty>
            <div class="log-line" v-for="(line, index) in logs" :key="index">[[ line ]]</div>
        </div>
    </el-card>
</div>
<script>
    const { createApp, ref, onMounted } = Vue;
    const { ElMessage } = ElementPlus;
    const app = createApp({
        setup() {
            const logs = ref([]);
            const loading = ref(false);

            const loadLog = async () => {
                loading.value = true;
                try {
                    const data = await api.get('/audit?action=readLog');
                    logs.value = data || [];
                } catch (e) {
                    // 错误提示已由拦截器处理
                } finally {
                    loading.value = false;
                }
            };

            onMounted(() => { loadLog(); });
            return { logs, loading, loadLog };
        }
    });
    app.config.compilerOptions.delimiters = ['[[', ']]'];
    app.use(ElementPlus);
    app.mount('#app');
</script>
</body>
</html>