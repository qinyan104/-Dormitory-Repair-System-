# 宿舍报修管理系统 (Dormitory Repair System)

本系统是基于 **Spring Boot 3**、**Vue 3 (Vite)** 和 **MySQL** 开发的前后端分离宿舍报修管理系统。它提供了一个完整的报修全生命周期管理方案，包含学生在线报修、管理员审批派单、维修人员接单处理、统计看板等核心功能，并集成了 AI 智能故障分类与派单推荐。

---

## 🌟 核心功能

### 学生端
- 在线提交报修、查看处理进度、服务评价、个人信息管理
- **AI 智能故障分类**：一键分析报修描述，自动推荐分类
- 实时 WebSocket 通知推送（工单状态变更即时知晓）
- 全流程垂直时间轴，直观展示每个处理节点

### 管理员端
- 报修工单全流程管理（受理、指派、完成、关闭）
- **AI 智能派单推荐**：根据维修人员技能、负载、评价自动推荐最优人选
- 支持自动派单（匹配度高时自动分配）
- 公告发布与管理、系统用户管理、报修分类配置
- 多维度数据统计看板 + **AI 数据洞察分析**
- 操作审计日志（AOP 切面自动记录）
- AI 验收评估：维修完成后自动风险评估
- Excel 工单导出

### 维修人员端
- 工作台概览、工单接单与处理、标记完成
- 个人工单统计、查看学生评价反馈

### 安全性
- JWT 认证（强制配置，启动时校验密钥强度）
- 验证码防护（Redis 驱动，5 分钟 TTL）
- IP 登录频率限制（60 秒窗口，10 次上限）
- 操作日志追踪 + 敏感字段自动脱敏（密码、验证码等）
- **HTTP 安全响应头**：CSP / HSTS / X-Frame-Options / X-Content-Type-Options / Referrer-Policy / Permissions-Policy
- **CORS 白名单**（仅允许显式配置的前端来源）
- **幂等性保护**：报修提交支持 `Idempotency-Key` 防重复
- **用户软删除**：删除用户 → 置为禁用状态，保留关联数据完整性
- **请求追踪**：每个 API 请求自动生成 `X-Trace-Id`，日志全链路可追溯

### 可视化增强
- 图形化五星评价系统（含心情图标）
- 全流程垂直时间轴展示工单处理节点

### 移动端 APP（Capacitor Android）
- 基于 **Capacitor 8** 构建的 Android 混合应用
- 学生端：首页、报修提交、工单列表、个人中心（底部 Tab 导航）
- 维修人员端：工作台、工单列表、个人中心
- 利用 WebSocket 实时接收工单推送通知
- 可打包为独立 APK 在手机上运行

---

## 🛠️ 技术栈

| 分层 | 技术 | 说明 |
|------|------|------|
| **后端** | Spring Boot 3.3.5 | 基础框架 |
| | Spring Security | 密码加密 (BCrypt) |
| | MyBatis Plus 3.5.8 | ORM，乐观锁 (@Version) |
| | JWT (jjwt 0.11.5) | 无状态认证 |
| | Redis (Lettuce) | 验证码存储 + 登录限流 + 幂等性 |
| | Spring WebSocket (STOMP) | 实时通知推送 |
| | DeepSeek API | AI 智能分析 |
| | Apache POI 5.2.5 | Excel 导出 |
| | SpringDoc OpenAPI 2.6.0 | Swagger API 文档 |
| | AOP (Log annotation) | 操作日志自动记录 |
| **前端** | Vue 3 + TypeScript 6 | 渐进式框架 |
| | Vite 8 | 构建工具（vendor 分包优化） |
| | Pinia 3 | 状态管理 |
| | Vue Router 5 | 路由（导航守卫 + 角色鉴权） |
| | Axios | HTTP 客户端（Token 自动注入） |
| | STOMP.js + SockJS | WebSocket 客户端 |
| | **Capacitor 8** | **Android 混合应用容器** |
| | Vanilla CSS | 定制化 Mastercard 风格 UI |
| **部署** | Docker + Docker Compose | 一键部署 |
| | Nginx | 反向代理 |
| | MySQL 8.0 | 数据库 |
| **CI** | GitHub Actions | 自动构建 + 测试 |

