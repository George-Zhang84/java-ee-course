// 全局 Axios 配置
const API_BASE = '/exp3/api'; // 与 Tomcat 部署上下文 /exp3 保持一致
const APP_CONTEXT = API_BASE.replace(/\/api\/?$/, '');

const api = axios.create({
    baseURL: API_BASE,
    timeout: 10000
});

let redirectingToLogin = false;

// 401 统一跳登录页。页面可能运行在 iframe 里，所以让最外层窗口跳转，
// 并且必须使用应用上下文路径（原来的 'login.jsp' 会跳到 /exp3/views/login.jsp 导致 404）。
function redirectToLogin(message) {
    if (redirectingToLogin) return;
    redirectingToLogin = true;
    ElMessage.error(message || '登录已过期，请重新登录');
    setTimeout(function () {
        window.top.location.href = APP_CONTEXT + '/login.jsp';
    }, 600);
}

// 响应拦截器：统一处理后端返回的错误码
api.interceptors.response.use(
    response => {
        const res = response.data;
        if (res && res.code === 200) {
            return res.data; // 直接返回核心数据
        }
        if (res && res.code === 401) {
            redirectToLogin(res.message);
            return Promise.reject(res);
        }
        ElMessage.error((res && res.message) || '操作失败');
        return Promise.reject(res);
    },
    error => {
        const response = error.response;
        if (response && response.status === 401) {
            redirectToLogin();
            return Promise.reject(error);
        }
        const message = response && response.data && (response.data.message || response.data.error);
        ElMessage.error(message || '网络错误，请检查后端是否启动');
        return Promise.reject(error);
    }
);
