<template>
  <div class="chat-panel">
    <div class="message-list" ref="messageListRef">
      <div v-for="(msg, idx) in chatStore.messages" :key="idx" class="message-item">
        <MarkdownMessage :content="msg.text" />
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
          :autosize="{ minRows: 1, maxRows: 4 }"
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
import { useHelpStore } from '../stores/helpStore'
import { useServerStore } from '../stores/serverStore'
import { setChatStoreBridge } from '../store.js'
import { transport } from '../transport/transport-manager.js'
import MarkdownMessage from './MarkdownMessage.vue'
import StyleSelector from './StyleSelector.vue'

const chatStore = useChatStore()
const helpStore = useHelpStore()
const serverStore = useServerStore()

const messageListRef = ref(null)
const inputRef = ref(null)
const inputText = ref('')

// 消息列表自动滚动到底部
watch(() => chatStore.messages.length, (newLen, oldLen) => {
  console.log('[ChatPanel] messages.length 变化: ' + oldLen + ' -> ' + newLen)
  nextTick(() => {
    const el = messageListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
})

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

  // 检查是否是 JSBridge 模式（Java 端 handle login）
  var isJSBridge = (window.xechat && typeof window.xechat.getState === 'function'
    && window.xechat.getState() !== '{}')

  if (isJSBridge) {
    // JSBridge：原样转发给 Java，Java 端 LoginCommandHandler 全权处理
    console.log('[ChatPanel] #login → JSBridge 转发: ' + rawText)
    window.xechat.execCommand(rawText)
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
      uuid: localStorage.getItem('xechat_uuid') || ('web-' + Math.random().toString(36).substring(2) + Date.now().toString(36)),
      pluginVersion: '',
      reconnected: false
    }
  }

  try {
    await transport.loginToServer(host, port, loginPayload)
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
    } else if (text.startsWith('#')) {
      console.log('[ChatPanel] → execCommand: ' + text)
      window.xechat.execCommand(text)
    } else {
      console.log('[ChatPanel] → sendMessage: ' + text)
      window.xechat.sendMessage(text)
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
  overflow-y: auto;
  padding: 12px 16px;
}

.message-item {
  padding: 4px 0;
  border-bottom: 1px solid var(--border-color);
}

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
