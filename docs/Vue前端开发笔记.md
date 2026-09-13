## 模块一：前端基础（三件套）

> **前置说明**：本文档仅列出 Vue 开发中最常用的 HTML/CSS/JS 语法，每条语法都标注“与 Vue 的关联”，以此实现从原生 DOM 操作平滑过渡到 Vue 模板。

---

### 1.1 HTML 高频标签

HTML 负责页面结构。在 Vue 中，99% 的 HTML 标签用法与原生一致，只是数据绑定方式不同。

#### 核心标签速查表

| 标签                     | 用途                        | Vue 中的关联                            | 原生示例                                   | Vue 示例                                     |
| ------------------------ | --------------------------- | --------------------------------------- | ------------------------------------------ | -------------------------------------------- |
| `<div>`                  | 块级容器（最常用布局元素）  | 作为组件根元素、布局容器                | `<div class="box">内容</div>`              | 相同                                         |
| `<span>`                 | 行内容器（包裹文本/小图标） | 用于内联文本或图标                      | `<span class="red">*</span>`               | 相同                                         |
| `<input>`                | 输入框                      | 绑定 `v-model` 实现双向数据绑定         | `<input type="text" id="name">`            | `<input v-model="name">`                     |
| `<button>`               | 按钮                        | 绑定 `@click` 事件                      | `<button onclick="submit()">登录</button>` | `<button @click="submit">登录</button>`      |
| `<form>`                 | 表单容器                    | 配合 `@submit.prevent` 阻止默认提交     | `<form action="/login" method="post">`     | `<form @submit.prevent="handleLogin">`       |
| `<label>`                | 表单标签（点击聚焦输入框）  | 配合 `for` 属性，用法不变               | `<label for="username">用户名</label>`     | 相同                                         |
| `<ul>` / `<li>`          | 无序列表                    | 配合 `v-for` 循环渲染                   | `<ul><li>苹果</li><li>香蕉</li></ul>`      | `<li v-for="item in list">{{ item }}</li>`   |
| `<td>` / `<tr>` / `<tr>` | 表格                        | 数据量大时改用 Element UI 的 `el-table` | 原生写法繁琐                               | 推荐 `el-table`                              |
| `<img>`                  | 图片                        | 动态绑定 `:src`                         | `<img src="logo.png">`                     | `<img :src="imageUrl">`                      |
| `<a>`                    | 链接                        | 使用 `<router-link>` 替代（Vue Router） | `<a href="/home">首页</a>`                 | `<router-link to="/home">首页</router-link>` |

#### 两个重要属性

- **`class`**：绑定 CSS 类名（样式控制）
- **`style`**：内联样式（动态样式常用）

```html
<!-- 原生写法 -->
<div class="container" style="color: red;">文字</div>

<!-- Vue 动态绑定（推荐） -->
<div :class="{ active: isActive }" :style="{ color: textColor }">文字</div>
```

---

### 1.2 CSS 核心布局

CSS 负责样式和布局。Vue 项目中推荐使用 **scoped CSS** 或 **CSS Modules**，但底层 CSS 语法仍是原生。

#### 盒模型（Box Model）

每个 HTML 元素都是一个“盒子”，包含：`content` → `padding` → `border` → `margin`。

| 属性               | 作用                       | 常用值                    |
| ------------------ | -------------------------- | ------------------------- |
| `width` / `height` | 内容区域宽/高              | 像素 `px`、百分比 `%`     |
| `padding`          | 内边距（内容到边框的距离） | 常用 `8px 12px`           |
| `border`           | 边框                       | `1px solid #ccc`          |
| `margin`           | 外边距（元素间的距离）     | 常用 `10px auto` 实现居中 |

#### 现代布局：Flexbox（95% 场景）

传统布局依靠 `float` + `position` 非常痛苦，**Flex 已成主流**。

**父容器属性**（作用在容器上，控制子元素排列）：

```css
.container {
  display: flex;              /* 开启弹性布局 */
  flex-direction: row;       /* 主轴方向：row(行) / column(列) */
  justify-content: center;   /* 主轴对齐：flex-start / center / space-between */
  align-items: center;       /* 交叉轴对齐：flex-start / center / stretch */
  flex-wrap: wrap;           /* 子元素换行 */
}
```

**子元素属性**（作用在子元素上，控制自身）：

```css
.item {
  flex: 1;                   /* 占据剩余空间比例 */
  align-self: flex-end;      /* 覆盖父容器 align-items */
}
```

**常用布局模板**：

```css
/* 1. 水平居中 */
.parent { display: flex; justify-content: center; }

/* 2. 垂直居中 */
.parent { display: flex; align-items: center; }

/* 3. 整体居中（经典登录框） */
.parent { display: flex; justify-content: center; align-items: center; height: 100vh; }
```

> **与 Vue 的关联**：Vue 组件样式中，`scoped` 属性会自动生成唯一属性选择器，避免样式冲突。但所写的 CSS 语法还是上面的这些。

#### 高频样式属性

| 分类 | 属性                          | 用途                      | 示例                              |
| ---- | ----------------------------- | ------------------------- | --------------------------------- |
| 布局 | `display`                     | 指定元素类型              | `display: flex;`                  |
|      | `position: fixed`             | 固定定位（导航栏）        | `position: fixed; top: 0;`        |
|      | `position: relative/absolute` | 相对/绝对定位（子绝父相） | `position: absolute; left: 10px;` |
| 外观 | `color`                       | 文字颜色                  | `color: #333;`                    |
|      | `background-color`            | 背景色                    | `background: #f5f5f5;`            |
|      | `font-size`                   | 文字大小                  | `font-size: 14px;`                |
|      | `border-radius`               | 圆角                      | `border-radius: 4px;`             |
| 间距 | `margin`                      | 外边距                    | `margin-top: 10px;`               |
|      | `padding`                     | 内边距                    | `padding: 8px 12px;`              |

#### 选择器简要（只用 3 种）

| 选择器     | 语法     | 作用                                 | 示例               |
| ---------- | -------- | ------------------------------------ | ------------------ |
| 类选择器   | `.类名`  | 选择所有带此类名的元素（**最常用**） | `.btn { }`         |
| 标签选择器 | `标签名` | 选择所有该标签元素（重置样式时用）   | `input { }`        |
| 后代选择器 | `父 .子` | 选择父元素内的子元素                 | `.form .input { }` |

> **避免**：ID 选择器（`#id`）在组件中尽量不用，因为 Vue 组件复用会导致 ID 冲突。

---

### 1.3 JavaScript 核心

Vue 组件逻辑部分使用 JavaScript（或 TypeScript）。以下只列出 **Vue 开发中高频使用的 JS 语法**。

#### 变量声明：`const` / `let`（放弃 `var`）

| 关键字  | 作用域 | 是否可变             | Vue 中使用场景                 |
| ------- | ------ | -------------------- | ------------------------------ |
| `const` | 块级   | 不可变（引用不可变） | 声明函数、常量配置、导入的模块 |
| `let`   | 块级   | 可变                 | 循环变量、临时计数值           |

```javascript
// 正确用法
const API_BASE = '/api';           // 常量
let count = 0;                     // 可变

// 不要用 var（作用域混乱）
var old = '不要这样写';
```

#### 箭头函数

**语法**：`(参数) => 表达式` 或 `(参数) => { 多条语句 }`

**在 Vue 中的优势**：
- 不会重新绑定 `this`，自动指向外层（Vue 组件实例）
- 代码更简洁

```javascript
// 传统函数（注意 this 指向问题）
setTimeout(function() {
  console.log(this); // 这里是 window，不是 Vue 实例
}, 1000);

// 箭头函数（推荐）
setTimeout(() => {
  console.log(this); // 自动继承外层 this（Vue 实例）
}, 1000);
```

#### 数组常用方法（替代 for 循环）

| 方法        | 作用               | Vue 中高频场景         |
| ----------- | ------------------ | ---------------------- |
| `map()`     | 遍历并返回新数组   | 提取字段、数据转换     |
| `filter()`  | 过滤元素           | 前端模糊搜索、条件筛选 |
| `find()`    | 找到第一个匹配元素 | 根据 id 查找对象       |
| `forEach()` | 遍历（无返回值）   | 数据拼装、批量操作     |
| `splice()`  | 添加/删除/替换元素 | 删除表格行、替换数组项 |

```javascript
const users = [{ id: 1, name: '张三' }, { id: 2, name: '李四' }];

// map：提取所有 name
const names = users.map(u => u.name);    // ['张三', '李四']

// filter：筛选 id>1
const filtered = users.filter(u => u.id > 1);   // [{ id:2, name:'李四' }]

// find：找 id=2
const target = users.find(u => u.id === 2);    // { id:2, name:'李四' }

// splice：删除索引 0 处的 1 个元素
users.splice(0, 1);
console.log(users);   // [{ id:2, name:'李四' }]
```

#### 对象常用方法

| 方法                  | 作用                             | 示例                                                         |
| --------------------- | -------------------------------- | ------------------------------------------------------------ |
| `Object.keys(obj)`    | 返回对象所有属性名组成的数组     | `Object.keys({ name: '张三', age: 18 })` → `['name', 'age']` |
| `Object.values(obj)`  | 返回对象所有属性值组成的数组     | `Object.values({ name: '张三', age: 18 })` → `['张三', 18]`  |
| `Object.entries(obj)` | 返回 `[key, value]` 对组成的数组 | `Object.entries({ name: '张三' })` → `[['name', '张三']]`    |

```javascript
const user = { name: '张三', age: 18 };

// 获取属性名数组
const keys = Object.keys(user);    // ['name', 'age']

// 获取属性值数组
const values = Object.values(user); // ['张三', 18]

// 获取键值对数组
const entries = Object.entries(user); // [['name', '张三'], ['age', 18]]
```

#### 正则表达式基础

正则表达式用于匹配字符串中的字符组合，常用于表单验证。

| 方法    | 所属     | 作用         | 示例                                |
| ------- | -------- | ------------ | ----------------------------------- |
| `test`  | 正则对象 | 判断是否匹配 | `/\d+/.test('abc123')` → `true`     |
| `match` | 字符串   | 提取匹配内容 | `'abc123'.match(/\d+/)` → `['123']` |

```javascript
// 验证邮箱格式
const email = 'test@example.com';
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
if (emailRegex.test(email)) {
  console.log('邮箱格式正确');
}
```

#### 对象解构（简化取值）

```javascript
const user = { id: 1, name: '张三', age: 20 };
// 传统写法
const id = user.id;
const name = user.name;

// 解构写法（推荐）
const { id, name } = user;
console.log(id, name);
```

#### 动态属性赋值

JavaScript 对象可以随时添加新属性，无需预先定义。

```javascript
const params = { pageNum: 1, pageSize: 10 };
params.status = 'active';   // 动态添加 status 属性
console.log(params);        // { pageNum: 1, pageSize: 10, status: 'active' }
```

**常见场景**：接口请求参数仅在条件满足时才添加。

```javascript
const params = { pageNum: 1, pageSize: 10 };
if (keyword.value) {
  params.keyword = keyword.value;   // 有关键词时才添加
}
```
#### 展开运算符

使用 `...` 将对象或数组展开，常用于拷贝、合并、批量传参。

```javascript
// 对象拷贝
const data = { name: '张三', age: 18 };
const copy = { ...data };
console.log(copy); // { name: '张三', age: 18 }

// 对象合并（后面覆盖前面）
const user = { name: '张三', age: 18 };
const extra = { city: '北京', age: 20 };
const merged = { ...user, ...extra };
console.log(merged); // { name: '张三', age: 20, city: '北京' }

// 数组拷贝
const arr = [1, 2, 3];
const newArr = [...arr]; // [1, 2, 3]

// 在 Vue 中批量传递 props
<Child v-bind="{ name: userName, age: userAge }" />
<!-- 等价于 -->
<Child :name="userName" :age="userAge" />
```

#### Promise 与 async/await（处理异步）

Promise 是 ES6 引入的异步编程解决方案，可以理解为一个“容器”，里面存放着未来才会结束的事件结果。

**Promise 的三种状态**：
- `pending`：进行中（初始状态）
- `fulfilled`：已成功（调用 `resolve`）
- `rejected`：已失败（调用 `reject`）

状态一旦改变就不会再变。

**Promise 的基本用法**：通过 `.then()` 处理成功，`.catch()` 处理失败。

