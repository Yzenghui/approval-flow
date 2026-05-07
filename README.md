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
- **个人信息**：修改邮箱、修改密码
- **完整状态机**：草稿 → 审批中 → 通过/驳回/撤回 → 修改重提

## 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+
- （可选）Docker 20.10+

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

### 3. 后端启动

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端默认监听 `8080` 端口。

若需修改数据库/Redis等配置，请编辑 `src/main/resources/application.yml`。

### 4. 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端默认监听 `5173` 端口，Vite 代理会将 `/api` 请求转发至后端 `8080`，无跨域问题。

## 生产部署

待补充。

## 项目结构

```
approval-flow/
├── backend/                # Spring Boot 后端
│   ├── src/
│   └── pom.xml
├── frontend/               # Vue 3 前端
│   ├── src/
│   ├── .env.development
│   ├── .env.production
│   └── package.json
├── docs/
│   └── sql/
│       └── schema.sql      # 建表脚本及初始数据
├── docker-compose.yml      # 容器编排（MySQL + 后端 + Nginx）
├── nginx.conf              # Nginx 配置（生产环境代理前端静态文件及 API）
├── .gitignore
└── README.md
```

## 许可证

本项目采用 [MIT License](LICENSE) 开源协议。