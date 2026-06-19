import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/auth/LoginView.vue'
import RegisterView from '../views/auth/RegisterView.vue'

import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('../layouts/AuthLayout.vue'),
      children: [
        {
          path: '',
          redirect: '/login'
        },
        {
          path: 'login',
          name: 'login',
          component: LoginView
        },
        {
          path: 'register',
          name: 'register',
          component: RegisterView
        }
      ]
    },
    {
      path: '/student',
      component: () => import('../layouts/StudentLayout.vue'),
      meta: { requiresAuth: true, role: 'STUDENT' },
      children: [
        {
          path: 'home',
          name: 'student-home',
          component: () => import('../views/student/HomeView.vue'),
          meta: { title: '首页' }
        },
        {
          path: 'repair/create',
          name: 'repair-create',
          component: () => import('../views/student/RepairCreateView.vue'),
          meta: { title: '我要报修' }
        },
        {
          path: 'repair/list',
          name: 'repair-list',
          component: () => import('../views/student/RepairListView.vue'),
          meta: { title: '我的报修' }
        },
        {
          path: 'repair/detail/:id',
          name: 'repair-detail',
          component: () => import('../views/student/RepairDetailView.vue'),
          meta: { title: '工单详情' }
        },
        {
          path: 'repair/feedback/:id',
          name: 'repair-feedback',
          component: () => import('../views/student/RepairFeedbackView.vue'),
          meta: { title: '评价工单' }
        },
        {
          path: 'profile',
          name: 'student-profile',
          component: () => import('../views/student/ProfileView.vue'),
          meta: { title: '个人中心' }
        },
        {
          path: 'notifications',
          name: 'student-notifications',
          component: () => import('../views/shared/NotificationView.vue'),
          meta: { title: '我的通知' }
        }
      ]
    },
    {
      path: '/repairer',
      component: () => import('../layouts/WorkerLayout.vue'),
      meta: { requiresAuth: true, role: 'REPAIRER' },
      children: [
        {
          path: 'dashboard',
          name: 'worker-dashboard',
          component: () => import('../views/worker/DashboardView.vue'),
          meta: { title: '工作台' }
        },
        {
          path: 'orders',
          name: 'worker-orders',
          component: () => import('../views/worker/OrdersView.vue'),
          meta: { title: '我的工单' }
        },
        {
          path: 'orders/:id',
          name: 'worker-order-detail',
          component: () => import('../views/worker/OrderDetailView.vue'),
          meta: { title: '工单详情' }
        },
        {
          path: 'profile',
          name: 'worker-profile',
          component: () => import('../views/worker/ProfileView.vue'),
          meta: { title: '个人中心' }
        },
        {
          path: 'notifications',
          name: 'worker-notifications',
          component: () => import('../views/shared/NotificationView.vue'),
          meta: { title: '我的通知' }
        }
      ]
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, role: 'ADMIN' },
      children: [
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('../views/admin/DashboardView.vue'),
          meta: { title: '控制台' }
        },
        {
          path: 'repairs',
          name: 'admin-repairs',
          component: () => import('../views/admin/RepairManageView.vue'),
          meta: { title: '工单管理' }
        },
        {
          path: 'notices',
          name: 'admin-notices',
          component: () => import('../views/admin/NoticeManageView.vue'),
          meta: { title: '公告发布' }
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('../views/admin/UserManageView.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'categories',
          name: 'admin-categories',
          component: () => import('../views/admin/CategoryManageView.vue'),
          meta: { title: '分类设置' }
        },
        {
          path: 'logs',
          name: 'admin-logs',
          component: () => import('../views/admin/OperationLogView.vue'),
          meta: { title: '操作日志' }
        },
        {
          path: 'ai-insights',
          name: 'admin-ai-insights',
          component: () => import('../views/admin/AIInsightsView.vue'),
          meta: { title: 'AI 洞察' }
        },
        {
          path: 'profile',
          name: 'admin-profile',
          component: () => import('../views/admin/ProfileView.vue'),
          meta: { title: '个人中心' }
        },
        {
          path: 'notifications',
          name: 'admin-notifications',
          component: () => import('../views/shared/NotificationView.vue'),
          meta: { title: '我的通知' }
        }
      ]
    }
  ]
})

// Navigation Guard
router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()
  const isLoggedIn = !!authStore.token

  if (to.meta.requiresAuth) {
    if (!isLoggedIn) {
      return next('/login')
    }

    if (to.meta.role && authStore.user && authStore.user.role !== to.meta.role) {
      const toast = useToast()
      toast.error('权限不足，无法访问该页面')
      const roleFallback: Record<string, string> = {
        ADMIN: '/admin/dashboard',
        STUDENT: '/student/home',
        REPAIRER: '/repairer/dashboard'
      }
      return next(roleFallback[authStore.user.role] || '/login')
    }
  }

  next()
})

export default router