```javascript
axios.get('/api/user')
  .then(response => {
    console.log(response.data);   // fulfilled 时执行
  })
  .catch(error => {
    console.error(error);         // rejected 时执行
  });
```

**async/await 语法糖**：`async/await` 是 Promise 的语法糖，让异步代码写起来像同步代码，更易读。

```javascript
async function getUser() {
  try {
    const response = await axios.get('/api/user');
    console.log(response.data);   // 成功
  } catch (error) {
    console.error(error);         // 失败
  }
}
```

**`await` 挂起了什么**：`async` 函数被调用时，会先同步执行到第一个 `await`；遇到 `await` 后函数在当前位置挂起（保存局部变量和执行位置），控制权交回调用者，其余代码照常执行。

```javascript
async function loadData() {
  console.log('1');                          // 同步执行
  const res = await axios.get('/api/user');  // 在此挂起，等待请求返回
  console.log('2');                          // 请求返回后从挂起处继续
  return res.data;
}

console.log('3');
loadData();   // 不阻塞，立即返回一个 pending 状态的 Promise
console.log('4');

// 输出顺序：1 → 3 → 4 → 2
```

> 挂起的只是**这个函数自身的执行流**，不是整个程序。JS 是单线程的，但事件循环会继续处理其他任务，等待期间其他代码照常运行；恢复执行属于**微任务**，比 `setTimeout` 更早。

**连续 `await` 是串行的**：每个 `await` 都会让函数停在原地，因此下面的写法总耗时 = A + B，无依赖关系时应改用 `Promise.all` 并发请求。

```javascript
// 串行：总耗时 = A + B
const userList = await getUserList();
const roleList = await getRoleList();

// 并发：总耗时 = max(A, B)
const [userList, roleList] = await Promise.all([getUserList(), getRoleList()]);
```

**哪些场景会返回 Promise？**

在 Vue 开发中，以下操作都返回 Promise，需要用 `async/await` 或 `.then()` 处理：

| 场景       | 示例                       | 来源         |
| ---------- | -------------------------- | ------------ |
| 接口请求   | `axios.get('/api/user')`   | axios 库     |
| 表单验证   | `formRef.value.validate()` | Element Plus |
| 路由跳转   | `router.push('/home')`     | Vue Router   |
| DOM 更新后 | `nextTick()`               | Vue 3        |

> Promise 是 ES6 的语言特性，以上示例来自第三方库和框架，它们的设计都遵循 Promise 规范。

#### 模板字符串

使用反引号 `` ` `` 包裹，通过 `${}` 嵌入变量，用于拼接字符串。

```javascript
const name = '张三';
const age = 20;
console.log(`用户：${name}，年龄：${age}`);  // 用户：张三，年龄：20
```

**在 Vue 中的常见场景**：动态拼接 class、style、提示文本。

```vue
<template>
  <div :class="`btn-${type}`">{{ `欢迎，${userName}` }}</div>
</template>
```

#### 可选链与空值合并

**可选链（`?.`）**：安全访问深层属性，避免因中间值为 `null/undefined` 而报错。

```javascript
const user = { profile: { name: '张三' } };
console.log(user.profile?.name);   // '张三'
console.log(user.address?.city);   // undefined（不报错）
```

**空值合并（`??`）**：仅当左侧为 `null` 或 `undefined` 时返回右侧默认值。

```javascript
const name = user.name ?? '匿名';
const age = user.age ?? 18;
```

两者常结合使用：

```javascript
const city = user.address?.city ?? '未知城市';
```

> 在 Vue 模板中同样支持，可简化条件渲染逻辑。

#### 模块导入导出（ES6 Module）

Vue 项目中每个 `.vue` 文件就是一个模块，通过 `import/export` 组织代码。

```javascript
// 导出（在 api/user.js 中）
export function getUserList(params) { ... }
export const BASE_URL = '/api';

// 导入（在组件中）
import { getUserList, BASE_URL } from '@/api/user';
```

> **注意**：`@` 是 Vue CLI / Vite 配置的路径别名，代表 `src` 目录。

---

### 1.4 与 Vue 的衔接

Vue 的核心思想是 **数据驱动视图**，大部分情况下不需要直接操作 DOM（原生 `document.getElementById`）。

#### 原生 DOM 操作 vs Vue 模板

| 场景          | 原生 JS                      | Vue 模板                                        |
| ------------- | ---------------------------- | ----------------------------------------------- |
| 获取输入框值  | `const val = input.value`    | `v-model="username"`，通过 `this.username` 获取 |
| 设置输入框值  | `input.value = '张三'`       | `this.username = '张三'`                        |
| 修改元素文本  | `div.innerText = 'hello'`    | `{{ message }}` 插值，改 `this.message`         |
| 显示/隐藏元素 | `el.style.display = 'none'`  | `v-if="isShow"` 或 `v-show="isShow"`            |
| 添加/删除类名 | `el.classList.add('active')` | `:class="{ active: isActive }"`                 |
| 监听点击事件  | `btn.onclick = handleClick`  | `@click="handleClick"`                          |
| 循环渲染列表  | `for` 循环 + `createElement` | `<li v-for="item in list">{{ item }}</li>`      |

> `v-for` 遍历数组时可用 `(item, index)`，遍历对象时可用 `(value, key)`，必须配合 `:key` 提供唯一标识。

#### 最小示例对比

**原生写法（不推荐在 Vue 中这样写）**：

```html
<input type="text" id="nameInput">
<button onclick="showName()">显示</button>
<div id="displayDiv"></div>

<script>
function showName() {
  const name = document.getElementById('nameInput').value;
  document.getElementById('displayDiv').innerText = name;
}
</script>
```

**Vue 写法（推荐）**：

```vue
<template>
  <div>
    <input v-model="name">
    <button @click="showName">显示</button>
    <div>{{ displayName }}</div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      name: '',
      displayName: ''
    }
  },
  methods: {
    showName() {
      this.displayName = this.name;
    }
  }
}
</script>
```

#### 关键转变

| 思维方式 | 原生 DOM                  | Vue                                  |
| -------- | ------------------------- | ------------------------------------ |
| 数据存储 | 散落在 DOM 属性、全局变量 | 统一在 `data()` 中                   |
| 界面更新 | 手动修改 DOM              | 修改数据自动触发视图更新             |
| 事件处理 | 直接在 `onclick` 写代码   | 定义 `methods`，模板中 `@click` 绑定 |
| 代码组织 | 函数堆砌                  | 组件化、模块化                       |

> **一句话总结**：使用Vue，要写的是 **“数据状态的逻辑”**，而不是 **“怎么操作 DOM”**。

---

## 模块二：环境与工具

> 本模块介绍搭建 Vue 开发环境所需的工具链，包括 Node.js 运行时、包管理器、构建工具（Vue CLI / Vite）、代码编辑器插件以及调试工具。掌握这些工具是开始 Vue 项目开发的第一步。

---

### 2.1 Node.js 与包管理器

#### Node.js

Vue 的开发工具链（如 Vue CLI、Vite）以及依赖管理（npm、pnpm）都基于 Node.js 运行时。无需关心 Node.js 的底层细节，只需正确安装并能够运行相关命令即可。

**安装步骤**：
1. 访问 [Node.js 官网](https://nodejs.org/zh-cn/)，选择 LTS（长期支持版，推荐 18.x 或 20.x）。
2. 根据操作系统下载安装包并执行安装（安装过程中勾选“自动添加环境变量”）。
3. 打开终端（CMD / PowerShell / Terminal），验证安装：
   ```bash
   node --version   # 应输出 v18.x.x 或更高
   npm --version    # 应输出 9.x.x 或更高

#### 包管理器

Vue 项目依赖大量第三方库（如 Vue 核心、路由、UI 库），通过包管理器自动下载和管理。推荐使用 **pnpm** 或 **npm**。

| 包管理器            | 特点                                          | 适用场景                     |
| ------------------- | --------------------------------------------- | ---------------------------- |
| npm（Node.js 自带） | 最通用，无需额外安装                          | 快速开始，不追求极致性能     |
| pnpm（推荐）        | 磁盘空间占用小，安装速度快，天然支持 monorepo | 日常开发，尤其是多个项目并存 |

**配置国内镜像（提升安装速度）**：

```bash
# npm 配置淘宝镜像
npm config set registry https://registry.npmmirror.com

# pnpm 配置淘宝镜像
pnpm config set registry https://registry.npmmirror.com
```

> 验证镜像：`npm get registry` 或 `pnpm get registry`，输出淘宝镜像地址即成功。

---

### 2.2 构建工具：Vue CLI 与 Vite

构建工具负责将 Vue 单文件组件（`.vue`）编译为浏览器可识别的 HTML/CSS/JS，并提供开发服务器（热更新）、代码压缩、打包等能力。

| 构建工具 | 底层打包器       | 开发服务器启动速度   | 生产构建速度 | 推荐场景           |
| -------- | ---------------- | -------------------- | ------------ | ------------------ |
| Vue CLI  | Webpack          | 较慢（需打包全量）   | 中等         | 维护 Vue 2 项目    |
| Vite     | esbuild / Rollup | **极快**（按需编译） | 快           | **Vue 3 项目首选** |

#### 安装与创建项目

**Vue CLI（Vue 2 项目）**：
```bash
npm install -g @vue/cli
vue create my-project
```

**Vite（Vue 3 项目）**：
```bash
# 使用 pnpm（推荐）
pnpm create vite my-project --template vue

# 或使用 npm
npm create vite@latest my-project -- --template vue
```

#### 开发服务器常用命令

| 命令                        | 作用                                                         |
| --------------------------- | ------------------------------------------------------------ |
| `npm run dev` 或 `pnpm dev` | 启动开发服务器（默认 `http://localhost:5173` for Vite，`8080` for Vue CLI） |
| `npm run build`             | 构建生产版本（输出 `dist` 目录）                             |
| `npm run preview`           | 本地预览生产构建结果                                         |

> 核心特性：**热更新（HMR）** —— 修改代码后无需手动刷新页面，浏览器自动更新。

#### 配置文件

| 构建工具 | 配置文件         | 用途                                  |
| -------- | ---------------- | ------------------------------------- |
| Vue CLI  | `vue.config.js`  | 修改 webpack 配置（如代理、路径别名） |
| Vite     | `vite.config.js` | 配置代理、插件、路径别名等            |

**示例：配置代理（解决开发环境跨域）**：

```javascript
// vite.config.js
export default {
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
}
```

---

### 2.3 VS Code 必备插件

VS Code 是目前 Vue 开发的首选编辑器，配合插件可大幅提升开发效率。

| 插件名                         | 作用                               | 备注                          |
| ------------------------------ | ---------------------------------- | ----------------------------- |
| **Vue - Official**（原 Volar） | Vue 3 语法高亮、智能提示、类型检查 | Vue 3 必备，Vue 2 可用 Vetur  |
| **TypeScript Vue Plugin**      | 支持在 Vue 文件中使用 TypeScript   | 配合 Volar 使用               |
| **ESLint**                     | 实时检查代码规范，提示语法错误     | 配合项目中的 `.eslintrc` 配置 |
| **Prettier**                   | 自动格式化代码，统一代码风格       | 建议配合保存时自动格式化      |
| **npm Intellisense**           | 自动补全 `import` 中的模块路径     | 提升开发体验                  |

**推荐配置（保存时自动格式化）**：

在 VS Code 的 `settings.json` 中添加：
```json
{
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  }
}
```

---

### 2.4 浏览器开发者工具与 Vue Devtools

#### 浏览器开发者工具

现代浏览器（Chrome / Edge / Firefox）内置开发者工具（按 `F12` 打开），常用面板：

| 面板         | 作用                               | Vue 开发中常用场景                   |
| ------------ | ---------------------------------- | ------------------------------------ |
| **Console**  | 查看日志、错误信息                 | 调试 `console.log`、查看接口返回数据 |
| **Network**  | 查看网络请求（请求头、参数、响应） | 调试接口调用、检查跨域问题           |
| **Elements** | 查看 DOM 结构、修改样式            | 调试 CSS 布局、查看组件渲染结果      |
| **Sources**  | 断点调试代码                       | 排查 JS 逻辑错误                     |

#### Vue Devtools（必装）

Vue 官方提供的浏览器扩展，用于调试 Vue 应用。建议安装，否则调试 Vue 组件状态会非常困难。

