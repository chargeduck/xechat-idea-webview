<template>
  <div class="tool-panel">
    <div class="panel-header">工具</div>
    <div class="tool-grid">
      <div
        v-for="(tool, idx) in state.tools"
        :key="idx"
        class="tool-card"
        :class="{ active: state.toolOpen && state.currentTool === idx }"
        @click="openTool(idx)"
      >
        <div class="tool-icon">{{ tool.name?.charAt(0) || 'T' }}</div>
        <div class="tool-name">{{ tool.name }}</div>
        <div v-if="tool.desc" class="tool-desc">{{ tool.desc }}</div>
      </div>
      <div v-if="state.tools.length === 0" class="empty-hint">暂无可用工具</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { state, loadTools } from '../store.js'

onMounted(() => loadTools())

function openTool(idx) {
  window.xechat && window.xechat.openTool(idx)
}
</script>

<style scoped>
.tool-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 600;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.tool-grid {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
  align-content: start;
}

.tool-card {
  padding: 14px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-secondary);
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;

  &:hover {
    border-color: var(--accent-color);
    background: var(--bg-tertiary);
  }

  &.active {
    border-color: var(--accent-color);
    background: var(--bg-tertiary);
  }
}

.tool-icon {
  width: 40px;
  height: 40px;
  margin: 0 auto 8px;
  border-radius: 8px;
  background: var(--accent-color);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}

.tool-name {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 2px;
}

.tool-desc {
  font-size: 11px;
  color: var(--text-muted);
  line-height: 1.4;
}

.empty-hint {
  grid-column: 1 / -1;
  color: var(--text-muted);
  text-align: center;
  padding: 40px 0;
  font-size: 13px;
}
</style>
