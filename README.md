---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: 71dfd8187db27d1ff1c323fdb6b34ebf_4679f16d93ee11f1bcfc525400e6dd8f
    ReservedCode1: QSquWjjLeYcwI9CrmweEHhrjdhB61rvC6POFmoC+Pfno4H8kAlC4xklZbeYWDik8YhcUZ0YE3qCzkJdldpnWzLP5ko09dknFL4tpIrEA1PwqjXJJgNdCSIpecRBrNcHQb6JgGdKDyMCKEQPcgUQfkft14xe9AgiyTsuj5zsfHNy7nR9JTO4Kv0qnJd8=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: 71dfd8187db27d1ff1c323fdb6b34ebf_4679f16d93ee11f1bcfc525400e6dd8f
    ReservedCode2: QSquWjjLeYcwI9CrmweEHhrjdhB61rvC6POFmoC+Pfno4H8kAlC4xklZbeYWDik8YhcUZ0YE3qCzkJdldpnWzLP5ko09dknFL4tpIrEA1PwqjXJJgNdCSIpecRBrNcHQb6JgGdKDyMCKEQPcgUQfkft14xe9AgiyTsuj5zsfHNy7nR9JTO4Kv0qnJd8=
---

# xechat-idea-webview

基于 Vue 3 + Element Plus 的 IntelliJ IDEA 插件前端，通过 JCEF 嵌入式浏览器渲染 UI，支持 JSBridge 和 WebSocket 双传输模式。

## 项目结构

```
xechat-idea-webview/
├── frontend/                          # Vue 3 前端项目（Vite 构建）
│   ├── src/
│   │   ├── main.js                    # 应用入口
│   │   ├── api.js                     # JSBridge / WebSocket 传输封装
│   │   ├── store.js                   # 全局事件总线
│   │   ├── utils/debug.js             # 调试浮层（Ctrl+Shift+D）
│   │   ├── components/                # Vue 组件
│   │   ├── styles/                    # 全局样式
│   │   ├── games/                     # 内置游戏渲染器
│   │   └── tools/                     # 内置工具渲染器
│   ├── vite.config.js                 # Vite 构建配置（IIFE + file:// 兼容）
│   └── package.json
└── xechat-webview-plugin/             # IntelliJ IDEA 插件（Java）
    └── src/main/java/cn/xeblog/plugin/
        ├── webview/WebViewPanel.java   # JCEF WebView 主面板
        ├── webview/bridge/JSBridge.java # Java ↔ JS 双向通信桥
        └── factory/MainWindowFactory.java # 工具窗口工厂
```

## 开发流程

### 前端开发

```bash
cd frontend
npm install
npm run dev          # 启动 Vite 开发服务器，浏览器访问 http://localhost:5173
```

### 构建并内联到插件

```bash
cd frontend
npm run build        # Vite 构建 + 内联脚本到插件 resources/web/index.html
```

构建产物为单文件 `index.html`（JS/CSS 全部内联），输出到 `xechat-webview-plugin/src/main/resources/web/`。

### 插件构建与调试

在 IDEA 中打开项目，执行 Gradle 任务：

```bash
./gradlew buildPlugin       # 构建插件 JAR
./gradlew runIde            # 启动沙盒 IDEA 调试插件
```

---

## 调试方法

### 方法一：JCEF DevTools（推荐，完整 Chrome DevTools）

#### 方式 A：代码自动弹窗（已内置）

插件启动时 `WebViewPanel.init()` 会自动调用 `browser.openDevtools()` 弹出 DevTools 窗口。如果弹出失败（控制台提示 `JCEF DevTools 打开失败`），使用方式 B 或 C。

#### 方式 B：IDEA Registry 右键开启

1. `Help` → `Find Action` → 输入 `Registry`
2. 搜索 `ide.browser.jcef.contextMenu.devTools.enabled`
3. 勾选启用
4. 重启 IDEA / RunIde
5. 在 JCEF 页面内**右键** → `Open DevTools`

开启一次后永久生效，无需每次手动操作。

#### 方式 C：Registry 远程调试端口

1. `Help` → `Find Action` → 输入 `Registry`
2. 搜索 `ide.browser.jcef.debug.port`
3. 设为 `9222`（或其他可用端口）
4. 重启后浏览器访问 `http://localhost:9222`
5. 在远程调试页面中选择对应的 JCEF 页面

### 方法二：vConsole（页面内轻量调试面板）

已内置到 `main.js`，**仅在 `file://` 协议（JCEF 环境）下自动启用**。浏览器开发模式下不会加载，不影响日常开发。

使用方式：RunIde 启动后，插件页面右下角会出现绿色 `vConsole` 按钮，点击展开调试面板：

| 标签 | 功能 |
|------|------|
| Console | 查看所有 `console.log/error/warn`，包括 `[Vue errorHandler]` 捕获的错误 |
| Network | 查看网络请求（fetch / XHR） |
| Element | 查看 DOM 结构和样式 |
| Storage | 查看 localStorage / sessionStorage / Cookie |

### 调试工具对比

| 特性 | JCEF DevTools | vConsole |
|------|:---:|:---:|
| Console | 完整 | 完整 |
| Network | 完整 | 完整 |
| Elements | 完整 | 基础 |
| Sources / 断点调试 | 完整 | 不支持 |
| Performance | 完整 | 不支持 |
| 需额外配置 | 不需要（代码自动弹窗） | 不需要（自动启用） |
| 适用场景 | 深度排查（堆栈追踪、断点、性能分析） | 快速查看日志和网络请求 |

### 调试浮层（Ctrl+Shift+D）

前端 `utils/debug.js` 提供了页面内调试浮层，按 `Ctrl+Shift+D` 打开，可查看：
- 历史 console 日志
- 错误快照（DOM 操作记录、异步回调栈）
- 实时日志流

调试浮层在 JCEF 和浏览器环境下均可使用。

---

## 技术要点

- **构建格式**：Vite 配置 `format: 'iife'`，解决 `file://` 协议下 ES module 的 CORS 限制
- **路径基准**：`base: './'`，确保 JCEF `file://` 加载时资源路径正确
- **JS Bridge**：JCEF 模式下通过 `window.xechat.httpGet`（Java 代理）发起外部请求，绕过 CORS
- **传输模式**：`api.js` 支持 `auto` / `jsbridge` / `websocket` 三种模式，自动检测运行环境
*（内容由AI生成，仅供参考）*