- 安装地址：Chrome 网上应用商店搜索“Vue Devtools”，或从 GitHub releases 下载。
- 核心功能：
  - 查看组件树（当前渲染的组件层级）
  - 查看组件 `data`、`props`、`computed` 的值（实时变化）
  - 查看 Vuex / Pinia 状态
  - 时间旅行调试（追溯状态变化）

> 使用技巧：开发模式下，浏览器地址栏右侧出现 Vue 图标（灰色表示未检测到 Vue，绿色表示已检测到），点击即可打开 Vue Devtools 面板。

---

## 模块三：Vue 2 核心（Options API）

> Vue 2 的核心思想是**响应式数据 + 组件化**，通过 Options API 组织代码。

---

### 3.1 响应式原理（Object.defineProperty）

Vue 2 通过 `Object.defineProperty` 拦截对象属性的读取和修改，实现数据变化时自动更新视图。

**核心机制**：
- 遍历 `data()` 返回的对象，为每个属性添加 `getter` / `setter`
- 在 `getter` 中收集依赖（哪些组件用到了该属性）
- 在 `setter` 中触发依赖更新（重新渲染组件）

**限制与注意事项**：

| 情况                 | Vue 2 能否检测 | 解决方案                                                     |
| -------------------- | -------------- | ------------------------------------------------------------ |
| 给已有对象添加新属性 | 否             | `Vue.set(obj, 'key', value)`                                 |
| 删除对象属性         | 否             | `Vue.delete(obj, 'key')`                                     |
| 通过索引修改数组元素 | 否             | `Vue.set(arr, index, newValue)` 或 `arr.splice(index, 1, newValue)` |
| 修改数组长度         | 否             | `arr.splice(newLength)`                                      |

```javascript
// 错误示例：添加新属性不会触发视图更新
data() {
  return { user: { name: '张三' } }
},
mounted() {
  this.user.age = 20;   // 无效，视图不会更新
}

// 正确示例
this.$set(this.user, 'age', 20);
```

> 这些限制在 Vue 3 的 Proxy 响应式系统中已不存在。

---

### 3.2 Options API 详解

Options API 将组件的不同部分（数据、方法、计算属性、生命周期等）分散在不同的选项中。

#### 组件结构

```javascript
export default {
  name: 'ComponentName',      // 组件名（建议与文件名一致）
  components: {},             // 注册子组件
  props: {},                  // 接收父组件传递的数据
  data() {                    // 响应式数据
    return { msg: '' }
  },
  computed: {},               // 计算属性（缓存）
  watch: {},                  // 监听器（响应数据变化）
  methods: {},                // 方法（事件处理、业务逻辑）
  // 生命周期钩子（按执行顺序）
  beforeCreate() {},
  created() {},
  beforeMount() {},
  mounted() {},
  beforeUpdate() {},
  updated() {},
  beforeDestroy() {},
  destroyed() {}
}
```

#### data

必须是**函数**，返回一个对象。每个组件实例维护独立的响应式数据。

```javascript
data() {
  return {
    username: '',
    count: 0
  }
}
```

#### methods

定义组件方法，用于事件处理、接口调用等。

```javascript
methods: {
  handleClick() {
    this.count++;
  },
  async fetchData() {
    const res = await fetch('/api/data');
    this.data = res.data;
  }
}
```

#### computed

**计算属性**，基于响应式数据派生新值，**有缓存**（依赖不变则不重新计算）。适用于复杂逻辑或需缓存的计算。

```javascript
computed: {
  fullName() {
    return `${this.firstName} ${this.lastName}`;
  },
  // 带 getter/setter
  fullNameWithSetter: {
    get() {
      return `${this.firstName} ${this.lastName}`;
    },
    set(val) {
      const arr = val.split(' ');
      this.firstName = arr[0];
      this.lastName = arr[1];
    }
  }
}
```

> 与 `methods` 的区别：`methods` 每次调用都会执行，`computed` 基于依赖缓存。

#### watch

监听响应式数据的变化，执行异步或开销较大的操作。

```javascript
watch: {
  // 监听 simple 类型
  username(newVal, oldVal) {
    console.log(`从 ${oldVal} 变为 ${newVal}`);
  },
  // 监听对象内部属性（深度监听）
  user: {
    handler(newVal) {
      console.log('user 变化', newVal);
    },
    deep: true,
    immediate: true   // 立即执行一次
  }
}
```

#### 生命周期钩子

| 钩子            | 触发时机                            | 常用场景                     |
| --------------- | ----------------------------------- | ---------------------------- |
| `beforeCreate`  | 实例刚创建，data/methods 不可用     | 极少使用                     |
| `created`       | data/methods 已初始化，但未挂载 DOM | **接口请求、初始化数据**     |
| `beforeMount`   | 模板编译前，虚拟 DOM 已存在         | 极少使用                     |
| `mounted`       | DOM 已挂载，可访问 DOM 元素         | **操作 DOM、初始化第三方库** |
| `beforeUpdate`  | 数据变化，视图重新渲染前            | 极少使用                     |
| `updated`       | 视图重新渲染后                      | 谨慎使用，避免死循环         |
| `beforeDestroy` | 组件销毁前                          | **清理定时器、解绑事件**     |
| `destroyed`     | 组件销毁后                          | 少用                         |

> **注意**：`created` 中不要操作 DOM（此时不存在），应放在 `mounted` 中。

#### 模板引用：$refs

通过 `ref` 属性标记 DOM 元素或子组件，之后在组件中通过 `this.$refs` 访问。

```vue
<template>
  <input ref="myInput" />
  <button @click="focusInput">聚焦输入框</button>
</template>

<script>
export default {
  methods: {
    focusInput() {
      this.$refs.myInput.focus();
    }
  }
}
</script>
```

> `$refs` 在组件挂载完成后（`mounted`）才能访问，且不能用于响应式数据的绑定。

---

### 3.3 组件通信

Vue 2 中组件通信有多种方式，下表列出常用场景：

| 通信类型 | 适用场景               | 方式                           |
| -------- | ---------------------- | ------------------------------ |
| 父 → 子  | 父组件传递数据给子组件 | `props`                        |
| 子 → 父  | 子组件触发父组件的事件 | `$emit`                        |
| 兄弟组件 | 无直接父子关系         | 事件总线（EventBus）或状态管理 |
| 跨层级   | 祖先 → 后代            | `provide / inject`             |
| 任意组件 | 全局状态               | Vuex                           |

#### 父传子：props

父组件：
```vue
<template>
  <Child :user-name="name" :age="18" />
</template>

<script>
import Child from './Child.vue';
export default {
  components: { Child },
  data() {
    return { name: '张三' }
  }
}
</script>
```

子组件：
```javascript
export default {
  props: {
    userName: {        // 父组件传递时用 userName，子组件内部使用时用 this.userName
      type: String,
      required: true,
      default: ''
    },
    age: {
      type: Number,
      default: 0
    }
  },
  mounted() {
    console.log(this.userName, this.age);
  }
}
```

> **注意**：props 不可在子组件中直接修改，应当通过 `$emit` 通知父组件修改。

#### 子传父：$emit

子组件：
```vue
<template>
  <button @click="handleClick">发送</button>
</template>

<script>
export default {
  methods: {
    handleClick() {
      this.$emit('update', { value: '子组件的数据' });
    }
  }
}
</script>
```

父组件：
```vue
<template>
  <Child @update="handleUpdate" />
</template>

<script>
export default {
  methods: {
    handleUpdate(payload) {
      console.log(payload.value);
    }
  }
}
</script>
```

#### 兄弟组件通信：事件总线（EventBus）

创建一个全局空 Vue 实例作为事件中转。

```javascript
// bus.js
import Vue from 'vue';
export default new Vue();
```

组件 A（发送）：
```javascript
import bus from './bus';
bus.$emit('event-name', data);
```

组件 B（接收）：
```javascript
import bus from './bus';
bus.$on('event-name', (data) => {
  console.log(data);
});
```

> **注意**：EventBus 在复杂项目中容易导致事件混乱，推荐升级为 Vuex 或直接迁移至 Vue 3 + Pinia。

#### 跨层级传递：provide / inject

祖先组件提供数据，后代组件注入使用。

祖先：
```javascript
export default {
  provide() {
    return {
      theme: 'dark'
    };
  }
}
```

后代：
```javascript
export default {
  inject: ['theme'],
  mounted() {
    console.log(this.theme);
  }
}
```

> **注意**：`provide` 传入的对象不是响应式的。如需响应式，可传入 `reactive` 对象（后续 Vue 3 章节详解）。

---

### 3.4 路由（Vue Router 3）

Vue Router 是 Vue 官方的路由管理器，用于构建单页应用（SPA），实现 URL 与组件的映射、页面跳转、导航守卫等功能。

#### 安装与基本配置

```bash
npm install vue-router@3
```

创建路由文件 `router/index.js`：
```javascript
import Vue from 'vue';
import VueRouter from 'vue-router';
import Home from '../views/Home.vue';
import About from '../views/About.vue';

Vue.use(VueRouter);

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/about',
    name: 'About',
    component: About
  },
  {
    path: '/user/:id',
    name: 'User',
    component: () => import('../views/User.vue')  // 懒加载
  }
];

const router = new VueRouter({
  mode: 'history',  // 去掉 URL 中的 #，需后端配合
  base: process.env.BASE_URL,
  routes
});

export default router;
```

在 `main.js` 中注册：
```javascript
import router from './router';
new Vue({
  router,
  render: h => h(App)
}).$mount('#app');
```

#### 路由跳转

**声明式（模板中）**：
```vue
<router-link to="/about">关于</router-link>
<router-link :to="{ name: 'User', params: { id: 123 } }">用户123</router-link>
```

**编程式（JS 中）**：
```javascript
this.$router.push('/about');
this.$router.push({ name: 'User', params: { id: 123 } });
this.$router.replace('/home');   // 替换历史记录
this.$router.go(-1);             // 后退
```

#### 获取路由参数

```vue
<template>
  <div>用户ID：{{ $route.params.id }}</div>
</template>

<script>
export default {
  mounted() {
    console.log(this.$route.params.id);
  },
  watch: {
    '$route.params.id'(newId) {
      // 同一组件路由参数变化时的处理
    }
  }
}
</script>
```

#### 路由守卫

**全局前置守卫**（跳转前执行，常用于权限验证）：
```javascript
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');
  if (to.meta.requiresAuth && !token) {
    next('/login');
  } else {
    next();
  }
});
```

**路由独享守卫**（在路由配置中）：
```javascript
{
  path: '/admin',
  component: Admin,
  beforeEnter: (to, from, next) => {
    // 权限检查
    next();
  }
}
```

**组件内守卫**：
```javascript
export default {
  beforeRouteEnter(to, from, next) {
    // 在渲染该组件前调用，此时无法访问 `this`
    next(vm => {
      // 通过 `vm` 访问组件实例
    });
  },
  beforeRouteUpdate(to, from, next) {
    // 路由参数变化但组件复用时的更新
    next();
  },
  beforeRouteLeave(to, from, next) {
    // 离开组件前的操作（如确认是否保存）
    next();
  }
}
```

---

### 3.5 状态管理（Vuex 可选）

**状态管理** 是多个组件之间共享、存储、修改响应式数据的架构模式。把公共数据集中到一个地方（Store），任何组件都能读写，且数据变化时所有使用处自动更新。

对于 Vue 2 项目，官方状态管理库是 **Vuex**。但如果应用简单，也可以不用 Vuex。

#### 数据共享方案：localStorage 与 Vuex

在 Vue 2 中，跨组件共享数据有两种常见方案：`localStorage` 和 Vuex。两者适用于不同场景，也可以配合使用。

| 对比维度   | `localStorage`                | Vuex                                       |
| ---------- | ----------------------------- | ------------------------------------------ |
| 数据持久性 | 页面刷新/关闭后数据还在       | 页面刷新后数据消失                         |
| 响应式     | 否（修改后视图不自动更新）    | 是（修改后所有用到的地方自动刷新）         |
| 跨组件通知 | 需手动刷新或事件总线          | 自动通知（修改 → 重新渲染）                |
| 适用场景   | 存 Token、用户 ID、主题偏好等 | 多组件共享的响应式数据（用户信息、购物车） |
| 调试追踪   | 困难                          | 支持时间旅行、开发者工具                   |

**简单判断**：
- 只需存 Token，页面刷新后能拿到即可 → 用 `localStorage`
- 需要在多个组件间共享响应式数据（如用户头像、昵称、购物车数量） → 用 Vuex

