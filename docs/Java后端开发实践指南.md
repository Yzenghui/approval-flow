## 模块一：项目基础与环境

### 1.1 Maven - 项目管理与依赖体系

**精准定义**：Apache 基金会维护的 Java 项目构建与依赖管理工具 
**核心价值**：  

- **依赖管理**：自动下载 jar 包 + 智能解决传递依赖冲突  
- **构建标准化**：统一编译、测试、打包流程（`clean → compile → test → package`）  
- **仓库体系**：连接全球开源库（中央仓库 ≈ Java 的 npm）  
  **关键认知**：所有 Spring Boot 项目本质都是 **Maven 项目 + Spring Boot 框架**，Maven 是底层基建。

#### 核心概念

##### POM（Project Object Model）

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <!-- POM 模型版本，固定为 4.0.0 -->
  <modelVersion>4.0.0</modelVersion>
  
  <!-- 项目坐标：唯一标识一个 Maven 项目 -->
  <groupId>com.ims</groupId>       <!-- 组织名，通常用域名倒写 -->
  <artifactId>backend</artifactId> <!-- 项目模块名称 -->
  <version>1.0.0</version>         <!-- 版本号 -->

  <!-- 继承 Spring Boot 官方父 POM，自动管理依赖版本和插件配置 -->
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.12</version> <!-- 使用 LTS 长期支持版本 -->
  </parent>

  <!-- 项目所需依赖 -->
  <dependencies>
    <!-- Web 开发起步依赖：包含 Spring MVC、内嵌 Tomcat、JSON 支持等 -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- Lombok：通过注解（如 @Data）在编译期自动生成 getter/setter 等样板代码 -->
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <scope>provided</scope> <!-- 仅编译时需要，不打包进最终 jar -->
    </dependency>
  </dependencies>
</project>
```

**最佳实践**：  

- 依赖**禁止手动指定 version**（除非特殊需求），避免版本冲突  
- 查兼容版本：[Spring Boot 2.7 依赖表](https://docs.spring.io/spring-boot/docs/2.7.12/reference/htmlsingle/#dependency-versions)

##### 仓库体系

| 仓库类型 | 路径示例                 | 作用                   |
| -------- | ------------------------ | ---------------------- |
| 本地仓库 | `~/.m2/repository`       | 缓存依赖，避免重复下载 |
| 中央仓库 | `repo1.maven.org/maven2` | 全球公共库（默认源）   |
| 镜像仓库 | `maven.aliyun.com`       | **国内强烈推荐配置**   |
| 私服     | 公司内网地址             | 企业内部依赖管理       |

##### 标准目录结构（Maven 约定）

```
project-root/
├── pom.xml               # 项目唯一配置文件
├── src/
│   ├── main/
│   │   ├── java/         # 源代码（包路径：com/ims/controller...）
│   │   └── resources/    # 配置文件、静态资源
│   └── test/
│       ├── java/         # 测试代码
│       └── resources/    # 测试配置
└── target/               # 编译输出目录（mvn clean 会删除）
```

**记住**：Maven 遵循 **“约定优于配置”** —— 严格按此结构存放文件，无需额外配置编译路径。

#### 常用命令速查

| 命令                            | 作用                  | 实用场景         |
| ------------------------------- | --------------------- | ---------------- |
| `mvn -v`                        | 验证 Maven 安装       | 首次使用必查     |
| `mvn clean`                     | 清理 target 目录      | 打包前清理       |
| `mvn compile`                   | 编译主代码            | 快速验证语法     |
| `mvn test`                      | 运行测试              | 持续集成         |
| `mvn package`                   | 打 jar/war 包         | 生成可部署包     |
| `mvn dependency:tree`           | **查看依赖树**        | **排查冲突神器** |
| `mvn clean package -DskipTests` | 跳过测试打包          | 临时快速构建     |
| `mvn spring-boot:run`           | 启动 Spring Boot 项目 | 本地开发调试     |

#### 依赖范围（Scope）精要

**Scope 是什么？**  
Maven 用 Scope 精确控制依赖在**编译、测试、运行**三个阶段是否生效，避免冗余 jar 打包、减少冲突、缩小产物体积。

##### Scope 作用速查表

| Scope      | 编译 | 测试 | 运行 | 典型场景与实操说明                                           |
| ---------- | ---- | ---- | ---- | ------------------------------------------------------------ |
| `compile`  | 是   | 是   | 是   | **默认值**。主业务代码必需（如 Spring Core、MyBatis）。无需显式声明。 |
| `provided` | 是   | 是   | 否   | **容器已提供**。如 Servlet API：编译需接口，但 Tomcat 运行时自带，打包会冲突。 |
| `runtime`  | 否   | 是   | 是   | **编译用接口，运行需实现**。如 JDBC 驱动：代码写 `Connection` 接口，运行需 MySQL 驱动。 |
| `test`     | 否   | 是   | 否   | **仅测试用**。如 JUnit、Mockito，绝不打入生产包。            |

##### 真实场景示例

```xml
<!-- 场景1：部署到外部 Tomcat（war 包） -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope> <!-- Tomcat 自带，打包排除 -->
</dependency>

<!-- 场景2：Spring Boot 内嵌 Tomcat（jar 包） -->
<!-- 无需声明 Servlet API！spring-boot-starter-web 已隐式包含且 Scope 合理 -->
<!-- 若错误设为 provided：java -jar 启动时将报 ClassNotFoundException -->

<!-- 场景3：MySQL 驱动（Spring Boot 项目） -->
<!-- 无需显式声明 scope！starter-jdbc 已设为 runtime -->
<!-- 手动添加时建议： -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope> <!-- 编译不依赖具体驱动 -->
</dependency>
```

##### 高频避坑指南

| 问题现象                            | 根本原因                          | 解决方案                                                    |
| ----------------------------------- | --------------------------------- | ----------------------------------------------------------- |
| 打包后运行报 `NoClassDefFoundError` | 误将运行时必需依赖设为 `provided` | 检查：是否部署到无该依赖的环境？内嵌容器项目慎用 `provided` |
| 多个版本 jar 冲突                   | 传递依赖引入冲突版本              | `mvn dependency:tree` 定位 → 用 `<exclusions>` 排除         |
| 测试能过但生产异常                  | `test` 依赖被误用于主代码         | 严格隔离测试代码与主代码包路径                              |

**黄金法则**：  

1. **Spring Boot 项目优先用 Starter**（如 `spring-boot-starter-web`），Scope 已由官方优化，**避免手动指定**；  
2. 仅当明确知道“哪个阶段需要此依赖”时，才显式声明 Scope；  
3. 打包前执行 `mvn dependency:tree -Dverbose` 预检依赖。

#### 可选依赖（Optional Dependencies）

**作用**：标记某依赖**仅本项目使用，不传递给下游项目**，实现“可插拔”功能解耦。  
**本质**：控制依赖的**传递性（Transitivity）**，而非生命周期。

##### 使用语法

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <optional>true</optional> <!-- 下游项目不会自动继承此依赖 -->
</dependency>
```

##### 典型场景

- **多数据库支持框架**：提供 MySQL/Oracle/H2 适配器，用户按需引入驱动  
- **日志绑定**：SLF4J 的 logback、log4j 绑定设为 optional，由使用者决定  
- **可选功能模块**：如“Excel 导出”、“短信通知”等非核心能力  

##### 与 Scope 对比

| 机制                        | 控制目标                         | 是否打入 jar | 适用角色            |
| --------------------------- | -------------------------------- | ------------ | ------------------- |
| `<scope>provided</scope>`   | **生命周期**（运行时由容器提供） | 不打包       | Web 应用开发者      |
| `<optional>true</optional>` | **传递性**（是否被下游继承）     | 打包         | **类库/框架开发者** |

> **黄金法则**：  
>
> - **业务项目**（如订单系统、用户中心）→ **几乎不用 `optional`**  
> - **工具库/SDK/Starter** → **善用 `optional` 避免强推依赖**

#### 实用技巧：配置阿里云镜像（国内加速）

在 `~/.m2/settings.xml` 的 `<mirrors>` 节点内添加：

```xml
<mirror>
  <id>aliyunmaven</id>
  <mirrorOf>central</mirrorOf>
  <name>阿里云公共仓库</name>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

配置后首次构建稍慢（下载索引），后续依赖下载速度提升 5-10 倍。

---

### 1.2 Spring Boot - 快速构建生产级应用

#### 核心理念：Spring Boot 是什么？为什么用它？

**一句话定义**：  
Spring Boot 是 Spring 官方推出的**脚手架框架**，核心目标：**让开发者用最少配置快速搭建可独立运行的生产级 Spring 应用**。

##### 传统 Spring vs Spring Boot

| 传统 Spring 痛点     | Spring Boot 方案             | 你获得的实际价值                       |
| -------------------- | ---------------------------- | -------------------------------------- |
| 手写冗长 XML 配置    | **自动配置**                 | 新建项目 5 分钟启动，专注业务逻辑      |
| 手动下载/配置 Tomcat | **内嵌容器**（Tomcat/Jetty） | `java -jar app.jar` 直接运行，无需部署 |
| 依赖版本冲突、漏引   | **Starter 依赖**             | 一行依赖引入完整技术栈，版本自动对齐   |
| 多环境配置分散难管理 | **统一配置文件**             | `dev/test/prod` 环境一键切换           |

##### Starter 依赖：开箱即用的技术套餐

- **是什么**：预整合 + 预配置的依赖集合，解决"引哪些包、版本多少、怎么配"的问题  

- **怎么用**：在 `pom.xml` 中添加一行，自动包含所有必要组件  

  ```xml
  <!-- Web 开发：自动包含 Spring MVC + 内嵌 Tomcat + JSON 支持 -->
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  ```

- **常用 Starter 速查**：

  - `spring-boot-starter-web`：RESTful API 开发  
  - `spring-boot-starter-data-jpa`：JPA 持久层  
  - `spring-boot-starter-test`：测试全家桶  
  - `spring-boot-starter-actuator`：生产监控  

> "需要什么功能 → 找对应 Starter → 加一行依赖 → 直接写业务代码"

---

#### 创建项目：Spring Initializr 全场景指南

##### 三种可靠方式（任选其一）

| 方式               | 操作路径                                           | 说明                                         |
| ------------------ | -------------------------------------------------- | -------------------------------------------- |
| **网页版（推荐）** | [https://start.spring.io](https://start.spring.io) | 100% 可靠，任何环境可用                      |
| **IDEA 旗舰版**    | New Project → Spring Initializr                    | 需 Ultimate 版（学生可免费申请）             |
| **IDEA 社区版**    | New Project → Maven → 手动填 pom                   | 无 Spring 插件时，**直接用网页版生成后导入** |

##### 网页版关键参数（必填）

1. **Project**: Maven / Java  
2. **Spring Boot**: `2.7.12`（LTS 长期支持版）  
3. **Group**: `com.ims`  
4. **Artifact**: `backend`  
5. **Dependencies**:  
   - Spring Web  
   - MyBatis Framework + MySQL Driver  
   - Lombok  
   - Spring Boot DevTools（开发时热重启）  
6. 点击 **Generate** → 解压 → 用 IDEA **Open**（非 Import）

---

#### 项目结构详解

```
backend/
├── pom.xml                     # Maven 配置（含 Spring Boot 依赖）
├── src/
│   ├── main/
│   │   ├── java/com/ims/
│   │   │   ├── ImsApplication.java  # 【启动类】必须在根包！
│   │   │   ├── controller/    # 接收 HTTP 请求（@RestController）
│   │   │   ├── service/       # 业务逻辑（@Service）
│   │   │   ├── mapper/        # MyBatis 数据访问接口（@Mapper）
│   │   │   ├── entity/        # 数据库实体类（与表字段一一对应）
│   │   │   ├── dto/           # 数据传输对象（用于请求/响应，避免暴露 entity）
│   │   │   ├── config/        # 自定义配置类（如 WebMvcConfig、MyBatisConfig 等）
│   │   │   ├── utils/         # 工具类（如 JwtUtil、DateUtils 等静态方法集合）
│   │   │   ├── enums/         # 枚举类（如 StatusEnum、RoleEnum 等）
│   │   │   └── exception/     # 全局异常处理 + 自定义异常类（如 BusinessException）
│   │   └── resources/
│   │       ├── application.yml      # 主配置文件（公共配置）
│   │       ├── application-dev.yml  # 开发环境配置（通过 spring.profiles.active 激活）
│   │       ├── static/          # 静态资源（CSS/JS/图片等，可选）
│   │       └── templates/       # 模板文件（Thymeleaf/Freemarker，前后端不分离时使用）
│   └── test/                    # 单元测试目录（含 @SpringBootTest 测试类）
└── target/                      # mvn package 生成的 jar 包位置（自动生成，勿提交）
```

**关键规则**：  

- 启动类 `ImsApplication.java` **必须位于根包**（如 `com.ims`），否则组件扫描失效  
- 包结构按**业务分层**（controller/service/mapper），非强制但强烈推荐  

---

#### 启动类与自动配置

##### 启动类：应用的唯一入口

```java
@SpringBootApplication // 三合一注解：配置类 + 自动配置 + 组件扫描
public class ImsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImsApplication.class, args); // 启动 Spring 容器
    }
}
```

##### `@SpringBootApplication` 三合一拆解

| 组合注解                   | 作用               | 补充说明                                   |
| -------------------------- | ------------------ | ------------------------------------------ |
| `@Configuration`           | 声明当前类为配置类 | 类本身会被注册为 Bean，可配合 `@Bean` 使用 |
| `@EnableAutoConfiguration` | 启用自动配置       | 核心是 `@Conditional` 条件装配机制         |
| `@ComponentScan`           | 自动扫描并注册组件 | 默认扫描**当前类所在包及其子包**           |

> **实践建议**：  
> 99% 场景直接使用 `@SpringBootApplication` 即可，无需单独拆分。仅在需要**自定义扫描路径**或**精细控制自动配置**时才单独使用各注解。

##### Bean 的核心地位

> **Spring Boot 应用 = 一堆被容器管理的 Bean**  
>
> - 通过 `@Component`/`@Service` 等注解，类被注册为 Bean  
> - 通过 `@Autowired`，Bean 之间建立依赖关系  
> - 自动配置的本质：**根据依赖条件化创建 Bean**（如检测到 Web 依赖 → 自动创建 Tomcat Bean）  
> - 启动日志中 `Mapped "{[GET],/health}"...` 即证明 Controller 已作为 Bean 注册成功  

##### 自动配置三步走

1. **依赖检测**：扫描 `pom.xml`（如 `starter-web` → 触发 Web 配置）  
2. **条件装配**：`@ConditionalOnClass(Tomcat.class)` 等注解控制 Bean 创建  
3. **灵活覆盖**：自定义 Bean 或配置文件即可替换默认行为  

##### 条件装配 `@Conditional` 系列（自动配置的核心）

> **作用**：动态开关，仅当指定条件满足时才注册 Bean，否则直接跳过

| 注解                           | 触发条件             | 典型场景                 |
| ------------------------------ | -------------------- | ------------------------ |
| `@ConditionalOnClass`          | 类路径**有**某类     | 检测依赖是否存在         |
| `@ConditionalOnMissingBean`    | 容器**无**某 Bean    | 提供默认实现，防覆盖     |
| `@ConditionalOnProperty`       | 配置项**匹配**指定值 | 开关控制（如 `enabled`） |
| `@ConditionalOnWebApplication` | 当前是 **Web 环境**  | Web 相关自动配置         |
| `@ConditionalOnBean`           | 容器**有**某 Bean    | 依赖其他 Bean 存在       |

> **核心价值**：实现"按需加载"和"智能组装"，是 Spring Boot **自动配置的基石**。

##### 实用调试技巧

| 场景             | 操作                                          | 作用             |
| ---------------- | --------------------------------------------- | ---------------- |
| 查看配置生效情况 | 启动参数加 `--debug`                          | 输出自动配置报告 |
| 临时改端口       | `--server.port=8081`                          | 无需改代码       |
| 禁用某自动配置   | `exclude = DataSourceAutoConfiguration.class` | 解决冲突时使用   |

---

#### Spring Boot 核心注解速查（仅 Spring 体系）

> **注**：本节仅聚焦 Spring Framework 与 Spring Boot 原生注解。MyBatis、Lombok 等框架注解将在后续"数据访问层""开发工具"章节专项讲解。

##### Bean 定义与组件注册  

> **作用：声明组件元数据（BeanDefinition），将无状态组件交由 Spring 容器管理生命周期与依赖**

| 注解              | 作用                                                         |
| ----------------- | ------------------------------------------------------------ |
| `@Component`      | 通用 Spring 组件，被组件扫描自动注册为 Bean                  |
| `@Service`        | 业务逻辑层组件（语义化 `@Component`，增强代码可读性）        |
| `@Repository`     | 数据访问层组件（语义化 `@Component`，自动将 JDBC 异常转换为 Spring 数据访问异常） |
| `@Controller`     | Web 控制器，方法返回值默认解析为视图名称（用于 Thymeleaf/JSP 等服务端渲染） |
| `@RestController` | RESTful 控制器（组合 `@Controller` + `@ResponseBody`，方法返回值直接写入 HTTP 响应体；在 Spring Boot 默认配置下，对象返回值通常被序列化为 JSON） |
| `@Configuration`  | 配置类，替代 XML 配置，内部方法配合 `@Bean` 向容器注册组件   |
| `@Bean`           | 在 `@Configuration` 类中，将方法返回值注册为 Spring 容器管理的 Bean |
| `@ComponentScan`  | 指定扫描包路径，自动发现并注册带 `@Component` 等注解的类为 Bean |
| `@EnableConfigurationProperties` | 在 `@Configuration` 类上声明，将 `@ConfigurationProperties` 标注的属性类显式注册为 Bean（属性类无组件扫描注解，需它激活配置绑定） |
| `@DependsOn`      | 强制控制 Bean 初始化顺序，确保指定 Bean 在当前 Bean 之前创建 |

> **核心说明**  
>
> - **BeanDefinition **：注解触发注册后，Spring 仅生成元数据（类路径、作用域、依赖关系等），**此时无对象实例**。  
>
> - **实例创建**  
>   • **创建时机**：由 Bean 自身的**作用域**决定  
>
>         单例（默认）：容器启动时初始化（`@Lazy` 标记则延迟至首次被注入时）  
>                                                                                                 
>         原型（`@Scope("prototype")`）：每次从容器获取时（注入或 `getBean()`）新建实例  
>   • **创建机制**：Spring 采用 **"链式创建"**（**通用机制，与作用域无关**）  
>     创建任意 Bean 时，均先递归创建其所有声明的依赖（遵循**各依赖自身的作用域规则**），并将这些**完整实例**传入目标 Bean 。
>
> - **注册原则**：  
>   **应注册**：无状态组件（Service/Repository/Controller/工具类），需 Spring 管理生命周期、事务、AOP 等  
>   **不应注册**：有状态数据对象（Entity/DTO/VO，如 `Student`），由业务逻辑 `new` 创建，避免单例污染  
>
> - **`@DependsOn` 使用场景**：  
>   解决**隐式依赖**（代码中无 `@Autowired` 但业务逻辑要求对方先就绪），如静态注册、资源预热等。**日常开发较少使用**，优先通过 `@Autowired` 显式依赖。

---

##### 依赖注入  

> **作用：将容器中已创建好的依赖 Bean 实例，按声明传递给目标 Bean，实现组件间协作**

| 注解         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| `@Autowired` | **核心依赖注入注解**：按类型自动注入 Spring 管理的其他 Bean 实例（即组件依赖），推荐用于构造器注入（不可变、易测试） |
| `@Qualifier` | **依赖注入辅助注解**：配合 `@Autowired` 使用，通过 Bean 名称精确指定注入目标（解决多实现场景） |
| `@Value`     | 注入配置属性或字面量（如 `@Value("${server.port}")`），属于**外部化配置机制，不涉及 Bean 依赖**，故不属于依赖注入范畴 |

> **注入行为规则**  
>
> 1. Spring **仅注入代码中明确声明的依赖**（构造器参数 / 标记 `@Autowired` 的字段或方法）  
> 2. 依赖实例按其**自身作用域**提供：单例复用、原型新建
> 3. 注入的是**已完成自身装配的完整实例**，可直接使用  
>
> **依赖声明规则**  
>
> | 注入方式   | 如何声明依赖                                                 |
> | ---------- | ------------------------------------------------------------ |
> | 构造器注入 | 构造器参数列表即为依赖声明；Spring Boot 2.6+ 起，若类仅有一个构造器，可省略 `@Autowired` |
> | 字段注入   | 在字段上使用 `@Autowired`（必须显式标注）                    |
> | Setter注入 | 在 setter 方法上使用 `@Autowired`（必须显式标注）            |
>
> **注入方式选择原则**  
>
> - **首选构造器注入**  
>   依赖通过构造函数传入，声明为 `private final`，确保对象创建即完整、线程安全、易于测试。  
>
>   ```java
>   @Service
>   public class OrderService {
>       private final PaymentService paymentService;
>                                 
>       // 单构造器：参数列表 = 依赖声明，@Autowired 可省略
>       public OrderService(PaymentService paymentService) {
>           this.paymentService = paymentService; // 参数已是容器注入完成的 Bean 实例
>       }
>   }
>                                 
>   @RestController
>   public class OrderController {
>       private final OrderService orderService;
>       public OrderController(OrderService orderService) { // OrderService 作为完整实例传入
>           this.orderService = orderService;
>       }
>                                 
>       @PostMapping("/orders")
>       public String createOrder() {
>           orderService.process();
>           return "success";
>       }
>   }
>   ```
>
> - **避免字段注入**  
>   隐藏依赖关系、无法设为 `final`、难以脱离 Spring 测试，**不符合现代 Spring 最佳实践**。  
>
>   ```java
>   // 不推荐：依赖声明依赖注解，且必须显式标记
>   @Service
>   public class OrderService {
>       @Autowired // 无此标记则不会注入
>       private PaymentService paymentService;
>   }
>   ```
>
> - **Setter 注入仅用于特殊场景**  
>   如可选依赖（配合 `@Autowired(required = false)`）、循环依赖（应优先重构避免）  
>
>   > **可选依赖注意**：构造器注入**不支持** `required = false`（Spring Boot 2.6+），推荐将可选依赖重构为 `Optional<T>` 或避免设计可选依赖  

##### Web 接口开发（Spring MVC）

**注解选择原则**：
`@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping` 等 **HTTP 方法专用注解，其底层通用注解均为 `@RequestMapping`**。它们通过预设 `method` 属性（如 `@GetMapping = @RequestMapping(method = GET)`）提供语义化封装。
**RESTful 开发应优先使用这些专用注解**——代码意图更清晰，是 Spring Boot 项目的标准实践。

| 注解                                               | 作用                       | 示例                           |
| -------------------------------------------------- | -------------------------- | ------------------------------ |
| `@GetMapping` / `@PostMapping`                     | 处理 GET/POST 请求         | `@GetMapping("/users")`        |
| `@PutMapping` / `@DeleteMapping` / `@PatchMapping` | 处理 PUT/DELETE/PATCH 请求 | `@PatchMapping("/users/{id}")` |
| `@PathVariable`                                    | 绑定 URL 路径变量          | `@PathVariable Long id`        |
| `@RequestParam`                                    | 绑定查询参数               | `@RequestParam String name`    |
| `@RequestBody`                                     | 绑定 JSON 请求体           | `@RequestBody UserDTO user`    |

**三种参数绑定方式对比**：

| 注解            | 数据位置                                 | 适用场景               | 示例 URL                    |
| --------------- | ---------------------------------------- | ---------------------- | --------------------------- |
| `@PathVariable` | URL 路径中（Path Variable）              | 资源标识（ID、编码等） | `GET /users/123`            |
| `@RequestParam` | URL 查询字符串（Query String）           | 分页、过滤、搜索条件   | `GET /users?page=1&size=10` |
| `@RequestBody`  | HTTP 请求体（Request Body，通常为 JSON） | 创建、更新等复杂数据   | `POST /users` + 请求体      |

> **注意**：`@PathVariable` 和 `@RequestParam` 都用于 GET 请求，数据都在 URL 中，但位置不同：路径变量用于定位资源，查询参数用于过滤条件。`@RequestBody` 用于非 GET 请求（POST/PUT/PATCH），数据在请求体中。

##### 配置与启动

| 注解                       | 作用                                                         |
| -------------------------- | ------------------------------------------------------------ |
| `@SpringBootApplication`   | 启动类核心注解，三合一：`@Configuration`（声明配置类） + `@EnableAutoConfiguration`（启用自动装配） + `@ComponentScan`（扫描组件，默认当前包及子包） |
| `@ConfigurationProperties` | 将 YAML/properties 配置按前缀绑定到 Java 对象，实现类型安全、支持嵌套结构和默认值，需配合 setter 或 Lombok 使用（配合 `@EnableConfigurationProperties` 注册为 Bean 后生效） |
| `@PropertySource`          | 加载额外的 `.properties` 配置文件（**原生不支持 `.yml`/`.yaml`**） |
| `@Profile`                 | 环境隔离，仅当激活的 Profile 与注解值匹配时才注册该组件      |

> **关键原则**：  
>
> - 所有 Web 注解（如 `@GetMapping`）**必须**用在 Spring 管理的 Bean 中（如 `@RestController` 标注的类），否则无效！  
> - 注解是"声明意图"，Spring Boot 自动实现细节——这正是"约定优于配置"的精髓。

---

#### 配置文件实战

##### YAML 写法（推荐：树状结构，清晰易维护）

```yaml
# application.yml
server:
  port: 8080
  servlet:
    context-path: /api
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ims
    username: root
    password: 123456
  profiles:
    active: dev  # 激活开发环境
