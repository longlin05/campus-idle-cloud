# 部署手册（单机云服务器 + Docker Compose）

> 本手册面向**单台 Linux 云服务器**的部署。全部服务由 `docker-compose.yml` 编排，
> 镜像采用**多阶段构建**（Maven/Node 构建在镜像内完成），服务器上无需安装 JDK/Maven/Node。

## 1. 前置条件

| 项目 | 要求 |
| --- | --- |
| 操作系统 | Linux（Ubuntu 22.04 / CentOS 7+ 等，x86_64） |
| Docker | ≥ 24（含 Compose v2 插件） |
| 内存 | ≥ 8GB（Nacos/Kafka/MySQL 各占约 1G，JVM 堆已有上限） |
| 磁盘 | ≥ 30GB（镜像、数据卷、日志） |
| 网络 | 可访问 Docker Hub（拉取 maven/node/mysql 等基础镜像） |

安装 Docker：

```bash
curl -fsSL https://get.docker.com | sh
systemctl enable --now docker
docker compose version   # 确认有 docker compose v2
```

## 2. 首次部署（一键）

```bash
# 1. 克隆代码
git clone <你的仓库地址> /opt/campus-idle-cloud
cd /opt/campus-idle-cloud

# 2. 配置环境变量（务必逐项填写！）
cp .env.example .env
vi .env        # 修改 JWT_SECRET、MySQL/Redis 密码、OSS 密钥、NACOS_AUTH_TOKEN 等

# 3. 构建并启动全部服务（首次构建约 10~20 分钟，取决于服务器性能与网络）
docker compose up -d --build

# 4. 查看状态（全部应为 healthy/running）
docker compose ps
```

> 首次启动时 MySQL 容器会自动执行 `sql/init.sql` 初始化 5 个数据库。
> **初始化脚本只在数据卷为空时执行**；已有数据卷不会重复执行。

### 防火墙放行（云服务器安全组 / firewalld）

| 端口 | 用途 |
| --- | --- |
| 8088 | 用户端 + 管理后台（Nginx，主入口） |
| 8443 | HTTPS（预留，尚未启用） |
| 8080 | 网关（调试用，生产可只对内网开放） |
| 8848 | Nacos 控制台（生产建议只对管理员 IP 开放） |
| 8858 | Sentinel 控制台（生产建议只对管理员 IP 开放） |
| 13306 / 6379 / 9092 | MySQL / Redis / Kafka 调试入口（生产建议防火墙关闭） |

## 3. 环境变量说明

见 [.env.example](.env.example)。**生产必须修改**：

| 变量 | 修改建议 |
| --- | --- |
| `JWT_SECRET` | `openssl rand -base64 48` 生成，所有服务共享 |
| `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` | 两处保持一致（应用以 root 连接） |
| `REDIS_PASSWORD` | 随机强密码 |
| `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` | 阿里云 RAM 子账号最小权限 Key |
| `NACOS_AUTH_TOKEN` | `openssl rand -base64 48`，必须是 base64 且解码后 >32 字节 |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | 登录 Nacos 控制台修改密码后保持同步 |
| `CORS_ALLOWED_ORIGINS` | 若前后端同源部署（默认 Nginx 托管）无需改；跨源时填实际域名/IP |

## 4. 数据库初始化与迁移

### 初始化（首次部署自动完成）
- `sql/init.sql` 挂载为 MySQL 容器初始化脚本，自动创建 `campus_auth` / `campus_user` / `campus_item` / `campus_order` / `campus_admin` 五个库及全部表。
- `sql/mysql-charset.cnf` 已挂载，全链路 utf8mb4，避免中文乱码。

### 增量补丁（新部署或从旧版本升级时，手动执行）

| 脚本 | 用途 | 执行位置 |
| --- | --- | --- |
| [sql/patch_fix_missing_schema.sql](sql/patch_fix_missing_schema.sql) | 结构完整补丁：缺失字段/表一次性补齐 | 全库 |
| [sql/create_user_tables.sql](sql/create_user_tables.sql) | campus_user 补建 Cart/CartItem/Notification 表 | `campus_user` |
| [sql/patch_category_fields.sql](sql/patch_category_fields.sql) | idle_category 补齐 status/update_time/is_deleted 列 | `campus_item` / `campus_admin` / `campus_order` |
| [sql/migration_add_order_snapshot.sql](sql/migration_add_order_snapshot.sql) | order_info 添加商品快照字段 | `campus_order` |
| [sql/alter_user_notification_add_batch_no.sql](sql/alter_user_notification_add_batch_no.sql) | user_notification 添加 batch_no 批次号 | `campus_user` |
| [sql/fix_category_data.sql](sql/fix_category_data.sql) | 修复 idle_category 中文乱码数据 | 含该表的库 |
| [sql/cleanup_data.sql](sql/cleanup_data.sql) | 清理商品/订单数据（慎用） | 全库 |