> 两者并不互斥，一个项目可以同时使用。例如：Token 存 `localStorage` 做持久化，用户信息存 Vuex 实现页面响应式更新。

#### 安装与配置

```bash
npm install vuex@3
```

创建 `store/index.js`：

```javascript
import Vue from 'vue';
import Vuex from 'vuex';

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    token: localStorage.getItem('token') || '',
    userInfo: null
  },
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token;
      localStorage.setItem('token', token);
    },
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo;
    }
  },
  actions: {
    async login({ commit }, { username, password }) {
      const res = await api.login({ username, password });
      commit('SET_TOKEN', res.token);
      commit('SET_USER_INFO', res.userInfo);
      return res;
    },
    logout({ commit }) {
      commit('SET_TOKEN', '');
      commit('SET_USER_INFO', null);
      localStorage.removeItem('token');
    }
  },
  getters: {
    isLoggedIn: state => !!state.token,
    userName: state => state.userInfo?.name || ''
  }
});
```

在 `main.js` 注册：

```javascript
import store from './store';
new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app');
```

#### 在组件中使用

```vue
<template>
  <div>
    <div v-if="isLoggedIn">欢迎，{{ userName }}</div>
    <button @click="handleLogout">退出</button>
  </div>
</template>

<script>
import { mapState, mapGetters, mapActions } from 'vuex';

export default {
  computed: {
    ...mapState(['userInfo']),
    ...mapGetters(['isLoggedIn', 'userName'])
  },
  methods: {
    ...mapActions(['logout']),
    handleLogout() {
      this.logout();
      this.$router.push('/login');
    }
  }
}
</script>
```

#### 何时使用 Vuex

| 场景                                               | 是否需要 Vuex                            |
| -------------------------------------------------- | ---------------------------------------- |
| 组件间共享数据很少（如只共享登录 Token）           | 不需要，用 `localStorage` + 直接读取即可 |
| 多个组件共享同一份响应式数据（如用户信息、购物车） | 推荐使用                                 |
| 状态变化需要触发多个组件自动更新                   | 推荐使用                                 |
| 有复杂异步操作（如接口链、多条 commit）            | 推荐使用                                 |

> **Vue 3 替代方案**：Pinia 更轻量、类型安全、Composition API 友好。新项目不推荐使用 Vuex，可考虑直接迁移至 Vue 3 + Pinia。

---

## 模块四：Vue 3 核心（Composition API）

> 本模块介绍 Vue 3 的核心特性，包括响应式原理（Proxy）、Composition API、生命周期变化、组件通信等。Vue 3 全面拥抱 TypeScript，性能更好，逻辑复用更灵活。

---

### 4.1 响应式原理（Proxy）

Vue 3 使用 ES6 的 `Proxy` 替代 `Object.defineProperty` 实现响应式，解决了 Vue 2 中以下限制：
- 无法检测对象新增属性
- 无法通过索引直接修改数组元素
- 无法直接修改数组长度

**核心特点**：

- `Proxy` 代理整个对象，而不是遍历属性
- 可以拦截 13 种基本操作（get、set、deleteProperty 等）
- 数组变化（索引赋值、length 修改）也可检测

**使用示例**：

```javascript
import { reactive } from 'vue';

const state = reactive({ user: { name: '张三' }, list: [1, 2, 3] });

// 以下操作均可触发视图更新
state.user.age = 20;           // 新增属性
state.list[0] = 100;           // 修改数组索引
state.list.length = 1;         // 修改数组长度
```

> 注意：`reactive` 只能代理对象类型（Object、Array、Map、Set），基本类型需使用 `ref`。

---

### 4.2 Composition API

Composition API 是 Vue 3 新增的逻辑组织方式，将组件按**功能逻辑**聚合，而非按选项（data、methods、computed）分散。

#### 基本写法（`setup` 函数）

```javascript
<script>
import { ref, reactive, computed, watch, onMounted } from 'vue';

export default {
  setup() {
    const count = ref(0);
    const state = reactive({ name: '张三', age: 18 });
    const doubleCount = computed(() => count.value * 2);
    const increment = () => { count.value++; };
    watch(count, (newVal, oldVal) => {
      console.log(`count从${oldVal}变为${newVal}`);
    });
    onMounted(() => { console.log('组件已挂载'); });
    return { count, state, doubleCount, increment };
  }
}
</script>
```

#### 推荐写法（`<script setup>` 语法糖）

```vue
<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue';

const count = ref(0);
const state = reactive({ name: '张三', age: 18 });
const doubleCount = computed(() => count.value * 2);
const increment = () => { count.value++; };
watch(count, (newVal) => { console.log(newVal); });
onMounted(() => { console.log('mounted'); });
</script>

<template>
  <button @click="increment">{{ count }} * 2 = {{ doubleCount }}</button>
  <div>{{ state.name }} - {{ state.age }}</div>
</template>
```

#### ref 与 reactive 的区别

| 特性         | `ref`                             | `reactive`                             |
| ------------ | --------------------------------- | -------------------------------------- |
| 数据类型     | 任意（主要包裹基本类型）          | 对象类型（Object/Array/Map/Set）       |
| 访问方式     | `.value`（模板中自动解包）        | 直接访问属性                           |
| 重新赋值     | 支持整体替换                      | 不支持（会丢失响应）                   |
| 解构后响应性 | 保持（需 `.value`）               | 丢失（需 `toRefs`）                    |
| 使用场景     | 基本类型、需要整体替换的数组/对象 | 深度对象、表单、不会整体替换的复杂状态 |

> `reactive` 解构会丢失响应性，可使用 `toRefs` 转为 `ref` 集合。

```javascript
const count = ref(0);
count.value = 10;          // ref 需要 .value 访问和修改

const state = reactive({ name: '李四', age: 18 });

// 错误：直接重新赋值会断开响应式连接
// state = { name: '王五' };

// 正确：使用 Object.assign 批量修改属性，保持响应式
Object.assign(state, { name: '王五', age: 20 });

// 解构：从 state 中取出 name 和 age（此时是普通值，不是响应式的）
// const { name, age } = state;  // 这样会丢失响应性

// 正确：使用 toRefs 包装，解构后的变量仍是响应式的（ref）
const { name, age } = toRefs(state);
// 此时 name 和 age 是 ref，需要通过 .value 访问
// name.value = '赵六' 会触发视图更新
```

#### nextTick：等待 DOM 更新

```javascript
import { nextTick } from 'vue';

const updateAndWait = async () => {
  count.value++;
  await nextTick();
  // DOM 已更新，可以操作
};
```

> 与 Vue 2 的 `this.$nextTick` 功能相同，用于在数据变化后等待 DOM 重新渲染。

#### 逻辑复用：Composables

将组件逻辑抽取为可复用的函数，称为 Composable。这是 Vue 3 实现逻辑复用的推荐方式，替代 Vue 2 中的 mixins。

```javascript
// composables/useCounter.js
import { ref } from 'vue';

export function useCounter(initial = 0) {
  const count = ref(initial);
  const increment = () => count.value++;
  const decrement = () => count.value--;
  return { count, increment, decrement };
}
```

在组件中使用：

```vue
<script setup>
import { useCounter } from '@/composables/useCounter';

const { count, increment } = useCounter(10);
</script>

<template>
  <button @click="increment">{{ count }}</button>
</template>
```

Composable 中可以包含响应式状态、方法、生命周期钩子等。多个组件调用同一个 Composable 时，**默认每次调用都会创建独立的状态**。如果需要在多个组件间共享同一份状态，可以在 Composable 函数外部定义模块级别的 `ref`：

```javascript
// composables/useSharedCounter.js
import { ref } from 'vue';

const sharedCount = ref(0);  // 模块级变量
export function useSharedCounter() {
  const increment = () => sharedCount.value++;
  return { count: sharedCount, increment };
}
```

但这种模式仅适用于简单的局部共享场景。**全局共享状态应使用 Pinia**。

---

### 4.3 生命周期变化

Vue 3 生命周期钩子名称与 Vue 2 有部分变化，同时 Composition API 中可在 `setup` 内直接使用对应的 `onXxx` 函数。

#### 映射表

| Vue 2 钩子      | Vue 3 Options API | Vue 3 Composition API      |
| --------------- | ----------------- | -------------------------- |
| `beforeCreate`  | `beforeCreate`    | 无需（`setup` 本身即替代） |
| `created`       | `created`         | 无需（`setup` 本身即替代） |
| `beforeMount`   | `beforeMount`     | `onBeforeMount`            |
| `mounted`       | `mounted`         | `onMounted`                |
| `beforeUpdate`  | `beforeUpdate`    | `onBeforeUpdate`           |
| `updated`       | `updated`         | `onUpdated`                |
| `beforeDestroy` | `beforeUnmount`   | `onBeforeUnmount`          |
| `destroyed`     | `unmounted`       | `onUnmounted`              |

**使用示例（Composition API）**：
```javascript
import { onMounted, onBeforeUnmount } from 'vue';

onMounted(() => {
  // 组件挂载后，可操作 DOM、发起请求
});

onBeforeUnmount(() => {
  // 组件卸载前，清理定时器、解绑事件
});
```

---

### 4.4 组件通信

Vue 3 的组件通信方式与 Vue 2 类似，但结合 Composition API 写法上有所变化。

| 通信类型                 | 方式             | Composition API 写法 |
| ------------------------ | ---------------- | -------------------- |
| 父 → 子                  | props            | `defineProps`        |
| 子 → 父                  | emits            | `defineEmits`        |
| 父 → 子（暴露方法/属性） | ref / expose     | `defineExpose`       |
| 跨层级                   | provide / inject | `provide` / `inject` |
| 全局状态                 | Pinia            | 独立章节             |

#### 父传子：defineProps

父组件：
```vue
<template>
  <Child :user-name="name" :age="18" />
</template>

<script setup>
import Child from './Child.vue';
const name = '张三';
</script>
```

子组件（使用 `defineProps` 编译宏）：
```vue
<template>
  <div>{{ userName }} - {{ age }}</div>
</template>

<script setup>
const props = defineProps({
  userName: {
    type: String,
    required: true
  },
  age: {
    type: Number,
    default: 0
  }
});

// 也可用数组形式（无校验）
// const props = defineProps(['userName', 'age']);
</script>
```

#### 子传父：defineEmits

子组件：
```vue
<template>
  <button @click="emitUpdate">发送</button>
</template>

<script setup>
const emit = defineEmits(['update']);

const emitUpdate = () => {
  emit('update', { value: '子组件的数据' });
};
</script>
```

父组件：
```vue
<template>
  <Child @update="handleUpdate" />
</template>

<script setup>
import Child from './Child.vue';

const handleUpdate = (payload) => {
  console.log(payload.value);
};
</script>
```

#### 父组件调用子组件方法/属性：defineExpose

子组件暴露方法或属性：
```vue
<script setup>
import { ref } from 'vue';

const childData = ref('子组件内部数据');
const childMethod = () => {
  console.log('子组件方法被调用');
};

defineExpose({
  childData,
  childMethod
});
</script>
```

父组件通过模板 ref 调用：
```vue
<template>
  <Child ref="childRef" />
  <button @click="callChild">调用子组件</button>
</template>

<script setup>
import { ref } from 'vue';
import Child from './Child.vue';

const childRef = ref(null);

const callChild = () => {
  console.log(childRef.value.childData);
  childRef.value.childMethod();
};
</script>
```

#### 模板 ref：获取 DOM 或组件实例

```vue
<template>
  <input ref="inputRef" />
</template>

<script setup>
import { ref, onMounted } from 'vue';

const inputRef = ref(null);
onMounted(() => {
  inputRef.value.focus();
});
</script>
```

#### 跨层级传递：provide / inject

祖先组件提供数据（可提供响应式数据）：
```vue
<script setup>
import { provide, ref } from 'vue';

const theme = ref('dark');
provide('theme', theme);
</script>
```

后代组件注入使用：
```vue
<script setup>
import { inject } from 'vue';

const theme = inject('theme');
</script>
```

> 与 Vue 2 的区别：Vue 3 的 `provide` / `inject` 配合 `ref` / `reactive` 可天然实现响应式，无需额外处理。

---

### 4.5 路由（Vue Router 4）

Vue Router 4 是专为 Vue 3 设计的路由版本，与 Composition API 集成更好。

#### 安装与配置

```bash
npm install vue-router@4
```

创建 `router/index.js`：