```

##### 多环境配置

```yaml
# application-dev.yml（开发环境）
logging:
  level:
    com.ims: DEBUG

# application-prod.yml（生产环境）
server:
  port: 80
spring:
  datasource:
    password: ${DB_PASSWORD}  # 从环境变量读取（安全！）
```

##### 环境隔离 `@Profile` 详解

> **作用**：环境过滤器，仅当激活的 Profile 名称与注解指定值**匹配**时，才注册该组件

| 用法                        | 说明                     | 场景         |
| --------------------------- | ------------------------ | ------------ |
| `@Profile("dev")`           | 仅开发环境生效           | 本地调试     |
| `@Profile({"dev", "test"})` | 开发**或**测试环境均生效 | 多环境共用   |
| `@Profile("!prod")`         | **非**生产环境生效       | 排除生产环境 |
| 类级别 `@Profile`           | 控制整个配置类           | 整类环境隔离 |
| 方法级别 `@Profile`         | 控制单个 Bean            | 精细控制     |

> **核心价值**：解决**环境差异化**问题（如数据库连接、Mock 服务切换），实现"一套代码，多处运行"。

##### `@PropertySource` 使用示例

```java
@Configuration
@PropertySource("classpath:custom.properties") // 加载额外 properties 文件
public class CustomConfig {
    @Value("${custom.key}")
    private String customValue;
}
```

> **注意**：  
> - **仅支持 `.properties` 格式**，加载 YAML 会报错或无效  
> - 若需加载 YAML，改用 `spring.config.import` 配置项  
> - 日常开发中，`application.yml` 已足够，此注解使用频率较低

##### 配置优先级（从高到低）

1. 命令行参数（`--server.port=9090`）  
2. 环境变量（`SPRING_DATASOURCE_PASSWORD`）  
3. 外部配置文件（jar 同目录 config/）  
4. jar 包内 `application.yml`  

> **生产建议**：敏感信息通过环境变量注入，**禁止写入代码库**。  
> **开发技巧**：本地用 `application-dev.yml`，提交时确保 `active: dev`，避免误连生产库。

---

#### 验证项目是否成功

1. **启动项目**：运行 `ImsApplication.main()`  

2. **观察日志**：  

   ```
   Tomcat started on port(s): 8080 (http)
   Started ImsApplication in 3.2 seconds
   ```

3. **创建临时测试接口**：

   ```java
   @RestController
   public class HealthController {
       @GetMapping("/health")
       public String health() {
           return "Spring Boot is running!";
       }
   }
   ```

4. **浏览器访问**：`http://localhost:8080/health` → 看到返回即成功！  
   （若 404，请检查：① 启动类是否在根包 ② Controller 是否被 `@RestController` 标注）

## 模块二：数据访问与持久层

### 2.1 MyBatis - 轻量级 SQL 映射框架

#### 什么是 MyBatis？核心价值何在？

**本质定义**：  
MyBatis 是一个**半自动化的持久层框架**，核心思想：**开发者专注编写原生 SQL，框架负责参数绑定、结果映射、连接管理等繁琐细节**。

##### 为什么选择 MyBatis？

- **vs JDBC**：  
  消除 `Connection/PreparedStatement/ResultSet` 手动管理、资源关闭、异常处理等样板代码，保留对 SQL 的完全控制权。
- **vs Hibernate/JPA**：  
  不隐藏 SQL（无“N+1 查询”黑盒问题），适合复杂查询、遗留数据库、需 DBA 优化的场景，学习曲线更平缓。

##### 在 Spring Boot 项目中的定位

MyBatis 本身是独立框架，通过 `mybatis-spring-boot-starter` 与 Spring Boot 无缝集成：  

- Starter 负责**自动配置**（数据源、SqlSessionFactory、事务管理器）  
- MyBatis 框架在**运行时**动态代理 Mapper 接口，执行 SQL 并映射结果  
- 事务由 Spring 管理（`@Transactional`），与 MyBatis 协同工作  
  → 开发者只需关注：**Mapper 接口 + SQL（XML/注解）**

---

#### 两种开发模式详解（按场景选择）

##### 模式一：XML 映射文件（推荐用于复杂 SQL）

**适用场景**：多表 JOIN、动态条件、批量操作、SQL 需 DBA 审核优化  
**核心优势**：  

- SQL 与 Java 代码完全分离，便于维护和协作  
- XML 支持 `<if>`、`<foreach>` 等强大动态标签，逻辑清晰  
- 支持 `<resultMap>` 精细控制字段映射（尤其字段名不一致时）  

```java
// Mapper 接口（仅声明，无需实现）
@Mapper
public interface OrderMapper {
    OrderDetail getOrderDetail(@Param("orderId") Long orderId);
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);
}
```

```xml
<!-- resources/mapper/OrderMapper.xml -->
<mapper namespace="com.ims.mapper.OrderMapper">
    <!-- 多表关联查询 + 结果映射 -->
    <resultMap id="orderDetailMap" type="OrderDetail">
        <id property="orderId" column="order_id"/>
        <result property="orderNo" column="order_no"/>
        <collection property="items" ofType="OrderItem">
            <id property="itemId" column="item_id"/>
            <result property="productName" column="product_name"/>
        </collection>
    </resultMap>
    
    <select id="getOrderDetail" resultMap="orderDetailMap">
        SELECT o.id AS order_id, o.order_no, 
               i.id AS item_id, i.product_name
        FROM orders o
        LEFT JOIN order_items i ON o.id = i.order_id
        WHERE o.id = #{orderId}
    </select>
    
    <!-- 动态批量更新 -->
    <update id="batchUpdateStatus">
        UPDATE orders 
        SET status = #{status}
        WHERE id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
    </update>
</mapper>
```

**配置要点**：  

- `application.yml` 中指定 XML 位置：`mybatis.mapper-locations=classpath:mapper/*.xml`  
- 开启驼峰映射：`mybatis.configuration.map-underscore-to-camel-case=true`（数据库下划线 → Java 驼峰）

---

##### 模式二：注解开发（推荐用于简单 CRUD）

**适用场景**：单表操作、SQL 简洁、快速开发、避免文件切换  
**核心优势**：代码紧凑，接口与 SQL 同文件，阅读效率高  

###### MyBatis 核心注解说明

| 注解       | 作用                                  | 关键属性                          | 示例                                                     |
| ---------- | ------------------------------------- | --------------------------------- | -------------------------------------------------------- |
| `@Mapper`  | 标记接口由 MyBatis 扫描代理           | 无                                | `@Mapper public interface UserMapper`                    |
| `@Select`  | 声明查询 SQL                          | `value`                           | `@Select("SELECT * FROM user WHERE id=#{id}")`           |
| `@Insert`  | 声明插入 SQL                          | `value`                           | `@Insert("INSERT INTO user(name) VALUES(#{name})")`      |
| `@Update`  | 声明更新 SQL                          | `value`                           | `@Update("UPDATE user SET name=#{name} WHERE id=#{id}")` |
| `@Delete`  | 声明删除 SQL                          | `value`                           | `@Delete("DELETE FROM user WHERE id=#{id}")`             |
| `@Param`   | 命名参数（多参数必需）                | `value`                           | `@Param("keyword") String keyword`                       |
| `@Options` | 配置主键回写等行为                    | `useGeneratedKeys`, `keyProperty` | `@Options(useGeneratedKeys=true, keyProperty="id")`      |
| `@Results` | 定义结果映射（替代 XML 的 resultMap） | `value`（@Result 数组）           | 见下方示例                                               |
| `@Result`  | 单字段映射                            | `property`, `column`, `id`        | `@Result(property="createTime", column="create_time")`   |

###### 完整注解开发示例

```java
@Mapper
public interface ProductMapper {
    
    // 基础查询（单参数可省略 @Param）
    @Select("SELECT * FROM product WHERE id = #{id}")
    Product getById(Integer id);
    
    // 多参数必须用 @Param
    @Select("SELECT * FROM product WHERE name LIKE CONCAT('%', #{keyword}, '%') AND price <= #{maxPrice}")
    List<Product> search(@Param("keyword") String keyword, @Param("maxPrice") BigDecimal maxPrice);
    
    // 插入 + 主键回写
    @Insert("INSERT INTO product(name, price, create_time) VALUES(#{name}, #{price}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);
    
    // 字段名不一致：使用 @Results 映射
    @Results({
        @Result(property = "createTime", column = "create_time", id = true),
        @Result(property = "updateTime", column = "update_time")
    })
    @Select("SELECT id, name, create_time, update_time FROM product WHERE id = #{id}")
    Product getByIdWithAlias(Integer id);
    
    // 动态 SQL（script 标签包裹）
    @Update("<script>" +
            "UPDATE product SET update_time = NOW() " +
            "<if test='name != null'>, name = #{name}</if>" +
            "<if test='price != null'>, price = #{price}</if>" +
            " WHERE id = #{id}" +
            "</script>")
    int update(Product product);
    
    @Delete("DELETE FROM product WHERE id = #{id}")
    int deleteById(Integer id);
}
```

###### 注解使用注意事项

- 复杂动态 SQL（>3 个条件）建议改用 XML，可读性更高  
- `${}` 拼接必须做白名单校验
- `@Results` 适用于简单字段映射，复杂嵌套仍推荐 XML 的 `<resultMap>`  

---

##### 模式选择建议

| 场景                           | 推荐模式 | 理由                         |
| ------------------------------ | -------- | ---------------------------- |
| 多表关联、复杂 WHERE、批量操作 | **XML**  | 标签结构清晰，便于维护和优化 |
| 单表 CRUD、简单查询            | **注解** | 代码集中，开发效率高         |
| 团队规范要求                   | 按规范   | 保持项目一致性               |
| 需要 DBA 直接审核 SQL          | **XML**  | SQL 独立文件，便于协作       |

---

#### 关键知识点精要

##### `#{}` 与 `${}` —— 安全生命线

| 语法           | 机制                     | 适用场景                                   | 风险                     |
| -------------- | ------------------------ | ------------------------------------------ | ------------------------ |
| `` #{value} `` | PreparedStatement 预编译 | 所有参数值（WHERE 条件、INSERT/UPDATE 值） | 安全，自动防 SQL 注入    |
| `` ${value} `` | 字符串直接拼接           | 动态表名、列名、ORDER BY / GROUP BY 字段   | 高危！必须严格白名单校验 |

