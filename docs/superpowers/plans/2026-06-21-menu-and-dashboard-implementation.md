# 前端菜单优化与微调实现计划

本计划指导并验证项目前端菜单整理（含图标、优先级排序和通知位置微调）的完整实现。

---

### Task 1: 侧边栏菜单图标与排序配置

**文件：**
- 修改：`dormitory-repair-frontend/build/plugins/router.ts`

- [ ] **Step 1: 在 `build/plugins/router.ts` 中定义图标与排序映射表**

```ts
const routeIcons: Record<string, string> = {
  // 管理员
  'admin-home': 'mdi:view-dashboard',
  'admin-repairs': 'material-symbols:construction',
  'admin-categories': 'material-symbols:category',
  'admin-users': 'mdi:account-multiple',
  'admin-notices': 'mdi:bulletin-board',
  'admin-logs': 'mdi:clipboard-text-clock-outline',
  'admin-ai-insights': 'mdi:brain',
  'admin-profile': 'mdi:account-cog',
  // 学生
  'student-home': 'mdi:home',
  'student-new-repair': 'mdi:plus-circle',
  'student-repairs': 'mdi:file-document-edit',
  'student-profile': 'mdi:account',
  // 维修工
  'worker-home': 'mdi:view-dashboard-outline',
  'worker-orders': 'material-symbols:build-circle-outline',
  'worker-profile': 'mdi:account-box',
  // 公共
  'notifications': 'mdi:bell-outline'
};

const routeOrders: Record<string, number> = {
  // 管理员
  'admin-home': 1,
  'admin-repairs': 2,
  'admin-categories': 3,
  'admin-users': 4,
  'admin-notices': 5,
  'admin-logs': 6,
  'admin-ai-insights': 7,
  'admin-profile': 8,
  // 学生
  'student-home': 1,
  'student-new-repair': 2,
  'student-repairs': 3,
  'student-profile': 4,
  // 维修工
  'worker-home': 1,
  'worker-orders': 2,
  'worker-profile': 3,
  // 公共
  'notifications': 9
};
```

- [ ] **Step 2: 在 `onRouteMetaGen` 钩子中注入属性**

```ts
if (routeIcons[key]) {
  meta.icon = routeIcons[key];
}
if (routeOrders[key]) {
  meta.order = routeOrders[key];
}
```

- [ ] **Step 3: 触发 elegant-router 路由元数据重构**

运行前端构建：
```bash
cd dormitory-repair-frontend
npm run build
```
检查 `src/router/elegant/routes.ts` 确保生成的路由元数据中，包含了正确的 `icon` 和 `order` 属性，且消息通知移至底部。
