<template>
  <div class="chat-panel">
    <div class="message-list" ref="messageListRef">
      <div v-for="(msg, idx) in chatStore.messages" :key="idx" class="message-item">
        <!-- 系统消息：VitePress 风格彩色容器块 -->
        <div v-if="msg.type === 'system'" :class="['system-msg-box', classifySystemMsg(msg.text)]">
          <span class="system-msg-icon">{{ systemMsgIcon(msg.text) }}</span>
          <div class="system-msg-body">
            <div class="system-msg-title">{{ systemMsgTitle(msg.text) }}</div>
            <div class="system-msg-text">{{ stripHtml(msg.text) }}</div>
          </div>
        </div>
        <!-- 普通消息：Markdown 渲染 -->
        <MarkdownMessage v-else :content="msg.text" />
      </div>
      <div v-if="chatStore.isEmpty" class="empty-hint">
        暂无消息
      </div>
    </div>

    <!-- 底部：命令行输入区域 -->
    <div class="input-area">
      <div class="input-row">
        <StyleSelector @select="onStyleSelect" />
        <el-input
          ref="inputRef"
          v-model="inputText"
          placeholder="输入命令或消息，Enter 发送 / Shift+Enter 换行"
          type="textarea"
          rows="3"
          @keydown.enter="onEnter"
          class="chat-input"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted } from 'vue'
import { useChatStore } from '../stores/chatStore'
import { useDeviceStore } from '../stores/deviceStore'
import { useHelpStore } from '../stores/helpStore'
import { useServerStore } from '../stores/serverStore'
import { setChatStoreBridge, state } from '../store.js'
import { transport } from '../transport/transport-manager.js'
import MarkdownMessage from './MarkdownMessage.vue'
import StyleSelector from './StyleSelector.vue'

const chatStore = useChatStore()
const deviceStore = useDeviceStore()
const helpStore = useHelpStore()
const serverStore = useServerStore()

const messageListRef = ref(null)
const inputRef = ref(null)
const inputText = ref('')

// 消息列表自动滚动到底部
watch(() => chatStore.messages.length, (newLen, oldLen) => {
  console.log('[ChatPanel] messages.length 变化: ' + oldLen + ' -> ' + newLen)
  nextTick(() => {
    // requestAnimationFrame 延迟到 JCEF 渲染管线完成后执行，避免同步 reflow 触发原生错误
    requestAnimationFrame(() => {
      const el = messageListRef.value
      if (el) el.scrollTop = el.scrollHeight
    })
  })
})

/**
 * 剥离 HTML 标签，获取纯文本
 */
function stripHtml(text) {
  return text.replace(/<[^>]+>/g, '')
}

/**
 * 根据系统消息内容分类，返回 CSS class 名
 * - error: 错误/警告类（版本过低、错误、失败、异常、超时、断开、拒绝）
 * - notice: 进入/离开通知类（进入、离开、加入、退出、上线、下线）
 * - info:  普通系统消息（公告、格言等）
 */
function classifySystemMsg(text) {
  const plain = stripHtml(text)
  if (/版本过低|错误|失败|异常|超时|断开|拒绝|禁止|无效|过期/.test(plain)) return 'error'
  if (/进入|离开|加入|退出|上线|下线|来到了|离开了/.test(plain)) return 'notice'
  return 'info'
}

/** 系统消息类型图标 */
function systemMsgIcon(text) {
  const cls = classifySystemMsg(text)
  if (cls === 'error') return '\u26A0'   // ⚠
  if (cls === 'notice') return '\u2192'  // →
  return '\u2139'                         // ℹ
}

/** 系统消息标题 */
function systemMsgTitle(text) {
  const cls = classifySystemMsg(text)
  if (cls === 'error') return '系统错误'
  if (cls === 'notice') return '系统通知'
  return '系统提示'
}

// 文字样式选中：将当前输入文本包裹为样式标签
function onStyleSelect(tag) {
  const text = inputText.value.trim()
  if (!text) {
    inputRef.value?.focus()
    return
  }
  inputText.value = tag.replace('{text}', text)
  inputRef.value?.focus()
}

