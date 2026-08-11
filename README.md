# 校园闲置物品二手交易平台（campus-idle-cloud）

基于 **Spring Cloud 微服务架构** 的校园二手闲置物品交易平台，提供商品发布与交易、购物车、订单、私信聊天、关注收藏、消息通知等完整闭环，并配套一套 Vue3 管理后台。

## 📌 项目简介

面向大学生校园场景的二手闲置物品交易平台，采用微服务 + 前后端分离架构：

- **后端**：Spring Boot 3 / Spring Cloud Alibaba 微服务集群（网关、认证、用户、商品、订单、管理后台）
- **前端**：Vue 3 + TypeScript + Pinia + Vite（用户端 + 管理后台）
- **基础设施**：Nacos 注册配置中心、MySQL、Redis、Kafka、Sentinel、Nginx

## ✨ 功能特性

### 用户端
- 注册 / 登录（密码登录 + 短信验证码登录）、忘记密码
- 商品发布 / 编辑 / 上下架，图片上传（阿里云 OSS）
- 商品浏览、关键词搜索、分类筛选、热门商品
- 购物车、订单（购买 / 出售）、收货地址管理
- 收藏、关注 / 粉丝、用户主页
- 站内私信聊天（Kafka 驱动）、消息中心（系统通知）
- 个人中心、账号设置（头像 / 昵称 / 邮箱）

### 管理后台
- 数据概览、用户管理、商品管理、订单管理
- 商品分类管理、轮播图（系统图片）管理
- 系统消息推送、管理员管理

## 🛠 技术栈

| 分类 | 技术 |
| --- | --- |
| 开发语言 | Java 21、TypeScript |
| 微服务框架 | Spring Boot 3.3.0、Spring Cloud 2023.0.3、Spring Cloud Alibaba 2023.0.1.0 |
| 网关 | Spring Cloud Gateway |
| 注册配置中心 | Nacos 2.3.2 |
| 持久化 | MySQL 8.0 + MyBatis-Plus |
| 缓存 | Redis 8.0 |
| 消息队列 | Apache Kafka 7.5.0 + ZooKeeper |
| 限流熔断 | Sentinel Dashboard 1.8.6 |
| 文件存储 | 阿里云 OSS |
| 前端 | Vue 3、Vite 8、Pinia、Vue Router、Axios |
| 部署 | Docker / Docker Compose（多阶段构建）、Nginx、GitHub Actions CI/CD |

## 🏗 项目结构

```
campus-idle-cloud/
├── Dockerfile         # 根多阶段构建：镜像内编译全部后端微服务（服务器无需 JDK/Maven/Node）
├── docker-compose.yml # 一键编排基础设施 + 全部微服务（密钥从 .env 读取）
├── .env.example       # 环境变量模板（复制为 .env 后填写真实密钥）
├── common/            # 公共模块：实体、DTO/VO、JWT、OSS、Kafka、Redis、统一异常与响应
├── campus-gateway/    # 网关服务 (8080)：路由转发、JWT 鉴权、跨域
├── campus-auth/       # 认证服务 (8081)：登录/注册/重置密码、默认管理员初始化
├── campus-user/       # 用户服务 (8082)：个人中心、购物车、收藏、关注、私信、消息、地址
├── campus-item/       # 商品服务 (8083)：商品 CRUD、分类、搜索、文件上传（OSS）
├── campus-order/      # 订单服务 (8084)：订单创建/支付流程、订单快照
├── campus-admin/      # 管理后台服务 (8085)：用户/商品/订单/分类/轮播图/系统消息管理
├── front/             # 前端工程：Vue3 用户端 + 管理后台
├── sql/               # SQL 初始化脚本 + 增量补丁
├── nginx/             # Nginx 镜像构建（前端托管 + 反代）与配置
├── scripts/           # 运维脚本（数据库备份等）
├── DEPLOYMENT.md      # 生产部署手册（服务器准备、迁移、备份、CI/CD、验证）
└── .github/           # GitHub Actions 自动构建部署工作流
```

### 微服务架构