```javascript
import { createRouter, createWebHistory } from 'vue-router';
import Home from '../views/Home.vue';

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/about', component: () => import('../views/About.vue') },
  // 路径变量：使用冒号 : 声明动态段
  { path: '/user/:id', name: 'User', component: () => import('../views/User.vue') }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
```

#### 路由参数与跳转

| 参数类型     | 路由配置    | 跳转方式                                 | 获取方式           |
| ------------ | ----------- | ---------------------------------------- | ------------------ |
| 路径变量     | `/user/:id` | `{ name: 'User', params: { id: 123 } }`  | `route.params.id`  |
| Query String | 无需配置    | `{ path: '/users', query: { page: 1 } }` | `route.query.page` |

在 `<script setup>` 中使用：

```vue
<script setup>
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();

// 路径变量跳转：方式一（推荐，命名路由 + params）
const goToUser = (id) => {
  router.push({ name: 'User', params: { id } });
};

// 路径变量跳转：方式二（字符串拼接，简单场景可用，但不推荐手动拼接）
const goToUser2 = (id) => {
  router.push(`/user/${id}`);
};

// Query String 跳转
const goToList = () => {
  router.push({ path: '/users', query: { page: 1, size: 10 } });
};

// 获取参数
const userId = route.params.id;     // 路径变量
const currentPage = route.query.page; // Query String
</script>

<template>
  <button @click="goToUser(123)">用户123（推荐）</button>
  <button @click="goToUser2(456)">用户456（拼接）</button>
  <button @click="goToList">列表</button>
</template>
```

> **注意**：路径变量跳转推荐使用 `name + params`，可避免手动拼接字符串带来的编码问题和路径错误；也可直接使用字符串拼接（如 `/user/${id}`），但不建议在复杂场景使用。`params` 只能与 `name` 配对使用，不能与 `path` 同时使用；`query` 可以与 `path` 或 `name` 搭配。

#### 路由守卫

**重要变更**：Vue Router 4 中，全局前置守卫不再需要调用 `next()`，而是通过 `return` 控制导航。

```javascript
// router/index.js
router.beforeEach((to, from) => {
  const token = localStorage.getItem('token');
  if (to.meta.requiresAuth && !token) {
    return '/login';      // 重定向
  }
  // 返回 true 或不返回，表示允许导航
});
```

**组件内守卫**（Composition API）：

```vue
<script setup>
import { onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router';

onBeforeRouteUpdate((to, from) => {
  // 路由参数变化时（同一组件复用）
  const newId = to.params.id;
});

onBeforeRouteLeave((to, from) => {
  const answer = confirm('确定要离开吗？');
  if (!answer) return false;   // 取消导航
});
</script>
```

> **说明**：不再使用 `next()`，直接 `return`：`false` 取消，字符串重定向，`true` 或 `undefined` 放行。

---

### 4.6 状态管理（Pinia）

Pinia 是 Vue 官方的替代 Vuex 的状态管理库，专为 Vue 3 设计，API 更简洁，TypeScript 支持更好。

#### 安装与配置

```bash
npm install pinia
```

在 `main.js` 中注册：
```javascript
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';

const pinia = createPinia();
createApp(App).use(pinia).mount('#app');
```

#### 定义 Store

创建 `stores/user.js`：
```javascript
import { defineStore } from 'pinia';

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    userName: (state) => state.userInfo?.name || ''
  },
  actions: {
    async login(credentials) {
      // 调用接口
      const res = await api.login(credentials);
      this.token = res.token;
      this.userInfo = res.userInfo;
      localStorage.setItem('token', res.token);
    },
    logout() {
      this.token = '';
      this.userInfo = null;
      localStorage.removeItem('token');
    }
  }
});
```

#### 组合式 API 风格的 Store（推荐）

Pinia 也支持类似 Composition API 的写法，更灵活：
```javascript
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '');
  const userInfo = ref(null);

  const isLoggedIn = computed(() => !!token.value);
  const userName = computed(() => userInfo.value?.name || '');

  async function login(credentials) {
    const res = await api.login(credentials);
    token.value = res.token;
    userInfo.value = res.userInfo;
    localStorage.setItem('token', res.token);
  }

  function logout() {
    token.value = '';
    userInfo.value = null;
    localStorage.removeItem('token');
  }

  return { token, userInfo, isLoggedIn, userName, login, logout };
});
```

#### 在组件中使用

```vue
<template>
  <div>
    <div v-if="isLoggedIn">欢迎，{{ userName }}</div>
    <button @click="handleLogout">退出</button>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user';
const userStore = useUserStore();

const handleLogout = () => {
  userStore.logout();
  // 跳转登录页
};
</script>
```

> Pinia 不需要 mutations，state 可直接修改（`userStore.token = 'xxx'`），也支持 `$patch` 批量更新。

---

### 4.7 `<script setup>` 语法糖

`<script setup>` 是 Vue 3 推荐的组件编写方式，相比普通 `setup` 函数更加简洁，顶层变量自动暴露给模板，无需 `return`。

#### 基本用法

```vue
<script setup>
import { ref } from 'vue';
import ChildComponent from './Child.vue';

const count = ref(0);
const increment = () => {
  count.value++;
};
</script>

<template>
  <button @click="increment">{{ count }}</button>
  <ChildComponent />
</template>
```

- 导入的组件自动注册，无需 `components` 选项  
- 顶层声明的变量、函数、导入的内容，模板中可直接使用

#### 使用 props 和 emits

```vue
<script setup>
const props = defineProps({
  title: String
});
const emit = defineEmits(['update']);

const handleClick = () => {
  emit('update', 'new value');
};
</script>

<template>
  <div>{{ title }}</div>
  <button @click="handleClick">更新</button>
</template>
```

`defineProps` 和 `defineEmits` 是编译宏，无需手动导入。

#### 与普通 `<script>` 同时使用

需要声明组件名或配置 `inheritAttrs` 等选项时，可以额外加一个普通 `<script>` 块：

```vue
<script>
export default {
  name: 'CustomName',
  inheritAttrs: false
};
</script>

<script setup>
// 业务逻辑
</script>
```

Vue 3.3+ 推荐使用 `defineOptions` 替代：

```vue
<script setup>
defineOptions({
  name: 'CustomName',
  inheritAttrs: false
});
</script>
```

#### TypeScript 支持（可选）

```vue
<script setup lang="ts">
interface Props {
  msg?: string;
  count?: number;
}

const props = withDefaults(defineProps<Props>(), {
  msg: 'hello',
  count: 0
});
</script>
```

#### 核心限制

- 不能使用 `async`（极少需要，可配合 `Suspense`）
- 定义事件必须用 `defineEmits`，不能像 Vue 2 直接 `this.$emit`
- 需要组件实例方法时，使用 `defineExpose` 暴露

---

## 模块五：Vue 2 → 3 迁移对照

> 本模块面向将 Vue 2 项目迁移至 Vue 3 的开发者，以速查表形式列出核心差异。Vue 3 大部分语法向下兼容（Options API 仍可用），但推荐使用 Composition API 和新的响应式系统。以下只列出**破坏性变更**和**推荐升级方式**。

---

### 5.1 语法差异速查表

| 项目       | Vue 2                          | Vue 3                             |
| ---------- | ------------------------------ | --------------------------------- |
| 响应式原理 | `Object.defineProperty`        | `Proxy`                           |
| 数据定义   | `data()` 返回对象              | `ref` / `reactive`                |
| 方法定义   | `methods`                      | 普通函数（`<script setup>`）      |
| 计算属性   | `computed`                     | `computed` (import from 'vue')    |
| 监听器     | `watch`                        | `watch` / `watchEffect`           |
| 生命周期   | `beforeDestroy` / `destroyed`  | `onBeforeUnmount` / `onUnmounted` |
| 模板根节点 | 必须单一根节点                 | 支持多根节点（Fragment）          |
| 异步组件   | `() => import('./Comp.vue')`   | `defineAsyncComponent`            |
| 自定义指令 | 钩子函数名不同                 | 新的钩子命名                      |
| 全局 API   | `Vue.component`                | `app.component`                   |
| 事件总线   | `new Vue()` (`$on`/`$emit`)    | 推荐用 mitt / pinia               |
| 过滤器     | 支持（`{{ value | filter }}`） | 移除（推荐用 computed / method）  |

---

### 5.2 生命周期映射表

| 功能       | Vue 2 Options API | Vue 3 Options API | Vue 3 Composition API |
| ---------- | ----------------- | ----------------- | --------------------- |
| 实例创建   | `beforeCreate`    | `beforeCreate`    | `setup()` 本身        |
| 实例初始化 | `created`         | `created`         | `setup()` 本身        |
| DOM 挂载前 | `beforeMount`     | `beforeMount`     | `onBeforeMount`       |
| DOM 挂载后 | `mounted`         | `mounted`         | `onMounted`           |
| 数据更新前 | `beforeUpdate`    | `beforeUpdate`    | `onBeforeUpdate`      |
| 数据更新后 | `updated`         | `updated`         | `onUpdated`           |
| 组件卸载前 | `beforeDestroy`   | `beforeUnmount`   | `onBeforeUnmount`     |
| 组件卸载后 | `destroyed`       | `unmounted`       | `onUnmounted`         |
| 错误捕获   | `errorCaptured`   | `errorCaptured`   | `onErrorCaptured`     |

---

### 5.3 组件写法对照

**Vue 2（Options API）**：
```javascript
export default {
  props: { msg: String },
  data() {
    return { count: 0 };
  },
  computed: {
    doubleCount() {
      return this.count * 2;
    }
  },
  methods: {
    increment() {
      this.count++;
    }
  },
  mounted() {
    console.log('mounted');
  }
}
```

**Vue 3（Composition API with `<script setup>`）**：
```vue
<script setup>
import { ref, computed, onMounted } from 'vue';

const props = defineProps(['msg']);
const count = ref(0);
const doubleCount = computed(() => count.value * 2);
const increment = () => { count.value++; };

onMounted(() => { console.log('mounted'); });
</script>
```

---

### 5.4 路由写法对照

| 功能         | Vue Router 3 (Vue 2)                                 | Vue Router 4 (Vue 3)                                    |
| ------------ | ---------------------------------------------------- | ------------------------------------------------------- |
| 创建路由     | `new VueRouter({ mode: 'history' })`                 | `createRouter({ history: createWebHistory() })`         |
| 路由模式     | `mode: 'history'`                                    | `createWebHistory()`                                    |
| 导航         | `this.$router.push()`                                | `useRouter().push()`                                    |
| 获取参数     | `this.$route.params`                                 | `useRoute().params`                                     |
| 全局前置守卫 | `router.beforeEach((to, from, next) => { next(); })` | `router.beforeEach((to, from) => { return '/login'; })` |
| 路由元信息   | `meta: { requiresAuth: true }`                       | `meta: { requiresAuth: true }`（用法相同）              |

**Vue 2 写法**：

```javascript
// router/index.js
export default new VueRouter({
  mode: 'history',
  routes
});
// 组件内
this.$router.push('/home');
```

**Vue 3 写法**：
```javascript
// router/index.js
import { createRouter, createWebHistory } from 'vue-router';
export default createRouter({
  history: createWebHistory(),
  routes
});
// 组件内
import { useRouter } from 'vue-router';
const router = useRouter();
router.push('/home');
```

---

### 5.5 常见迁移踩坑与解决方案

| 问题                           | 原因                                               | 解决方案                                  |
| ------------------------------ | -------------------------------------------------- | ----------------------------------------- |
| `this.$on` / `this.$once` 报错 | Vue 3 移除了全局事件总线                           | 使用 `mitt` 库或 Pinia 替代               |
| `Vue.set` / `Vue.delete` 报错  | Proxy 响应式不再需要                               | 直接赋值即可 `obj.newProp = value`        |
| `filters` 不生效               | Vue 3 移除了过滤器                                 | 改用 `computed` 或全局方法                |
| `$listeners` 不存在            | Vue 3 合并到 `$attrs`                              | 使用 `$attrs` 透传                        |
| `this.$children` 不存在        | 移除了该 API                                       | 使用 `ref` 明确引用子组件                 |
| 异步组件报错                   | 写法变更                                           | 使用 `defineAsyncComponent`               |
| 自定义指令钩子不同             | 钩子函数重命名                                     | 对照新钩子文档修改                        |
| `v-model` 行为不同             | 同一组件支持多个 `v-model`                         | 修改 `v-model` 默认 prop/event 名         |
| 全局 API 无法使用              | `Vue.prototype` 改为 `app.config.globalProperties` | `app.config.globalProperties.$http = ...` |