**`#{}`：安全参数绑定**  

- `@Param("名称")` 是参数命名契约：为 Java 参数指定逻辑名，使 SQL 中 `` `#{名称}` `` 精准绑定  
- **单参数**：可省略 `@Param`（MyBatis 自动绑定），但**强烈建议显式标注**（提升可读性、避免隐式歧义）  
- **多参数**：**必须使用 `@Param`**（否则 `` `#{xxx}` `` 无法识别对应参数）  
- **对象参数**：若不使用@Param，MyBatis 自动访问对象属性，SQL 中直接使用 #{propertyName}；若显式添加 @Param("obj")，则需写 #{obj.propertyName}

```java
// Mapper 接口示例（注解模式 · 绑定规则与 XML 完全一致）
public interface UserMapper {
    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectById(@Param("id") Long id); // 单参数显式标注
    
    @Select("SELECT * FROM user WHERE name = #{name} AND age = #{age}")
    List<User> findByNameAndAge(
        @Param("name") String name, 
        @Param("age") Integer age
    ); // 多参数必须 @Param 命名
    
    @Insert("INSERT INTO user(name, age) VALUES(#{name}, #{age})")
    void insert(User user); // 对象参数直接用属性名
}
```

**`${}`：动态结构拼接 **  

- **校验必须前置到 Java 服务层**：MyBatis 无能力在 SQL 层（XML/注解）做安全校验  
- **必须四重防护**：空值处理 → 大小写统一 → 字符集过滤 → 白名单匹配  
- **禁止在 SQL 层校验**：`${}` 是纯字符串替换，无任何安全机制  

```java
// Service 层：严格校验（与 Mapper 实现方式无关）
public List<User> getUsers(String orderBy) {
    String field = (orderBy != null) ? orderBy.trim().toLowerCase() : "id";
    if (!field.matches("^[a-z0-9_]+$") || 
        !Set.of("id", "username", "create_time").contains(field)) {
        field = "id";
    }
    return userMapper.orderBy(field); // 传入已净化值
}
```

```java
// Mapper 接口（注解模式）：仅做结构拼接，不负责校验
public interface UserMapper {
    @Select("SELECT id, username FROM user ORDER BY ${orderBy}")
    List<User> orderBy(@Param("orderBy") String orderBy); // orderBy 已经服务层严格校验
}
```

**关键共识**  

- **机制统一**：`@Param` 绑定规则、`${}` 校验要求，在 XML 与注解模式下完全一致  
- **形式自由**：选择 XML 或注解仅是 SQL 存储形式差异，不影响安全逻辑  
- **开发建议**：  
  • 简单 CRUD → 注解 + `@Param`（简洁高效）  
  • 复杂动态 SQL → XML（支持 `<if>`/`<foreach>`）  
  • **所有参数值 → 优先用 `` #{} ``**  
  • **动态结构 → 用 `${}` + 服务层白名单校验**

##### 缓存机制

- **一级缓存**：默认开启，作用域为单次 SqlSession（单次请求内相同查询不重复查库）  
- **二级缓存**：需手动开启（`<cache/>`），作用域为 Mapper Namespace  
  → **生产环境慎用**：分布式场景易脏读，建议用 Redis 替代  

##### 核心配置（`application.yml`）

```yaml
mybatis:
  mapper-locations: classpath:mapper/*.xml      # XML 文件路径
  type-aliases-package: com.ims.entity          # 实体类别名简化
  configuration:
    map-underscore-to-camel-case: true          # db_column → javaProperty
    cache-enabled: false                        # 建议关闭二级缓存
    lazy-loading-enabled: false                 # 禁用懒加载
```

---

#### 与 Spring Boot 深度整合要点

##### 自动配置生效条件

1. 引入依赖：

   ```xml
   <dependency>
       <groupId>org.mybatis.spring.boot</groupId>
       <artifactId>mybatis-spring-boot-starter</artifactId>
       <version>3.0.3</version>
   </dependency>
   ```

2. 配置数据源（`application.yml`）：

   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/ims?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
       username: root
       password: 123456
       driver-class-name: com.mysql.cj.jdbc.Driver
   ```

3. **无需**手动配置 `SqlSessionFactory`、`DataSourceTransactionManager` —— Starter 自动完成！

##### 事务管理（Spring 原生能力）

```java
@Service
public class AccountService {
    
    @Autowired
    private AccountMapper accountMapper;
    
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        accountMapper.deduct(fromId, amount);
        accountMapper.add(toId, amount);
    }
}
```

> **关键认知**：  
>
> - MyBatis **只负责 SQL 执行**  
> - `@Transactional` 是 Spring 的注解，由 Spring 事务管理器控制回滚  
> - 二者通过 Starter 无缝协作  

---

#### 最佳实践清单

| 项目         | 推荐做法                                   | 避坑指南             |
| ------------ | ------------------------------------------ | -------------------- |
| **SQL 位置** | 复杂用 XML，简单用注解                     | 避免注解中写超长 SQL |
| **参数传递** | 多参数必用 `@Param`                        | 避免 `arg0` 难维护   |
| **字段映射** | 开启驼峰转换 + 合理用 `@Results`/ResultMap | 避免硬编码字段名     |
| **SQL 安全** | 99% 场景用 `#{}`                           | `${}` 必须白名单校验 |
| **缓存**     | 一级缓存默认用，二级缓存慎用               | 分布式环境用 Redis   |
| **日志**     | 开发开 SQL 日志，生产关闭                  | 避免敏感信息泄露     |
| **分页**     | 使用 PageHelper 插件                       | 避免手写 LIMIT 逻辑  |

------

### 2.2 MyBatis-Plus - MyBatis 的增强工具

#### 什么是 MyBatis-Plus？为什么需要它？

**精准定义**：  
MyBatis-Plus（简称 MP）是 MyBatis 的**增强工具**，在 MyBatis 基础上**只做增强不做改变**，旨在简化开发、提高效率。

**核心价值**：

- **无侵入**：对 MyBatis 零改动，可直接与现有 MyBatis 项目共存
- **通用 CRUD**：内置 `BaseMapper<T>`，单表操作无需写 XML 或注解
- **强大条件构造器**：`QueryWrapper` / `LambdaQueryWrapper` 实现动态 SQL，避免手拼字符串
- **分页插件**：物理分页，自动处理 COUNT 和 LIMIT
- **代码生成器**：根据数据库表自动生成 Entity、Mapper、Service、Controller 代码
- **乐观锁、逻辑删除、自动填充**等实用特性开箱即用

> **选型对比**：
>
> | 场景                      | 推荐方案                     |
> | ------------------------- | ---------------------------- |
> | 复杂多表关联、需 DBA 审核 | 原生 MyBatis（XML）          |
> | 单表 CRUD 占比 80% 以上   | **MyBatis-Plus**（大幅提效） |
> | 两者并存                  | 完全兼容，可按需混用         |

---

#### 快速集成：在 Spring Boot 中使用

##### 依赖引入（替换或补充 mybatis-spring-boot-starter）

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.5</version>
</dependency>
<!-- 若需代码生成器，额外添加 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-generator</artifactId>
    <version>3.5.5</version>
</dependency>
```

> **注意**：  
> - MyBatis-Plus Starter 已包含 MyBatis 核心依赖，**无需再单独引入** `mybatis-spring-boot-starter`  
> - 与原生 MyBatis 共存时，注意 Mapper 扫描路径统一

##### 配置文件（`application.yml`）

```yaml
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml   # XML 存放位置（支持通配）
  type-aliases-package: com.ims.entity           # 实体类别名
  global-config:
    db-config:
      id-type: auto                              # 主键策略：auto/assign_id/assign_uuid
      logic-delete-field: deleted                # 全局逻辑删除字段
      logic-delete-value: 1                      # 逻辑已删除值
      logic-not-delete-value: 0                  # 逻辑未删除值
  configuration:
    map-underscore-to-camel-case: true           # 驼峰映射
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 开发环境开启 SQL 日志
```

##### 启动类配置

```java
@SpringBootApplication
@MapperScan("com.ims.mapper")   // 扫描 Mapper 接口（MP 推荐）
public class ImsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImsApplication.class, args);
    }
}
```

---

#### 核心注解速查

| 注解          | 作用                                       | 属性                                                         | 示例                                                  |
| ------------- | ------------------------------------------ | ------------------------------------------------------------ | ----------------------------------------------------- |
| `@TableName`  | 映射实体类与数据库表名                     | `value`：表名<br>`autoResultMap`：是否自动构建 resultMap     | `@TableName("sys_user")`                              |
| `@TableId`    | 标识主键字段                               | `value`：字段名<br>`type`：主键策略（`IdType.AUTO` / `ASSIGN_ID` / `INPUT` 等） | `@TableId(type = IdType.AUTO)`                        |
| `@TableField` | 标识非主键字段，可配置映射、填充、存在性等 | `value`：字段名<br>`fill`：填充策略（`INSERT` / `UPDATE` / `INSERT_UPDATE`）<br>`exist`：是否为数据库字段 | `@TableField(fill = FieldFill.INSERT)`                |
| `@TableLogic` | 逻辑删除标记                               | `value`：未删除值（默认 0）<br>`delval`：已删除值（默认 1）  | `@TableLogic`                                         |
| `@Version`    | 乐观锁版本号字段                           | 无，标注在 `Integer` / `Long` / `Date` 类型字段上            | `@Version`                                            |
| `@EnumValue`  | 枚举类中标注“实际存储到数据库的值”         | 无，标注在枚举的某个属性上                                   | 见下方示例                                            |
| `@TableType`  | 指定映射类型处理器                         | `typeHandler`：自定义类型处理器类                            | `@TableField(typeHandler = JacksonTypeHandler.class)` |

**注解使用示例**：

```java
@TableName("user")
@Data
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableLogic
    private Integer deleted;
    
    @Version
    private Integer version;
}

// 枚举类示例
public enum GenderEnum {
    MALE(1, "男"),
    FEMALE(2, "女");
    
    @EnumValue  // 存储 1 或 2 到数据库
    private Integer code;
    private String desc;
}
```

> **主键策略选择**：
> - `IdType.AUTO`：数据库自增（适合单机、分库分表前）
> - `IdType.ASSIGN_ID`：雪花算法生成 Long 型 ID（**分布式推荐**）
> - `IdType.ASSIGN_UUID`：生成 32 位 UUID（String 类型）
> - `IdType.INPUT`：手动赋值（**不推荐**，易出错）

---

#### 核心特性实战

##### 1. BaseMapper：零 SQL 单表 CRUD

```java
// 实体类（与 MyBatis 完全一致）
@Data
@TableName("user")   // MP 注解：指定表名
public class User {
    @TableId(type = IdType.AUTO)   // 主键策略：自增
    private Long id;
    private String username;
    private String password;
    private Integer age;
    @TableLogic                     // 逻辑删除字段
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)   // 自动填充：插入时自动赋值
    private LocalDateTime createTime;
}

// Mapper 接口：继承 BaseMapper 即可获得全部 CRUD 方法
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 无需任何方法，即拥有 insert/deleteById/updateById/selectById/selectList 等
}

// Service 中直接使用
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    
    public User getById(Long id) {
        return userMapper.selectById(id);      // 单查
    }
    
    public List<User> listByAge(Integer minAge) {
        return userMapper.selectList(
            new LambdaQueryWrapper<User>().ge(User::getAge, minAge)
        );
    }
    
    public void save(User user) {
        userMapper.insert(user);               // 插入后自动回写主键 id
    }
}
```

**BaseMapper 常用方法速查**：

| 方法                                   | 作用                                                         |
| -------------------------------------- | ------------------------------------------------------------ |
| `insert(T entity)`                     | 插入（主键回写）                                             |
| `deleteById(Serializable id)`          | 按主键删除（逻辑）                                           |
| `deleteBatchIds(Collection idList)`    | 根据主键集合批量删除                                         |
| `delete(Wrapper<T> wrapper)`           | 按条件删除（逻辑）                                           |
| `updateById(T entity)`                 | 按主键更新（实体中带@TableId的字段作为条件，非空字段作为更新值） |
| `update(T entity, Wrapper<T> wrapper)` | 按条件更新（wrapper决定条件，实体中非空字段作为更新值）      |
| `selectById(Serializable id)`          | 按主键查询                                                   |
| `selectBatchIds(Collection idList)`    | 根据**主键**集合批量查询（idList 为空时会 SQL 报错，调用前需自行判空） |
| `selectOne(Wrapper wrapper)`           | 按条件查询单条记录（无记录返回 null，多条抛异常）            |
| `selectList(Wrapper wrapper)`          | 条件查询列表                                                 |
| `selectCount(Wrapper<T> wrapper)`      | 按条件查询总记录数                                           |
| `selectPage(Page page, Wrapper)`       | 分页查询                                                     |

> `BaseMapper` 不提供批量插入方法。如需批量操作，可让 Service 接口继承 `IService`，实现类继承 `ServiceImpl`，然后使用 `saveBatch()` 方法（分批执行单条 INSERT），避免循环单条插入导致的性能问题。如需一条 SQL 完成批量插入（性能更高），可通过自定义 SQL 注入器实现 `InsertBatchSomeColumn`。

---

##### 2. 条件构造器：告别手写 WHERE

| 构造器                  | 特点                                | 推荐场景                 |
| ----------------------- | ----------------------------------- | ------------------------ |
| `QueryWrapper<T>`       | 字段名用字符串，灵活但易错          | 动态表名、复杂 SQL 拼接  |
| `LambdaQueryWrapper<T>` | 方法引用 `User::getAge`，编译期检查 | **日常开发首选**         |
| `UpdateWrapper<T>`      | 专门用于更新（支持 SET 片段）       | 批量更新或字段值动态组合 |
| LambdaUpdateWrapper<T>  | 方法引用，编译期检查，支持 SET 片段 | 日常更新首选             |

**方法引用的本质**：

`LambdaQueryWrapper` 和 `LambdaUpdateWrapper` 中的 `User::getAge` 写法，**并不是调用 `getAge()` 方法**，而是利用 Java 8 的方法引用语法，让 MP 在运行时通过反射获取 getter 方法的名称，再解析出对应的数据库字段名（如 `age`）。

这种设计的优势：
- **编译期类型安全**：字段名写错了，编译直接报错，无需等到运行时
- **重构友好**：实体类字段改名时，IDE 会自动更新所有方法引用
- **代码补全**：IDE 能提示 `User::` 后面有哪些方法可选

因此，**日常开发优先使用 `LambdaQueryWrapper` / `LambdaUpdateWrapper`**，避免字符串硬编码字段名。

```java
// LambdaQueryWrapper 示例（类型安全，IDE 自动补全）
@Test
public void testWrapper() {
    // 1. 组合查询：年龄在 18-30 之间，用户名包含 "admin"，按年龄降序
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
        .between(User::getAge, 18, 30)
        .like(User::getUsername, "admin")
        .orderByDesc(User::getAge);
    List<User> users = userMapper.selectList(wrapper);
    
    // 2. 动态条件：条件成立时才拼接
    String name = null;  // 模拟从请求参数获取，可能为 null
    LambdaQueryWrapper<User> wrapper2 = new LambdaQueryWrapper<User>()
        .eq(StringUtils.hasText(name), User::getUsername, name)  // 推荐用 StringUtils
        .ge(User::getAge, 18)
        .select(User::getId, User::getUsername, User::getAge);   // 只查询指定字段
    List<User> users2 = userMapper.selectList(wrapper2);
    
    // 3. 多字段 OR 搜索（使用 and 加括号，or 改连接符）
    String keyword = "admin";
    LambdaQueryWrapper<User> wrapper3 = new LambdaQueryWrapper<User>()
        .and(w -> w
            .like(User::getUsername, keyword)
            .or()
            .like(User::getName, keyword)
            .or()
            .like(User::getEmail, keyword)
        );
    // SQL: WHERE (username LIKE '%admin%' OR name LIKE '%admin%' OR email LIKE '%admin%')
    // 括号内的 OR 条件从左到右短路匹配：满足一个即停止判断后续条件
    List<User> users3 = userMapper.selectList(wrapper3);
}

// 条件更新
@Test
public void testUpdate() {
    // 将用户名为 "zhangsan" 的用户年龄更新为 25
    LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
        .set(User::getAge, 25)
        .eq(User::getUsername, "zhangsan");
    userMapper.update(null, wrapper);
    
    // 另一种写法：使用实体对象 + wrapper 条件
    User user = new User();
    user.setAge(25);
    LambdaUpdateWrapper<User> wrapper2 = new LambdaUpdateWrapper<User>()
        .eq(User::getUsername, "zhangsan");
    userMapper.update(user, wrapper2);  // 第一个参数传实体，第二个传条件
}
```

**条件构造器常用方法速查**：

| 方法                       | 说明                             | 对应 SQL             |
| -------------------------- | -------------------------------- | -------------------- |
| `eq / ne`                  | 等于 / 不等于                    | `= / <>`             |
| `gt / ge`                  | 大于 / 大于等于                  | `> / >=`             |
| `lt / le`                  | 小于 / 小于等于                  | `< / <=`             |
| `like / notLike`           | 模糊匹配                         | `LIKE / NOT LIKE`    |
| `between / notBetween`     | 区间范围                         | `BETWEEN AND`        |
| `isNull / isNotNull`       | 空值判断                         | `IS NULL`            |
| `in / notIn`               | IN 查询                          | `IN (...) / NOT IN`  |
| `orderByAsc / orderByDesc` | 升序排序 / 降序排序              | `ORDER BY`           |
| `set`                      | 设置更新字段（仅 UpdateWrapper） | `SET column = value` |
| `last`                     | 拼接 SQL 末尾                    | **慎用**，有注入风险 |

**逻辑连接与括号**：

| 方法            | 作用                           | 示例                             | 生成 SQL      |
| --------------- | ------------------------------ | -------------------------------- | ------------- |
| `and(w -> ...)` | 加括号包裹条件，连接符仍为 AND | `.and(w -> w.eq(...).like(...))` | `AND ( ... )` |
| `or()`          | 将后续条件的连接符改为 OR      | `.eq(...).or().eq(...)`          | `OR ...`      |
| `or(w -> ...)`  | 加括号包裹，连接符改为 OR      | `.or(w -> w.eq(...).like(...))`  | `OR ( ... )`  |

> **注意**：MP 中多个条件默认用 AND 连接。使用 `or()` 可切换为 OR，使用 `and()` 可添加括号确保逻辑优先级正确（例如 `(A OR B) AND C` 必须用括号包裹 `A OR B`）。

------

##### 3. 分页查询：物理分页 + 自动 COUNT

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor paginationInterceptor() {
        // 1. 创建一个拦截器容器
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 2. 往容器里添加一个分页拦截器，指定数据库类型为 MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        // 3. 返回容器（Spring 会管理这个 Bean）
        return interceptor;
    }
}

// Service 中使用
public Page<User> pageByCondition(int pageNum, int pageSize, String username) {
    // Page 是 MyBatis-Plus 提供的分页类（com.baomidou.mybatisplus.extension.plugins.pagination.Page）
    // 构造参数：pageNum = 当前页码，pageSize = 每页条数
    Page<User> page = new Page<>(pageNum, pageSize);
    
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
        .like(StringUtils.hasText(username), User::getUsername, username);
    
    // 执行分页查询：MP 自动拼接 LIMIT 并执行 COUNT，结果自动填充到 page 对象
    // selectPage 会直接修改传入的 page 对象，同时返回该对象（传入和返回是同一个实例）
    return userMapper.selectPage(page, wrapper);
    // 返回的 page 包含：records（数据列表）、total（总条数）、pages（总页数）等
}
```