---

## 📐 系统架构

```
┌─────────────────────────────────────────────┐
│                Frontend (Vue 3)              │
│  Admin Layout · Student Layout · Worker      │
│      WebSocket (STOMP) · Auth (JWT)          │
├────────────────┬────────────────────────────┤
│   Nginx (80)   │  Vite Dev Server (5173)     │
├────────────────┴────────────────────────────┤
│              Backend (Spring Boot 8080)       │
│  Controller → Service → Mapper (MyBatis+)    │
│  JWT Interceptor · AOP Log · WebSocket       │
│  Security Headers · TraceId Filter           │
│  AI Service (DeepSeek) · Excel Export        │
├────────────────────┬───────────────────────┤
│     MySQL 8.0      │      Redis 7          │
└────────────────────┴───────────────────────┘
```

---

## 📋 工单状态流 (6 状态)

```
1 待受理 ──→ 2 已派单 ──→ 3 维修中 ──→ 4 待确认 ──→ 5 已完成
    │            │            │            │
    └──── 6 已取消 ←──────────┴────────────┘
```

所有状态转换由 `RepairStatusEnum` 验证，非法跳转被拒绝（如 1→5）。

---

## 📁 项目结构

```
dormitory-repair-backend/
├── src/main/java/com/example/dormitoryrepair/
│   ├── common/          # 公用模块
│   │   ├── annotation/  # @Log 注解
│   │   ├── aspect/      # AOP 日志切面
│   │   ├── auth/        # JWT 认证 + 拦截器 + 安全头 + TraceId
│   │   ├── enums/       # RepairStatusEnum, UserRoleEnum
│   │   ├── exception/   # 全局异常处理（含 7 种异常类型）
│   │   ├── result/      # ApiResponse 统一响应
│   │   └── util/        # DateTimeRangeParser, ExcelExportUtil, IdempotencyHelper
│   ├── config/          # 配置类（Security, Redis, WebSocket, MyBatisPlus, Swagger）
│   ├── controller/      # API 控制器
│   ├── dto/             # 请求/响应 DTO（按业务分包）
│   ├── entity/          # 实体类（MyBatis Plus）
│   ├── mapper/          # 数据访问层
│   └── service/         # 业务逻辑层
│       └── impl/        # 服务实现
├── src/main/resources/
│   ├── application.yml  # 主配置
│   ├── logback-spring.xml # 结构化日志（JSON/Console 双模式）
│   ├── db/migration/    # Flyway 迁移
│   └── mapper/          # XML Mapper
└── src/test/            # 146 个单元测试

dormitory-repair-frontend/
├── src/
│   ├── api/             # Axios API 封装
│   ├── components/ui/   # 共享 UI 组件 (Ui* 前缀)
│   ├── composables/     # 组合式函数 (WebSocket, Toast, Confirm)
│   ├── constants/       # 状态常量
│   ├── layouts/         # 布局组件 (Admin/Student/Worker + Mobile)
│   ├── router/          # 路由配置 + 导航守卫
│   ├── stores/          # Pinia 状态管理
│   ├── styles/          # Mastercard 设计令牌 + 基础样式
│   ├── types/           # TypeScript 类型定义
│   ├── utils/           # 工具函数
│   └── views/           # 页面视图（按角色分包）
└── vite.config.ts       # Vite 配置（proxy + 分包优化）
```

---

## ✅ 测试覆盖 (146 个)

| 测试层 | 数量 | 说明 |
|--------|------|------|
| Controller | 124 | Auth / RepairOrder / Category / Notice / User / AI / Statistics / Feedback / File / Notification |
| Service | 5 | AiServiceTest |
| AOP | 2 | LogAspectTest |
| Auth | 1 | JwtAuthInterceptorTest |
| Exception | 1 | GlobalExceptionHandlerTest |
| Utility | 2 | DateTimeRangeParserTest |
| Smoke | 7 | 状态机 / 枚举 / 非法跳转检测 |
| **合计** | **146** | 零失败，零错误 |