**示例：异步组件迁移**

Vue 2：
```javascript
const AsyncComp = () => import('./Comp.vue');
```

Vue 3：
```javascript
import { defineAsyncComponent } from 'vue';
const AsyncComp = defineAsyncComponent(() => import('./Comp.vue'));
```

**示例：自定义指令迁移**

Vue 2（钩子 `bind`, `update`, `unbind`）：
```javascript
Vue.directive('focus', {
  bind(el) { el.focus(); }
});
```

Vue 3（钩子 `mounted`, `updated`, `unmounted`）：
```javascript
app.directive('focus', {
  mounted(el) { el.focus(); }
});
```

---

## 模块六：UI 库

> 本模块介绍 Vue 生态中主流的 UI 组件库：Element UI（Vue 2）和 Element Plus（Vue 3）。UI 库提供开箱即用的组件（表单、表格、弹窗、按钮等），极大提升开发效率。以下两个库的 API 高度相似，但存在少量破坏性差异。

---

### 6.1 Element UI（Vue 2）vs Element Plus（Vue 3）

| 对比维度 | Element UI                | Element Plus                      |
| -------- | ------------------------- | --------------------------------- |
| Vue 版本 | 2.x                       | 3.x                               |
| 安装包   | `element-ui`              | `element-plus`                    |
| 样式前缀 | `el-`                     | `el-`（一致）                     |
| 全局注册 | `Vue.use(ElementUI)`      | `app.use(ElementPlus)`            |
| 按需引入 | `babel-plugin-component`  | `unplugin-vue-components`（推荐） |
| 图标库   | 内置字体图标              | 独立包 `@element-plus/icons-vue`  |
| 主要差异 | 部分组件 props/事件名不同 | 详见官方迁移指南                  |

**安装与注册（Vue 2 + Element UI）**：
```bash
npm install element-ui
```

```javascript
import Vue from 'vue';
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';

Vue.use(ElementUI);
```

**安装与注册（Vue 3 + Element Plus）**：
```bash
npm install element-plus
```

```javascript
import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/theme-chalk/index.css';

const app = createApp(App);
app.use(ElementPlus);
```

---

### 6.2 核心组件详解

Element UI / Plus 提供了大量组件，本小节仅列出高频组件，写法在两个版本中基本一致（除少数差异）。

#### 按钮（el-button）

| 属性/事件  | 类型                                          | 作用                           |
| ---------- | --------------------------------------------- | ------------------------------ |
| `type`     | `primary / success / warning / danger / info` | 按钮样式                       |
| `size`     | `large / default / small`                     | 尺寸                           |
| `disabled` | boolean                                       | 是否禁用                       |
| `loading`  | boolean                                       | 是否加载中（自动出现旋转图标） |
| `@click`   | function                                      | 点击事件                       |

```vue
<template>
  <el-button type="primary" @click="handleClick">主按钮</el-button>
  <el-button type="warning" :loading="loading">加载中</el-button>
</template>
```

#### 输入框（el-input）

| 属性/事件      | 类型                                  | 作用                                   |
| -------------- | ------------------------------------- | -------------------------------------- |
| `v-model`      | string / number                       | 双向绑定                               |
| `type`         | `text / password / number / textarea` | 输入类型                               |
| `placeholder`  | string                                | 占位提示                               |
| `clearable`    | boolean                               | 是否显示清空按钮                       |
| `disabled`     | boolean                               | 是否禁用                               |
| `size`         | `large / default / small`             | 尺寸                                   |
| `@keyup.enter` | 回车键触发                            | 登录表单常用（Vue 3 中无需 `.native`） |

```vue
<template>
  <el-input v-model="username" placeholder="用户名" clearable />
  <el-input v-model="password" type="password" show-password @keyup.enter="login" />
</template>
```

#### 数字输入框（el-input-number）

用于输入数字，支持增减按钮控制。

| 属性                | 说明     | 可选值  | 默认值      |
| ------------------- | -------- | ------- | ----------- |
| `v-model`           | 双向绑定 | number  | —           |
| `min`               | 最小值   | number  | `-Infinity` |
| `max`               | 最大值   | number  | `Infinity`  |
| `step`              | 步长     | number  | `1`         |
| `placeholder`       | 占位提示 | string  | —           |
| `controls-position` | 按钮位置 | `right` | —           |
| `disabled`          | 是否禁用 | boolean | `false`     |

```vue
<template>
  <el-input-number
    v-model="count"
    :min="1"
    :max="10"
    :step="1"
    controls-position="right"
    placeholder="请输入数量"
  />
</template>

<script setup>
import { ref } from 'vue';
const count = ref(1);
</script>
```

#### 下拉选择（el-select / el-option）

用于从预设选项中选择，支持单选或多选。

| 组件        | 属性          | 说明                 | 可选值          | 默认值  |
| ----------- | ------------- | -------------------- | --------------- | ------- |
| `el-select` | `v-model`     | 绑定选中值           | string / number | —       |
|             | `multiple`    | 是否多选（值为数组） | boolean         | `false` |
|             | `clearable`   | 是否可清空           | boolean         | `false` |
|             | `filterable`  | 是否可搜索           | boolean         | `false` |
|             | `placeholder` | 占位提示             | string          | —       |
|             | `disabled`    | 是否禁用             | boolean         | `false` |
| `el-option` | `label`       | 选项显示文本         | string          | —       |
|             | `value`       | 选项对应的值         | string / number | —       |
|             | `disabled`    | 是否禁用该选项       | boolean         | `false` |

```vue
<template>
  <!-- 单选 -->
  <el-select v-model="status" placeholder="请选择状态" clearable>
    <el-option label="待审核" value="pending" />
    <el-option label="已完成" value="approved" />
    <el-option label="已驳回" value="rejected" />
  </el-select>

  <!-- 多选 + 可搜索 -->
  <el-select v-model="roles" multiple filterable placeholder="请选择角色">
    <el-option label="管理员" value="admin" />
    <el-option label="审批人" value="approver" />
  </el-select>
</template>

<script setup>
import { ref } from 'vue';

const status = ref('');
const roles = ref([]);
</script>
```

#### 表单（el-form / el-form-item）

| 组件           | 属性/事件 | 作用                    |
| -------------- | --------- | ----------------------- |
| `el-form`      | `:model`  | 表单数据对象            |
|                | `:rules`  | 表单验证规则            |
|                | `ref`     | 用于触发表单验证        |
|                | `size`    | 统一设置表单内组件尺寸  |
| `el-form-item` | `label`   | 标签文字                |
|                | `prop`    | 对应 `model` 中的字段名 |

```vue
<template>
  <el-form :model="form" :rules="rules" ref="formRef">
    <el-form-item label="用户名" prop="username">
      <el-input v-model="form.username" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submitForm">提交</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { reactive, ref } from 'vue';

const form = reactive({ username: '' });
const rules = { username: [{ required: true, message: '请输入用户名', trigger: 'blur' }] };
const formRef = ref(null);

const submitForm = async () => {
  await formRef.value.validate();
  // 提交逻辑
};
</script>
```

#### 表格（el-table / el-table-column）

| 组件              | 属性        | 作用             |
| ----------------- | ----------- | ---------------- |
| `el-table`        | `:data`     | 表格数据数组     |
|                   | `border`    | 是否显示边框     |
|                   | `stripe`    | 是否显示斑马纹   |
|                   | `v-loading` | 是否有加载动画   |
|                   | `size`      | 表格尺寸         |
| `el-table-column` | `prop`      | 对应数据的字段名 |
|                   | `label`     | 列标题           |
|                   | `width`     | 列宽             |

```vue
<template>
  <el-table :data="tableData" border stripe v-loading="loading">
    <el-table-column prop="id" label="ID" width="80" />
    <el-table-column prop="name" label="姓名" />
    <el-table-column label="操作" width="150">
      <!-- #default="{ row, $index }" 是插槽语法，row 为当前行数据，$index 为当前行索引 -->
      <template #default="{ row, $index }">
        <el-button type="text" @click="edit(row)">编辑</el-button>
        <el-button type="text" @click="remove($index)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { ref } from 'vue';
const tableData = ref([{ id: 1, name: '张三' }]);
const loading = ref(false);

const edit = (row) => {
  console.log('编辑', row);
};
const remove = (index) => {
  tableData.value.splice(index, 1);
};
</script>
```

#### 弹窗（el-dialog）

| 属性                          | 作用                                  |
| ----------------------------- | ------------------------------------- |
| `v-model` / `v-model:visible` | 控制显示隐藏（Vue 2: `visible.sync`） |
| `title`                       | 弹窗标题                              |
| `width`                       | 宽度                                  |

```vue
<template>
  <el-button @click="dialogVisible = true">打开弹窗</el-button>
  <el-dialog v-model="dialogVisible" title="提示" width="400px">
    <span>弹窗内容</span>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue';
const dialogVisible = ref(false);
</script>
```

> **Vue 2 vs Vue 3 差异**：Element UI 使用 `visible.sync`，Element Plus 使用 `v-model`（双向绑定）。

#### 分页（el-pagination）

| 属性/事件         | 类型     | 作用                                                   |
| ----------------- | -------- | ------------------------------------------------------ |
| `:current-page`   | number   | 当前页码                                               |
| `:page-size`      | number   | 每页条数                                               |
| `:total`          | number   | 总条数                                                 |
| `:page-sizes`     | array    | 每页条数选项，如 `[10, 20, 50]`                        |
| `layout`          | string   | 布局组件，如 `total, sizes, prev, pager, next, jumper` |
| `@current-change` | function | 页码改变时触发                                         |
| `@size-change`    | function | 每页条数改变时触发                                     |

```vue
<template>
  <el-pagination
    v-model:current-page="currentPage"
    v-model:page-size="pageSize"
    :page-sizes="[10, 20, 50]"
    :total="total"
    layout="total, sizes, prev, pager, next, jumper"
    @current-change="handleCurrentChange"
    @size-change="handleSizeChange"
  />
</template>

<script setup>
import { ref } from 'vue';

const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(100);

const handleCurrentChange = (val) => {
  currentPage.value = val;
  // 请求新数据
};
const handleSizeChange = (val) => {
  pageSize.value = val;
  currentPage.value = 1;
  // 请求数据
};
</script>
```

> **特性说明**：
>
> - `page-sizes` 只能传入数组，例如 `[10, 20, 50]`。用户只能从预设选项中选择每页条数，不能自定义输入。
> - 当 `current-page` 超出实际总页数时，组件会自动修正为最后一页的有效页码，并触发 `current-change` 事件。
> - 通过 `v-model:current-page` 和 `v-model:page-size` 可实现双向绑定，简化状态管理。

#### 菜单（el-menu / el-menu-item）

| 属性             | 作用                               |
| ---------------- | ---------------------------------- |
| `router`         | 启用路由模式，`index` 作为跳转路径 |
| `collapse`       | 是否折叠                           |
| `default-active` | 当前激活菜单的 `index`             |
| `@select`        | 菜单选择事件                       |

动态菜单生成（配合 `v-for` 和动态图标）：

```vue
<template>
  <el-menu router :collapse="isCollapse" default-active="/dashboard">
    <el-menu-item
      v-for="item in menus"
      :key="item.path"
      :index="item.path"
    >
      <el-icon>
        <!-- 使用 :is 动态渲染图标组件，item.icon 为组件名（如 'User'） -->
        <component :is="item.icon" />
      </el-icon>
      <span>{{ item.title }}</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { User, Goods, ShoppingCart } from '@element-plus/icons-vue';

const menus = [
  { path: '/user', title: '用户管理', icon: User },
  { path: '/goods', title: '商品管理', icon: Goods },
  { path: '/order', title: '订单管理', icon: ShoppingCart }
];
</script>
```

> `:is="item.icon"` 是 Vue 动态组件语法，根据变量值渲染对应组件。此处用于动态显示 Element Plus 图标。

#### 选项卡（el-tabs / el-tab-pane）

| 属性/事件    | 作用            |
| ------------ | --------------- |
| `v-model`    | 当前激活的 tab  |
| `@tab-click` | 点击 tab 时触发 |

```vue
<template>
  <el-tabs v-model="activeTab" @tab-click="handleTabClick">
    <el-tab-pane label="用户信息" name="info">内容1</el-tab-pane>
    <el-tab-pane label="修改密码" name="password">内容2</el-tab-pane>
  </el-tabs>
</template>
```

