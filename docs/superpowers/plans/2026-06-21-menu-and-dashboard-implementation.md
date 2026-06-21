# 前端菜单优化与全屏监控大屏实现计划

本计划指导并验证项目前端菜单整理及全屏监控大屏的完整实现。

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
  'worker-profile': 'mdi:account-box'
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
  'worker-profile': 3
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

运行前端构建或测试启动前端：
```bash
cd dormitory-repair-frontend
npm run build
```
检查 `src/router/elegant/routes.ts` 确保生成的路由元数据中，包含了正确的 `icon` 和 `order` 属性。

---

### Task 2: 后端统计 API 开发

**文件：**
- 修改：`dormitory-repair-backend/src/main/java/com/example/dormitoryrepair/mapper/RepairOrderMapper.java`
- 修改：`dormitory-repair-backend/src/main/resources/mapper/RepairOrderMapper.xml`
- 修改：`dormitory-repair-backend/src/main/java/com/example/dormitoryrepair/controller/StatisticsController.java`

- [ ] **Step 1: 在 `RepairOrderMapper.java` 接口中声明新统计方法**

```java
List<Map<String, Object>> countGroupByBuilding();
List<Map<String, Object>> getSevenDayTrend();
List<Map<String, Object>> getWorkerPerformance();
```

- [ ] **Step 2: 在 `RepairOrderMapper.xml` 中编写对应的 SQL 节点**

```xml
<select id="countGroupByBuilding" resultType="java.util.HashMap">
    SELECT u.dormitory_building AS building, COUNT(o.id) AS count
    FROM repair_order o
    JOIN sys_user u ON o.user_id = u.id
    WHERE u.dormitory_building IS NOT NULL AND u.dormitory_building != ''
    GROUP BY u.dormitory_building
    ORDER BY count DESC
</select>

<select id="getSevenDayTrend" resultType="java.util.HashMap">
    SELECT 
        DATE_FORMAT(o.submit_time, '%m-%d') AS date, 
        COUNT(o.id) AS submitCount,
        SUM(CASE WHEN o.repair_status = 5 THEN 1 ELSE 0 END) AS completeCount
    FROM repair_order o
    WHERE o.submit_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
    GROUP BY DATE(o.submit_time)
    ORDER BY DATE(o.submit_time) ASC
</select>

<select id="getWorkerPerformance" resultType="java.util.HashMap">
    SELECT 
        u.real_name AS workerName,
        COUNT(o.id) AS completedCount,
        COALESCE(AVG(f.score), 0.0) AS averageScore,
        COALESCE(AVG(TIMESTAMPDIFF(MINUTE, o.worker_accept_time, o.worker_complete_time)), 0) AS avgRepairTimeMinutes
    FROM sys_user u
    LEFT JOIN repair_order o ON u.id = o.worker_id AND o.repair_status = 5
    LEFT JOIN repair_feedback f ON o.id = f.repair_order_id
    WHERE u.role = 'REPAIRER'
    GROUP BY u.id, u.real_name
    ORDER BY completedCount DESC, averageScore DESC
</select>
```

- [ ] **Step 3: 在 `StatisticsController.java` 中暴露只允许 `ADMIN` 调用的映射方法**

```java
@GetMapping("/building")
public ApiResponse<List<Map<String, Object>>> building() {
    ensureAdmin();
    return ApiResponse.success(repairOrderMapper.countGroupByBuilding());
}

@GetMapping("/trend")
public ApiResponse<List<Map<String, Object>>> trend() {
    ensureAdmin();
    return ApiResponse.success(repairOrderMapper.getSevenDayTrend());
}

@GetMapping("/worker-ranking")
public ApiResponse<List<Map<String, Object>>> workerRanking() {
    ensureAdmin();
    return ApiResponse.success(repairOrderMapper.getWorkerPerformance());
}
```

- [ ] **Step 4: 后端编译与接口单元测试校验**

执行后端编译命令：
```bash
mvn compile
```
确认编译成功无报错。

---

### Task 3: 前端大屏路由与自适应布局搭建

**文件：**
- 修改：`dormitory-repair-frontend/src/router/routes/index.ts`
- 创建：`dormitory-repair-frontend/src/views/admin-big-screen/index.vue`
- 修改：`dormitory-repair-frontend/src/service/api/statistics.ts`

- [ ] **Step 1: 在前端 `statistics.ts` 补充 API 请求包装器**

```ts
export function fetchStatisticsBuilding() {
  return request<any[]>({
    url: '/statistics/building',
    method: 'get'
  });
}

export function fetchStatisticsTrend() {
  return request<any[]>({
    url: '/statistics/trend',
    method: 'get'
  });
}

export function fetchStatisticsWorkerRanking() {
  return request<any[]>({
    url: '/statistics/worker-ranking',
    method: 'get'
  });
}
```

- [ ] **Step 2: 声明并导出自定义的 Blank 路由**

在 `src/router/routes/index.ts` 的 `customRoutes` 列表中增加：
```ts
const customRoutes: CustomRoute[] = [
  {
    name: 'admin-big-screen',
    path: '/admin-big-screen',
    component: 'layout.blank$view.admin-big-screen',
    meta: {
      title: '监控大屏',
      i18nKey: 'route.admin-big-screen',
      roles: ['ADMIN'],
      constant: false
    }
  }
];
```

- [ ] **Step 3: 编写大屏主页面 `admin-big-screen/index.vue`**
- 搭建支持一键全屏/退出全屏的标题控制区域。
- 采用 `scale` 按 `1920x1080` 进行整屏自适应布局（避免不同设备拉伸或错乱）。
- 左右两侧采用卡片玻璃质感，中心配置核心 KPI 指标面板及宿舍楼排行。

---

### Task 4: 图表渲染与接口对接

**文件：**
- 修改：`dormitory-repair-frontend/src/views/admin-big-screen/index.vue`
- 修改：`dormitory-repair-frontend/src/views/admin-home/index.vue`（大屏入口）

- [ ] **Step 1: 引入 ECharts 并绘制四个可视化部件**
1. 工单分类及状态分布（结合已有的 `/statistics/category` 与 `/statistics/status`）。
2. 近 7 天新增及完工折线图（对接 `/statistics/trend`）。
3. 各宿舍楼栋报修负荷热力图（对接 `/statistics/building`）。
4. 维修工完工量及好评排行看板（对接 `/statistics/worker-ranking`）。

- [ ] **Step 2: 在管理控制台首页增加“监控大屏”跳转入口**
在管理端 Dashboard（`src/views/admin-home/index.vue`）中，在“总工单数”卡片或页面顶部右上角添加跳转按钮，方便一键进入 `/admin-big-screen`。

- [ ] **Step 3: 整体功能校验与打包构建验证**

运行打包构建确保无 TypeScript 及 Vite 编译报错：
```bash
npm run build
```
保证构建输出 `✓ built`。
