# 审批流系统 (Approval Flow)

轻量级审批流组件，支持动态表单、多级审批、撤回/驳回/修改重提等完整流程。可嵌入其他业务系统，也提供独立的 Vue 3 + Element Plus 演示前端。

## 技术栈

### 后端

- Java 21 + Spring Boot 3.5.0
- MyBatis-Plus（Lambda 安全查询）
- Sa-Token（RBAC 权限认证）
- Aviator（轻量级表达式引擎，用于节点条件判断）
- MySQL 8.0+（JSON 类型存储动态表单结构）
- BCrypt（密码加密）

### 前端

- Vue 3 + Vite
- Element Plus
- Axios + Pinia
- Vue Router

## 核心功能

- **用户认证**：登录、注册（默认 STAFF 角色）
- **动态表单**：管理员可配置任意 JSON 表单字段，用户发起时自动渲染
- **申请发起**：支持草稿保存、正式提交
- **审批流程**：逐级审批（条件表达式控制走向）、通过/驳回/撤回/修改重提
- **流程定义**：管理员配置流程名称、适用角色、表单结构、审批节点（相对关系/指定用户/指定角色）
- **用户管理**：管理员增删改查用户、重置密码、修改上下级关系
- **个人信息**：修改邮箱、修改密码（弹窗形式）
- **完整状态机**：草稿 → 审批中 → 通过/驳回/撤回 → 修改重提

## 相关文档

- [《审批流组件设计文档》](docs/审批流组件设计文档.md) - 系统架构、数据库设计、状态机、接口规范、核心难点解析
- [《Java后端开发实践指南》](docs/Java后端开发实践指南.md) - Spring Boot / MyBatis-Plus / Sa-Token / Aviator 实战总结
- [《Vue前端开发笔记》](docs/Vue前端开发笔记.md) - Vue 2→3 迁移、Composition API、工程化实践

## 性能压测

按「基线 → 归因 → 优化 → 对比」的流程分场景压测，工具链 k6 + Prometheus + Grafana。五个场景基线已全部完成，下阶段进入优化后重跑对比。

**测量纪律**：每轮压测前清理业务数据（`approval_instance` / `approval_task` / `mq_outbox` + Redis 限流 key，不动 user / role / department / definition 种子），随后等待 30s 让 outbox scheduler 与 consumer 收敛；正式取数前先跑预热轮预热 mysql buffer pool / OS page cache，冷机首轮数据不作数。

### 测量环境

- 被测后端：Windows 11，13th Gen i5-13450HX（10C16T），JVM 默认调度
- 施压端 k6 v2.3.0：VMware Ubuntu 虚拟机（2 核 4G），与 MySQL 8.0 / Redis 7 / RocketMQ 4.9.6 同机
- 观测栈：Prometheus（15s 抓取）+ Grafana（4 面板），部署于虚拟机
- 测量时关闭 MyBatis SQL 打印与 debug 日志，排除日志 IO 对延迟的干扰
- 统计窗口 1m（15s 抓取 × 4 点），与 90s 单轮时长匹配

### 基线总览

| 场景 | 轮数 | QPS | p95 (s) | 错误率 | 瓶颈归因 |
| ---- | ---- | --- | ------- | ------ | -------- |
| login | 3 | 29.60 | 2.1362 | 0% | E 核调度 × BCrypt 固有成本（环境约束，代码无罪） |
| submit | 5 | 74.66 | 0.7831 | 0% | HK=10 连接池（submit / scheduler / consumer 三方共享） |
| approve | 5 | 88.11* | 1.1315 | 0% | HK=10 连接池（同 submit） |
| pending-list | 5 | 73.82 | 0.78 | 0% | HK=10 连接池（读写全卡，非仅写路径） |
| outbox-flush | 3 | 13.90 条/s | — | 0% | scheduler 单线程 + 逐条同步发送（与连接池无关） |

\* approve QPS 含每次迭代 2 个请求（GET pending + POST approve），实际 approve 吞吐约为该值的一半；撞任务率 ~1.6% 属可控噪声。

