# 审批流系统 (Approval Flow)

轻量级审批流组件，支持动态表单、多级审批、撤回/驳回/修改重提等完整流程。可嵌入其他业务系统，也提供独立的 Vue 3 + Element Plus 演示前端。

## 技术栈

### 后端

- Java 21 + Spring Boot 3.5.0
- MyBatis-Plus（Lambda 安全查询）
- Sa-Token（RBAC 权限认证 + 限流）
- Aviator（轻量级表达式引擎，用于节点条件判断）
- MySQL 8.0+（JSON 类型存储动态表单结构）
- Redis 7+（Sa-Token 限流 + 缓存）
- RocketMQ 4.9+（本地消息表发件箱，事件最终一致）
- BCrypt（密码加密，登录安全设计的固有成本）

### 前端

- Vue 3 + Vite
- Element Plus
- Axios + Pinia
- Vue Router

## 核心功能

**用户域**
- **用户认证**：登录、注册（默认 STAFF 角色）、登录限流（基于 Sa-Token + Redis）
- **个人信息**：修改邮箱、修改密码、查看自己的通知与任务

**审批域**
- **动态表单**：管理员可配置任意 JSON 表单字段，用户发起时自动渲染
- **申请发起**：支持草稿保存、正式提交、撤回、修改重提（驳回后进入新一轮审批）
- **审批流程**：逐级审批（条件表达式控制走向）、通过/驳回/转交
- **消息通知**：待办提醒、审批结果通知，按用户聚合展示未读数
- **完整状态机**：5 状态（0草稿 → 1审批中 → 2通过 / 3驳回 / 4撤回），驳回后可修改重提交

**管理域**（ADMIN 角色）
- **流程定义**：配置流程名称、适用角色、表单结构、审批节点（相对关系/指定用户/指定角色）
- **用户管理**：增删改查用户、重置密码、修改上下级关系
- **MQ 发件箱管理**：实时查看本地消息表四类状态（待发/退避中/已发/死信）、消息检索与人工干预

## 相关文档

- [《审批流组件设计文档》](docs/审批流组件设计文档.md) - 系统架构、数据库设计、状态机、接口规范、核心难点解析
- [《性能压测报告》](docs/性能压测报告.md) - 5 场景基线、归因诊断、2 项优化与容量扫描的完整实验记录
- [《Java后端开发实践指南》](docs/Java后端开发实践指南.md) - Spring Boot / MyBatis-Plus / Sa-Token / Aviator 实战总结
- [《Vue前端开发笔记》](docs/Vue前端开发笔记.md) - Vue 2→3 迁移、Composition API、工程化实践

## 性能压测

按「基线 → 归因 → 优化 → 对比」的流程分场景压测（k6 + Prometheus + Grafana），业务验收目标 30 QPS、p95 < 1s。共 5 个场景基线、2 项优化与 1 项容量扫描，完整测量数据与归因见 **[《性能压测报告》](docs/性能压测报告.md)**。

**基线 vs 优化后（最终对照）**：

| 场景 | 基线 QPS | 优化后 QPS | 变化 | 基线 p95 (s) | 优化后 p95 (s) | 变化 |
| ---- | -------- | ---------- | ---- | ------------ | -------------- | ---- |
| login\* | 29.60 | 86.00 | **+191%** | 2.1362 | 0.23 | **-89%** |
| submit | 74.66 | 72.91 | -2% | 0.7831 | 0.776 | 持平 |
| approve | 88.11 | 109.31 | +24% | 1.1315 | 1.232 | +9% |
| pending-list | 73.82 | 97.71 | +32% | 0.78 | 0.131 | **-83%** |
| outbox-flush | 13.90 条/s | 40.65 条/s | **+192.5%** | — | — | — |

\* login 优化为部署层变更（JVM 绑定 P 核），非代码改动；submit 优化后 QPS 持平，但容量扫描实测 **210 QPS**（业务目标 7 倍余量），90 QPS 封顶归因 k6 负载模型而非系统瓶颈。

两项优化均为配置级改动（HikariCP 10→30、outbox batch 100→500 + 扫描间隔 5s→1s），代码零改动，全程错误率 0%、死信 0。

**生产部署建议**：

| 配置级别 | 规格 | 预期能力 | 适用场景 |
| -------- | ---- | -------- | -------- |
| 最小可行 | 2 核 4G + HK=30 | 70+ QPS | 内部 demo / 小团队（<50 用户） |
| 推荐 | 4 核 8G + HK=30 + 中间件独立部署 | 200+ QPS | 中小企业生产（<500 用户） |

**关键参数**：HK ≥ 30、mysql `max_connections` ≥ 150、`innodb_flush_log_at_trx_commit` 生产建议 1（持久性优先），开发/压测可用 2。

**监控阈值**：`hk_pending` 持续 > 20 / `tc_busy` > 80% / `swap_used` > 1G / `mq_outbox` PENDING 净增长。

