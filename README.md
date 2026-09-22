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