---

##### 4. 逻辑删除：配置即生效

```java
// 实体类字段标记 @TableLogic
@TableLogic
private Integer deleted;

// 执行 deleteById 时，MP 自动将 deleted 更新为 1，而非物理删除
userMapper.deleteById(1L);  
// 实际 SQL: UPDATE user SET deleted=1 WHERE id=1 AND deleted=0

// 查询时自动过滤已删除数据
userMapper.selectList(null);  
// 实际 SQL: SELECT ... FROM user WHERE deleted=0
```

**逻辑删除自动过滤规则**：

| 操作类型              | 是否自动追加 `AND deleted=0` | 说明                                                     |
| :-------------------- | :--------------------------- | :------------------------------------------------------- |
| 查询（`select` 系列） | 是                           | 自动过滤已删除数据，已删除的记录不可见                   |
| 更新（`update` 系列） | 是                           | 防止误更新已删除的数据                                   |
| 删除（`delete` 系列） | 是                           | 只对未删除的记录执行逻辑删除（将 `deleted` 从 0 改为 1） |
| 插入（`insert` 系列） | 否                           | 新插入的记录不存在删除状态问题                           |

> **注意**：自定义 Mapper 方法（如手写 `@Select` 或 `@Update`）不会自动追加过滤条件，需要手动加上 `AND deleted=0` 才能与 MP 默认行为保持一致。

> **黄金法则**：
>
> - 生产环境**强制使用逻辑删除**，物理删除仅用于数据清洗场景
> - 全局配置后，无需在每个 Mapper XML 中手动加 `deleted=0` 条件

---

##### 5. 自动填充：统一处理创建时间、更新时间

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}

// 实体类字段标记填充策略
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createTime;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updateTime;
```

**自动填充特性说明**：

MyBatis-Plus 的自动填充遵循 **“非 null 不覆盖”** 原则：
- 插入或更新时，若实体中自动填充字段为 `null`，则 MP 会调用 `MetaObjectHandler` 中的方法自动赋值
- 若实体中自动填充字段**已有值**（非 `null`），则 MP **不会覆盖**，保留开发者传入的值

因此，想要自动填充生效，**不要手动给自动填充字段赋值**（保持 `null`）。

>  若更新时字段值实际未发生变化，MyBatis-Plus 会跳过 `UPDATE` SQL 的执行，自动填充也不会触发。

```java
// 错误：先查询再修改，updateTime 已有旧值，自动填充不生效
User user = userMapper.selectById(id);
user.setEmail("new@email.com");
userMapper.updateById(user);  // updateTime 不会被更新

// 正确：新建实体，只设置 ID 和要修改的字段
User user = new User();
user.setId(id);
user.setEmail("new@email.com");
userMapper.updateById(user);  // updateTime 为 null，触发自动填充

// 也正确：使用 UpdateWrapper，无实体，自动填充生效
userMapper.update(null, new LambdaUpdateWrapper<User>()
    .eq(User::getId, id)
    .set(User::getEmail, "new@email.com"));
```

------

#### 与原生 MyBatis 混用策略

| 需求                       | 实现方式                                            | 说明                         |
| -------------------------- | --------------------------------------------------- | ---------------------------- |
| 单表简单 CRUD              | MP 的 `BaseMapper` + `LambdaQueryWrapper`           | 零 XML，效率最高             |
| 复杂多表关联查询           | 原生 MyBatis XML（或注解）                          | 保持 SQL 完全可控            |
| 复杂更新（如批量更新）     | `UpdateWrapper` 或原生 XML                          | 按需选择                     |
| 自定义 SQL 与 Wrapper 混合 | Mapper 接口中定义方法 + `@Param(Constants.WRAPPER)` | 既复用动态条件，又自定义 SQL |

**混合示例**：

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 自定义查询：关联订单表，同时复用 MP 的条件构造器
    List<UserVO> selectUserWithOrders(@Param(Constants.WRAPPER) Wrapper<User> wrapper);
}
```

```xml
<!-- UserMapper.xml -->
<select id="selectUserWithOrders" resultType="com.ims.vo.UserVO">
    SELECT u.id, u.username, o.order_no
    FROM user u
    LEFT JOIN orders o ON u.id = o.user_id
    ${ew.customSqlSegment}   <!-- MP 自动将 wrapper 条件替换进来 -->
</select>
```

---

#### 常见避坑指南

| 问题现象                         | 根本原因                          | 解决方案                                    |
| -------------------------------- | --------------------------------- | ------------------------------------------- |
| **`updateById` 更新了所有字段**  | MP 默认按实体所有非 null 字段更新 | 使用 `UpdateWrapper` 指定更新字段           |
| **逻辑删除后唯一索引冲突**       | 已删除数据和正常数据共享唯一索引  | 设计时允许重复，或联合 `deleted` 做唯一索引 |
| **分页总记录数不对**             | COUNT 子查询被复杂 JOIN 影响      | 手动写 COUNT SQL 或用 `optimizeJoin` 配置   |
| **Lambda 表达式序列化异常**      | 实体类未实现 `Serializable`       | 让实体类实现 `Serializable`                 |
| **`saveOrUpdate` 性能差**        | 先查后插，多一次 DB 交互          | 明确业务场景，优先 `insert` 或 `update`     |
| **代码生成器生成后仍需手动调整** | 表字段类型映射不完美              | 自定义 `InjectionConfig` 或手动修正         |

---

#### 最佳实践清单

| 项目           | 推荐做法                                                | 避坑指南                             |
| -------------- | ------------------------------------------------------- | ------------------------------------ |
| **主键策略**   | 分布式用 `assign_id`（雪花算法），单机用 `auto`         | 避免 `input`（需手动赋值）           |
| **条件构造器** | 始终用 `LambdaQueryWrapper`                             | 禁止用字符串 `QueryWrapper` 写字段名 |
| **分页**       | 配合 `Page` 对象 + 分页插件                             | 不要自己手写 `LIMIT` 后再查 `COUNT`  |
| **逻辑删除**   | 全局配置 + `@TableLogic`                                | 物理删除必须经过代码审查             |
| **自动填充**   | 统一使用 `MetaObjectHandler`                            | 不在业务代码中手动设置时间           |
| **SQL 日志**   | 开发环境开启，生产关闭                                  | 生产开启会打印全部数据行             |
| **性能**       | 大批量操作使用 `insertBatchSomeColumn` 或 `updateBatch` | 避免循环调用 `insert`/`update`       |

> **总结**：  
> MyBatis-Plus 解决了 MyBatis 在单表 CRUD 和动态条件场景下的繁琐问题，**与原生 MyBatis 完全兼容**。建议新项目直接使用 MyBatis-Plus，复杂 SQL 继续用 XML，两者取长补短。

------

## 模块三：核心机制与架构

### 3.1 分层架构 —— 职责清晰，代码可维护

#### 为什么需要分层？从“一团乱麻”到“井井有条”

**单层代码的典型问题**：

```java
@RestController
public class UserController {
    @GetMapping("/users/{id}")
    public Object getUser(@PathVariable Integer id) {
        // 1. 手动验证参数
        if (id == null || id <= 0) return "参数错误";
        
        // 2. 直连数据库（硬编码SQL）
        try (Connection conn = DriverManager.getConnection(...)) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM user WHERE id=?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            // 3. 手动映射结果
            if (rs.next()) {
                // 4. 业务逻辑混在Controller里
                if (rs.getInt("vip") == 1) {
                    // 5. 直接拼接返回JSON
                    return "{\"name\":\"" + rs.getString("name") + "\",\"discount\":0.8}";
                }
            }
        } catch (Exception e) {
            return "系统错误";
        }
        return "用户不存在";
    }
}
```

**痛点**：  

- 修改一个字段需改多处  
- 无法单元测试业务逻辑  
- 多人协作冲突频繁  
- 安全漏洞（SQL拼接、无异常处理）  
- 代码无法复用  

**分层的价值**：  

> **“修改一处，不影响其他” —— 这是可维护性的黄金标准**

---

#### Spring Boot 项目中的三层实践（Controller → Service → Mapper）

> **重要澄清**：  
> 此“三层”是**经典三层架构在现代框架下的高效落地**：  
>
> - **Controller** = 表示层（处理HTTP）  
> - **Service** = 业务逻辑层（核心规则）  
> - **Mapper** = 数据访问层（MyBatis 对 DAO 模式的轻量实现）  
>
> **Mapper 接口即 DAO 接口**，MyBatis 在运行时自动生成代理实现，省略了手写 DAO 实现类，但**分层思想与职责边界依然严格存在**。

##### Controller 层：专注“收发”，不碰业务

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService; // 仅依赖Service
    
    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable @Min(1) Integer id) {
        // 职责：参数校验 + 调用Service + 统一返回
        User user = userService.getUserById(id);
        return Result.success(convertToVO(user)); // VO：返回给前端的对象
    }
    
    // 简单转换（可用 Spring 的 BeanUtils.copyProperties 简化，复杂转换建议用 MapStruct）
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setName(user.getUsername());
        vo.setVip(user.getVipLevel() > 0);
        return vo;
    }
}
```

**该做**：  

- 接收/解析 HTTP 请求参数  
- 基础参数校验（`@Valid`、`@Min`）  
- 调用 Service 获取结果  
- 转换为前端友好的 VO（View Object）  
- 返回统一响应格式（`Result<T>`）  

**禁止**：  

- 写 if/else 业务判断（如“VIP打8折”）  
- 直接调用 Mapper  
- 拼接 SQL 或处理数据库异常  

---

##### Service 层：专注“业务”，不碰协议

```java
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PointService pointService; // 可组合其他Service
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User getUserById(Integer id) {
        // 职责：业务规则 + 事务管理 + 多数据源协调
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        
        // 业务规则：计算用户等级（示例）
        if (user.getPoint() >= 10000) {
            user.setLevel("VIP");
            // 同步更新积分明细（调用其他Service）
            pointService.recordLevelUpgrade(user.getId());
        }
        
        return user;
    }
}
```

**该做**：  

- 封装核心业务规则  
- 管理事务（`@Transactional`）  
- 组合多个 Mapper 或 Service  
- 抛出业务异常（自定义 `BusinessException`）  

**禁止**：  

- 获取 HttpServletRequest/Response  
- 返回 ResponseEntity 或处理 HTTP 状态码  
- 包含 SQL 语句或 MyBatis 注解  

---

##### Mapper 层：专注“存取”，不碰逻辑

```java
@Mapper
public interface UserMapper {
    // 职责：声明数据库操作，由 MyBatis 自动生成实现
    User selectById(@Param("id") Integer id);
    int insert(User user);
    int updateById(User user);
    int deleteById(@Param("id") Integer id);
    
    // 动态查询（SQL 逻辑在 XML 中，接口保持纯净）
    List<User> selectByCondition(@Param("name") String name, @Param("minPoint") Integer minPoint);
}
```

**该做**：  

- 声明 CRUD 方法  
- 使用 `@Param` 明确参数  
- 保持接口简洁（实现细节在 XML/注解中）  

**禁止**：  

- 在接口中写 if/else 业务判断  
- 返回 Map 或 Object（应返回明确实体类）  
- 包含事务注解（事务属于 Service 层）  

---

#### 依赖注入（DI）：分层的“粘合剂”

```java
// 传统方式：紧耦合，无法替换，难测试
public class UserController {
    private UserService userService = new UserServiceImpl(); 
}

// Spring 方式：松耦合，易替换，易测试
@RestController
public class UserController {
    @Autowired // Spring IoC 容器自动注入实现类
    private UserService userService; 
}
```

**IoC 容器的核心价值**：  

- 你只需声明“我需要什么”（`@Autowired`）  
- Spring 负责“创建并装配好”（扫描 `@Service`、`@Mapper`）  
- **层与层之间通过接口依赖，而非具体实现** → 修改实现不影响上层  

> 这正是“面向接口编程”在 Spring 中的落地体现。

---

#### 常见误区与最佳实践

| 误区                        | 正确做法                                    | 为什么                                          |
| --------------------------- | ------------------------------------------- | ----------------------------------------------- |
| **Controller 写业务逻辑**   | 业务规则移至 Service                        | Controller 变“胖”，难测试、难复用               |
| **Service 直接返回 Entity** | 转为 VO/DTO 再返回                          | 避免暴露数据库字段（如 password）、防止循环引用 |
| **Mapper 方法命名模糊**     | `selectByStatusAndCreateTime`               | 清晰表达意图，避免注释                          |
| **Service 调用 Controller** | 严格单向依赖：Controller → Service → Mapper | 循环依赖导致架构崩坏                            |
| **在 Mapper 做字段转换**    | 转换逻辑放在 Service 或 Converter           | 保持 Mapper 纯粹性                              |

**黄金法则**：  

> **“上层可感知下层存在，但绝不绕过中间层”**  
>
> - Controller 可知 Service，但**不可直连 Mapper**  
> - Service 可知 Mapper，但**不可感知 Controller**  

---

#### 为什么这“三层”值得坚持？

| 维度           | 无分层                 | 有分层                          |
| -------------- | ---------------------- | ------------------------------- |
| **修改成本**   | 改一处，动全身         | 改业务？只动 Service            |
| **单元测试**   | 无法隔离测试           | Mock Mapper 测 Service          |
| **团队协作**   | 前后端、后端内部冲突多 | Controller 接口定稿后，并行开发 |
| **技术演进**   | 换框架=重写            | 换 ORM？只改 Mapper 层          |
| **代码可读性** | “这是什么逻辑？”       | “哦，这是 Service 层的优惠计算” |

> 举例：  
>
> - 需求：用户积分规则变更 → **只改 UserService**  
> - 需求：数据库从 MySQL 迁至 PostgreSQL → **只改 Mapper XML**  
> - 需求：前端需要新字段 → **加 VO + Controller 转换，不动 Service**  

---

### 3.2 Spring AOP - 横切关注点与代码解耦

#### 什么是 AOP？为什么需要它？

**核心痛点**：
在传统的 OOP（面向对象编程）中，我们习惯将功能垂直拆分（如 Controller、Service）。但随着系统变大，会发现有一些代码是**横切**在多个业务逻辑之中的，例如：

- **日志记录**：每个接口都需要记录入参和出参。
- **性能监控**：每个方法都需要统计耗时。
- **事务管理**：保证数据一致性。

如果在每个方法里都手动写这些代码，会导致**代码冗余**且**难以维护**（修改日志格式需要改 100 个文件）。

**AOP 的解决方案**：
AOP（Aspect Oriented Programming，面向切面编程）允许你将这些**横切关注点（Cross-Cutting Concerns）**提取出来，动态地织入到指定的业务逻辑前后，实现**非侵入式**的功能增强。

#### 核心组件与工作流

Spring AOP 的本质是**代理模式**（Proxy Pattern）的实现。其工作流程始于配置，终于通知执行。

##### 启动开关：`@EnableAspectJAutoProxy`

**核心作用**：
这是 AOP 的总开关。通常由 Spring Boot 自动配置开启，它会向容器注册 `AnnotationAwareAspectJAutoProxyCreator`。

**关键机制**：
- **Proxy 机制**：Spring 会在 Bean 初始化后，检查是否有匹配的切面。如果有，就创建一个代理对象（Proxy）替换原生 Bean。
- **属性控制**：
  - `proxyTargetClass = false`（默认）：优先使用 **JDK 动态代理**（基于接口）。
  - `proxyTargetClass = true`：强制使用 **CGLIB 代理**（基于继承，适用于无接口类）。
  - `exposeProxy = false`：通常不暴露代理对象，避免滥用。

##### 切面定义：`@Aspect`

**核心作用**：
标记一个类为切面类（Aspect），告诉 Spring：“我包含通用的增强逻辑”。

**使用规范**：
- 必须配合 `@Component` 注册到 Spring 容器。
- 一个切面类可以包含多个切入点和通知方法。

```java
@Aspect
@Component
public class LoggingAspect {
    // 切面逻辑...
}
```

##### 切入点定义：`@Pointcut`

**核心作用**：
定义**“在哪里增强”**。使用 AspectJ 表达式语言精准定位连接点（Join Points）。

**常用表达式速查**：

| 表达式类型     | 示例                                           | 说明                                   |
| :------------- | :--------------------------------------------- | :------------------------------------- |
| **execution**  | `execution(* com.ims.service..*(..))`          | 匹配 service 包下所有类的所有方法      |
| **annotation** | `@annotation(com.ims.annotation.LogExecution)` | 匹配带有特定注解的方法（推荐，更灵活） |
| **within**     | `within(com.ims.controller.*)`                 | 匹配特定包下的所有方法                 |

#### 通知类型：五大核心注解

通知（Advice）定义了**“何时增强”**以及**“如何增强”**。它们的执行顺序构成了 AOP 的调用链。

| 注解              | 时机                   | 典型场景                 | 特点                               |
| :---------------- | :--------------------- | :----------------------- | :--------------------------------- |
| `@Before`         | 目标方法执行**前**     | 权限校验、参数预处理     | 无法获取返回值                     |
| `@AfterReturning` | 目标方法**成功**返回后 | 记录操作结果、缓存更新   | 可获取返回值，若抛异常则不执行     |
| `@AfterThrowing`  | 目标方法**抛出异常**后 | 统一异常日志、告警       | 可获取异常信息                     |
| `@After`          | 目标方法**最终**结束后 | 资源释放（类似 finally） | 无论成功或失败都会执行             |
| `@Around`         | **包裹**整个目标方法   | 性能监控、事务管理       | **最强大**，需手动调用 `proceed()` |

**`@Around` 的特殊性**：
它是最底层的实现机制，其他四种通知本质上都是 `@Around` 的简化版。它拥有对流程的完全控制权（决定是否执行、修改参数、修改返回值）。

#### 执行顺序与优先级

当多个切面作用于同一个方法时，执行顺序遵循**“先进后出”**（类似栈）的原则。

##### `@Order` 注解控制

- **前置逻辑**：`@Order(1)` 先执行，`@Order(2)` 后执行。
- **后置逻辑**：`@Order(2)` 先执行，`@Order(1)` 后执行（包裹在最外层的最后清理）。

##### 同一切面内的执行流

> **正常流程**：
> `@Around(前)` → `@Before` → **目标方法** → `@Around(后)` → `@AfterReturning` → `@After`
>
> **异常流程**：
> `@Around(前)` → `@Before` → **目标方法(抛异常)** → `@AfterThrowing` → `@After`

#### 典型实战场景（代码模板）

##### 场景 1：统一操作日志

利用 AOP 记录用户操作，避免在每个 Service 方法中写日志代码。

```java
@Aspect
@Component
@Order(1) // 优先级较高
public class LogAspect {
    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    // 定义切入点：拦截所有 Service 包下的公共方法
    @Pointcut("execution(public * com.ims.service..*.*(..))")
    public void servicePointcut() {}

