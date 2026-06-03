import { createApp } from 'vue'
import {
  ElAlert,
  ElAside,
  ElButton,
  ElContainer,
  ElIcon,
  ElInput,
  ElLoading,
  ElMain,
  ElOption,
  ElSegmented,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus'
import {
  ChatDotRound,
  DataLine,
  Download,
  Histogram,
  Refresh,
  Search,
  Tickets,
  User,
  VideoCamera,
} from '@element-plus/icons-vue'
import 'element-plus/theme-chalk/base.css'
import 'element-plus/theme-chalk/el-alert.css'
import 'element-plus/theme-chalk/el-aside.css'
import 'element-plus/theme-chalk/el-button.css'
import 'element-plus/theme-chalk/el-container.css'
import 'element-plus/theme-chalk/el-icon.css'
import 'element-plus/theme-chalk/el-input.css'
import 'element-plus/theme-chalk/el-loading.css'
import 'element-plus/theme-chalk/el-main.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-option.css'
import 'element-plus/theme-chalk/el-popper.css'
import 'element-plus/theme-chalk/el-scrollbar.css'
import 'element-plus/theme-chalk/el-segmented.css'
import 'element-plus/theme-chalk/el-select.css'
import 'element-plus/theme-chalk/el-table.css'
import 'element-plus/theme-chalk/el-table-column.css'
import 'element-plus/theme-chalk/el-tag.css'
import App from './App.vue'
import './styles/main.css'

const app = createApp(App)

;[
  ElAlert,
  ElAside,
  ElButton,
  ElContainer,
  ElIcon,
  ElInput,
  ElMain,
  ElOption,
  ElSegmented,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
].forEach((component) => app.use(component))

app.use(ElLoading)

;[
  ChatDotRound,
  DataLine,
  Download,
  Histogram,
  Refresh,
  Search,
  Tickets,
  User,
  VideoCamera,
].forEach((component) => app.component(component.name, component))

app.mount('#app')