**优化优先级**：HikariCP pool size 10→30 覆盖全部 HTTP 场景（第一抓手）；outbox 消化能力 13.9 < 74 条/s 存在净积压，需并行发送 / 增大 batch（独立优化项）。

### 场景 login：基线

100 VU × 90s × 3 轮，取中位数（P2）：

| 指标 | R1 | R2 | R3 | 中位数 |
| ---- | --- | --- | --- | --- |
| QPS | 27.84 | 29.60 | 30.13 | **29.60** |
| p95 (s) | 2.1408 | 2.1347 | 2.1362 | **2.1362** |
| p99 (s) | 2.4120 | 2.3866 | 2.3898 | **2.3898** |
| 错误率 | 0% | 0% | 0% | **0%** |

### 场景 login：基线归因

数字难看，但诊断链完整，结论是「环境约束而非代码缺陷」：

1. **排除法定位**：CPU 利用率 19% 但延迟 2.1s——线程既不算题也不闲着；Hikari 连接池 pending=0、Redis 平均 0.51ms、k6 与服务端计时吻合、单 VU 仅 228ms——DB / 缓存 / 网络 / 单请求路径全部排除
2. **jstack 抓栈**：所有 Tomcat 线程 RUNNABLE，栈顶集中在 `BCrypt.checkpw`（strength=10，单次约 100-130ms，登录安全设计的固有成本）
3. **逐核利用率**：16 个逻辑核仅 3 个活跃且不饱和——Windows 将后台 JVM 调度至 E 核，有效并行度 ≈ 4
4. **VU 扫描验证**：4/8/16/32 VU 延迟 100→200→400→700ms 线性翻倍，确认有效并行度边界
5. **控制变量实验**：仅绑 P 核（affinity 0xFFF）→ QPS 86 / p95 0.23s；仅切高性能电源 → 无改善。**确认大小核调度是决定性变量，电源计划无关**

| 实验 | QPS | p95 (s) | 结论 |
| ---- | --- | -------- | ---- |
| 默认调度（基线） | 29.60 | 2.1362 | E 核有效算力决定吞吐上限 |
| 绑定 P 核 | 86 | 0.23 | 调度归因实锤，代码无罪 |
| 仅高性能电源 | ~57 峰值 | 2.1 | 电源非决定变量 |

**结论**：login 吞吐上限 = E 核有效算力 ÷ BCrypt 单次成本，属部署环境约束；BCrypt strength=10 为安全设计不做降低，基线数字有效存档。生产部署建议绑定物理核或容器 CPU 亲和性规避此类调度问题。

### 场景 submit：基线

100 VU × 90s × 5 轮，取中位数：

| 指标 | R1 | R2 | R3 | R4 | R5 | 中位数 |
| ---- | --- | --- | --- | --- | --- | --- |
| QPS 峰值 | 65.51 | 76.56 | 74.66 | 74.51 | 75.47 | **74.66** |
| p95 (s) | 1.4223 | 0.4214 | 1.1930 | 0.7687 | 0.7831 | **0.7831** |
| p99 (s) | 1.7336 | 0.4988 | 1.7649 | 0.9996 | 0.9469 | **0.9996** |
| HK_PEND 峰值 | 63 | 26 | 61 | 35 | 36 | **36** |
| 错误率 | 0% | 0% | 0% | 0% | 0% | **0%** |

### 场景 submit：基线归因

1. **连接池硬瓶颈**：HK_MAX=10 长期满载，HK_PEND 峰值 50+，TC_BUSY < 200——Tomcat 线程没用满，硬瓶颈在 HK
2. **P2 抖动来源**：submit / outbox scheduler / RocketMQ consumer 三方共享 HK，scheduler 5s 周期扫表 + consumer 写 task 各持锁 1-2s，submit 排队等待
3. **QPS 提升**：R2 起 QPS 升到 74-78（第 1 次 baseline 仅 53），buffer pool + OS page cache 预热完成