/**
 * 处理 #login 命令。
 * JSBridge 模式：转发给 Java LoginCommandHandler（NettyClient）。
 * WebSocket 模式：解析服务器参数，重连 WS 并发送 LOGIN。
 */
async function handleLogin(rawText) {
  var parts = rawText.trim().split(/\s+/)
  var username = ''
  var host = ''
  var port = ''
  var sIdx = -1

  for (var i = 1; i < parts.length; i++) {
    var p = parts[i]
    if (p === '-s' && i + 1 < parts.length) { sIdx = parseInt(parts[++i]) }
    else if (p === '-h' && i + 1 < parts.length) { host = parts[++i] }
    else if (p === '-p' && i + 1 < parts.length) { port = parts[++i] }
    else if (p.charAt(0) !== '-') { username = p }
  }

  // 未指定服务器参数时默认连第 0 个
  if (sIdx === -1 && !host && !port) {
    sIdx = 0
  }

  // 检查是否是 JSBridge 模式（Java 端 handle login）
  var isJSBridge = (window.xechat && typeof window.xechat.getState === 'function'
    && window.xechat.getState() !== '{}')

  if (isJSBridge) {
    // JSBridge：补上 -s 0 后转发给 Java
    var cmd = username ? '#login ' + username + ' -s ' + sIdx : rawText + ' -s ' + sIdx
    console.log('[ChatPanel] #login → JSBridge 转发: ' + cmd)
    window.xechat.execCommand(cmd)
    return
  }

  // WebSocket 模式：前端解析服务器
  if (sIdx >= 0) {
    if (serverStore.servers.length === 0) {
      chatStore.addMessage('正在获取鱼塘列表...')
      try {
        await serverStore.fetchServers(false)
      } catch (e) {
        chatStore.addMessage('获取鱼塘列表失败: ' + e.message)
        return
      }
    }
    var srv = serverStore.servers[sIdx]
    if (!srv) {
      chatStore.addMessage('无效的服务器编号: ' + sIdx + '，有效范围 0~' + (serverStore.servers.length - 1))
      return
    }
    // Web 端：检查 WS 连通性
    if (srv.wsAlive === false) {
      chatStore.addMessage('服务器 ' + (srv.name || srv.ip) + ' 不支持 WebSocket，请使用 IDEA 插件登录')
      return
    }
    host = srv.ip
    port = String(srv.port)
  }

  if (!host || !port) {
    chatStore.addMessage('请指定服务器： #login [-s 编号] 或 #login -h IP -p 端口 [昵称]')
    return
  }

  if (!username) username = 'User-' + Date.now().toString(36)

  chatStore.addMessage('正在连接服务器 ' + host + ':' + port + ' ...')

  var loginPayload = {
    action: 'LOGIN',
    body: {
      username: username,
      status: 'FISHING',
      platform: 'WEB',
      uuid: deviceStore.uuid,
      pluginVersion: (srv && srv.version) || '',
      reconnected: false
    }
  }

  try {
    await transport.loginToServer(host, port, loginPayload)
    state.online = true
    state.username = username
    chatStore.addMessage('已连接到 ' + host + ':' + port + '，登录中...')
  } catch (e) {
    chatStore.addMessage('连接服务器失败: ' + (e.message || '未知错误'))
  }
}

// 发送逻辑
async function send() {
  const text = inputText.value.trim()
  if (!text) return

  console.log('[ChatPanel] send() 触发, text=' + text.substring(0, 50))

  try {
    if (text === '#help') {
      chatStore.addMessage(helpStore.helpText)
    } else if (text === '#clean') {
      chatStore.clear()
    } else if (text.startsWith('#showServer')) {
      chatStore.addMessage('正在获取鱼塘列表...')
      const forceRefresh = text.includes('-c')
      try {
        await serverStore.fetchServers(forceRefresh)
        chatStore.addMessage(serverStore.serverTable)
      } catch (e) {
        chatStore.addMessage('鱼塘列表获取失败: ' + e.message)
      }
    } else if (text.startsWith('#login')) {
      await handleLogin(text)
    } else if (text.startsWith('#loging')) {
      // 简化登录：#loging {nickname} → #login {nickname} -s 0
      var nickname = text.slice('#loging'.length).trim()
      if (!nickname) {
        chatStore.addMessage('用法： #loging {昵称}，例如 #loging test')
      } else {
        await handleLogin('#login ' + nickname + ' -s 0')
      }
    } else if (text.startsWith('#')) {
      console.log('[ChatPanel] → execCommand: ' + text)
      if (transport.mode === 'jsbridge') {
        window.xechat.execCommand(text)
      } else {
        transport.execCommand(text)
      }
    } else {
      console.log('[ChatPanel] → sendMessage: ' + text)
      if (transport.mode === 'jsbridge') {
        window.xechat.sendMessage(text)
      } else {
        transport.sendMessage(text)
      }
    }
    console.log('[ChatPanel] send() 完成')
  } catch (e) {
    console.error('[ChatPanel] 发送失败', e)
  }

  inputText.value = ''
}

