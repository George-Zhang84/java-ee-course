import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import './styles/main.css'

import App from './App.vue'
import router from './router'
import { permissionDirective } from './directives/permission'

const app = createApp(App)

// 图标全量注册：模板里可直接写 <el-icon><ShoppingCart /></el-icon>
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
// 中文语言包：分页、日期选择器等内置文案统一中文
app.use(ElementPlus, { locale: zhCn })
app.directive('permission', permissionDirective)

app.mount('#app')
