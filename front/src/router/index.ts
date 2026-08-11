import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAdminStore } from '@/stores/admin'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/HomeView.vue'), meta: { title: '首页' } },
      { path: 'products', name: 'Products', component: () => import('@/views/ProductsView.vue'), meta: { title: '全部商品' } },
      { path: 'product/:id', name: 'ProductDetail', component: () => import('@/views/ProductDetailView.vue'), meta: { title: '商品详情' } },
      { path: 'cart', name: 'Cart', component: () => import('@/views/CartView.vue'), meta: { title: '购物车', requiresAuth: true } },
      { path: 'favorites', name: 'Favorites', component: () => import('@/views/FavoritesView.vue'), meta: { title: '我的收藏', requiresAuth: true } },
      { path: 'publish', name: 'Publish', component: () => import('@/views/PublishView.vue'), meta: { title: '发布商品', requiresAuth: true } },
      { path: 'publish/:id', name: 'PublishEdit', component: () => import('@/views/PublishView.vue'), meta: { title: '编辑商品', requiresAuth: true } },
      { path: 'message-center', name: 'MessageCenter', component: () => import('@/views/MessageCenterView.vue'), meta: { title: '消息中心', requiresAuth: true } },
      { path: 'chat/:userId?', name: 'Chat', component: () => import('@/views/ChatView.vue'), meta: { title: '私信', requiresAuth: true } },
      { path: 'user-center', name: 'UserCenter', component: () => import('@/views/UserCenterView.vue'), meta: { title: '个人中心', requiresAuth: true } },
      { path: 'account-settings', name: 'AccountSettings', component: () => import('@/views/AccountSettingsView.vue'), meta: { title: '账号设置', requiresAuth: true } },
      { path: 'addresses', name: 'Addresses', component: () => import('@/views/AddressesView.vue'), meta: { title: '收货地址', requiresAuth: true } },
      { path: 'follow', name: 'Follow', component: () => import('@/views/FollowView.vue'), meta: { title: '关注与粉丝', requiresAuth: true } },
      { path: 'my-products', name: 'MyProducts', component: () => import('@/views/MyProductsView.vue'), meta: { title: '我的发布', requiresAuth: true } },
      { path: 'order-confirm', name: 'OrderConfirm', component: () => import('@/views/OrderConfirmView.vue'), meta: { title: '确认订单', requiresAuth: true } },
      { path: 'order/:id', name: 'OrderDetail', component: () => import('@/views/OrderDetailView.vue'), meta: { title: '订单详情', requiresAuth: true } },
      { path: 'user-home/:userId', name: 'UserHome', component: () => import('@/views/UserHomeView.vue'), meta: { title: '用户主页' } },
    ],
  },
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue'), meta: { title: '用户登录' } },
  { path: '/register', name: 'Register', component: () => import('@/views/RegisterView.vue'), meta: { title: '用户注册' } },
  { path: '/forget-password', name: 'ForgetPassword', component: () => import('@/views/ForgetPasswordView.vue'), meta: { title: '忘记密码' } },
  { path: '/admin/login', name: 'AdminLogin', component: () => import('@/views/admin/AdminLoginView.vue'), meta: { title: '管理员登录' } },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAdminAuth: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/DashboardView.vue'), meta: { title: '数据概览' } },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UsersView.vue'), meta: { title: '用户管理' } },
      { path: 'products', name: 'AdminProducts', component: () => import('@/views/admin/ProductsView.vue'), meta: { title: '商品管理' } },
      { path: 'orders', name: 'AdminOrders', component: () => import('@/views/admin/OrdersView.vue'), meta: { title: '订单管理' } },
      { path: 'categories', name: 'AdminCategories', component: () => import('@/views/admin/CategoriesView.vue'), meta: { title: '分类管理' } },
      { path: 'system-images', name: 'AdminSystemImages', component: () => import('@/views/admin/SystemImagesView.vue'), meta: { title: '轮播图管理' } },
      { path: 'messages', name: 'AdminMessages', component: () => import('@/views/admin/MessagesView.vue'), meta: { title: '系统消息' } },
      { path: 'admins', name: 'AdminAdmins', component: () => import('@/views/admin/AdminsView.vue'), meta: { title: '管理员管理' } },
    ],
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFoundView.vue'), meta: { title: '页面不存在' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const adminStore = useAdminStore()

  const isAdminRoute = to.path.startsWith('/admin') && to.name !== 'AdminLogin'

  document.title = (to.meta.title ? `${to.meta.title} - ` : '') + (isAdminRoute ? '管理后台' : '大学二手交易平台')

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdminAuth && !adminStore.isLoggedIn) {
    next({ name: 'AdminLogin', query: { redirect: to.fullPath } })
    return
  }

  next()
})

export default router
