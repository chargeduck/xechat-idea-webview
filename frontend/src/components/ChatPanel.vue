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
      <el-input
        ref="inputRef"
        v-model="inputText"
        placeholder="输入命令或消息，Enter 发送 / Shift+Enter 换行"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 4 }"
        @keydown.enter="onEnter"
      >
        <template #prefix>
          <span class="cmd-prefix">#</span>
        </template>
      </el-input>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted } from 'vue'
import { useChatStore } from '../stores/chatStore'
import { useHelpStore } from '../stores/helpStore'
import { setChatStoreBridge } from '../store.js'
import MarkdownMessage from './MarkdownMessage.vue'

const chatStore = useChatStore()
const helpStore = useHelpStore()

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

// 发送逻辑
function send() {
  const text = inputText.value.trim()
  if (!text) return

  console.log('[ChatPanel] send() 触发, text=' + text.substring(0, 50) + ', xechat=' + (typeof window.xechat))

  try {
    if (text.startsWith('#')) {
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
    chatStore.addMessages(helpStore.helpTexts)
    console.log('[ChatPanel] 已从 helpStore 注入 HELP 提示，共 ' + helpStore.helpTexts.length + ' 条')
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

.cmd-prefix {
  color: var(--text-muted);
  font-size: 14px;
  user-select: none;
}
</style>