// Enter 发送，Shift+Enter 换行
function onEnter(e) {
  if (e.shiftKey) {
    return
  }
  e.preventDefault()
  send()
}

// 键盘焦点默认在输入框; 无消息时自动注入 HELP 提示; 建立 JSBridge→chatStore 桥接
onMounted(() => {
  console.log('[ChatPanel] 组件挂载, chatStore.messages.length=' + chatStore.messages.length)
  setChatStoreBridge(chatStore)
  // 启动时静默拉取服务器列表（#showServer -c），写入 Pinia 供后续 #login / #loging 使用
  serverStore.fetchServers(true).catch(function(e) {
    console.warn('[ChatPanel] 初始化获取鱼塘列表失败:', e)
  })
  if (chatStore.isEmpty) {
    chatStore.addMessage(helpStore.helpText)
  }
  inputRef.value?.focus()
})
</script>

<style scoped>
.chat-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* ===== 消息列表 ===== */
.message-list {
  flex: 1;
  overflow-y: scroll;
  padding: 12px 16px;
  contain: layout style;
}

.message-item {
  padding: 4px 0;
  border-bottom: 1px solid var(--border-color);
}

/* ===== 系统消息：VitePress 风格彩色容器块 ===== */
.system-msg-box {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 4px 0;
  padding: 6px 12px 6px 10px;
  border-left: 4px solid;
  border-radius: 0 4px 4px 0;
  font-size: 12px;
  line-height: 1.5;
}

.system-msg-box .system-msg-icon {
  flex-shrink: 0;
  font-size: 13px;
  margin-top: 2px;
}

.system-msg-box .system-msg-body {
  flex: 1;
  min-width: 0;
}

.system-msg-box .system-msg-title {
  font-weight: 600;
  margin-bottom: 2px;
  font-size: 12px;
}

.system-msg-box .system-msg-text {
  word-break: break-word;
}

.system-msg-box.error {
  border-color: var(--danger-color);
  background: rgba(244, 71, 71, 0.08);
}
.system-msg-box.error .system-msg-icon { color: var(--danger-color); }
.system-msg-box.error .system-msg-title { color: var(--danger-color); }

.system-msg-box.notice {
  border-color: var(--success-color);
  background: rgba(78, 201, 176, 0.08);
}
.system-msg-box.notice .system-msg-icon { color: var(--success-color); }
.system-msg-box.notice .system-msg-title { color: var(--success-color); }

.system-msg-box.info {
  border-color: var(--accent-color);
  background: rgba(0, 122, 204, 0.08);
}
.system-msg-box.info .system-msg-icon { color: var(--accent-color); }
.system-msg-box.info .system-msg-title { color: var(--accent-color); }

/* 浅色主题下系统消息背景微调 */
:root.light .system-msg-box.error  { background: rgba(244, 71, 71, 0.06); }
:root.light .system-msg-box.notice { background: rgba(78, 201, 176, 0.06); }
:root.light .system-msg-box.info   { background: rgba(0, 120, 212, 0.06); }

.message-item:last-child {
  border-bottom: none;
}

.empty-hint {
  color: var(--text-muted);
  text-align: center;
  padding: 40px 0;
  font-size: 13px;
}

/* ===== 底部输入区域 ===== */
.input-area {
  border-top: 1px solid var(--border-color);
  padding: 8px 12px;
  background: var(--bg-secondary);
}

.input-row {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.input-row .chat-input {
  flex: 1;
}
</style>