**outbox 容量规划**：scheduler 吞吐 40.65 条/s 覆盖业务目标 30/s（1.35× 余量），生产峰值 > 40/s 持续积压时可启用并行发送改造（预期 100-150/s）。

## 环境要求

- JDK 21+
- Node.js 20+（`start.js` 启动脚本要求）
- MySQL 8.0+、Redis 7+、RocketMQ 4.9+（三项中间件均由 `middleware/docker-compose.yml` 一键编排，或自行部署）
- Maven 3.8+（或直接使用项目自带的 `mvnw` 包装器，无需本机安装）

## 快速启动（本地开发）

### 1. 克隆代码

```bash
git clone https://gitee.com/bide-time/approval-flow.git
cd approval-flow
```

### 2. 启动中间件（MySQL / Redis / RocketMQ）

`middleware/` 目录提供所需中间件的一键编排，网络与数据卷均显式命名，与所在目录名解耦（拷贝到任意目录运行结果一致）：

| 服务 | 容器名 | 端口 | 用途 |
|------|-------|------|------|
| MySQL 8.0 | `approval-mysql` | 3306 | 业务库 + 本地消息表 |
| Redis 7 | `approval-redis` | 6379 | Sa-Token 限流 |
| RocketMQ NameServer | `rmqnamesrv` | 9876 | 注册中心 |
| RocketMQ Broker | `rmqbroker` | 10909 / 10911 | 消息代理 |
| RocketMQ Dashboard | `rmqdashboard` | 8082 | 控制台（`http://localhost:8082`） |

> **启动前：按部署形态调整 IP**（compose 本身不挑平台，Windows Docker Desktop / Linux 均可运行；环境相关的只有以下两处，**先改再启动**，否则 RocketMQ 收发会失败）：
>
> | 部署形态 | `middleware/broker.conf` 的 `brokerIP1` | `MW_HOST`（后端 dev 配置） |
> |---------|----------------------------------------|-----------------------------------|
> | Docker 装在本机（Windows Docker Desktop / 本地 Linux） | 改为 `127.0.0.1` | 无需设置（默认 `127.0.0.1`） |
> | 中间件在 VMware 虚拟机 / 远程 Linux 服务器 | 虚拟机 / 服务器的可达 IP | 设为同左，创建方式见下 |
>
> 原因：RocketMQ 客户端先连 NameServer 获取 Broker 地址再直连，`brokerIP1` 声明错误则发送 / 消费全部失败。
>
> `MW_HOST` 的设置方式：在 `backend/` 下新建 `.env` 文件（properties 格式，已被 gitignore 忽略不入库），写入一行（IP 仅为示例，替换为你的中间件机器的实际可达地址）：
>
> ```properties
> MW_HOST=192.168.149.128    # 例：中间件部署在 192.168.149.128 这台虚拟机上
> ```
>
> 后端启动时经 `application-dev.yml` 的 `config.import` 自动读取该文件；Docker 装本机则无需创建（默认 `127.0.0.1`）。

```bash
cd middleware
docker compose up -d      # 后台启动全部中间件
docker compose down       # 停止（保留数据卷）
docker compose down -v    # 停止并清空数据卷（下次启动重新初始化）
```

- 首次启动（数据卷为空）时 MySQL 自动执行 `schema.sql` 初始化 `approval_flow` 数据库与种子数据（流程定义、节点配置、用户角色等），无需手动建表
- 不使用 Docker 时请自行部署中间件，并手动执行初始化脚本：

```bash
mysql -u root -p -e "CREATE DATABASE approval_flow DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p approval_flow < middleware/schema.sql
```

### 3. 启动服务

#### 方式一：一键启动脚本（推荐）

项目根目录提供了 Node.js 脚本，可同时启动前后端服务：

```bash
# 启动
node start.js

# 停止
node stop.js
```

- 脚本会自动检测 Java / Node.js 环境、安装前端依赖
- 后端（Spring Boot，端口 8080）先启动，等待 15 秒后再启动前端（Vite，端口 5173）
- 前端 Vite 代理自动将 `/api` 请求转发至后端 8080，无跨域问题

#### 方式二：手动启动

分别启动后端和前端：

```bash
# 后端（推荐使用项目自带的 mvnw 包装器，无需本机安装 Maven）
cd backend
./mvnw spring-boot:run   # Windows: mvnw.cmd spring-boot:run

# 前端（另开一个终端）
cd frontend
npm install
npm run dev
```

后端默认监听 `8080` 端口，前端默认监听 `5173` 端口。

### 4. 运行测试

后端已内置 17 个测试用例，覆盖发件箱状态机、消费者幂等、调度器异常隔离、限流、并发审批等核心逻辑：

```bash
cd backend
./mvnw test                       # 跑全部非 manual 标签的测试
./mvnw test -Dgroups=manual       # 跑 RocketMQ 端到端冒烟测试（需真实 MQ 环境）
```

### 5. 配置说明（环境变量与 profile）

配置按「公共 + 环境」两层拆分，三个文件均入库：