    @Around("servicePointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        Object[] args = pjp.getArgs();
        
        log.info("【AOP 日志】方法 {} 开始执行，参数: {}", methodName, Arrays.toString(args));

        long start = System.currentTimeMillis();
        try {
            // 1. 前置处理：如参数校验
            Object result = pjp.proceed(); // 2. 执行目标方法
            long cost = System.currentTimeMillis() - start;
            
            // 3. 返回后处理
            log.info("【AOP 日志】方法 {} 执行成功，耗时: {}ms", methodName, cost);
            return result;
        } catch (Exception e) {
            // 4. 异常处理：这里可以捕获并包装异常，再抛给全局异常处理器
            log.error("【AOP 日志】方法 {} 执行异常: {}", methodName, e.getMessage());
            throw e;
        }
    }
}
```

##### 场景 2：接口性能监控

利用 `@Around` 精确统计方法耗时。

```java
@Aspect
@Component
public class PerformanceAspect {
    @Around("@annotation(com.ims.annotation.PerformanceMonitor)")
    public Object monitor(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        Object result = pjp.proceed();
        long duration = (System.nanoTime() - start) / 1_000_000; // 转毫秒
        
        if (duration > 1000) {
            // 超过 1秒 记录为慢查询
            log.warn("慢方法警告: {} 耗时 {}ms", pjp.getSignature(), duration);
        }
        return result;
    }
}
```

#### 高频避坑指南

| 问题现象           | 根本原因                          | 解决方案                                                     |
| :----------------- | :-------------------------------- | :----------------------------------------------------------- |
| **AOP 不生效**     | 类内部方法调用（Self-invocation） | 1. 通过 `ApplicationContext` 获取代理对象调用 2. 重构代码，将逻辑移到另一个 Bean 中 |
| **循环依赖**       | 切面 Bean 与目标 Bean 互相依赖    | 检查切面逻辑，避免在切面中直接注入使用了该切面的目标类       |
| **CGLIB 代理失败** | 方法为 `private` 或 `final`       | 切面只能代理 `public` 方法；CGLIB 无法代理 `final` 类或方法  |
| **多切面顺序混乱** | 未指定优先级                      | 明确使用 `@Order` 注解或实现 `Ordered` 接口                  |

**黄金法则**：
1. **优先使用注解式切入点**（`@annotation`）：比 `execution` 表达式更灵活，只需在方法上加个注解即可生效。
2. **慎用 `exposeProxy`**：虽然可以通过 `AopContext.currentProxy()` 强制获取代理解决自调用问题，但这会增加代码对 Spring API 的耦合，建议通过代码重构（拆分 Bean）来解决。
3. **性能考量**：AOP 基于动态代理，虽然性能损耗极小，但**不要**在高频核心算法路径上使用过于复杂的切面逻辑。

---

### 3.3 事务管理与事件同步

Spring 事务管理依赖 AOP 代理，`@Transactional` 负责声明事务边界，而 `@TransactionalEventListener` 则将事件机制与事务生命周期绑定。

---

#### 事务基础设施激活：`@EnableTransactionManagement`

`@EnableTransactionManagement` 是事务功能的总开关，在容器启动时注册事务切面、拦截器等基础设施。没有它，`@Transactional` 将完全不生效。

**用法**：在任意 Spring 配置类或主启动类上添加：

```java
@SpringBootApplication
@EnableTransactionManagement
public class ImsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImsApplication.class, args);
    }
}
```

> Spring Boot 的某些 starter（如 `spring-boot-starter-data-jpa`）会自动开启事务支持，但显式声明始终是最安全的做法。

---

#### 事务边界声明：`@Transactional`

将 `@Transactional` 标注在 **public 方法** 或 **类** 上，即可为该类中所有 public 方法开启事务。

```java
@Service
public class OrderService {
    
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Order order) {
        orderMapper.insert(order);
        orderItemMapper.batchInsert(order.getItems());
        // 若任何一步抛出异常，事务自动回滚
    }
}
```

##### 关键属性速查

| 属性          | 说明               | 推荐值                                   |
| ------------- | ------------------ | ---------------------------------------- |
| `rollbackFor` | 指定回滚的异常类型 | `Exception.class`（必须）                |
| `propagation` | 事务传播行为       | `REQUIRED`（默认）                       |
| `isolation`   | 隔离级别           | 数据库默认（MySQL 为 `REPEATABLE_READ`） |
| `timeout`     | 超时（秒）         | 按业务设置                               |
| `readOnly`    | 只读事务优化       | 查询方法设为 `true`                      |

**传播行为典型场景**：

| 传播行为       | 场景                     |
| -------------- | ------------------------ |
| `REQUIRED`     | 普通业务方法（默认）     |
| `REQUIRES_NEW` | 独立子事务（如日志记录） |
| `SUPPORTS`     | 只读查询                 |

##### 失效场景与解决方案

| 失效场景                  | 解决方案                                   |
| ------------------------- | ------------------------------------------ |
| 自调用（`this.method()`） | 通过 `ApplicationContext` 获取代理对象调用 |
| 非 public 方法            | 改为 public                                |
| 异常被 catch 且未抛出     | 重新抛出或手动标记回滚                     |
| 未指定 `rollbackFor`      | 添加 `rollbackFor = Exception.class`       |

**自调用修复示例**：

```java
@Service
public class OrderService {
    @Autowired
    private ApplicationContext context;
    
    public void saveOrder(Order order) {
        // 获取代理对象再调用，事务生效
        context.getBean(OrderService.class).updateOrder(order);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Order order) { ... }
}
```

---

#### 事件监听与事务同步：`@EventListener` vs `@TransactionalEventListener`

在业务中常需要在事务提交后执行操作（如发送消息），避免因事务回滚导致不一致。Spring 提供两种事件监听：

- `@EventListener`：立即执行，与发布者同一事务。
- `@TransactionalEventListener`：在事务提交后执行。

##### 对比

| 维度           | `@EventListener`     | `@TransactionalEventListener` |
| -------------- | -------------------- | ----------------------------- |
| 执行时机       | 事件发布后立即执行   | 事务提交后执行                |
| 事务上下文     | 共享发布者事务       | 无事务（主事务已提交）        |
| 数据可见性     | 可能脏读             | 只读已提交数据                |
| 监听器异常影响 | 导致主事务回滚       | 不影响主事务                  |
| 适用场景       | 缓存、日志、轻量操作 | 短信、邮件、MQ 等强一致性操作 |

##### 实战示例：用户注册

```java
// 事件
@Data
public class UserRegisteredEvent {
    private final User user;
}

// 业务服务
@Service
public class UserService {
    @Autowired
    private ApplicationEventPublisher publisher;
    
    @Transactional(rollbackFor = Exception.class)
    public void register(User user) {
        userMapper.insert(user);
        publisher.publishEvent(new UserRegisteredEvent(user));
    }
}

// 短信服务（等待事务提交）
@Component
public class SmsService {
    @TransactionalEventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        smsClient.send(event.getUser().getPhone(), "注册成功");
    }
}

// 缓存服务（立即执行）
@Component
public class CacheService {
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        cache.put("user:" + event.getUser().getId(), event.getUser());
    }
}
```

> `@TransactionalEventListener` 默认在 `AFTER_COMMIT` 阶段触发，也可通过 `phase` 指定其他阶段（如 `AFTER_ROLLBACK`、`BEFORE_COMMIT`）。

---

#### 事务与事件协同的最佳实践

##### 与 MyBatis 整合

Spring Boot + MyBatis 项目中，`@Transactional` 直接作用于 Mapper 方法，无需额外配置：

```java
@Service
public class ProductService {
    @Transactional(rollbackFor = Exception.class)
    public void batchInsert(List<Product> products) {
        products.forEach(productMapper::insert);
    }
}
```

##### 事务边界原则

> **黄金法则**：事务方法只做数据库操作，不做远程调用、文件 IO、耗时计算。

| 应该做的                     | 不应该做的               |
| ---------------------------- | ------------------------ |
| 数据库 CRUD                  | 发送 HTTP 请求           |
| 调用同一事务内的其他 DB 操作 | 调用第三方 API           |
| 发布事件（交给监听器）       | 在事务中执行大量循环逻辑 |

---

#### 关键规范与避坑清单

##### 事务激活
- 确认 `@EnableTransactionManagement` 存在于配置类（Spring Boot 主类即可）。
- 自定义数据源时，务必显式添加。

##### 事务边界
- `@Transactional` 仅用于 `public` 方法。
- 必须指定 `rollbackFor = Exception.class`。
- 禁止在类内通过 `this` 调用事务方法。
- 查询方法加上 `@Transactional(readOnly = true)`。

##### 事件监听
- 强一致性外部调用 → `@TransactionalEventListener`。
- 轻量级、允许脏读操作 → `@EventListener`。
- 避免在监听器中开启新事务。

##### 典型错误

| 错误                                | 后果                   | 正确做法                                 |
| ----------------------------------- | ---------------------- | ---------------------------------------- |
| 忘记 `@EnableTransactionManagement` | 事务完全不生效         | 显式添加                                 |
| 在 `@EventListener` 中调用外部服务  | 外部异常导致业务回滚   | 改用 `@TransactionalEventListener`       |
| 事务中发送 MQ                       | 事务回滚后消息已发出   | 将 MQ 放在 `@TransactionalEventListener` |
| 事务中执行大批量循环                | 长事务阻塞连接池       | 分批或拆分事务                           |
| 捕获异常后不抛出                    | 异常被吞，事务正常提交 | 重新抛出或手动回滚                       |

------

## 模块四：接口与安全

### 4.1 Spring Security - 登录认证与基础权限控制

#### 核心作用与默认行为

**本质定义**：  
Spring Security 是 Spring 生态的**安全框架**，专注解决：  
**认证（Authentication）**：你是谁？（登录验证）  
**授权（Authorization）**：你能做什么？（接口/方法权限）  

##### 引入 starter 后的默认行为

| 行为                  | 说明                                                | 开发影响                         |
| --------------------- | --------------------------------------------------- | -------------------------------- |
| 拦截所有 HTTP 请求    | 除 `/error`、`/favicon.ico` 外                      | 启动后首次访问自动跳转 `/login`  |
| 生成随机密码          | 控制台输出 `Using generated security password: xxx` | 用 `user` + 控制台密码可临时登录 |
| 启用 CSRF 保护        | 表单提交需携带 `_csrf` token                        | 前后端分离项目需关闭或适配       |
| 表单登录 + HTTP Basic | 双模式支持                                          | 适合快速验证                     |

> **关键认知**：  
>
> - **不是“加了就安全”**：默认配置仅用于开发测试，生产必须显式配置  
> - **安全是链式流程**：请求 → 过滤器链（认证/授权）→ 业务逻辑  

---

#### 基础配置：从禁用到启用（按需选择）

##### 场景1：开发阶段临时禁用（快速调试接口）

```java
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
```

> **仅限本地开发**！提交代码前务必移除 `exclude`

##### 场景2：启用基础认证

```java
@Configuration
@EnableWebSecurity // 启用 Web 安全支持（Spring Security 6+ 核心注解）
public class SecurityConfig {