#### 卡片（el-card）

| 插槽     | 作用     |
| -------- | -------- |
| `header` | 卡片头部 |
| 默认插槽 | 卡片内容 |

```vue
<template>
  <el-card>
    <template #header>卡片标题</template>
    <div>卡片内容</div>
  </el-card>
</template>
```

#### 描述列表（el-descriptions / el-descriptions-item）

用于详情页展示只读信息，支持多列布局和边框。

| 组件                   | 属性      | 作用             |
| ---------------------- | --------- | ---------------- |
| `el-descriptions`      | `title`   | 列表标题         |
|                        | `:column` | 每行列数，默认 3 |
|                        | `border`  | 是否显示边框     |
| `el-descriptions-item` | `label`   | 条目标签         |

```vue
<template>
  <el-descriptions title="用户信息" :column="2" border>
    <el-descriptions-item label="姓名">张三</el-descriptions-item>
    <el-descriptions-item label="年龄">25</el-descriptions-item>
    <el-descriptions-item label="地址">北京市朝阳区</el-descriptions-item>
    <el-descriptions-item label="邮箱">zhangsan@example.com</el-descriptions-item>
  </el-descriptions>
</template>
```

#### 标签（el-tag）

用于标记和分类信息（如状态、类型）。

| 属性       | 选项                                | 作用                         |
| ---------- | ----------------------------------- | ---------------------------- |
| `type`     | `success / warning / danger / info` | 标签样式（无 type 时为灰色） |
| `size`     | `large / default / small`           | 尺寸                         |
| `closable` | boolean                             | 是否可关闭（右侧显示 ×）     |
| `@close`   | function                            | 关闭时触发的回调             |

```vue
<template>
  <el-tag type="success">已完成</el-tag>
  <el-tag type="warning">进行中</el-tag>
  <el-tag type="danger">已驳回</el-tag>
  <el-tag type="info">待审核</el-tag>
  <el-tag closable @close="handleClose">可关闭标签</el-tag>
</template>

<script setup>
const handleClose = () => {
  console.log('标签被关闭');
};
</script>
```

#### 徽标（el-badge）

用于在图标或文字右上角显示数字、圆点标记（如顶栏通知的未读数）。

| 属性        | 说明                              | 可选值                                        | 默认值   |
| ----------- | --------------------------------- | --------------------------------------------- | -------- |
| `value`     | 显示的数字或文本                  | string / number                               | —        |
| `max`       | 超过后显示 `{max}+`（仅数字生效） | number                                        | `99`     |
| `hidden`    | 是否隐藏徽标                      | boolean                                       | `false`  |
| `is-dot`    | 只显示小圆点，不显示数字          | boolean                                       | `false`  |
| `show-zero` | `value` 为 0 时是否仍显示         | boolean                                       | `true`   |
| `type`      | 徽标颜色                          | `primary / success / warning / danger / info` | `danger` |

```vue
<template>
  <!-- 数字徽标：未读数为 0 时隐藏 -->
  <el-badge :value="unreadCount" :hidden="unreadCount === 0">
    <el-icon @click="goNotifications"><Bell /></el-icon>
  </el-badge>

  <!-- 圆点徽标 -->
  <el-badge is-dot>
    <el-button>消息</el-button>
  </el-badge>
</template>

<script setup>
import { ref } from 'vue';
import { Bell } from '@element-plus/icons-vue';

const unreadCount = ref(3);
</script>
```

> **注意**：徽标内容通过 `value` 传入，被标记的元素放在默认插槽中（图标、按钮或文字均可）。`show-zero` 默认为 `true`，即 `value` 为 `0` 时仍会显示，未读数场景需配合 `:hidden` 控制。

#### 警告提示（el-alert）

用于在页面中展示操作结果、系统状态等提示信息。

| 属性          | 说明             | 可选值                             | 默认值  |
| ------------- | ---------------- | ---------------------------------- | ------- |
| `title`       | 标题             | string                             | —       |
| `description` | 详细描述         | string                             | —       |
| `type`        | 类型             | `success / warning / info / error` | `info`  |
| `show-icon`   | 是否显示图标     | boolean                            | `false` |
| `closable`    | 是否可关闭       | boolean                            | `true`  |
| `@close`      | 关闭时触发的回调 | function                           | —       |

```vue
<template>
  <el-alert
    title="操作成功"
    description="数据已保存"
    type="success"
    show-icon
    :closable="true"
    @close="handleClose"
  />
  <el-alert
    title="网络异常"
    description="请检查网络连接后重试"
    type="error"
    show-icon
    :closable="false"
  />
</template>

<script setup>
const handleClose = () => {
  console.log('提示框已关闭');
};
</script>
```

#### 单选框（el-radio / el-radio-group）

| 属性      | 作用                                              |
| --------- | ------------------------------------------------- |
| `v-model` | 绑定选中值（单选组写在 `el-radio-group` 上）      |
| `value`   | 该选项对应的值（2.6.0 新增，推荐写法）            |
| `label`   | 选项文字，未写 `value` 时会充当值，但该用法已废弃 |

```vue
<template>
  <el-radio-group v-model="gender">
    <el-radio value="male">男</el-radio>
    <el-radio value="female">女</el-radio>
  </el-radio-group>
</template>
```

> **版本差异**：`value` 为 2.6.0 新增；`label` 充当值的旧用法已废弃、将于 3.0.0 移除，组内只写 `label` 会触发控制台警告。

#### 复选框（el-checkbox / el-checkbox-group）

用于多选场景，支持单个或成组使用。

| 组件                | 属性       | 说明                             | 可选值                             | 默认值  |
| ------------------- | ---------- | -------------------------------- | ---------------------------------- | ------- |
| `el-checkbox`       | `v-model`  | 双向绑定                         | boolean                            | —       |
|                     | `value`    | 选项值（2.6.0 起，group 内生效） | string / number / boolean / object | —       |
|                     | `disabled` | 是否禁用                         | boolean                            | `false` |
| `el-checkbox-group` | `v-model`  | 绑定选中值的数组                 | array                              | —       |

```vue
<template>
  <!-- 单个复选框 -->
  <el-checkbox v-model="isRequired">必填</el-checkbox>

  <!-- 复选框组 -->
  <el-checkbox-group v-model="selectedOptions">
    <el-checkbox value="option1">选项一</el-checkbox>
    <el-checkbox value="option2">选项二</el-checkbox>
    <el-checkbox value="option3">选项三</el-checkbox>
  </el-checkbox-group>
</template>

<script setup>
import { ref } from 'vue';
const isRequired = ref(true);
const selectedOptions = ref(['option1', 'option3']);
</script>
```

> **版本差异**：与单选框同理，`value` 为 2.6.0 新增，`label` 充当值的旧用法已废弃、将于 3.0.0 移除。

#### 开关（el-switch）

用于切换布尔或二元状态（如启用/禁用、开/关）。

| 属性             | 说明           | 可选值                    | 默认值  |
| ---------------- | -------------- | ------------------------- | ------- |
| `v-model`        | 双向绑定       | boolean / number / string | —       |
| `active-value`   | 打开时对应的值 | boolean / number / string | `true`  |
| `inactive-value` | 关闭时对应的值 | boolean / number / string | `false` |
| `disabled`       | 是否禁用       | boolean                   | `false` |

```vue
<template>
  <!-- 默认 true/false 模式 -->
  <el-switch v-model="enabled" />

  <!-- 自定义 1/0 模式 -->
  <el-switch v-model="status" :active-value="1" :inactive-value="0" />
</template>

<script setup>
import { ref } from 'vue';
const enabled = ref(true);
const status = ref(1);
</script>
```

#### 消息提示（ElMessage）

```javascript
import { ElMessage } from 'element-plus'; // Element UI 为 Message

ElMessage.success('操作成功');
ElMessage.error('操作失败');
ElMessage.warning('请先登录');
ElMessage.info('信息已保存');
```

#### 确认弹窗（ElMessageBox）

用于删除、提交等危险操作前询问用户确认。

```javascript
import { ElMessageBox, ElMessage } from 'element-plus';

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该用户吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    // 用户点击确定后执行
    await deleteUser(id);
    ElMessage.success('删除成功');
  } catch {
    ElMessage.info('已取消删除');
  }
};
```

> `ElMessageBox.confirm` 返回 Promise，点击确定进入 `try`，点击取消进入 `catch`。

------

### 6.3 按需引入配置

全量引入会打包所有组件，项目体积较大。按需引入可减小产物体积。

**Vue 2 + Element UI + babel-plugin-component**：

1. 安装插件：
```bash
npm install babel-plugin-component -D
```
2. 配置 `babel.config.js`：
```javascript
module.exports = {
  presets: ['@vue/cli-plugin-babel/preset'],
  plugins: [
    [
      'component',
      {
        libraryName: 'element-ui',
        styleLibraryName: 'theme-chalk'
      }
    ]
  ]
};
```
3. 在组件中按需引入：
```javascript
import { Button, Input } from 'element-ui';
export default {
  components: { ElButton: Button, ElInput: Input }
};
```

**Vue 3 + Element Plus + unplugin-vue-components（推荐）**：

1. 安装插件：
```bash
npm install -D unplugin-vue-components
```
2. 配置 `vite.config.js`：
```javascript
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';

export default {
  plugins: [
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ]
};
```
3. 在模板中直接使用组件，无需手动引入：
```vue
<template>
  <el-button type="primary">按钮</el-button>
</template>
```
插件会自动处理 `import` 和样式。

---

### 6.4 主题定制

**Vue 2 + Element UI**：通过覆盖 Element UI 变量实现。

在 `vue.config.js` 中配置：
```javascript
module.exports = {
  css: {
    loaderOptions: {
      scss: {
        additionalData: `@import "@/styles/element-variables.scss";`
      }
    }
  }
};
```

在 `src/styles/element-variables.scss` 中定义变量：
```scss
$--color-primary: #409eff;
$--color-success: #67c23a;
```

**Vue 3 + Element Plus**：推荐使用 CSS Variables。

```css
:root {
  --el-color-primary: #409eff;
  --el-color-success: #67c23a;
  --el-border-radius-base: 8px;
}
```

或在 `vite.config.js` 中配置 scss 变量覆盖（类似 Vue 2）。

---

## 模块七：工程化实践

> 本模块介绍 Vue 项目中工程化相关的核心实践，包括环境变量配置、接口请求封装（Axios）、开发代理配置以及代码规范（ESLint / Prettier）。这些实践可提升代码质量、团队协作效率和项目可维护性。

---

### 7.1 环境变量配置

不同环境（开发、测试、生产）通常对应不同的接口地址。通过 `.env` 文件管理环境变量，开发与生产共用一套代码。

#### 文件命名规则

| 文件名             | 用途                        |
| ------------------ | --------------------------- |
| `.env.development` | 开发环境（`npm run dev`）   |
| `.env.production`  | 生产环境（`npm run build`） |

#### 变量定义

以 `VITE_` 开头的变量才会暴露给客户端：

```env
# .env.development
# axios 的 baseURL，使用路径前缀 /api
VITE_API_BASE_URL = /api
# Vite 代理转发的目标地址，仅开发环境使用
VITE_PROXY_TARGET = http://localhost:8080

# .env.production（生产环境由 Nginx 处理转发，无需代理变量）
VITE_API_BASE_URL = /api
```

> `baseURL` 推荐设为路径前缀（如 `/api`），而非完整域名：
>
> - 业务代码中 `request.get('/user/login')` 会自动拼接为 `/api/user/login`
> - 开发环境通过 Vite 代理将 `/api` 转发到后端
> - 生产环境通过 Nginx 转发
> - 两套环境均无需修改业务代码
>
> 如果将 `baseURL` 设为完整域名（如 `http://localhost:8080/api`），开发环境要么需要 Vite 代理做 rewrite 去掉域名部分，要么后端配置 CORS 允许跨域，均增加了复杂度。

#### 变量使用

```javascript
// Vite 项目中读取
const baseURL = import.meta.env.VITE_API_BASE_URL;
const title = import.meta.env.VITE_APP_TITLE;

// Vue CLI 项目（变量名必须以 VUE_APP_ 开头）
const baseURL = process.env.VUE_APP_API_BASE_URL;
```

> 不同构建工具读取环境变量的方式不同：Vite 使用 `import.meta.env`，Vue CLI 使用 `process.env`。

---