**结论**：submit 吞吐上限 = HK=10 ÷ (submit 事务 + scheduler 周期 + consumer 异步 3 路并发占用)；HK pool size 与 outbox batch 是优化阶段核心抓手。

### 场景 approve：基线

100 VU × 90s × 5 轮，取中位数：

| 指标 | R1 | R2 | R3 | R4 | R5 | 中位数 |
| ---- | --- | --- | --- | --- | --- | --- |
| QPS 峰值 | 88.11 | 84.71 | 94.53 | 95.78 | 76.42 | **88.11** |
| p95 (s) | 1.1315 | 1.7274 | 0.9786 | 0.9253 | 2.5489 | **1.1315** |
| p99 (s) | 1.7303 | 2.3277 | 1.8303 | 1.7578 | 3.1439 | **1.8303** |
| HK_PEND 峰值 | 50 | 63 | 50 | 50 | 31 | **50** |
| approve 通过率 | 98.38% | 97.88% | 98.48% | 98.32% | 98.54% | **98.38%** |
| HTTP 错误率 | 0% | 0% | 0% | 0% | 0% | **0%** |

### 场景 approve：基线归因

1. **撞任务失败 ~1.6%**：pageSize=50 + 随机选 task 把撞任务率从 50%+ 降到 1.6%，残留来自时序问题（list approve 后滚动导致 offset 偏移），不是系统错误
2. **连接池硬瓶颈**：HK_MAX=10，HK_PEND 50 + TC_BUSY < 200 → 同 submit，硬瓶颈在 HK
3. **approve POST 反而比 submit 短（520ms vs 1.71s）**：submit 多一次未缓存 SELECT definition + INSERT instance 主键回填，比 approve 慢；approve 步骤更多但实测更快
4. **QPS 数字含 GET pending**：approve 每次迭代 2 个 HTTP 请求（GET + POST），submit 只有 1 个 POST；k6 的 http_reqs 把两者都计入，approve QPS 计数虚高约 1.5-2x

**结论**：approve 吞吐上限 ≈ HK=10 ÷ (approve 事务 + scheduler 周期 + consumer 异步)；撞任务率 1.6% 属可控噪声，可不处理。

### 场景 pending-list：基线

100 VU × 90s × 5 轮，取中位数：

| 指标 | R1 | R2 | R3 | R4 | R5 | 中位数 |
| ---- | --- | --- | --- | --- | --- | --- |
| QPS 峰值 | 73.82 | 77.86 | 73.18 | 72.10 | 79.55 | **73.82** |
| p95 (s) | 0.77* | 0.84 | 0.77 | 0.99 | 0.78 | **0.78** |
| p99 (s) | 0.97* | 0.86 | 1.36 | 1.40 | 0.91 | **0.97** |
| HK_PEND 峰值 | 78 | 50 | 79 | 32 | 29 | **50** |
| 错误率 | 0% | 0% | 0% | 0% | 0% | **0%** |

\* R1 全周期 p95=5.46s 出现在压测尾部（QPS 已回落至 9.68），非稳态数据；取稳态段（15:50:33~15:51:05）p95≈0.77s，与其余轮次一致。

### 场景 pending-list：基线归因

1. **HK=10 是全路径瓶颈**：纯读路径（3 个 SELECT、无事务无锁）同样 HK_PEND 峰值 50~79，QPS 稳定 72~80——与 submit/approve 同级，确认连接池卡所有路径，不只是写
2. **P95 未达理论 <0.3s**：读请求本身快，但 100 VU 抢 10 个连接排队，等待时间计入 p95
3. **读路径稳定性好于写路径**：无锁无事务，错误率 0%，QPS 波动小（72~80），不受 scheduler/consumer 周期性持连接影响

**结论**：HK=10 是全路径瓶颈（读+写都卡）；优化阶段增大 pool size 是第一优先级抓手，收益覆盖所有场景。

### 场景 outbox-flush：基线

SQL 预置 10k PENDING + shell 轮询 overview 测量（非 k6 场景），3 轮：