    // 密码加密器（必须！禁止明文存储）
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 安全过滤器链配置
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 接口权限规则（按路径配置）
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/public/**", "/error").permitAll() // 公开接口
                .requestMatchers("/admin/**").hasRole("ADMIN")                // 需 ADMIN 角色
                .anyRequest().authenticated()                                 // 其他接口需登录
            )
            // 登录配置
            .formLogin(form -> form
                .loginPage("/login")           // 自定义登录页（前端提供）
                .loginProcessingUrl("/doLogin") // 登录提交接口
                .defaultSuccessUrl("/index")   // 登录成功跳转
                .failureUrl("/login?error")    // 失败跳转
                .permitAll()
            )
            // 退出配置
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            // 前后端分离项目关键配置（二选一）
            .csrf(csrf -> csrf.disable()) // 关闭 CSRF（需配合 Token 验证）
            // .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**")) // 或仅忽略 API 路径
            .cors(cors -> cors.configurationSource(corsConfigurationSource())); // 启用 CORS

        return http.build();
    }

    // CORS 配置（前后端分离必备）
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:8080")); // 前端地址
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

---

#### 数据库用户认证（结合 MyBatis）

##### 步骤1：实现 UserDetailsService（查询数据库）

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper; 

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 查询用户（含角色）
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        
        // 2. 查询角色（示例：逗号分隔 "ADMIN,USER"）
        List<String> roles = userMapper.selectRolesByUserId(user.getId());
        
        // 3. 构建 UserDetails（密码必须是 BCrypt 加密后的）
        return User.builder()
            .username(user.getUsername())
            .password(user.getPassword()) // 数据库存储的已是加密密码
            .authorities(roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList()))
            .disabled(!user.getEnabled()) // 账号是否启用
            .build();
    }
}
```

##### 步骤2：配置认证管理器（关联密码加密器）

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder()); // 关键：匹配数据库加密密码
        return provider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) 
            throws Exception {
        return config.getAuthenticationManager();
    }
    
    // ... filterChain 配置
}
```

##### 步骤3：注册时密码加密（Service 层示例）

```java
@Service
public class UserService {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserMapper userMapper;
    
    public void register(User user) {
        // 注册前加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
    }
}
```

##### 步骤4：登录时密码校验（两种方式）

Spring Security 推荐让框架自动完成密码校验，但如果你需要**自定义登录接口**（如返回 JWT 或 SaToken），可以手动调用 `matches`：

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO dto) {
        // 1. 根据用户名查询用户
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 2. 手动校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 3. 生成 Token（JWT 或其他）
        String token = generateToken(user);
        return Result.success(token);
    }
}
```

> **说明**：`passwordEncoder.matches(rawPassword, encodedPassword)` 是 Spring Security 提供的密码比对方法，内部使用 BCrypt 算法（或你配置的其他编码器）进行安全比对。

#### 权限控制方式

| 级别       | 方式                          | 适用场景                         | 示例                                                         |
| ---------- | ----------------------------- | -------------------------------- | ------------------------------------------------------------ |
| **URL 级** | `requestMatchers().hasRole()` | 接口路径权限（粗粒度）           | `.requestMatchers("/admin/**").hasRole("ADMIN")`             |
| **方法级** | `@PreAuthorize`               | 服务方法权限（细粒度）           | `@PreAuthorize("hasRole('ADMIN')")``public void deleteUser(Long id)` |
| **数据级** | 业务逻辑判断                  | 行级权限（如“只能删自己的数据”） | `if (!order.getUserId().equals(currentUserId)) throw ...`    |

##### 启用方法级权限（需额外配置）

```java
@Configuration
@EnableMethodSecurity // 替代旧版 @EnableGlobalMethodSecurity
public class MethodSecurityConfig { }
```

```java
@Service
public class OrderService {
    
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.username")
    public Order getOrder(Long id, String userId) {
        // 仅 ADMIN 或本人可查
        return orderMapper.selectById(id);
    }
}
```

---

#### 与 Spring Boot 整合要点

##### 必须依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<!-- 前后端分离项目建议加（处理 CORS） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

##### 关键配置（`application.yml`）

```yaml
spring:
  security:
    user:
      name: dev_user      # 仅开发环境临时用（生产必须禁用！）
      password: dev_pass
      roles: USER
```

> **生产环境务必删除此配置**！依赖数据库认证

##### 常见问题速查

| 问题                  | 原因                         | 解决方案                                                |
| --------------------- | ---------------------------- | ------------------------------------------------------- |
| 登录后仍跳转 `/login` | Session 未创建或 CSRF 未处理 | 检查 `formLogin` 配置；前后端分离需关闭 CSRF 或传 token |
| 密码校验失败          | 数据库存储明文密码           | 注册时用 `passwordEncoder.encode()` 加密                |
| 跨域请求被拦截        | 未配置 CORS                  | 实现 `CorsConfigurationSource` Bean                     |
| 角色判断失效          | 未加 `ROLE_` 前缀            | `hasRole("ADMIN")` 要求权限为 `ROLE_ADMIN`              |

---

#### 最佳实践清单

| 项目         | 推荐做法                                    | 避坑指南                             |
| ------------ | ------------------------------------------- | ------------------------------------ |
| **密码存储** | BCrypt 加密（强度 10-12）                   | 禁止明文、MD5、SHA1                  |
| **登录接口** | 前后端分离用 `/doLogin` + JSON              | 避免直接用默认 `/login` 表单         |
| **CSRF**     | 前后端分离项目关闭 + Token 验证             | 不要全局关闭而不加替代方案           |
| **角色命名** | 数据库存 `ADMIN`，代码用 `hasRole("ADMIN")` | Spring Security 自动加 `ROLE_` 前缀  |
| **权限粒度** | URL 级 + 关键方法级组合                     | 避免所有权限写在 Controller          |
| **开发调试** | 临时禁用安全（`exclude`）                   | 提交前务必恢复安全配置               |
| **错误处理** | 自定义 `AuthenticationEntryPoint` 返回 JSON | 避免返回 HTML 登录页（前端无法解析） |

---

### 4.2 RESTful API 设计 —— 规范即效率，约定即生产力

#### 什么是 REST？—— 架构风格的本质

> **REST（Representational State Transfer）是一种软件架构风格，由 Roy T. Fielding 在 2000 年博士论文中提出。它不是协议、不是标准、更不是技术框架，而是一组为构建可伸缩、分布式系统设计的架构约束。**  
> —— *关键澄清：REST 与 HTTP 无必然绑定，但 HTTP 协议的设计高度契合 REST 约束，故 Web API 常基于 HTTP 实现 RESTful*

##### REST 的六大核心约束（缺一不可）

| 约束                              | 本质含义                                         | Web API 中的体现                                    |
| --------------------------------- | ------------------------------------------------ | --------------------------------------------------- |
| **客户端-服务器分离**             | 关注点分离：客户端专注交互，服务端专注数据与安全 | 前后端解耦，独立演进                                |
| **无状态（Stateless）**           | **每个请求必须包含处理所需全部信息**             | 服务端不存 Session；认证靠 Token（如 JWT）          |
| **可缓存（Cacheable）**           | 响应必须明确声明是否可缓存                       | `GET /users/1` 响应带 `Cache-Control: max-age=3600` |
| **统一接口（Uniform Interface）** | **REST 的灵魂**：用统一方式操作资源              | 资源用 URI 标识，操作用标准 HTTP 方法               |
| **分层系统（Layered System）**    | 客户端无需感知中间层                             | 网关、负载均衡、CDN 可透明插入                      |
| **按需代码（Code on Demand）**    | 服务器可扩展客户端功能（可选）                   | Web API 场景极少使用（如返回 JS 脚本）              |

> **核心结论**：  
>
> - **RESTful = 满足 REST 约束（尤其是统一接口 + 无状态）的系统**  
> - 日常所说的 “RESTful API” 特指：**基于 HTTP 协议，严格遵循“统一接口”约束设计的 Web API**  
> - 误区：”用 JSON 就是 RESTful“ → 本质是**资源操作语义是否符合约束**

---

#### RESTful API 的核心设计要素

##### 要素 1：资源（Resource）—— 一切皆资源

- **定义**：系统中任何可被标识、操作的实体（用户、订单、文章）  
- **关键**：用**名词**抽象资源，**禁用动词**  

  ```diff
  - /getUser?id=1      # 动词 + 参数（RPC 风格）
  + /users/1           # 名词资源标识（RESTful）
  ```

##### 要素 2：URI 设计规范

| 原则            | 正确示例                | 错误示例                     | 原因                     |
| --------------- | ----------------------- | ---------------------------- | ------------------------ |
| **名词复数**    | `/users`                | `/user`                      | 资源集合语义清晰         |
| **层级清晰**    | `/orders/123/items`     | `/getOrderItems?orderId=123` | 表达资源从属关系         |
| **小写+短横线** | `/api/v1/user-profiles` | `/API/V1/UserProfiles`       | URL 规范、避免编码问题   |
| **避免动词**    | `DELETE /users/1`       | `POST /deleteUser`           | 操作语义由 HTTP 方法承载 |

##### 要素 3：HTTP 方法 = 操作语义（统一接口的核心）

| 方法     | 幂等性 | 语义         | 必须满足的约束                     |
| -------- | ------ | ------------ | ---------------------------------- |
| `GET`    | 是     | 获取资源     | 安全（不修改状态）、可缓存         |
| `POST`   | 否     | 创建子资源   | 返回 `201 Created` + `Location` 头 |
| `PUT`    | 是     | 全量替换资源 | 重复提交结果不变（适合网络重试）   |
| `PATCH`  | 否     | 部分更新资源 | 仅修改传入字段，避免清空未传字段   |
| `DELETE` | 是     | 删除资源     | 重复删除无副作用                   |

> **语义对比**：  
>
> - `PUT /users/1` + `{ "name": "张三" }` → **清空其他字段**，仅保留 name  
> - `PATCH /users/1` + `{ "name": "张三" }` → **仅更新 name**，其他字段保留

##### 要素 4：HTTP 状态码 = 协议层结果

| 状态码                     | 语义                              | **必须返回的场景**            |
| -------------------------- | --------------------------------- | ----------------------------- |
| `200 OK`                   | 操作成功                          | `GET`/`PUT`/`PATCH` 成功      |
| `201 Created`              | **资源创建成功**                  | `POST` 成功，且返回新资源位置 |
| `204 No Content`           | 操作成功，无返回体                | `DELETE` 成功                 |
| `400 Bad Request`          | 客户端参数错误                    | 参数校验失败、JSON 格式错误   |
| `401 Unauthorized`         | **未认证**（缺 Token/Token 无效） | 需跳转登录                    |
| `403 Forbidden`            | **已认证但无权限**                | 用户有 Token 但无操作权限     |
| `404 Not Found`            | 资源不存在                        | 请求的 `/users/999` 不存在    |
| `422 Unprocessable Entity` | 语义错误（如唯一约束冲突）        | 用户名已存在                  |

> **黄金法则**：  
> **HTTP 状态码表达“协议层结果”，业务细节由响应体 `code/message` 补充**  
> 例：`400 Bad Request` + `{"code": "INVALID_PHONE", "message": "手机号格式错误"}`

---

#### 实践规范：从理论到落地

##### 统一响应格式（所有接口强制一致）

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { "id": 1, "name": "张三" },
  "timestamp": 1704067200000
}
```

| 字段        | 作用         | 规则                                              |
| ----------- | ------------ | ------------------------------------------------- |
| `code`      | 业务状态码   | `200`=成功，`4001`=参数错误（与 HTTP 状态码互补） |
| `message`   | 人类可读提示 | 直接用于前端提示                                  |
| `data`      | 业务数据     | 无数据时返回 `null` 或空对象                      |
| `timestamp` | 响应时间戳   | 防缓存、校验数据时效性                            |

##### 全局异常处理（保障状态码正确性）

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<?>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity.badRequest() // HTTP 400
                .body(Result.error("PARAM_ERROR", msg));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<?>> handleForbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN) // HTTP 403
                .body(Result.error("NO_PERMISSION", "无操作权限"));
    }
}
```

##### 参数传递规范

| 场景      | 注解                      | 示例                        | 规则                        |
| --------- | ------------------------- | --------------------------- | --------------------------- |
| 资源标识  | `@PathVariable`           | `GET /users/{id}`           | 必填，配合 `@Min(1)` 校验   |
| 过滤/分页 | `@RequestParam`           | `?page=1&size=10`           | `defaultValue` 避免 null    |
| 创建/更新 | `@RequestBody` + `@Valid` | `POST /users` + JSON        | **必须用 DTO**，隔离 Entity |
| 认证信息  | `@RequestHeader`          | `Authorization: Bearer xxx` | 无状态认证核心              |

##### 高质量接口示例

```java
// 分页查询（GET + Query String）
@GetMapping("/users")
public Result<PageResult<UserVO>> list(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer minAge) {
    // URL 示例：GET /api/users?page=2&size=20&keyword=张&minAge=18
    PageResult<UserVO> result = userService.list(page, size, keyword, minAge);
    return Result.success(result);
}

// 查询单个资源（GET + PathVariable）
@GetMapping("/users/{id}")
public Result<UserVO> getById(@PathVariable Long id) {
    return Result.success(userService.getById(id));
}

// 创建资源（POST + RequestBody + 返回 201）
@PostMapping("/users")
@ResponseStatus(HttpStatus.CREATED)
public Result<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
    UserVO user = userService.create(dto);
    return Result.success(user);
}

// 全量更新（PUT + PathVariable + RequestBody）
@PutMapping("/users/{id}")
public Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
    return Result.success(userService.update(id, dto));
}

// 部分更新（PATCH + PathVariable + RequestBody）
@PatchMapping("/users/{id}")
public Result<UserVO> patch(@PathVariable Long id, @Valid @RequestBody UserPatchDTO dto) {
    return Result.success(userService.patch(id, dto));
}

// 删除资源（DELETE + PathVariable）
@DeleteMapping("/users/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable Long id) {
    userService.delete(id);
}
```

---

#### 常见误区澄清

| 误区                                      | 正确认知           | 依据                                                         |
| ----------------------------------------- | ------------------ | ------------------------------------------------------------ |
| “URI 里带版本号（`/v1/users`）不 RESTful" | **推荐做法**       | 满足“分层系统”约束，便于灰度发布                             |
| “所有操作都用 POST"                       | 违反统一接口       | 丢失 HTTP 语义（幂等性、缓存、安全方法）                     |
| “返回 200 + 自定义 code=404 表示失败"     | 混淆协议层与业务层 | 客户端无法用标准 HTTP 库处理（如 axios 拦截器失效）          |
| “RESTful 必须用 JSON"                     | 无关               | 资源表述（Representation）可为 JSON/XML/HTML，由 `Accept` 头协商 |
| “PUT 和 PATCH 可互换"                     | 语义不同           | PUT 全量替换（幂等），PATCH 部分更新（非幂等）               |

---

### 4.3 JWT 认证与权限控制 —— 无状态安全的核心实践

#### 为什么需要认证？先厘清两个概念

| 概念                       | 含义                       | 问题场景                               |
| :------------------------- | :------------------------- | :------------------------------------- |
| **认证（Authentication）** | “你是谁？”（验证身份）     | 用户登录后，后续请求如何证明“我是我”？ |
| **授权（Authorization）**  | “你能做什么？”（验证权限） | 普通用户能否删除他人数据？             |

> **核心痛点**：
>
> HTTP 是无状态协议 → 每次请求服务器“失忆” → **必须有一种机制让服务器“认出你”且“知道你能做什么”**

---

#### Session vs JWT：本质差异与选型依据

##### Session 认证（有状态）

```text
1. 登录 → 服务端创建 Session（存内存/Redis）→ 返回 JSESSIONID（Cookie）
2. 后续请求 → 浏览器自动带 Cookie → 服务端查 Session 验证
```

**适用**：传统单体 Web 应用（服务端渲染）
**痛点**：
-   服务端需存储 Session → 集群需同步（Redis 增加复杂度）
-   跨域/移动端支持弱（Cookie 依赖浏览器）
-   CSRF 攻击风险（需额外防护）

##### JWT 认证（无状态）

```text
1. 登录 → 服务端生成 Token（含用户信息+签名）→ 返回给前端
2. 后续请求 → 前端手动带 Token（Header: Authorization: Bearer xxx）→ 服务端验签解析
```

**本质价值**：
-   **无状态**：Token 自包含用户信息，服务端无需存储 → 天然支持水平扩展
-   **跨平台**：Header 传递，完美适配 Web/APP/小程序
-   **解耦**：认证服务可独立（如 OAuth2 接入）
-   **权限内嵌**：Payload 可携带角色/权限，减少数据库查询

**注意**：
-   Token 一旦签发无法主动失效（需配合短期有效期+刷新机制）
-   Payload **Base64 可解码** → **严禁存放密码等敏感信息**

> **选型指南**：
>
> -   前后端分离项目、微服务架构、移动端 → **首选 JWT**
> -   传统服务端渲染（Thymeleaf/JSP）→ Session 仍适用

---

#### JWT 结构：三段式设计的精妙之处

```text
Header.Payload.Signature （三段用 . 连接，每段 Base64Url 编码）
```

| 部分          | 内容                                             | **作用**                                   | **能否被篡改？**             |
| :------------ | :----------------------------------------------- | :----------------------------------------- | :--------------------------- |
| **Header**    | `{"alg":"HS256","typ":"JWT"}`                    | 告知验证方用什么算法验签                   | 篡改后签名失效               |
| **Payload**   | `{"sub":"1001","role":"admin","exp":1704067200}` | **携带业务数据**（用户ID、角色、过期时间） | 可解码查看！但篡改后签名失效 |
| **Signature** | HMACSHA256(Header+Payload, 密钥)                 | **防篡改核心**：服务端用密钥重新计算比对   | 无法伪造（密钥不泄露前提下） |

##### Payload 关键字段（标准 + 自定义）

```json
{
  "sub": "1001", // 标准：主体（用户ID）
  "username": "zhangsan", // 自定义：用户名（方便后续使用）
  "role": "admin", // 自定义：角色（用于权限控制）
  "permissions": ["user:delete", "order:view"], // 自定义：细粒度权限
  "iat": 1704063600, // 标准：签发时间（Issued At）
  "exp": 1704067200 // 标准：过期时间（Expiration Time）→ **必须设置！**
}
```

> **为什么 Payload 能放权限？**
>
> 服务端验签通过后，直接从 Token 读取角色/权限 → **避免每次请求查数据库**，提升性能

---

#### Spring Boot 中 JWT 实现：关键步骤与原理

##### 核心依赖（Maven）

```xml
<!-- JWT 工具库（推荐 java-jwt） -->
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>4.4.0</version>
</dependency>
```

##### 工具类：生成与验证（核心逻辑）

```java
@Component
public class JwtUtil {
    private static final String SECRET = "your-secure-secret-key"; // 实际用环境变量
    private static final long EXPIRATION = 2 * 60 * 60 * 1000; // 2小时

    // 生成 Token（登录成功时调用）
    public String generateToken(Integer userId, String username, String role) {
        return JWT.create()
                .withSubject(userId.toString()) // sub
                .withClaim("username", username)
                .withClaim("role", role)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION))
                .sign(Algorithm.HMAC256(SECRET));
    }

    // 验证 Token（拦截器中调用）
    public boolean validateToken(String token) {
        try {
            JWT.require(Algorithm.HMAC256(SECRET))
                    .build()
                    .verify(token); // 验签 + 自动校验 exp
            return true;
        } catch (JWTVerificationException e) {
            return false; // 签名无效/过期/被篡改
        }
    }

    // 从 Token 获取用户ID（验签通过后调用）
    public Integer getUserId(String token) {
        DecodedJWT jwt = JWT.decode(token);
        return Integer.parseInt(jwt.getSubject());
    }
}
```

##### 拦截器：统一认证入口

```java
@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 白名单放行（登录、注册等）
        String uri = request.getRequestURI();
        if (uri.contains("/login") || uri.contains("/register")) {
            return true;
        }

        // 2. 获取 Token（规范格式：Authorization: Bearer <token>）
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        String token = authHeader.substring(7); // 去掉 "Bearer "

        // 3. 验证 Token
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 4. 将用户信息存入请求（后续 Controller 可通过 request.getAttribute 获取）
        Integer userId = jwtUtil.getUserId(token);
        request.setAttribute("userId", userId);
        return true;
    }
}
```

##### 注册拦截器（WebMvcConfigurer）

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**") // 拦截所有 API
                .excludePathPatterns("/api/public/**"); // 公开接口放行
    }
}
```

---

#### 安全加固：必须遵守的实践准则

| 风险                 | 正确做法                                         | 为什么                                                       |
| :------------------- | :----------------------------------------------- | :----------------------------------------------------------- |
| **Token 泄露**       | 前端存于内存（非 LocalStorage）；传输全程 HTTPS  | 防 XSS 窃取；防中间人攻击                                    |
| **Payload 敏感信息** | 仅放用户ID、角色、权限；**绝不放密码、手机号**   | Token 可被 Base64 解码                                       |
| **Token 永不过期**   | 设置合理 `exp`（如 2 小时）；配合刷新 Token 机制 | 降低泄露后危害窗口                                           |
| **密钥硬编码**       | 密钥存环境变量/配置中心；定期轮换                | 防代码泄露导致签名体系崩塌                                   |
| **权限校验缺失**     | 拦截器验签后，**关键接口需二次校验权限**         | 防“普通用户篡改 Token 角色字段”（虽签名会失效，但需纵深防御） |

##### 权限校验示例（Controller 层）

```java
@GetMapping("/users/{id}")
public Result<User> getUser(@PathVariable Integer id, HttpServletRequest request) {
    Integer currentUserId = (Integer) request.getAttribute("userId");
    String userRole = jwtUtil.getRole(request.getHeader("Authorization").substring(7));

    // 二次校验：非管理员只能查自己
    if (!"admin".equals(userRole) && !currentUserId.equals(id)) {
        throw new BusinessException("无权访问他人数据");
    }
    return Result.success(userService.getById(id));
}
```

---

#### 常见误区澄清

| 误区                     | 正解                                                         |
| :----------------------- | :----------------------------------------------------------- |
| “JWT 是加密的”           | **仅编码（Base64Url），非加密**！Payload 可被任何人解码，靠签名防篡改 |
| “JWT 比 Session 更安全”  | 安全取决于实现：JWT 需防 XSS，Session 需防 CSRF，无绝对优劣  |
| “Token 放 Cookie 更安全” | 放 Cookie 可防 XSS 窃取，但需加 `HttpOnly` + `SameSite`；放 Header 更灵活（适配 APP） |
| “JWT 能完全替代 Session” | 需要“强制下线”“单点登录”等场景，仍需服务端状态（如 Token 黑名单） |

---

### 4.4 异常处理与数据验证

#### 异常处理层次

```
┌─────────────────────────────────────────────────────┐
│      用户看到的友好提示（"用户名不能为空"）          │ ← 前端展示
├─────────────────────────────────────────────────────┤
│      全局异常处理器（统一转标准JSON+记录日志）       │ ← 统一处理
├─────────────────────────────────────────────────────┤
│      Service层抛出异常（业务规则校验失败）          │ ← 业务异常
├─────────────────────────────────────────────────────┤
│      数据验证失败异常（@Valid触发校验失败）          │ ← 参数异常
├─────────────────────────────────────────────────────┤
│      SQL执行异常（MyBatis抛出）                   │ ← 系统异常
└─────────────────────────────────────────────────────┘
```

> **分层价值**：  
>
> - 用户无感知技术细节  
> - 前端基于HTTP状态码精准处理  
> - 运维通过日志快速定位问题层级  

---

#### 数据验证：注解体系与使用全流程

##### 核心依赖（必须显式引入）

```xml
<!-- 提供@Valid、@NotBlank等注解能力 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

> **作用**：集成 Hibernate Validator（Bean Validation 2.0 参考实现）  
> **无此依赖** → `@Valid` 失效，校验注解被忽略，启动可能报错  

---

##### DTO校验注解示例（专用于接口层，非Entity！）

```java
@Data  // @Data：Lombok 注解，编译时自动生成 getter、setter、toString、equals 和 hashCode 方法
public class UserRegisterDTO {
    
    @NotBlank(message = "用户名不能为空") 
    @Size(min = 3, max = 20, message = "用户名需3-20位")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "仅支持字母、数字、下划线")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", 
        message = "密码需8位以上，含字母和数字"
    )
    private String password;
    
    @Email(message = "邮箱格式不正确")
    @NotBlank
    private String email;
    
    @Min(value = 18, message = "年龄需满18岁")
    @Max(value = 100, message = "年龄不能超过100岁")
    private Integer age;
    
    // 分组验证示例（更新时可选）
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误", groups = UpdateGroup.class)
    private String phone;
}
```

> **Lombok `@Data` 继承注意事项**：  
> 当子类使用 `@Data` 且继承了父类时，Lombok 生成的 `equals()` / `hashCode()` **默认不包含父类字段**。  
> 这会导致两个子类对象仅凭自身字段判断相等，忽略父类字段，在 `HashSet` / `HashMap` 等判重场景下可能引发逻辑错误。  
> **解决方案**：对子类显式添加 `@EqualsAndHashCode(callSuper = true)` 以纳入父类字段。

##### 常用校验注解速查表

| 注解              | 作用                      | 适用类型                    | 关键特性                |
| ----------------- | ------------------------- | --------------------------- | ----------------------- |
| `@NotBlank`       | 非null + 非空（trim后>0） | `String`                    | 比`@NotNull`更严格      |
| `@NotEmpty`       | 非null + size>0           | `Collection`/`Map`/`String` | 适用于列表校验          |
| `@NotNull`        | 不能为null                | 任意对象                    | 不校验空字符串          |
| `@Size`           | 长度/元素数量范围         | `String`/集合               | `min=3, max=20`         |
| `@Min`/`@Max`     | 数值范围                  | `Number`                    | 支持`long`/`BigDecimal` |
| `@Email`          | 邮箱格式                  | `String`                    | 内置正则验证            |
| `@Pattern`        | 自定义正则                | `String`                    | 复杂规则（如密码强度）  |
| `@Future`/`@Past` | 时间校验                  | `Date`/`LocalDateTime`      | 预约场景必备            |

---

##### Controller中触发校验（关键三要素）

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // 完整校验链：@RequestBody + @Valid + DTO
    @PostMapping("/register")
    public Result<?> register(
        @Valid          // 1. 触发校验（核心！无此注解校验失效）
        @RequestBody    // 2. 将JSON Body反序列化为Java对象（必须！）
        UserRegisterDTO dto  // 3. DTO含校验注解
    ) {
        // 校验通过 → 执行业务逻辑
        // 校验失败 → **直接抛出MethodArgumentNotValidException，不进入方法体！**
        userService.register(dto);
        return Result.success();
    }
}
```

##### 三要素作用深度解析

| 组件               | 作用                                | 缺失后果                               | 验证方式                                 |
| ------------------ | ----------------------------------- | -------------------------------------- | ---------------------------------------- |
| **`@RequestBody`** | 将HTTP Body的JSON转为Java对象       | Spring尝试从URL参数绑定 → 字段全为null | Postman发JSON，不加此注解必失败          |
| **`@Valid`**       | **触发**Hibernate Validator校验流程 | 校验注解被忽略 → 非法数据流入Service层 | 故意传空username，无@Valid则方法体被执行 |
| **DTO校验注解**    | 定义字段规则（如`@NotBlank`）       | 无规则可校验 → 校验逻辑失效            | 注释掉@NotBlank，空username可通过        |

> **工作流闭环**：  
> 客户端发JSON → `@RequestBody`转对象 → `@Valid`触发校验 →  
> 通过 → 执行方法体  
> 失败 → 抛`MethodArgumentNotValidException` → **由全局处理器捕获并返回友好提示**

---

##### 分组验证（高级用法）

```java
// 定义分组接口
public interface RegisterGroup {}
public interface UpdateGroup {}

// DTO中指定分组
@NotBlank(groups = RegisterGroup.class, message = "注册时手机号必填")
@Pattern(regexp = "^1[3-9]\\d{9}$", groups = {RegisterGroup.class, UpdateGroup.class})
private String phone;

// Controller中指定生效分组
@PostMapping("/register")
public Result<?> register(
    @Validated(RegisterGroup.class)  // 仅校验RegisterGroup标记的字段
    @RequestBody UserRegisterDTO dto
) { ... }
```

> **价值**：同一DTO支持多场景（注册需手机号，更新可选）  
> **关键**：`@Validated` 支持分组，`@Valid` 不支持（需配合groups使用）

---

##### 关键注意事项

| 问题                 | 正确实践                                            | 为什么                                           |
| -------------------- | --------------------------------------------------- | ------------------------------------------------ |
| **校验注放置Entity** | 放专用DTO（`dto/request`包）                        | 避免数据库字段与接口耦合，防止密码等敏感字段泄露 |
| **忽略`@Valid`**     | Controller参数必须加`@Valid`                        | 无`@Valid` → 校验注解形同虚设                    |
| **不处理校验异常**   | 全局异常处理器捕获`MethodArgumentNotValidException` | 否则返回500错误，用户体验差                      |
| **复杂规则硬编码**   | 自定义校验注解（如`@SafeUsername`）                 | 保持DTO简洁，复用校验逻辑                        |
| **嵌套对象未校验**   | 嵌套字段加`@Valid`（如`@Valid Address address`）    | 否则子对象字段校验失效                           |

---

#### 全局异常处理器

```java
@Slf4j
@RestControllerAdvice  // 全局异常处理（作用于所有@RestController）
public class GlobalExceptionHandler {
    
    // 处理触发的参数验证异常（@Valid校验失败时抛出）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<String>> handleValidationException(MethodArgumentNotValidException e) {
        // 提取首个错误信息（或拼接所有错误）
        String errorMsg = e.getBindingResult().getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)  // 获取@NotBlank等注解的message
            .findFirst()
            .orElse("参数校验失败");
        
        log.warn("参数校验失败: {}", errorMsg); // 记录关键错误
        return ResponseEntity.badRequest().body(Result.error(400, errorMsg));
    }
    
    // 处理业务异常（Service层抛出）
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<String>> handleBusinessException(BusinessException e) {
        log.warn("业务异常[code={}]: {}", e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus())
                .body(Result.error(e.getCode(), e.getMessage()));
    }
    
    // 处理资源不存在异常（语义化404）
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Result<String>> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(404, "资源不存在"));
    }
    
    // 处理所有未捕获的系统异常（兜底）
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleException(Exception e) {
        // 记录完整堆栈 + 请求上下文（traceId便于链路追踪）
        String traceId = MDC.get("traceId");
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        log.error("系统异常[traceId={}][url={}]: {}", 
                  traceId, request.getRequestURI(), e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "系统繁忙，请稍后重试"));
    }
}
```

> **闭环验证**：  
>
> 1. 前端发送 `{"username": "", "email": "invalid"}`  
> 2. Controller中 `@Valid @RequestBody` 触发校验  
> 3. 抛出 `MethodArgumentNotValidException`  
> 4. 本处理器捕获 → 返回 `{"code":400, "message":"用户名不能为空"}`  
> 5. 前端精准提示用户 → **用户体验闭环完成**  

---

#### 自定义异常类（业务语义化）

```java
// 业务异常基类（携带业务码 + HTTP状态码）
public class BusinessException extends RuntimeException {
    private final int businessCode;
    private final int httpStatus; // 400/403/404等
    
    public BusinessException(int businessCode, String message, int httpStatus) {
        super(message);
        this.businessCode = businessCode;
        this.httpStatus = httpStatus;
    }
    
    // 快捷构造：默认400
    public BusinessException(String message) {
        this(1000, message, HttpStatus.BAD_REQUEST.value());
    }
    
    public int getCode() { return businessCode; }
    public int getHttpStatus() { return httpStatus; }
}

// 具体业务异常（语义清晰）
public class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException(BigDecimal need, BigDecimal current) {
        super(1001, 
              String.format("余额不足！需%.2f元，当前%.2f元", need, current), 
              HttpStatus.BAD_REQUEST.value());
    }
}

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super(1002, "用户不存在", HttpStatus.NOT_FOUND.value());
    }
}

// Service中使用示例
@Service
public class UserService {
    public User getUser(Integer id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new UserNotFoundException(); // 抛出语义化异常
        }
        return user;
    }
    
    public void deductBalance(Long userId, BigDecimal amount) {
        User user = getUser(userId);
        if (user.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, user.getBalance());
        }
        // 扣款逻辑...
    }
}
```

> **与全局异常处理器联动**：  
>
> - `UserNotFoundException` → 被`@ExceptionHandler(BusinessException.class)`捕获 → 返回404  
> - 前端收到`{"code":1002, "message":"用户不存在"}` → 精准提示"用户不存在，请检查ID"  
> - **业务语义直达前端，无需猜测错误原因**  

**完整闭环验证**：  
**DTO定义规则 → Controller用`@Valid @RequestBody`触发 → 校验失败由全局处理器返回友好提示 → 前端精准展示错误**  
**Service抛业务异常 → 全局处理器映射HTTP状态码 → 前端基于状态码差异化处理**

------

### 4.5 SaToken - 轻量级权限认证框架

#### 什么是 SaToken？为什么需要它？

**精准定义**：  
SaToken 是一个轻量级 Java 权限认证框架，主要解决：**登录认证、权限认证、Session 会话、单点登录、OAuth2 集成**等问题。

**核心价值**：

- **简单易用**：API 设计直观，几行代码即可完成登录认证
- **功能全面**：提供登录、权限、角色、踢人、会话管理、记住我等一站式方案
- **高扩展性**：支持 Redis 存储 Token，天然支持分布式
- **零耦合**：不依赖 Spring Security 或 Shiro，可独立使用
- **前后端分离友好**：默认支持 Token 风格的认证（类似 JWT，但可主动失效）

> **选型对比**：
>
> | 框架            | 特点                                     | 适用场景                         |
> | --------------- | ---------------------------------------- | -------------------------------- |
> | Spring Security | 功能强大，配置复杂，安全规范严格         | 大型企业级应用，需标准化安全方案 |
> | Apache Shiro    | 轻量，配置简单，但社区活跃度下降         | 传统单体项目                     |
> | **SaToken**     | 极简 API，功能丰富，支持分布式，国产开源 | 中小型项目、快速开发、前后端分离 |

---

#### 快速集成：在 Spring Boot 中使用

##### 依赖引入

```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot-starter</artifactId>
    <version>1.37.0</version>
</dependency>
<!-- 可选：SaToken 整合 Redis（分布式必加） -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-redis-jackson</artifactId>
    <version>1.37.0</version>
</dependency>
```

##### 配置文件（`application.yml`）

```yaml
sa-token:
  token-name: satoken           # Token 名称（请求头或参数）
  timeout: 7200                 # Token 有效期（秒），默认 30 天
  activity-timeout: 1800        # Token 临时有效期（指定时间内无操作则过期）
  is-concurrent: true           # 是否允许同一账号多处登录
  is-share: true                # 多端登录时是否共享 Token
  token-style: uuid             # Token 风格：uuid / simple-uuid / random-32 / random-128
  is-log: true                  # 是否输出操作日志
  # Redis 配置（使用 Redis 时自动存储 Token）
  alone-redis:
    host: localhost
    port: 6379
    password: 
    database: 0
```

##### 启动类配置（无需额外注解）

SaToken 通过 Starter 自动装配，直接使用 `StpUtil` 静态工具类即可。

```java
@SpringBootApplication
public class ImsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImsApplication.class, args);
    }
}
```

---

#### 核心 API 速查

| 方法                                   | 作用                                                         | 返回    |
| -------------------------------------- | ------------------------------------------------------------ | ------- |
| `StpUtil.login(Object loginId)`        | 登录（指定用户ID）                                           | void    |
| `StpUtil.logout()`                     | 当前账号退出登录                                             | void    |
| `StpUtil.logout(Object loginId)`       | 强制指定账号退出                                             | void    |
| `StpUtil.isLogin()`                    | 判断当前是否登录                                             | boolean |
| `StpUtil.getLoginId()`                 | 获取当前登录用户ID，可根据需要转为指定类型（如 `getLoginIdAsLong()`、`getLoginIdAsString()`） | Object  |
| `StpUtil.getTokenValue()`              | 获取当前 Token 值                                            | String  |
| `StpUtil.hasRole(String role)`         | 判断是否有指定角色                                           | boolean |
| `StpUtil.hasPermission(String perm)`   | 判断是否有指定权限                                           | boolean |
| `StpUtil.checkRole(String role)`       | 检查角色，无则抛异常                                         | void    |
| `StpUtil.checkPermission(String perm)` | 检查权限，无则抛异常                                         | void    |
| `StpUtil.kickout(Object loginId)`      | 踢人下线                                                     | void    |
| `StpUtil.replaced()`                   | 是否被顶下线（多端互斥）                                     | boolean |

---

#### 实战示例：登录认证与接口拦截

##### 1. 登录接口（生成 Token）

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO dto) {
        // 1. 校验用户名密码（自定义逻辑）
        User user = userService.authenticate(dto.getUsername(), dto.getPassword());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 2. SaToken 登录：传入用户ID
        // 底层机制：生成随机 Token（默认 UUID），建立 Token ↔ userId 映射关系并存入 Session 存储（内存或 Redis）
        StpUtil.login(user.getId());
        
        // 3. 可选：将用户信息存入 Session（后续请求可通过 Token 获取）
        StpUtil.getSession().set("user", user);
        
        // 4. 返回 Token（前端后续请求需在 Header 中携带此 Token）
        return Result.success(StpUtil.getTokenValue());
    }
    
    @PostMapping("/logout")
    public Result<Void> logout() {
        // 注销：删除 Token ↔ userId 映射关系，使 Token 失效
        StpUtil.logout();
        return Result.success();
    }
    
    @GetMapping("/info")
    public Result<User> getLoginUser() {
        // 从当前登录会话中获取用户 ID（SaInterceptor 已根据 Token 还原出 userId）
        Integer userId = (Integer) StpUtil.getLoginId();
        User user = userService.getById(userId);
        return Result.success(user);
    }
}
```

##### 2. 密码加密与校验（BCrypt）

SaToken 内置了 `BCrypt` 工具类（来自 jBCrypt 库），提供 BCrypt 密码哈希，无需额外依赖。

```java
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;  // 继承 MyBatis-Plus 的 BaseMapper
    
    // 注册时：加密密码存入数据库
    public void register(UserRegisterDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        // 使用 BCrypt.hashpw() 加密（SaToken 内置）
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        userMapper.insert(user);
    }
    
    // 登录时：校验密码（authenticate 方法内部）
    public User authenticate(String username, String rawPassword) {
        // 使用 LambdaQueryWrapper 按用户名查询（也可自定义 selectByUsername 方法）
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null) {
            return null;
        }
        // 使用 BCrypt.checkpw() 比对
        boolean matched = BCrypt.checkpw(rawPassword, user.getPassword());
        return matched ? user : null;
    }
}
```

> **注意**：`BCrypt.hashpw()` 每次生成不同的哈希值（自动加盐），`BCrypt.checkpw()` 用于比对原始密码与哈希。需要导入 `import cn.dev33.satoken.secure.BCrypt;`。

##### 3. 全局拦截器配置（替代 Security 过滤器链）

注册 Sa-Token 拦截器，拦截所有 API 请求，从请求头中解析 Token 并获取当前登录用户 ID，为后续的注解鉴权提供基础。若未配置拦截器，`@SaCheckXxx` 注解将不生效。

```java
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    
    // 注册 Sa-Token 拦截器，实现注解鉴权
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/api/**")           // 拦截所有 API
                .excludePathPatterns("/api/auth/login", "/api/auth/register"); // 放行
    }
}
```

##### 4. 使用注解进行权限控制（方法级）

在 Controller 方法上通过注解声明访问要求（如必须登录、拥有某角色或权限），代码简洁且可读。Sa-Token 拦截器会根据当前用户的实际权限与注解要求进行比对，决定是否放行。

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // 必须登录才能访问
    @SaCheckLogin
    @GetMapping("/profile")
    public Result<User> profile() {
        return Result.success(userService.getById((Integer) StpUtil.getLoginId()));
    }
    
    // 需要 admin 角色（角色名需与 StpInterface 返回的字符串完全一致，区分大小写）
    @SaCheckRole("admin")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        userService.deleteById(id);
        return Result.success();
    }
    
    // 需要 user:add 权限（权限名需与 StpInterface 返回的字符串完全一致，区分大小写）
    @SaCheckPermission("user:add")
    @PostMapping
    public Result<Void> add(@RequestBody User user) {
        userService.save(user);
        return Result.success();
    }
    
    // 组合条件：登录且具有 admin 或 user:view 权限
    @SaCheckLogin
    @SaCheckPermission(value = {"admin", "user:view"}, mode = SaMode.OR)
    @GetMapping("/list")
    public Result<List<User>> list() {
        return Result.success(userService.list());
    }
}
```

> **注解说明**：
>
> - `@SaCheckLogin`：校验是否登录
> - `@SaCheckRole("xxx")`：校验角色（支持多角色，默认 AND 模式）
> - `@SaCheckPermission("xxx")`：校验权限
> - `@SaCheckSafe`：校验二级认证（如支付前二次验证）

---

#### 权限与角色数据源集成（结合数据库）

实现 `StpInterface` 接口，告诉 Sa-Token 如何根据用户 ID 获取其拥有的角色和权限列表。这些数据将用于 `@SaCheckRole` 和 `@SaCheckPermission` 注解的比对。若未实现该接口，注解鉴权将无法工作。

```java
@Component
public class StpInterfaceImpl implements StpInterface {
    
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    
    // 返回指定 loginId 拥有的角色集合
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Integer userId = (Integer) loginId;
        // 自定义 Mapper 方法：根据用户 ID 查询角色编码列表
        List<String> roles = roleMapper.selectRolesByUserId(userId);
        return roles;  // 如 ["admin", "user"]
    }
    
    // 返回指定 loginId 拥有的权限集合
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Integer userId = (Integer) loginId;
        // 自定义 Mapper 方法：根据用户 ID 查询权限编码列表
        List<String> permissions = userMapper.selectPermissionsByUserId(userId);
        return permissions;  // 如 ["user:add", "user:delete", "order:view"]
    }
}
```

> **工作原理**：  
> SaToken 在调用 `StpUtil.hasPermission()` 或 `@SaCheckPermission` 时，自动调用 `StpInterface.getPermissionList()` 获取当前用户的权限列表进行比对。开发者只需负责从数据库查询即可。

---

#### 前后端分离 Token 传递规范

| 场景              | 默认配置                                       | 推荐实践                                       |
| ----------------- | ---------------------------------------------- | ---------------------------------------------- |
| **Token 传递**    | 前端放在请求头 `satoken: xxxxx`                | 遵循规范，前端从响应头 `satoken` 取 Token      |
| **自定义 Header** | 修改配置 `sa-token.token-name: Authorization`  | 可改为 `Authorization: Bearer ` 风格，需自适配 |
| **跨域支持**      | 需配置 CORS 或关闭 CSRF（SaToken 不涉及 CSRF） | 同 Spring Security CORS 配置                   |

**前端适配示例（axios）**：

```javascript
// 登录后，服务端返回 Token 在响应头 satoken 中
axios.post('/api/auth/login', data).then(res => {
    const token = res.headers['satoken'];
    localStorage.setItem('token', token);
});

// 后续请求自动携带
axios.defaults.headers.common['satoken'] = localStorage.getItem('token');
```

---

#### 常见场景解决方案

##### 场景 1：踢人下线（强制指定用户退出）

```java
// 管理员强制踢出某个用户
@SaCheckRole("admin")
@PostMapping("/kick/{userId}")
public Result<Void> kickUser(@PathVariable Integer userId) {
    StpUtil.kickout(userId);
    return Result.success();
}
```

##### 场景 2：顶下线（同一账号只能一处登录）

配置 `sa-token.is-concurrent: false` 即可。当新用户登录时，旧 Token 自动失效。

##### 场景 3：二级认证（敏感操作前验证密码）

```java
// 敏感操作前开启二级认证（有效期 60 秒）
@PostMapping("/pay")
public Result<Void> pay() {
    // 检查是否已认证
    if (!StpUtil.isSafe()) {
        return Result.error("请进行二次验证");
    }
    // 执行支付...
    return Result.success();
}

// 单独接口进行二次验证
@PostMapping("/verify-password")
public Result<Void> verifyPassword(@RequestParam String password) {
    // 校验密码（自定义）
    if (userService.checkPassword(StpUtil.getLoginId(), password)) {
        StpUtil.openSafe(60);  // 开启二级认证，有效期 60 秒
        return Result.success();
    }
    return Result.error("密码错误");
}
```

##### 场景 4：分布式 Session（共享用户会话）

```java
// 存入 Session 的数据会自动同步到 Redis
StpUtil.getSession().set("user", user);
User cached = (User) StpUtil.getSession().get("user");
```

> 前提：已配置 `sa-token.alone-redis` 或项目已使用 Redis，SaToken 会自动将 Session 和 Token 存入 Redis，实现多实例共享。

---

#### 与 Spring Security / JWT 的对比

| 维度               | Spring Security + JWT                                  | SaToken                                        |
| ------------------ | ------------------------------------------------------ | ---------------------------------------------- |
| **配置复杂度**     | 高（需要配置 SecurityFilterChain、JWT 工具类、拦截器） | 极低（几行 yaml 配置即可）                     |
| **Token 主动失效** | 不支持（JWT 无状态，需额外黑名单）                     | 支持（踢人、顶下线、强制注销）                 |
| **Session 管理**   | 需手动实现 Redis 共享                                  | 内置 Redis 集成，自动同步                      |
| **权限注解**       | `@PreAuthorize` 需额外启用                             | `@SaCheckRole` / `@SaCheckPermission` 开箱即用 |
| **学习曲线**       | 陡峭                                                   | 平缓                                           |
| **功能丰富度**     | 认证授权 + OAuth2 强大但复杂                           | 认证授权 + 踢人 + 二级认证 + 同端互斥等        |
| **适合场景**       | 大型企业、需严格安全规范                               | 中小型项目、快速迭代、前后端分离               |

> **选型建议**：  
> - 团队已熟悉 Spring Security 且有安全合规要求 → 继续用 Spring Security  
> - 追求开发效率、功能灵活、轻量集成 → **SaToken 更合适**  
> - SaToken 可与 JWT 共存（SaToken 默认使用 UUID 风格 Token，但支持自定义 JWT 风格）

---

#### 高频避坑指南

| 问题现象                          | 根本原因                           | 解决方案                                    |
| --------------------------------- | ---------------------------------- | ------------------------------------------- |
| **`StpUtil.getLoginId()` 抛异常** | 未登录或 Token 已过期              | 先调用 `StpUtil.isLogin()` 判断或捕获异常   |
| **注解鉴权不生效**                | 未注册 SaInterceptor               | 在 WebMvcConfigurer 中添加拦截器            |
| **分布式下 Token 不同步**         | 未配置 Redis                       | 添加 `sa-token-redis-jackson` 依赖并配置    |
| **登录后 Token 不变**             | 默认同一账号多次登录返回相同 Token | 设置 `sa-token.is-share: false` 每次新建    |
| **权限数据每次都查数据库**        | 默认无缓存                         | 自行在 `StpInterface` 实现中添加 Redis 缓存 |
| **跨域请求 Token 丢失**           | 前端未正确携带 `satoken` 请求头    | 检查 axios 拦截器，或改用 Cookie 模式       |

---

#### 最佳实践清单

| 项目             | 推荐做法                                                     | 避坑指南                     |
| ---------------- | ------------------------------------------------------------ | ---------------------------- |
| **Token 存储**   | 生产环境必须用 Redis（`sa-token-alone-redis`）               | 单机内存模式不支持集群       |
| **登录信息**     | 登录后存入 `StpUtil.getSession()`，避免重复查库              | Session 数据自动同步 Redis   |
| **权限加载**     | 实现 `StpInterface`，并增加缓存（Caffeine/Redis）            | 避免每次鉴权都查询数据库     |
| **注解鉴权**     | 统一使用 `@SaCheckLogin` + `@SaCheckRole`                    | 不要混用原生 `@PreAuthorize` |
| **踢人功能**     | 管理员操作需加角色校验                                       | 防止普通用户滥用             |
| **日志记录**     | 开发环境开启 `sa-token.is-log: true`，生产关闭               | 避免 Token 泄露到日志        |
| **Token 有效期** | 根据业务设置 `timeout`（如 2 小时），配合 `activity-timeout` 实现自动续期 | 避免永不过期                 |

> **总结**：  
> SaToken 是一个“傻瓜式”但功能强大的权限认证框架，适合追求开发效率和快速上手的团队。与 Spring Security 相比，SaToken 的学习成本更低，但功能覆盖度足以满足绝大多数业务场景。在前后端分离、分布式部署的项目中，SaToken + Redis 是一套非常成熟稳定的组合。

------

## 模块五：项目部署

### 5.1 部署方式与配置

#### 前后端协作部署概述

在工程实践中，前后端项目的部署方式取决于项目规模、团队协作方式以及运行环境。以下先梳理开发阶段的联调方式，再介绍三种主流的生产部署方案。

##### 开发阶段（联调）

**同源策略**：浏览器要求页面只能请求“协议 + 域名 + 端口”完全相同的地址。前后端端口不同时（如前端 `5173`、后端 `8080`），浏览器会正常发出请求，后端也能收到并处理，但浏览器会拦截响应数据，不交给前端 JavaScript，导致前端无法获取返回结果，这就是跨域问题。

前后端分别在自己的环境中运行：
- 后端：Spring Boot 运行在本地或开发服务器，例如 `http://localhost:8080`
- 前端：Vue 开发服务器运行在 `http://localhost:5173`（Vite）

解决跨域有两种方式：
- **后端配置 CORS**：后端在响应头中设置 `Access-Control-Allow-Origin: http://localhost:5173`，明确告知浏览器：该响应的数据允许被该源的前端代码读取。浏览器收到响应后会检查该头，通过则把数据交给前端，否则拦截。
- **前端配置代理**（Vite 或 Webpack Dev Server 自带）：浏览器请求与页面同源的前端开发服务器（如 `http://localhost:5173`），不涉及跨域；开发服务器再将请求转发给真正的后端（如 `http://localhost:8080`）。后端处理完成后将响应返回给开发服务器，由开发服务器再转发给浏览器。由于服务器之间没有跨域限制，整个过程对浏览器而言始终是同源请求，从而避免了同源策略拦截。

如果后端暂时无法本地运行，也可以提前部署到云端（公网服务器或内网穿透工具如 ngrok），前端直接调用公网 API 地址进行联调。

**多环境配置**：

- 前端使用 `.env.development` 和 `.env.production` 切换 API 基地址
- 后端使用 `application-dev.yml` 和 `application-prod.yml` 切换数据库、端口等配置

**请求路径规则（指导 API 地址配置）**：

浏览器根据请求路径的写法决定拼接方式，这直接影响 `.env` 中 `VITE_API_BASE_URL` 的配置和代码中的请求路径。

| 写法               | 类型     | 浏览器处理           | 示例（页面在 `a.com/instances`） | 适用场景               |
| ------------------ | -------- | -------------------- | -------------------------------- | ---------------------- |
| `/api/user`        | 绝对路径 | 拼接到 origin 后     | `a.com/api/user`                 | **推荐**，配合代理     |
| `api/user`         | 相对路径 | 拼接到当前页面路径后 | `a.com/instances/api/user`       | 容易错乱，不推荐       |
| `http://b.com/api` | 完整 URL | 直接使用             | `b.com/api`                      | 用于直连后端（会跨域） |

> 因此 `.env` 中通常将 `VITE_API_BASE_URL` 配置为 `/api`（绝对路径），再通过 Vite 代理将 `/api` 转发到真实后端地址。这样无论页面路径多深，请求地址始终正确。

##### 生产部署的三种方式

| 方式                         | 适用场景                            | 优点                                  | 缺点                       |
| ---------------------------- | ----------------------------------- | ------------------------------------- | -------------------------- |
| **合并部署**（前后端同 JAR） | 中小型个人项目、快速演示            | 一个 JAR 包含全部，部署简单，无需跨域 | 前后端耦合，更新需重新打包 |
| **Nginx 分离部署**           | 需要独立更新前后端、HTTPS、负载均衡 | 前后端独立部署，Nginx 承担路由和代理  | 需要额外配置 Nginx         |
| **容器化部署**（Docker）     | 团队协作、云原生环境                | 环境一致，易于扩缩容                  | 学习成本较高，小项目不必须 |

> **关于同域**：合并部署和 Nginx 分离部署下，前后端访问同一域名/端口，不存在跨域问题。容器化部署（Docker）是否同域取决于路由配置：通过 Nginx 代理则同域，直接暴露不同端口则跨域。生产环境通常建议同域，以避免 CORS 配置和额外请求开销。

**合并部署操作步骤**：
1. 前端执行 `npm run build`，生成 `dist` 目录（包含 `index.html`、JS、CSS 等静态资源）
2. 将 `dist` 目录的内容复制到后端项目的 `src/main/resources/static` 目录
3. 后端 Spring Boot 打包为 JAR 文件（`mvn clean package`）
4. 将 JAR 文件上传到服务器，执行 `java -jar backend.jar`（需安装 Java 17 或更高版本，以及 MySQL）
5. 访问 `http://服务器IP:端口`，前后端自动同源，无需跨域配置

> 若前端使用 Vue Router 的 `history` 模式，需要后端添加 fallback 规则：将所有非 API、非静态资源的请求转发到 `index.html`。Spring Boot 中可通过 `WebMvcConfigurer.addViewControllers` 实现，匹配所有不含文件后缀的路径。

**Nginx 分离部署要点**：
- 前端 `dist` 目录上传到服务器的指定目录（如 `/var/www/frontend`）
- 后端 JAR 包单独运行（或使用 `systemd` 管理），监听某个端口（例如 8080）
- Nginx 配置虚拟主机：访问 `/` → 返回前端 `index.html`；访问 `/api/**` → 代理转发到后端 `http://localhost:8080/api/**`
- 配置 `try_files $uri $uri/ /index.html;` 解决前端路由刷新 404 问题

**容器化部署（Docker）**：
- 前端、后端分别编写 `Dockerfile`，打包为镜像
- 使用 `docker-compose` 编排前端、后端、MySQL 等服务
- 部署到云服务器或容器平台（如阿里云容器服务、Railway 等）

> 中小型项目优先选择合并部署或 Nginx 分离部署；容器化部署适合微服务架构和团队协作，当前阶段不必强求。

---

#### 打包方式：为什么 Fat JAR 是现代 Spring Boot 的唯一选择？

| 方式                            | 适用场景                                 | 优势                                                         | 劣势                                                         | 选择建议                       |
| ------------------------------- | ---------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------ |
| **Fat JAR（Spring Boot 推荐）** | 所有 Spring Boot 项目                    | 1. 单文件部署，无需额外应用服务器<br>2. 自包含所有依赖<br>3. 启动命令简单（`java -jar app.jar`） | 1. 文件较大（50-100MB）<br>2. 不能热更新单个类               | **99% 场景首选**               |
| **WAR 包（传统方式）**          | 仅适用于需要集成到传统 Tomcat 的遗留系统 | 1. 支持热更新<br>2. 适合多应用共享 Tomcat                    | 1. 依赖外部 Tomcat<br>2. 部署流程复杂<br>3. 与 Spring Boot 设计理念冲突 | **除非有特殊需求，否则不推荐** |

> **为什么 Fat JAR 是标准？**  
> Spring Boot 的设计哲学是“开箱即用”。Fat JAR 完美实现：
> - 开发者专注业务，无需配置 Tomcat
> - 运维只需一个 JAR 文件，部署即服务
> - 容器化（Docker）天然友好（单文件镜像更小）

---

#### 生产环境配置：安全、性能、可维护性三重保障

**核心原则**：敏感信息不写死，配置可动态调整。

```yaml
# application-prod.yml（生产环境配置）
server:
  port: 8080
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/plain
  error:
    whitelabel:
      enabled: false   # 关闭默认错误页，避免泄露堆栈

spring:
  datasource:
    # 生产环境 useSSL=true；开发环境可使用 useSSL=false&allowPublicKeyRetrieval=true
    url: ${DB_URL:jdbc:mysql://prod-db:3306/ims?useSSL=true}
    username: ${DB_USERNAME:ims_user}
    password: ${DB_PASSWORD:}   # 必须从环境变量获取
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:20}
      minimum-idle: ${DB_MIN_IDLE:5}
      connection-timeout: ${DB_TIMEOUT:30000}
      leak-detection-threshold: 60000   # 检测连接泄漏，生产必备

# 日志配置（生产环境关键）
logging:
  level:
    org.springframework: WARN   # 降低框架日志级别
    com.ims: INFO               # 业务日志级别
  file:
    name: /var/log/ims/ims.log
  logback:
    rollingpolicy:
      max-file-size: 100MB      # 单个日志文件 100MB，避免过大
      max-history: 30           # 保留 30 天日志
      total-size-cap: 3GB       # 总日志大小上限

# Spring Boot Actuator（监控必备）
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics   # 仅暴露必要端点，不暴露 env
  endpoint:
    health:
      show-details: always     # 显示详细健康状态
      probes:
        enabled: true          # 启用 K8s 探针
    metrics:
      enabled: true
```

**关键安全配置解读**：

| 配置项                        | 作用              | 为什么重要                                   |
| ----------------------------- | ----------------- | -------------------------------------------- |
| `password: ${DB_PASSWORD:}`   | 从环境变量读取    | 避免密码硬编码在 Git 仓库                    |
| `useSSL=true`                 | 启用 SSL 加密连接 | 防止数据在网络传输中被窃听，生产环境必须开启 |
| `show_sql: false`             | 关闭 SQL 日志     | 防止敏感 SQL 泄露到日志                      |
| `health.show-details: always` | 显示详细健康状态  | 便于运维快速定位问题                         |
| `max-file-size: 100MB`        | 限制单日志大小    | 避免磁盘被日志占满                           |

> **环境变量配置示例（Linux）**：
> ```bash
> export DB_PASSWORD="StrongP@ss123"
> export DB_URL="jdbc:mysql://prod-db:3306/ims?useSSL=true"
> ```

