# 前端菜单优化设计方案

> 日期：2026-06-21  
> 状态：Completed  
> 范围：前端各角色侧边栏菜单图标、排序层次优化以及消息通知栏位置微调  

---

## 1. 目标与背景

由于项目升级为 **Soybean Admin v2.2.0** 架构，当前系统存在以下前端菜单管理痛点：
1. **菜单管理无序**：左侧菜单栏图标均为默认，排序凌乱，无法体现管理员、学生、维修工各自功能模块的“主次关系”。
2. **公共菜单错位**：原本适用于各角色的“消息通知”菜单栏位置默认展示在列表第一位，不符合导航逻辑，需微调至菜单底端。

本方案旨在：
- 在构建层通过 `elegant-router` 机制，为各角色分配专门的图标与合理的排序权重。
- 调整“消息通知”的显示顺序与图标，使其更具层次感。

---

## 2. 菜单栏图标与排序层次优化

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

#### 4. 公共页面调整 - 消息通知移至底部
- `notifications` (消息通知)：`mdi:bell-outline` | 权重: `9`  
  *(注：将其排序设置为 9 并在三端共享，使其在侧边栏中落入最下方，符合常用习惯)*

---

## 3. 详情子页面隐藏策略（已有逻辑保持）
以下辅助页面将自动设置 `hideInMenu: true`，不出现在左侧菜单中，避免干扰菜单结构，但在面包屑和标签页中正常高亮父级：
- `student-repair-detail` / `student-repair-feedback` -> 隐藏，激活并高亮 `student-repairs`
- `worker-order-detail` -> 隐藏，激活并高亮 `worker-orders`

---

## 4. 验证标准

1. **菜单图标验证**：启动前端开发服务器后，管理员、学生、维修工三端菜单图标显示正常，且具有逻辑排序。
2. **位置验证**：“消息通知”应整齐排列在侧边栏底端，显示为铃铛图标。
3. **打包测试**：运行 `npm run build` 保证前端无编译错误。