执行示例（在服务器项目目录）：

```bash
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" campus_item' < sql/patch_category_fields.sql
```

> ⚠️ 各补丁脚本**不是幂等的**（如无 `IF NOT EXISTS`），重复执行可能报错，注意记录已执行的脚本。

## 5. 备份与恢复

### 备份（脚本保留最近 7 份）

```bash
./scripts/backup-db.sh
```

定时任务（每天 03:00）：

```bash
crontab -e
0 3 * * * cd /opt/campus-idle-cloud && ./scripts/backup-db.sh >> ./backups/backup.log 2>&1
```

备份文件在 `./backups/campus_db_*.tar.gz`，**建议定期同步到其他机器/对象存储**。

### 恢复

```bash
tar -xzf backups/campus_db_20260811_030000.tar.gz -C /tmp
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < /tmp/campus_auth_20260811_030000.sql
# 其余库同理
```

## 6. CI/CD 自动部署（GitHub Actions）

推送 `main` 分支后自动：构建镜像 → 推送镜像仓库 → SSH 登录服务器拉取并重启。

### 配置仓库 Secrets

在 GitHub 仓库 **Settings → Secrets and variables → Actions** 添加：

| Secret | 示例值 |
| --- | --- |
| `IMAGE_PREFIX` | `registry.cn-hangzhou.aliyuncs.com/你的命名空间/`（含尾部斜杠） |
| `REGISTRY_HOST` | `registry.cn-hangzhou.aliyuncs.com` |
| `REGISTRY_USERNAME` | 镜像仓库账号 |
| `REGISTRY_PASSWORD` | 镜像仓库密码 |
| `DEPLOY_HOST` | 服务器公网 IP |
| `DEPLOY_USER` | 服务器 SSH 用户（如 `root`） |
| `DEPLOY_SSH_KEY` | 服务器 SSH 私钥（PEM 格式） |
| `DEPLOY_PATH` | 服务器上仓库目录，如 `/opt/campus-idle-cloud` |

### 服务器侧一次性准备

```bash
# 1. 首次克隆（若尚未部署）
git clone <仓库> /opt/campus-idle-cloud && cd /opt/campus-idle-cloud
cp .env.example .env && vi .env

# 2. 让 git pull 跳过 .env（已 gitignore，git pull 不会动它）
```

之后每次 push main，工作流自动执行 `git pull + docker compose pull + docker compose up -d`。

## 7. 验证清单

部署完成后逐项验证：

- [ ] `docker compose ps` 全部 `healthy`
- [ ] 浏览器打开 `http://<服务器IP>:8088` 显示首页
- [ ] 打开 `http://<服务器IP>:8088/admin` 显示管理员登录页
- [ ] 注册/登录普通账号成功
- [ ] 发布商品、上传图片成功（OSS）
- [ ] 打开 Nacos 控制台 `http://<IP>:8848/nacos`，各服务均在线注册
- [ ] 中文（商品标题、分类名）显示无乱码
- [ ] 修改默认管理员密码（`13800138000/admin123` 或 `13800000000/123456`）

## 8. 常见问题

| 问题 | 排查 |
| --- | --- |
| 服务反复重启 / 注册不上 Nacos | `docker compose logs -f micro-xxx` 看日志；确认 `.env` 的 `NACOS_USERNAME/PASSWORD` 与控制台一致 |
| 上传 413 | Nginx 已设 `client_max_body_size 10m`，如仍报错检查网关/后端配置 |
| 中文乱码 | 确认 MySQL 容器已加载 utf8mb4 配置：`docker compose exec mysql mysql -uroot -p -e "SHOW VARIABLES LIKE 'character_set%'"` |
| 前端 404 | Nginx `try_files` 已配 SPA 路由，确认访问的是 8088 而非 5173（5173 是开发端口） |
| 启动顺序 | compose `depends_on` + healthcheck 已保证顺序；若 Nacos 未就绪时服务已启动，稍等自动重连 |

## 9. 升级流程

```bash
# 手动方式
cd /opt/campus-idle-cloud
git pull
docker compose up -d --build        # 重新构建并滚动重启
docker compose ps

# CI 方式：直接 push 到 main，工作流自动完成
```

> 数据库结构变更时，升级前先备份（第 5 节），再按第 4 节执行对应补丁脚本。