### 7.2 Axios 封装

Axios 是 Vue 项目中最常用的 HTTP 客户端库，用于调用后端接口。建议在项目中统一封装 Axios 实例，添加请求/响应拦截器，统一处理 Token、错误提示、请求超时等。

#### 安装与基础实例

```bash
npm install axios
```

创建 `src/utils/request.js`：

```javascript
import axios from 'axios';
import { ElMessage } from 'element-plus';  // Element Plus 消息提示

// 创建 Axios 实例
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,  // 基础 URL，从环境变量读取，所有请求的 URL 都会以该值为前缀
  timeout: 10000,  // 请求超时（毫秒）
  headers: { 'Content-Type': 'application/json;charset=utf-8' }
});

// 请求拦截器（在发送请求前执行）
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器（在接收到响应后执行）
service.interceptors.response.use(
  (response) => {
    // 直接返回业务数据，跳过 axios 包装层
    return response.data;
  },
  (error) => {
    const status = error.response?.status;
    let message = '请求失败，请重试';

    if (status === 401) {
      message = '登录已过期，请重新登录';
      // 清空 token 并跳转到登录页
      localStorage.removeItem('token');
      window.location.href = '/login';
    } else if (status === 403) {
      message = '无权限访问';
    } else if (status === 404) {
      message = '请求的资源不存在';
    } else if (status >= 500) {
      message = '服务器错误，请稍后重试';
    } else if (error.response?.data?.msg) {
      message = error.response.data.msg;
    }

    ElMessage.error(message);
    return Promise.reject(error);
  }
);

export default service;
```

#### 请求参数位置

前端调用接口时，参数可以放在三个不同位置：

| 位置         | axios 配置方式                      | 示例 URL                       | 使用场景                     |
| ------------ | ----------------------------------- | ------------------------------ | ---------------------------- |
| 路径变量     | 拼接到 URL 字符串中                 | `/api/users/123`               | 资源 ID、编码等              |
| Query String | **必须**使用 `params` 字段          | `/api/users?page=1&size=10`    | 分页、过滤、搜索             |
| 请求体       | `data` 字段（或直接作为第二个参数） | `/api/users`（数据在 body 中） | 创建、更新（POST/PUT/PATCH） |

> **注意**：
> - `params` 是 axios 的固定字段名，**必须显式写出**，不能省略或改名。
> - `data` 在 GET 方法中不存在；在 POST/PUT/PATCH 中可简写：第二个参数直接传对象即可（无需写 `data:`），但也可显式写出。

```javascript
// 路径变量：模板字符串拼接
axios.get(`/api/users/${userId}`);

// Query String：必须使用 params（显式写出）
axios.get('/api/users', { params: { page: 1, size: 10 } });

// 请求体：直接将对象作为第二个参数
axios.post('/api/users', { name: '张三', age: 18 });
```

#### 模块化接口封装

按业务模块拆分接口，便于维护。创建 `src/api/user.js`：

```javascript
import request from '@/utils/request';

export function login(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data
  });
}

export function getUserInfo() {
  return request({
    url: '/user/info',
    method: 'get'
  });
}

export function updateUser(data) {
  return request({
    url: '/user/update',
    method: 'put',
    data
  });
}
```

#### 在组件中使用

```vue
<script setup>
import { login, getUserInfo } from '@/api/user';
import { ref } from 'vue';

const form = ref({ username: '', password: '' });

const handleLogin = async () => {
  const res = await login(form.value);
  localStorage.setItem('token', res.token);
  // 跳转首页
};
</script>
```

---

### 7.3 代理配置

开发环境中，前端服务（如 `http://localhost:5173`）与后端接口（如 `http://localhost:8080`）存在跨域问题，通过代理将前端请求转发到后端可解决跨域。

#### Vite 代理配置（`vite.config.js`）

```javascript
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.');

  return {
    plugins: [vue()],
    server: {
      proxy: {
        [env.VITE_API_BASE_URL]: {
          target: env.VITE_PROXY_TARGET,
          changeOrigin: true,
        },
      },
    },
  };
});
```

**效果**：例如，前端请求 `/api/user/login` → 代理转发到 `http://localhost:8080/api/user/login`

#### Vue CLI 代理配置（`vue.config.js`）

Vue CLI 使用 `VUE_APP_` 前缀的环境变量：

```javascript
const apiBaseUrl = process.env.VUE_APP_API_BASE_URL || '/api';
const proxyTarget = process.env.VUE_APP_PROXY_TARGET || 'http://localhost:8080';

module.exports = {
  devServer: {
    proxy: {
      [apiBaseUrl]: {
        target: proxyTarget,
        changeOrigin: true,
      },
    },
  },
};
```

---

### 7.4 代码规范（ESLint / Prettier）

代码规范可统一团队代码风格，减少格式差异导致的合并冲突。

#### ESLint（代码质量检查）

Vue CLI 创建项目时可选择预设 ESLint 配置。手动安装：

```bash
npm install -D eslint eslint-plugin-vue @vue/eslint-config-standard
```

**.eslintrc.js** 示例：
```javascript
module.exports = {
  root: true,
  env: { node: true },
  extends: [
    'plugin:vue/vue3-recommended',   // Vue 3 规则
    '@vue/standard'                  // JS 规则
  ],
  rules: {
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off'
  }
};
```

#### Prettier（代码格式化）

```bash
npm install -D prettier eslint-plugin-prettier @vue/eslint-config-prettier
```

**.prettierrc** 示例：
```json
{
  "semi": true,
  "singleQuote": true,
  "trailingComma": "none",
  "printWidth": 100
}
```

**VS Code 配置（保存时自动格式化）**：

`.vscode/settings.json`：
```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  }
}
```

#### 运行检查命令

```bash
# 检查并修复
npm run lint

# 或在 package.json 中配置
"scripts": {
  "lint": "eslint src --ext .js,.vue --fix",
  "format": "prettier --write src/**/*.{js,vue,css}"
}
```

---

## 模块八：构建与部署

> 本模块介绍 Vue 项目的开发调试技巧、生产环境构建优化、Nginx 部署配置以及容器化部署（Docker + Nginx）。掌握这些技能可将 Vue 应用稳定、高效地部署到生产环境。

---

### 8.1 开发调试

#### 开发服务器

Vue CLI 或 Vite 提供的开发服务器支持热更新（HMR），修改代码后页面自动刷新，无需手动重启。

**启动命令**：
```bash
npm run dev   # 或 pnpm dev
```

启动后终端输出本地访问地址（如 `http://localhost:5173`）和网络访问地址（如同局域网其他设备访问）。

#### VS Code 断点调试

1. 在 VS Code 中打开项目
2. 点击左侧“运行和调试”图标（或按 `Ctrl+Shift+D`）
3. 创建 `launch.json`：

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "chrome",
      "request": "launch",
      "name": "Launch Chrome against localhost",
      "url": "http://localhost:5173",
      "webRoot": "${workspaceFolder}/src",
      "breakOnLoad": true,
      "sourceMapPathOverrides": {
        "webpack:///src/*": "${webRoot}/*"
      }
    }
  ]
}
```

4. 在代码中设置断点，按 `F5` 启动调试。

#### 浏览器开发者工具

- **Console**：查看日志、错误信息，执行临时代码
- **Network**：查看接口请求（请求头、参数、响应），定位跨域、404 等问题
- **Elements**：查看 DOM 结构、实时修改样式
- **Vue Devtools**（必装）：查看组件树、组件 data/props/computed 值、Pinia/Vuex 状态

#### 常见问题快速定位

| 现象       | 可能原因                 | 排查方法                                |
| ---------- | ------------------------ | --------------------------------------- |
| 页面空白   | 路由配置错误、组件未注册 | 查看 Console 报错、检查路由守卫         |
| 接口无响应 | 代理配置错误、后端未启动 | 查看 Network 面板请求状态、检查代理配置 |
| 样式不生效 | scoped 限制、类名冲突    | 检查浏览器 Elements 面板、查看样式来源  |
| 热更新失效 | 文件监听问题             | 重启 dev server，检查文件是否在 src 内  |

---

### 8.2 生产构建优化

生产构建时，Vite / Vue CLI 会进行代码压缩、Tree Shaking（移除未使用代码）、代码分割等优化。

#### 构建命令

```bash
npm run build
```

构建产物输出到 `dist/` 目录，包含优化后的 HTML/CSS/JS 及静态资源。

#### Vite 构建优化配置

```javascript
// vite.config.js
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  build: {
    // 输出目录
    outDir: 'dist',
    // 生成 source map（生产环境建议关闭）
    sourcemap: false,
    // chunk 大小警告限制（单位 KB）
    chunkSizeWarningLimit: 500,
    // 代码分割
    rollupOptions: {
      output: {
        manualChunks: {
          // 将 vue 相关库打包到一起
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          // 将 Element Plus 单独打包
          'element-plus': ['element-plus']
        }
      }
    }
  }
});
```

#### 代码分割策略

| 类别     | 示例                   | 分割方式        |
| -------- | ---------------------- | --------------- |
| 框架库   | vue, vue-router, pinia | `manualChunks`  |
| UI 库    | element-plus, vant     | `manualChunks`  |
| 业务组件 | 路由懒加载             | 动态 `import()` |

**路由懒加载**（自动分割）：
```javascript
const routes = [
  {
    path: '/home',
    component: () => import('@/views/Home.vue')  // 独立 chunk
  }
];
```

#### 构建产物分析

可视化分析打包体积，找出优化点：

```bash
npm run build -- --report   # Vue CLI
```

Vite 中使用 `rollup-plugin-visualizer`：
```bash
npm install -D rollup-plugin-visualizer
```

```javascript
import { visualizer } from 'rollup-plugin-visualizer';

export default {
  plugins: [
    vue(),
    visualizer({ open: true })
  ]
};
```

---

### 8.3 Nginx 部署配置

Vue 打包后的 `dist/` 目录是纯静态文件，可使用 Nginx 托管。

#### 基础配置（`/etc/nginx/conf.d/vue.conf`）

```nginx
server {
    listen 80;
    server_name example.com;   # 替换为你的域名或 IP
    root /var/www/vue-project/dist;   # dist 目录路径
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;   # 支持 history 模式路由
    }

    # 代理 API 请求到后端（可选）
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

#### history 模式刷新 404 处理

Vue 使用 `history` 路由模式时，刷新非首页路径会 404，因为 Nginx 找不到对应的静态文件。解决方案：将所有请求重定向到 `index.html`，由前端路由接管。

上述配置中的 `try_files $uri $uri/ /index.html;` 即实现了该功能。

#### 启用 gzip 压缩（提升性能）

```nginx
gzip on;
gzip_types text/plain text/css application/json application/javascript text/xml application/xml text/javascript;
gzip_min_length 1024;
```

#### 缓存策略

```nginx
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

#### 部署步骤

1. 构建项目：`npm run build`
2. 将 `dist/` 目录上传到服务器（如 `/var/www/vue-project/dist`）
3. 安装并启动 Nginx
4. 将上述配置添加到 Nginx 并重启：`nginx -s reload`

---

### 8.4 容器化部署（Docker + Nginx）

使用 Docker 将 Vue 应用打包成镜像，便于统一部署环境和扩容。

#### Dockerfile

创建项目根目录下的 `Dockerfile`：

```dockerfile
# 构建阶段（使用 Node.js 镜像编译）
FROM node:18-alpine as builder

WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 生产阶段（使用 Nginx 镜像托管静态文件）
FROM nginx:stable-alpine as production

# 将构建产物复制到 Nginx 的 html 目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制自定义 Nginx 配置（可选）
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

#### 自定义 Nginx 配置（`nginx.conf`）

```nginx
server {
    listen 80;
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}
```

#### 构建与运行

```bash
# 构建镜像（注意最后的 . 表示当前目录）
docker build -t vue-app .

# 运行容器
docker run -d -p 8080:80 --name vue-app vue-app
```

访问 `http://localhost:8080` 即可看到应用。

#### docker-compose（推荐）

创建 `docker-compose.yml`：

```yaml
version: '3'
services:
  vue-app:
    build: .
    ports:
      - "8080:80"
    restart: always
```

启动：
```bash
docker-compose up -d
```

#### 多阶段构建优势

- 构建阶段包含 Node.js 环境，用于编译项目
- 生产阶段仅包含 Nginx 和静态文件，镜像体积小（约 20-30 MB）
- 最终镜像不含源代码和 node_modules，更安全
