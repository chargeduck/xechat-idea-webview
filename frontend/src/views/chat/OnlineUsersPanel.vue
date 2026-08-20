<template>
  <div class="online-users-panel">
    <div class="panel-header">
      <span>在线用户</span>
      <el-tag size="small" type="info">{{ onlineStore.count }} 人</el-tag>
    </div>
    <div class="user-list" v-if="onlineStore.users.length">
      <div v-for="u in onlineStore.users" :key="u.username" class="user-item">
        <span class="user-status-icon">{{ statusIcon(u.status) }}</span>
        <span class="user-name">{{ u.username }}</span>
        <span class="user-region" v-if="u.shortRegion">[{{ u.shortRegion }}]</span>
        <span class="user-platform">{{ platformIcon(u.platform) }}</span>
      </div>
    </div>
    <div v-else class="empty-hint">暂无在线用户</div>
  </div>
</template>

<script setup>
import { useOnlineUsersStore } from '@/stores/onlineUsersStore'

const onlineStore = useOnlineUsersStore()

function statusIcon(s) {
  switch (s) {
    case 'FISHING': return '🎣'
    case 'WORKING': return '💼'
    case 'PLAYING': return '🎮'
    default: return '🟢'
  }
}

function platformIcon(p) {
  switch (p) {
    case 'IDEA': return '☕'
    case 'WEB':
    case 'VSCODE': return '🌐'
    default: return ''
  }
}
</script>

<style scoped>
.online-users-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  padding: 12px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-color);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.user-list {
  flex: 1;
  overflow-y: auto;
  padding-top: 8px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 4px;
  font-size: 13px;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color-light, rgba(128, 128, 128, 0.1));
}

.user-status-icon {
  font-size: 14px;
}

.user-name {
  font-weight: 500;
}

.user-region {
  color: var(--text-secondary);
  font-size: 12px;
}

.user-platform {
  margin-left: auto;
  font-size: 13px;
}

.empty-hint {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
