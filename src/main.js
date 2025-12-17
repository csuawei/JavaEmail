import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import App from './App.vue'
import VueRouter from 'vue-router'
import router from './router'
import axios from 'axios'


Vue.config.productionTip = false

Vue.prototype.$axios=axios

Vue.prototype.$httpUrl='http://localhost:8081'

Vue.use(ElementUI)
Vue.use(VueRouter)


new Vue({
  router,
  render: h => h(App),
}).$mount('#app')
