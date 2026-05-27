import App from './App.vue'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import './styles/main.css'
import { router } from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

const bar = document.createElement('div')
bar.id = 'oa-progress-bar'
bar.style.cssText = 'position:fixed;top:0;left:0;height:3px;background:var(--oa-primary);z-index:99999;transition:width 0.3s ease;width:0'
document.body.appendChild(bar)

let progress = 0
let timer: ReturnType<typeof setTimeout> | null = null

function startProgress() {
  progress = 0
  bar.style.width = '0%'
  bar.style.opacity = '1'
  timer = setInterval(() => {
    if (progress < 90) {
      progress += 10
      bar.style.width = progress + '%'
    }
  }, 100)
}

function finishProgress() {
  if (timer) clearInterval(timer)
  progress = 100
  bar.style.width = '100%'
  setTimeout(() => {
    bar.style.opacity = '0'
    setTimeout(() => {
      bar.style.width = '0%'
    }, 300)
  }, 200)
}

router.beforeEach(() => startProgress())
router.afterEach(() => finishProgress())
router.onError(() => finishProgress())

app.mount('#app')
