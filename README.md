# 宿舍报修管理系统 (Dormitory Repair System)

本系统是基于 **Spring Boot 3**、**Vue 3 (Vite)** 和 **MySQL** 开发的前后端分离宿舍报修管理系统。它提供了一个完整的报修全生命周期管理方案，包含学生在线报修、管理员审批派单、维修人员接单处理、统计看板等核心功能。

## 🌟 核心功能

- **学生端**：在线提交报修、AI智能故障分类、查看处理进度、服务评价、个人信息管理、查看校园公告。
- **管理员端**：报修工单全流程管理（指派、完成、关闭）、AI智能派单推荐、公告发布与管理、系统用户管理、报修分类配置、多维度数据统计看板、操作审计日志。
- **维修人员端**：工作台概览、工单接单与处理、标记完成、个人工单统计、**查看学生评价反馈**。
- **可视化增强**：引入 **图形化五星评价系统**（含心情图标）及 **全流程垂直时间轴**，直观展示报修处理每一个节点。
- **安全性**：JWT 安全认证、验证码防护（Redis 驱动）、操作日志追踪。

---

## 🛠️ 技术栈

- **后端**: Spring Boot 3, Spring Security, MyBatis Plus, JWT, Redis, Maven, DeepSeek AI API
- **前端**: Vue 3, TypeScript, Vite, Pinia, Vue Router, Vanilla CSS (定制化 Mastercard 风格 UI)
- **部署**: Docker, Docker Compose, Nginx
- **数据库**: MySQL 8.0

---

## 1. 运行环境准备

在开始部署前，请确保您的电脑已安装以下环境：

*   **Java**: JDK 17 (必须，推荐配置 `JAVA_HOME`)
*   **Maven**: 3.6+
*   **Node.js**: 18+ (建议 20.x)
*   **MySQL**: 8.0+
*   **Redis**: 7.x (用于验证码)
*   **Docker Desktop** (推荐，一键启动)

---

## 2. 快速部署 (Docker 推荐)

在项目根目录（含 `docker-compose.yml`）下执行：

```bash
docker compose up -d --build
```

- **访问系统**: `http://localhost` (默认 80 端口)
- **后端 API**: `http://localhost:8080`
- **外部数据库连接**: 端口 `3307` (用户名 `root`，密码 `root`)

---

## 3. 手动开发部署

### 后端启动
1. 使用 IntelliJ IDEA 打开 `dormitory-repair-backend`。
2. 确保 JDK 版本为 17。
3. 配置 DeepSeek API Key（用于AI功能）：在 `.env` 文件填入 `DEEPSEEK_API_KEY=sk-你的key`，或用 `$env:DEEPSEEK_API_KEY="sk-你的key"` 设置环境变量。
4. 运行 `DormitoryRepairApplication.java`。

### 前端启动
1. 在 `dormitory-repair-frontend` 目录下：
   ```bash
   npm install
   npm run dev
   ```
2. 访问 `http://localhost:5173`。

---

## 4. 快速测试账号

系统预设测试账号（密码均为 `123456`）：

| 角色 | 用户名 | 说明 |
| :--- | :--- | :--- |
| **管理员** | `admin` | 全权限管理、数据看板 |
| **学生** | `student01` | 发起报修、确认评价 |
| **维修人员** | `repairer01` | 接单、维修处理 |

---

## 📸 系统演示
系统结构图、流程图、时序图位于 `docs/` 目录。

课程答辩或最终提交时，建议根据本地实际运行结果补充页面截图。

---

## 常见问题排查 

1.  **后端编译报错**: 请务必使用 **JDK 17**。JDK 21+ 可能会导致 Lombok 冲突。
2.  **验证码无法显示**: 检查 Redis 容器是否正常运行。
3.  **前端打包**: 运行 `npm run build` 验证 TypeScript 类型与生产构建。
