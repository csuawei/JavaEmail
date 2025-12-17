import VueRouter from "vue-router";
import login from "@/components/login.vue";
import Register from "@/components/Register.vue";
import Index from "@/components/Index.vue";
import WriteLetter from "@/components/WriteLetter.vue";
import Inbox from "@/components/Inbox.vue";
import Outbox from "@/components/Outbox.vue";
import Draft from "@/components/Draft.vue";
import Contact from "@/components/Contact.vue";
const routes = [
    {
        path:'/',
        redirect: '/login', 
    },
    {
        path:'/login',
        component: login
    },
    {
        path:'/register',
        component: Register
    },
    {
        path:'/index',
        component: Index,

        children:[
 // 写信路由
        {
          path: '/letter/write',
          name: 'WriteLetter',
          component: WriteLetter
        },
        // 收件箱路由（默认子路由）
        {
          path: '/letter/inbox',
          name: 'Inbox',
          component: Inbox
        },
        // 发件箱路由
        {
          path: '/letter/outbox',
          name: 'Outbox',
          component: Outbox
        },
        // 草稿箱路由
        {
          path: '/letter/draft',
          name: 'Draft',
          component: Draft
        },
        // 联系人路由
        {
          path: '/contact',
          name: 'Contact',
          component: Contact
        },
        {
        path: '/letter/detail', // 详情页子路由，与收件箱路由同级
        name: 'MailDetail',
        component: () => import('@/components/MailDetail.vue')
        }
        ]
    }
]

const router = new VueRouter(
    {
        mode:'history',
        routes
    }
)

// 导航守卫，前置处理
router.beforeEach((to, from, next) => {
    let isAuthenticated = !!sessionStorage.getItem('userInfo')
    // 如果路由要跳转到除了登录和注册的界面的话就判断是否已经登录，如果没有登录就强制跳到登录界面
    if (to.path !== '/login' && to.path !== '/register' && !isAuthenticated) {
        next({ path: '/login' })
        Message({
            message: '请先登录！',
            type: "warning",
        });
    } else next()
})

export default router;