```
                        ┌────────────┐
       浏览器 ───────────►│   Nginx    │  :8088 / :8443（前端静态页 + /api 反代）
                        └─────┬──────┘
                              │ /api
                        ┌─────▼──────┐        ┌──────────────┐
                        │  Gateway   │────────► Nacos 注册中心│
                        │   :8080    │        └──────────────┘
                        └──┬───┬──┬──┘
           ┌───────────────┘   │  └────────────────┐
           ▼                   ▼                    ▼
     ┌──────────┐      ┌──────────┐          ┌──────────┐
     │campus-auth│      │campus-user│  ...    │campus-admin│
     │   :8081  │      │   :8082  │          │   :8085  │
     └──────────┘      └────┬─────┘          └──────────┘
                            │ Kafka / MySQL / Redis / OSS
```

### 数据库

每个业务服务使用独立数据库，登录走 `campus_auth` 库：

| 数据库 | 归属服务 |
| --- | --- |
| campus_auth | 认证服务（含用户登录信息） |
| campus_user | 用户服务 |
| campus_item | 商品服务 |
| campus_order | 订单服务 |
| campus_admin | 管理后台服务 |

## 🚀 快速开始

> 📖 **生产部署请看 [DEPLOYMENT.md](DEPLOYMENT.md)**（服务器准备、环境变量、数据库迁移、备份、CI/CD、验证清单）。

> ⚠️ **重要：安全配置**（详见下方「环境变量」一节）
> 仓库中的敏感信息已替换为占位符，运行前请先替换为真实值：
> - 所有数据库 / Redis 密码默认占位 `changeme`
> - 阿里云 OSS 密钥默认占位 `your-aliyun-access-key-id` / `your-aliyun-access-key-secret`

### 方式一：Docker Compose 一键启动（推荐）

环境要求：Docker、Docker Compose。镜像采用**多阶段构建**，服务器上无需安装 JDK/Maven/Node —— `docker compose up --build` 会在镜像内完成全部编译与前端打包。

```bash
# 0. 配置环境变量（复制模板并填写真实密钥，详见下方「环境变量」一节）
cp .env.example .env

# 1. 启动全部服务（MySQL、Nacos、Redis、Kafka、Sentinel、各微服务、Nginx）
docker compose up -d --build

# 2. 查看状态
docker compose ps
```

> 前端（用户端 + 管理后台）由 Nginx 镜像内构建托管，用户端默认通过 http://localhost:8088 访问（含 `/admin` 管理后台）。

> Nacos 控制台：http://localhost:8848/nacos（默认账号 `nacos`/`nacos`，**已开启认证**，首次登录请修改；服务连接账号与 token 见 `.env`）
> Sentinel 控制台：http://localhost:8858（默认账号 `sentinel`/`sentinel`）