| 指标 | R1 | R2 | R3 | 中位数 |
| ---- | --- | --- | --- | --- |
| initial pending | 10000 | 10000 | 10000 | 10000 |
| 总时长（s） | 703 | 719 | 734 | **719** |
| 平均速率（条/s） | 14.22 | 13.90 | 13.62 | **13.90** |
| dead 数 | 0 | 0 | 0 | **0** |

### 场景 outbox-flush：基线归因

1. **实测 13.90 条/s vs 理论 17-18 条/s**：反推每条成本——719s ÷ 100 轮 = 7.19s/轮，减去 fixedDelay 5s → 单批 100 条耗时 ~2.2s → **21.9ms/条**（RocketMQ 同步 send 等 Broker ACK + markSent 单行 update 各占约一半），Broker 与 mysql 同在 4G VM 抢资源
2. **消费侧零异常**：dead=0，幂等碰撞 seed 生效——10k 条消息全部走 DuplicateKeyException 幂等忽略路径，notification 零污染、无 RECONSUME 循环
3. **积压风险（生产容量规划关键数字）**：scheduler 消化能力 14 条/s < submit 峰值产生速率 74 条/s——持续满负载下 outbox 每秒净积压 ~60 条，负载回落后才能追赶
4. **吞吐公式**：BATCH_LIMIT ÷ (scan-interval + 单批发送耗时)，瓶颈是单线程逐条同步发送

**结论**：outbox flush 吞吐由 scheduler 单线程 + 逐条同步发送的架构决定；优化方向是并行发送 / 增大 batch / 缩短间隔，与连接池优化互不影响。

## 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

## 快速启动（本地开发）

### 1. 克隆代码

```bash
git clone https://gitee.com/bide-time/approval-flow.git
cd approval-flow
```

### 2. 数据库初始化

创建数据库 `approval_flow`，执行 `docs/sql/schema.sql`：

```bash
mysql -u root -p -e "CREATE DATABASE approval_flow DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p approval_flow < docs/sql/schema.sql
```

（脚本中已包含流程定义、节点配置、用户角色等示例数据）

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
# 后端
cd backend
mvn spring-boot:run

# 前端（另开一个终端）
cd frontend
npm install
npm run dev
```

后端默认监听 `8080` 端口，前端默认监听 `5173` 端口。

### 4. 配置说明（环境变量与 profile）

配置按「公共 + 环境」两层拆分，三个文件均入库：

| 文件                               | 职责         | 内容                                                             |
| ---------------------------------- | ------------ | ---------------------------------------------------------------- |
| `backend/.../application.yml`      | 公共层       | 应用名、Sa-Token、MyBatis-Plus 通用配置、MQ 组名与重试策略       |
| `backend/.../application-dev.yml`  | 本地开发环境 | 本地地址与凭据、打印 SQL、debug 日志                             |
| `backend/.../application-prod.yml` | 生产环境     | 连接信息全部由环境变量注入（无默认值）、关闭 SQL 打印、info 日志 |

规律：**`application.yml` 只回答「有哪些配置项」，`application-{profile}.yml` 只回答「这套环境的值是什么」**。切换环境无需改代码，改本地环境地址直接改 `application-dev.yml` 即可。

```bash
# 本地开发：默认激活 dev，配置来自 application-dev.yml
mvn spring-boot:run

# 生产部署：显式指定 profile 并注入真实连接信息
SPRING_PROFILES_ACTIVE=prod \
MYSQL_HOST=10.0.0.12 MYSQL_USERNAME=app_flow MYSQL_PASSWORD=*** \
REDIS_HOST=10.0.0.13 REDIS_PASSWORD=*** \
ROCKETMQ_NAME_SERVER=10.0.0.14:9876 \
java -jar target/approval-flow-backend.jar
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
│   ├── .env.production
│   └── package.json
├── docs/
│   └── sql/
│       └── schema.sql      # 建表脚本及初始数据
├── start.js                # 一键启动脚本（同时启动前后端）
├── stop.js                 # 一键停止脚本（按端口终止进程）
├── .gitignore
└── README.md
```

## 许可证

本项目采用 [MIT License](LICENSE) 开源协议。