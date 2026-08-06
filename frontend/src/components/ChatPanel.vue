<template>
  <div class="chat-panel">
    <div class="message-list" ref="messageListRef">
      <div v-for="(msg, idx) in state.messages" :key="idx" class="message-item">
        <MarkdownMessage :content="msg.text" />
      </div>
      <div v-if="state.messages.length === 0" class="empty-hint">
        暂无消息
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { state } from '../store.js'
import MarkdownMessage from './MarkdownMessage.vue'

const messageListRef = ref(null)

watch(() => state.messages.length, () => {
  nextTick(() => {
    const el = messageListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
})
</script>

<style scoped>
.chat-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

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
</style>