> 数据库增量补丁脚本（`sql/` 下，如 `patch_fix_missing_schema.sql`）的执行方式见 [DEPLOYMENT.md](DEPLOYMENT.md#4-数据库初始化与迁移)。

### 方式二：本地手动启动（开发调试）

**1. 启动基础设施（Docker）**

```bash
# 只启动基础设施，微服务在 IDE 中分别启动
docker compose up -d mysql nacos redis zookeeper kafka sentinel
```

**2. 初始化数据库**

`sql/init.sql` 已挂载为 MySQL 容器初始化脚本，也可手动导入到各 `campus_*` 库。

**3. 启动后端微服务（IDE 或命令行）**

模块依赖顺序：`common` → `campus-gateway` / `campus-auth` / `campus-user` / `campus-item` / `campus-order` / `campus-admin`。

```bash
mvn clean install -DskipTests          # 根目录先打包 common 等公共依赖
mvn spring-boot:run -pl campus-gateway
# 依次启动其余微服务...
```

**4. 启动前端**

```bash
cd front
npm install
npm run dev        # 默认 http://localhost:5173（已配置 /api 代理到网关 8080）
```

### 访问地址

| 入口 | 地址 |
| --- | --- |
| 用户端（开发） | http://localhost:5173 |
| 用户端（Nginx） | http://localhost:8088 |
| 管理后台 | http://localhost:5173/admin 或 http://localhost:8088/admin |
| 网关 | http://localhost:8080 |

## 🔐 环境变量与安全配置

所有服务均支持通过环境变量覆盖默认配置（`${VAR:default}` 形式）。**推送 / 部署前请务必替换占位符：**

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MYSQL_ROOT_PASSWORD` | `changeme` | MySQL root 密码（docker-compose） |
| `MYSQL_PASSWORD` | `changeme` | 各服务 MySQL 连接密码 |
| `REDIS_PASSWORD` | `changeme` | Redis 密码 |
| `OSS_ACCESS_KEY_ID` | `your-aliyun-access-key-id` | 阿里云 OSS AccessKey ID |
| `OSS_ACCESS_KEY_SECRET` | `your-aliyun-access-key-secret` | 阿里云 OSS AccessKey Secret |
| `OSS_ENDPOINT` | `oss-cn-beijing.aliyuncs.com` | OSS endpoint |
| `OSS_BUCKET_NAME` | `campus-idle` | OSS Bucket 名称 |
| `JWT_SECRET` | `your-secret-key-change-in-production` | JWT 签名密钥（所有服务共享，生产必须更换） |
| `NACOS_SERVER_ADDR` | `localhost:8848` | Nacos 地址 |
| `NACOS_AUTH_ENABLE` | `true` | Nacos 认证开关（遇兼容性问题可临时关） |
| `NACOS_USERNAME` | `nacos` | 服务连接 Nacos 的账号（与控制台账号一致） |
| `NACOS_PASSWORD` | `nacos` | 服务连接 Nacos 的密码 |
| `NACOS_AUTH_TOKEN` | 见 `.env.example` | Nacos 认证 token（base64，>32 字节，生产必须更换） |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:8088,http://localhost:5173` | 网关允许跨域的来源（逗号分隔） |
| `KAFKA_SERVER` | `localhost:9092` | Kafka 地址 |

> **注意**：以上变量统一从 `.env` 读取（模板见 [.env.example](.env.example)），生产环境请填写真实凭据，不要使用默认值。`.env` 已被 `.gitignore` 忽略，不会上传 git。

### 默认账号

| 角色 | 手机号 | 密码 |
| --- | --- | --- |
| 管理员 | 13800138000 | admin123 |
| 管理员 | 13800000000 | 123456 |

> 默认管理员由认证服务启动时自动初始化（`AdminInitializer`），首次登录后请尽快修改。

## 🌐 网关路由

| 前缀 | 后端服务 | 端口 |
| --- | --- | --- |
| `/api/auth/**` → `/auth/**` | campus-auth | 8081 |
| `/api/user/**` → `/user/**` | campus-user | 8082 |
| `/api/product/**`、`/api/item/**` → `/item/**` | campus-item | 8083 |
| `/api/cart/**`、`/api/favorite/**`、`/api/chat/**`、`/api/follow/**`、`/api/notification/**` | campus-user | 8082 |
| `/api/orders/**`、`/order/**` | campus-order | 8084 |
| `/api/admin/**` → `/admin/**` | campus-admin | 8085 |

网关通过全局 `AuthFilter` 校验 JWT，将用户身份写入 `X-User-*` 请求头转发给下游，并剥离客户端伪造的同名头。

## 🔌 端口一览

| 端口 | 服务 |
| --- | --- |
| 8080 | campus-gateway |
| 8081 | campus-auth |
| 8082 | campus-user |
| 8083 | campus-item |
| 8084 | campus-order |
| 8085 | campus-admin |
| 5173 | Vite 前端开发服务 |
| 8088 / 8443 | Nginx (HTTP / HTTPS) |
| 13306 | MySQL（宿主机映射） |
| 6379 | Redis |
| 8848 / 9848 / 9849 | Nacos |
| 2181 | ZooKeeper |
| 9092 / 29092 | Kafka |
| 8858 | Sentinel Dashboard |

## 📖 后续规划

持续完善微服务模块与业务功能（评价体系、支付对接、消息推送、数据统计等）。

## 📄 License

本项目基于 [MIT License](LICENSE) 开源。
