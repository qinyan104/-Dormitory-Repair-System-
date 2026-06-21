# 前端菜单优化与全屏数字孪生大屏设计方案

> 日期：2026-06-21  
> 状态：Ready for Review  
> 范围：前端菜单图标/顺序优化，全屏数据大屏路由与组件设计，后端统计数据接口设计  

---

## 1. 目标与背景

由于项目升级为 **Soybean Admin v2.2.0** 架构，当前系统存在以下两个前端痛点：
1. **菜单管理无序**：左侧菜单栏图标均为默认，排序凌乱，无法体现管理员、学生、维修工各自功能的“主次关系”。
2. **缺乏答辩亮点**：传统的平铺数据表格在大屏投影或项目汇报中缺乏说服力，需要建立一个极具科技感的全屏数据监控大屏，作为数据洞察的图形化入口。

本方案旨在：
- 在构建层通过 `elegant-router` 机制，为各角色分配专门的图标与排序权重。
- 引入一个独立全屏大屏页面，通过 ECharts 实现报修分类、楼栋热力分布、完工率趋势 and 工时绩效排行。
- 补充对应的后端统计接口，打通数据全链路。

---

## 2. 第一阶段：菜单栏图标与排序层次优化

我们将在前端 `build/plugins/router.ts` 的 `onRouteMetaGen(routeName)` 钩子中增加规则映射，自动为生成的路由元数据填充 `icon` 和 `order`。

### 2.1 规则映射表

#### 1. 管理员 (ADMIN) - 主次分明，审计/AI置底
- `admin-home` (管理控制台)：`mdi:view-dashboard` | 权重: `1`
- `admin-repairs` (报修工单管理)：`material-symbols:construction` | 权重: `2`
- `admin-categories` (报修类别管理)：`material-symbols:category` | 权重: `3`
- `admin-users` (用户管理)：`mdi:account-multiple` | 权重: `4`
- `admin-notices` (公告管理)：`mdi:bulletin-board` | 权重: `5`
- `admin-logs` (操作日志)：`mdi:clipboard-text-clock-outline` | 权重: `6`
- `admin-ai-insights` (AI 数据洞察)：`mdi:brain` | 权重: `7`
- `admin-profile` (个人中心)：隐藏 (`hideInMenu: true`)，不干扰系统架构

#### 2. 学生 (STUDENT) - 高频提单优先
- `student-home` (学生首页)：`mdi:home` | 权重: `1`
- `student-new-repair` (提交报修)：`mdi:plus-circle` | 权重: `2`
- `student-repairs` (我的报修)：`mdi:file-document-edit` | 权重: `3`
- `student-profile` (个人中心)：`mdi:account` | 权重: `4`

#### 3. 维修人员 (REPAIRER) - 快速接单优先
- `worker-home` (工作台首页)：`mdi:view-dashboard-outline` | 权重: `1`
- `worker-orders` (工单列表)：`material-symbols:build-circle-outline` | 权重: `2`
- `worker-profile` (个人中心)：`mdi:account-box` | 权重: `3`

---

## 3. 第二阶段：全屏数据监控大屏设计

### 3.1 后端统计 API 扩展

新增统计接口由 `StatisticsController` 提供，所有接口均需要有 `ADMIN` 角色保护。

#### 1. 宿舍楼栋分布接口 (`GET /api/statistics/building`)
统计每个宿舍楼栋的报修件数。
- **SQL 逻辑**：
  ```sql
  SELECT u.dormitory_building AS building, COUNT(o.id) AS count
  FROM repair_order o
  JOIN sys_user u ON o.user_id = u.id
  WHERE u.dormitory_building IS NOT NULL AND u.dormitory_building != ''
  GROUP BY u.dormitory_building
  ORDER BY count DESC
  ```

#### 2. 7日新增与完工趋势接口 (`GET /api/statistics/trend`)
提供折线图所需的每日动态数据。
- **SQL 逻辑**：
  ```sql
  SELECT 
      DATE_FORMAT(o.submit_time, '%m-%d') AS date, 
      COUNT(o.id) AS submitCount,
      SUM(CASE WHEN o.repair_status = 5 THEN 1 ELSE 0 END) AS completeCount
  FROM repair_order o
  WHERE o.submit_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
  GROUP BY DATE(o.submit_time)
  ORDER BY DATE(o.submit_time) ASC
  ```

#### 3. 维修工绩效排行榜接口 (`GET /api/statistics/worker-ranking`)
衡量维修人员的工作量与好评度。
- **SQL 逻辑**：
  ```sql
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
  ```

---

### 3.2 前端路由配置与 Blank Layout 绑定

为了使大屏不受 Soybean Admin 侧边栏 and 头部面包屑遮挡，我们将其绑定在 `BlankLayout` 布局上。
在 `src/router/routes/index.ts` 中配置自定义路由：

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

大屏页面文件放置在 `src/views/admin-big-screen/index.vue`。

---

### 3.3 大屏页面布局与 ECharts 架构

页面使用 `16:9` 的自适应 `div` 容器，配合 `CSS transform: scale()` 技术实现任意分辨率屏幕下的无滚轮铺满。

#### 1. 布局结构 (Flex & Grid)
- **Header (头部)**：流光科技感标题栏，包含系统名称、实时时间和“返回控制台”快捷按钮。
- **Left Panel (左面板)**：
  - Widget 1：工单状态环形图 (Pie/Doughnut Chart)。
  - Widget 2：7 日工单趋势折线图 (Line Chart，包含新增线与完成线)。
- **Center Panel (中央主看板)**：
  - 上方：今日核心数字指标看板（待受理数、今日新增数、本月完工率）。
  - 下方：宿舍楼栋报修热力排行（3D 象形柱图或带渐变色的水平条形图）。
- **Right Panel (右面板)**：
  - Widget 3：维修工完工绩效柱状图 (Bar Chart)。
  - Widget 4：实时报修轮播工单流（实时滚动弹幕，突出当前正在发生的紧急报修）。

#### 2. 大屏配色方案 (Theme settings)
- **背景色**：`#0b0d1b` (宇宙极暗蓝)
- **卡片底色**：`rgba(16, 20, 38, 0.6)` 并带模糊效果 (`backdrop-filter: blur(10px)`) 和浅蓝虚化边框。
- **重点点缀色**：
  - 故障橙 (Signal Orange): `#f0a020`
  - 成功绿 (Neon Teal): `#18a058`
  - 科技蓝: `#2080f0`

---

## 4. 异常处理与降级机制

1. **ECharts 渲染兜底**：大屏在数据接口超时或报错时，图表保持加载骨架屏或渲染 Empty 空态，核心流程不闪退。
2. **自适应降级**：若浏览器不支持 `scale`，则页面回退至流动网格布局（Grid Wrap），自适应宽度。

---

## 5. 验证标准

1. **菜单图标验证**：启动前端开发服务器后，管理员、学生、维修工三端菜单图标显示正常，且具有逻辑排序。
2. **大屏路由加载**：直接访问 `/admin-big-screen` 应该显示纯全屏（无后台侧边栏），只有管理员可以访问。
3. **打包测试**：运行 `npm run build` and `mvn compile` 保证前后端均无编译错误。