| 文件                               | 职责         | 内容                                                             |
| ---------------------------------- | ------------ | ---------------------------------------------------------------- |
| `backend/.../application.yml`      | 公共层       | 应用名、Sa-Token、MyBatis-Plus 通用配置、MQ 组名与重试策略       |
| `backend/.../application-dev.yml`  | 本地开发环境 | 中间件地址 `MW_HOST`（`backend/.env` 或环境变量，默认 `127.0.0.1`）、打印 SQL、debug 日志 |
| `backend/.../application-prod.yml` | 生产环境     | 连接信息全部由环境变量注入（无默认值）、关闭 SQL 打印、info 日志 |

规律：**`application.yml` 只回答「有哪些配置项」，`application-{profile}.yml` 只回答「这套环境的值是什么」**。切换环境无需改代码，也无需改任何配置文件——dev 改 `MW_HOST` 环境变量，prod 注入各自的连接变量即可。

```bash
# 本地开发：默认激活 dev，配置来自 application-dev.yml
./mvnw spring-boot:run

# 生产部署：显式指定 profile 并注入真实连接信息
SPRING_PROFILES_ACTIVE=prod \
MYSQL_HOST=10.0.0.12 MYSQL_USERNAME=app_flow MYSQL_PASSWORD=*** \
REDIS_HOST=10.0.0.13 REDIS_PASSWORD=*** \
ROCKETMQ_NAME_SERVER=10.0.0.14:9876 \
java -jar target/approval-flow-backend.jar
```

### 6. 压测与观测（可选）

压测脚本与观测栈已入库，配合[《性能压测报告》](docs/性能压测报告.md)可复现全部实验。

> **启动前：按后端位置调整压测目标**（k6 与 Prometheus 均默认后端与压测工具同机——全在本机则无需任何改动；后端在虚拟机 / 远程机器时，环境相关的只有以下两处，**先改再用**）：
>
> | 组件 | 推荐改法 |
> |------|---------|
> | k6（跑在宿主机） | 在运行 k6 的机器上执行一次 `echo 'export TARGET=http://<后端IP>:8080' >> ~/.bashrc`，再执行 `source ~/.bashrc`（当前终端立即生效，或重开终端），之后所有场景直接 `k6 run xxx.js` |
> | Prometheus（跑在容器内） | `observability/prometheus.yml` 中 backend 的 targets 改为 `<后端IP>:8080`，然后 `docker compose restart prometheus` |
>
> 写法为何不同：k6 在宿主机读环境变量即可；Prometheus 在容器内，`localhost` 指容器自身、抓不到宿主后端，必须写后端可达地址。

**启动观测栈**（需先完成第 2 步启动中间件）：

```bash
cd observability
docker compose up -d
```

- Grafana：`http://localhost:3000`（admin / admin），预置 4 个面板（API 摘要 / JVM / MySQL-Redis / Tomcat-Hikari）
- Prometheus：`http://localhost:9090`，默认抓取目标 `host.docker.internal:8080`（Prometheus 运行在容器内，该域名指其宿主机——后端与观测栈同机时零配置）

**执行压测**（需安装 [k6](https://k6.io)）：

```bash
cd k6
chmod +x *.sh            # 首次需要
./seed.sh                # 造压测用户（Windows PowerShell/CMD 改用 `bash *.sh`）
k6 run login.js          # 场景脚本：login / submit / approve / pending-list
./cleanup.sh             # 清空压测数据
```

## 项目结构

```
approval-flow/
├── backend/                # Spring Boot 后端
│   ├── src/main/resources/
│   │   ├── application.yml             # 公共配置（环境无关）
│   │   ├── application-dev.yml         # 本地开发环境
│   │   └── application-prod.yml        # 生产环境（环境变量注入）
│   └── pom.xml
├── frontend/               # Vue 3 前端
│   ├── src/
│   ├── .env.development
│   └── package.json
├── docs/
│   ├── 审批流组件设计文档.md  # 系统架构、数据库设计、状态机、接口规范
│   ├── 性能压测报告.md        # 压测完整实验记录（基线 / 归因 / 优化 / 容量扫描）
│   ├── Java后端开发实践指南.md
│   └── Vue前端开发笔记.md
├── middleware/             # 中间件一键编排（MySQL/Redis/RocketMQ/Dashboard）
│   ├── docker-compose.yml
│   ├── broker.conf         # RocketMQ Broker 配置（brokerIP1 按环境修改）
│   └── schema.sql          # 建表脚本及初始数据（首次启动 MySQL 自动执行）
├── k6/                     # k6 压测脚本与数据工具（TARGET 环境变量指定后端地址）
├── observability/          # 观测栈编排（Prometheus + Grafana + 3 个 exporter）
├── start.js                # 一键启动脚本（同时启动前后端）
├── stop.js                 # 一键停止脚本（按端口终止进程）
├── LICENSE                 # MIT 许可证
├── .gitignore
└── README.md
```

## 许可证

本项目采用 [MIT License](LICENSE) 开源协议。
