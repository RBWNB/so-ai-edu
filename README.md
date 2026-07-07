# 🌊 海洋学堂 (Ocean Academy)

> 面向海洋生物教育的全栈互动平台 —— 探索蔚蓝世界，智识海洋万物

海洋学堂是一个集**海洋百科**、**AI 智能识物**、**社区互动**、**答题闯关**、**积分激励**于一体的综合性海洋教育平台。项目采用前后端分离架构，支持 C 端用户学习互动与 B 端管理员数据运营。

---

## 🧭 目录

- [功能概览](#功能概览)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [环境要求](#环境要求)
- [部署指南](#部署指南)
- [API 文档](#api-文档)
- [项目特色](#项目特色)

---

## 功能概览

### 🎓 C 端 — 教育互动平台

| 模块 | 说明 |
|------|------|
| **首页** | 海洋主题动态背景、知识推荐、高光时刻广播 |
| **海洋百科** | 海洋物种分类浏览、详细信息查阅、生态数据展示 |
| **智识海物** | AI 图像识别 + RAG 智能问答，上传图片即可识别海洋生物 |
| **海友社区** | 用户发布观察帖、点赞、评论互动，社区高光时刻自动推送 |
| **答题闯关** | 选择题闯关模式，支持错题本复习 |
| **积分商店** | 答题/社区互动获取积分，兑换虚拟奖励 |
| **个人中心** | 学习档案、等级成长、个人发帖管理、头像框装扮 |
| **系统通知** | 互动消息铃铛提醒、全站广播推送、未读红点 |

### 🛠️ B 端 — 管理后台

| 模块 | 说明 |
|------|------|
| **数据看板** | KPI 核心指标、物种分类统计、AI 词云、活跃度趋势图 |
| **物种管理** | 海洋物种 CRUD、保护等级标注、分类学层级维护 |
| **生态系统管理** | 生态系统信息录入与编辑 |
| **题库管理** | 闯关题目的增删改查、支持 Excel 批量导入导出 |
| **RAG 知识库** | 上传文档（PDF/Word 等）构建 AI 知识库，支持分段管理 |
| **观察审核** | 审核社区用户发布的内容 |
| **积分商店管理** | 上架/下架虚拟商品、配置兑换规则 |
| **用户管理** | 用户账号状态管理、信息查看 |
| **角色管理** | RBAC 权限控制（ADMIN / MANAGER / USER） |
| **操作日志** | 管理员操作审计追踪 |
| **全站广播** | 手动发送系统广播、手动触发 AI 高光时刻推送 |

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 运行语言 |
| Spring Boot | 3.2.5 | 应用框架 |
| Spring Security | — | 认证与 RBAC 鉴权 |
| JWT | 0.12.3 | 无状态 Token 认证 |
| MyBatis-Plus | 3.5.6 | ORM 与代码生成 |
| MySQL | 8.0 | 主数据库 |
| Redis (Stack) | latest | 缓存 + 向量存储 |
| LangChain4j | 1.0.0-beta2 | AI 编排框架 |
| DashScope (百炼) | — | 阿里云大模型（对话 + 嵌入） |
| Apache Tika | 2.9.2 | 文档解析（PDF/Word） |
| SpringDoc OpenAPI | 2.5.0 | API 文档（Swagger UI） |
| Quartz | — | 定时任务调度 |
| EasyExcel | 3.3.4 | Excel 导入导出 |
| 七牛云 SDK | 7.7.0 | 对象存储 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4 | 渐进式框架 |
| Vite | 8.0 | 构建工具 |
| Element Plus | 2.13 | UI 组件库 |
| Vue Router | 4.6 | 路由管理 |
| Pinia | 2.3 | 状态管理 |
| Axios | 1.15 | HTTP 请求 |
| ECharts | 5.6 | 数据可视化 |
| Leaflet | 1.9 | 地图展示 |
| Three.js | 0.184 | 3D 粒子背景 |
| marked | 18.0 | Markdown 渲染 |
| DOMPurify | 3.4 | XSS 防护 |

### 运维

| 技术 | 用途 |
|------|------|
| Docker Compose | 一键编排 MySQL + Redis + Backend + Nginx |
| Nginx | 前端静态资源 + 反向代理 |
| Prometheus | 指标暴露（Actuator + Micrometer） |

---

## 项目结构

```
so-ai-edu/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/gdou/marine/
│   │   ├── annotation/               # 自定义注解（@Log 等）
│   │   ├── aspect/                   # AOP 切面（日志、权限）
│   │   ├── config/                   # Spring 配置（Security、CORS、AI 等）
│   │   ├── controller/               # 接口控制器（21 个）
│   │   ├── dto/                      # 数据传输对象
│   │   ├── entity/                   # 数据库实体
│   │   ├── mapper/                   # MyBatis 映射器
│   │   ├── security/                 # JWT 过滤器、登录入口
│   │   ├── service/                  # 业务接口
│   │   │   └── impl/                 # 业务实现
│   │   ├── utils/                    # 工具类（雪花 ID、文件上传等）
│   │   └── vo/                       # 视图对象
│   ├── src/main/resources/
│   │   ├── application.yml           # 主配置
│   │   └── mapper/                   # MyBatis XML
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── api/                      # API 接口封装
│   │   ├── assets/styles/            # 全局样式
│   │   ├── components/               # 公共组件
│   │   ├── composables/              # 组合式函数
│   │   ├── layout/                   # 布局组件（EduLayout / AdminLayout）
│   │   ├── router/                   # 路由配置 + 守卫
│   │   ├── store/                    # Pinia 状态（auth、theme）
│   │   ├── utils/                    # 工具函数（http 封装等）
│   │   └── views/                    # 页面视图
│   │       ├── edu/                  # C 端页面
│   │       ├── admin/                # B 端页面
│   │       └── common/               # 公共页面（登录、403、404）
│   ├── Dockerfile
│   ├── nginx.conf                    # Nginx 配置
│   └── package.json
├── docker-compose.yml                # 一键部署编排
├── marine_db.sql                     # 数据库初始化脚本
├── Seed_Data.sql                     # 种子数据
└── README.md
```

---

## 快速开始

### 环境要求

| 工具 | 最低版本 |
|------|----------|
| JDK | 21+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| npm / pnpm | 9+ / 8+ |
| Docker + Compose | 20.10+ / 2.0+ |
| MySQL | 8.0（Docker 提供） |
| Redis | 7.x（Docker 提供） |

### 1. 克隆项目

```bash
git clone <repository-url>
cd so-ai-edu
```

### 2. 启动基础设施（MySQL + Redis）

```bash
docker compose up -d mysql redis
```

等待约 30 秒让 MySQL 初始化完成（首次会执行 `marine_db.sql` 建表）。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动后监听 `http://localhost:8080`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器启动后访问 `http://localhost:5173`。

### 5. 访问应用

| 入口 | 地址 |
|------|------|
| C 端用户端 | `http://localhost:5173` |
| B 端管理后台 | `http://localhost:5173/admin` |
| Swagger API 文档 | `http://localhost:8080/doc.html` |
| Actuator 健康检查 | `http://localhost:8080/actuator/health` |

---

## 部署指南

### Docker Compose 一键部署（推荐）

```bash
# 在项目根目录执行
docker compose up -d --build
```

服务启动后：

| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx（前端） | `80` | 最终入口，访问 `http://<服务器IP>` |
| Backend | `8081` | 暴露供调试（生产建议不暴露） |
| MySQL | `13306` | 数据库（生产建议不暴露） |
| Redis | `16379` | 缓存（生产建议不暴露） |

### 环境变量

后端关键配置（通过环境变量覆盖 `application.yml`）：

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/marine_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your-password
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
SPRING_AI_DASHSCOPE_API_KEY=your-dashscope-api-key
JWT_SECRET=your-jwt-secret
```

---

## API 文档

启动后端后访问 Swagger UI：` http://localhost:8080/swagger-ui/index.html`

主要接口模块：

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/auth/login` | 登录获取 JWT Token |
| `GET`  | `/species` | 物种列表查询 |
| `GET`  | `/quiz-question` | 获取闯关题目 |
| `POST` | `/exam/submit` | 提交考试答案 |
| `GET`  | `/notification/list` | 消息通知列表 |
| `POST` | `/notification/admin/broadcast` | 发送全站广播（管理员） |
| `POST` | `/rag/chat` | RAG 智能问答 |
| `GET`  | `/visual/summary` | 数据看板汇总 |

---

## 项目特色

### 🎨 深海视觉体验
- Three.js 粒子海洋背景，支持鼠标视差、波纹交互、导航 Hover 吸引力场
- 护眼模式（暗色主题）一键切换
- 动态波浪背景 + 流体滑动导航指示器
- 多种头像框装扮（海洋 / 彩虹 / 霓虹 / 极光等）

### 🤖 AI 深度集成
- **RAG 智能问答**：基于 LangChain4j + Redis 向量存储，文档上传后自动分段嵌入
- **智识海物**：接入阿里百炼（DashScope）大模型，支持海洋生物图片识别
- **高光时刻**：AI 自动分析社区活跃内容，生成推荐文案并全站广播

### 🔐 安全设计
- Spring Security + JWT 无状态认证
- RBAC 三级角色（ADMIN / MANAGER / USER）
- 路由级守卫 + API 级鉴权
- XSS 防护（DOMPurify）
- 操作日志审计

### 📊 数据可视化
- ECharts 多维度统计图表
- 物种保护等级分布、分类学层级统计
- AI 问答词云
- 用户活跃度趋势

### 🏆 成长激励
- 答题积分 + 等级成长体系
- 错题本自动收集
- 积分商店兑换
- 成就徽章系统

---

> © 2026 海洋学堂 · 探索蔚蓝世界