---

## 🚀 快速部署

### Docker 一键部署（推荐）

```bash
# 1. 确保已安装 Docker Desktop
# 2. 配置 AI 密钥（可选）
#    编辑 .env 文件填入 DEEPSEEK_API_KEY=sk-你的key
# 3. 设置 JWT 密钥（必需）
#    编辑 .env 文件填入 JWT_SECRET=你的32位以上密钥
# 4. 启动
docker compose up -d --build
```

- **访问系统**: `http://localhost` (默认 80 端口)
- **API 文档**: `http://localhost:8080/swagger-ui/index.html`
- **健康检查**: `http://localhost:8080/api/health` | `/live` | `/ready`
- **后端 API**: `http://localhost:8080`
- **数据库连接**: 端口 `3307` (用户名 `root`，密码 `root`)

### 手动开发部署

#### 环境要求
- Java JDK 17+（推荐配置 `JAVA_HOME`）
- Maven 3.6+
- Node.js 18+（建议 20.x）
- MySQL 8.0+
- Redis 7.x

#### 后端

```bash
cd dormitory-repair-backend

# 设置必要环境变量
export JWT_SECRET=your-32-char-min-secret-key
export DEEPSEEK_API_KEY=sk-your-key  # 可选，AI 功能需要

# 启动
mvn spring-boot:run
```

#### 前端 (Web)
```bash
cd dormitory-repair-frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

#### 移动端 APP (Android APK)

```bash
cd dormitory-repair-frontend

# 构建前端生产包
npm run build

# 同步到 Android 工程
npx cap sync android

# 用 Android Studio 打开打包
npx cap open android
# 然后在 Android Studio 中 Build → Build Bundle(s) / APK(s) → Build APK
```

也可直接使用 `npm run cap:sync` 一步完成构建 + 同步。

---

## 🔑 快速测试账号

| 角色 | 用户名 | 密码 |
|:---|:---|:---|
| **管理员** | `admin` | `123456` |
| **学生** | `student01` | `123456` |
| **维修人员** | `repairer01` | `123456` |

---

## 📡 API 文档

启动后端后访问 Swagger UI：
- **Swagger**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

主要端点：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/register` | 注册 |
| GET | `/api/auth/captcha` | 获取验证码 |
| POST | `/api/repair-order` | 提交报修（支持 Idempotency-Key） |
| GET | `/api/repair-order/page` | 管理员分页查询 |
| PUT | `/api/repair-order/accept/{id}` | 受理（含 AI 自动派单） |
| PUT | `/api/repair-order/assign/{id}` | 指派维修人员 |
| GET | `/api/statistics/overview` | 统计概览 |
| POST | `/api/ai/classify` | AI 故障分类 |
| GET | `/api/ai/recommend-worker` | AI 派单推荐 |
| GET | `/api/health` | 完整健康检查（DB + Redis） |

---

## 🔧 常见问题排查

1. **后端编译失败**：本项目已适配 JDK 17 和 JDK 25。Lombok (1.18.40) 和 Mockito (5.16.0) 已升级以避免反射冲突。
2. **启动报错 "JWT secret must be configured"**：请通过环境变量或 `.env` 文件设置 `JWT_SECRET`。
3. **验证码无法显示**：检查 Redis 容器是否正常运行。
4. **AI 功能不可用**：确认已设置 `DEEPSEEK_API_KEY`。AI 失败不影响核心报修流程。
5. **前端打包**：运行 `npm run build` 验证 TypeScript 类型与生产构建。
6. **Swagger 无法访问**：确认后端已启动，访问 `http://localhost:8080/swagger-ui/index.html`。

---

## 📄 文档索引

| 文件 | 说明 |
|------|------|
| `CHANGELOG.md` | 版本变更记录 |
| `COMMIT_CONVENTION.md` | 提交信息规范 |
| `AGENTS.md` | AI 辅助开发的上下文指南 |
| `docs/` | 系统结构图、流程图、时序图 |

---

## 📜 许可证

MIT License — 仅用于课程设计学习交流。
