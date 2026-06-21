import type { RouteMeta } from 'vue-router';
import ElegantVueRouter from '@elegant-router/vue/vite';
import type { RouteKey } from '@elegant-router/types';

export function setupElegantRouter() {
  return ElegantVueRouter({
    layouts: {
      base: 'src/layouts/base-layout/index.vue',
      blank: 'src/layouts/blank-layout/index.vue'
    },
    routePathTransformer(routeName, routePath) {
      const key = routeName as RouteKey;

      if (key === 'login') {
        const modules: UnionKey.LoginModule[] = ['pwd-login', 'code-login', 'register', 'reset-pwd', 'bind-wechat'];

        const moduleReg = modules.join('|');

        return `/login/:module(${moduleReg})?`;
      }

      return routePath;
    },
    onRouteMetaGen(routeName) {
      const key = routeName as RouteKey;

      const constantRoutes: RouteKey[] = ['login', '403', '404', '500'];

      const meta: Partial<RouteMeta> = {
        title: key,
        i18nKey: `route.${key}` as App.I18n.I18nKey
      };

      if (constantRoutes.includes(key)) {
        meta.constant = true;
      }

      // Route icons mapping
      const routeIcons: Record<string, string> = {
        // Admin
        'admin-home': 'mdi:view-dashboard',
        'admin-repairs': 'material-symbols:construction',
        'admin-categories': 'material-symbols:category',
        'admin-users': 'mdi:account-multiple',
        'admin-notices': 'mdi:bulletin-board',
        'admin-logs': 'mdi:clipboard-text-clock-outline',
        'admin-ai-insights': 'mdi:brain',
        'admin-profile': 'mdi:account-cog',
        // Student
        'student-home': 'mdi:home',
        'student-new-repair': 'mdi:plus-circle',
        'student-repairs': 'mdi:file-document-edit',
        'student-profile': 'mdi:account',
        // Worker
        'worker-home': 'mdi:view-dashboard-outline',
        'worker-orders': 'material-symbols:build-circle-outline',
        'worker-profile': 'mdi:account-box',
        // Common
        'notifications': 'mdi:bell-outline'
      };

      // Route orders mapping
      const routeOrders: Record<string, number> = {
        // Admin
        'admin-home': 1,
        'admin-repairs': 2,
        'admin-categories': 3,
        'admin-users': 4,
        'admin-notices': 5,
        'admin-logs': 6,
        'admin-ai-insights': 7,
        'admin-profile': 8,
        // Student
        'student-home': 1,
        'student-new-repair': 2,
        'student-repairs': 3,
        'student-profile': 4,
        // Worker
        'worker-home': 1,
        'worker-orders': 2,
        'worker-profile': 3,
        // Common
        'notifications': 9
      };

      if (routeIcons[key]) {
        meta.icon = routeIcons[key];
      }
      if (routeOrders[key]) {
        meta.order = routeOrders[key];
      }

      // Role-based menu/route isolation.
      // Route names follow the `<role>-<page>` convention (flat, hyphenated),
      // e.g. `admin-home`, `student-repairs`, `worker-orders`. We assign `roles`
      // by prefix. Routes with no `roles` (notifications, etc.) stay visible to everyone.
      if (key === 'home') {
        meta.roles = ['STUDENT', 'ADMIN', 'REPAIRER'];
      } else if (key.startsWith('admin')) {
        meta.roles = ['ADMIN'];
      } else if (key.startsWith('student')) {
        meta.roles = ['STUDENT'];
      } else if (key.startsWith('worker')) {
        meta.roles = ['REPAIRER'];
      }

      // Detail / sub pages are reached by navigation, not from the menu.
      // Hide them and highlight their parent list page while active.
      const hiddenSubPages: Partial<Record<RouteKey, RouteKey>> = {
        'student-repair-detail': 'student-repairs',
        'student-repair-feedback': 'student-repairs',
        'worker-order-detail': 'worker-orders'
      };

      const activeMenu = hiddenSubPages[key as RouteKey];
      if (activeMenu) {
        meta.hideInMenu = true;
        meta.activeMenu = activeMenu;
      }

      return meta;
    }
  });
}
