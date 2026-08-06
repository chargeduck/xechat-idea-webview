<template>
  <div class="room-panel">
    <div class="panel-header">
      {{ state.roomGameName || '游戏房间' }}
      <span class="room-id">#{{ state.roomId }}</span>
    </div>

    <div class="room-body">
      <div class="player-list">
        <div
          v-for="player in state.roomPlayers"
          :key="player.id"
          class="player-item"
        >
          <span class="player-name">{{ player.username }}</span>
          <el-tag
            :type="player.readied ? 'success' : 'info'"
            size="small"
          >
            {{ player.readied ? '已准备' : '未准备' }}
          </el-tag>
        </div>
        <div v-if="state.roomPlayers.length === 0" class="empty-hint">
          等待玩家加入...
        </div>
      </div>

      <div class="room-actions">
        <el-button
          :type="state.roomReady ? 'warning' : 'success'"
          size="small"
          @click="state.roomReady ? unready() : ready()"
        >
          {{ state.roomReady ? '取消准备' : '准备' }}
        </el-button>
        <el-button
          type="danger"
          size="small"
          plain
          @click="leaveRoom()"
        >
          离开房间
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { state } from '../store.js'

function ready() {
  window.xechat && window.xechat.roomReady()
}

function unready() {
  window.xechat && window.xechat.roomUnready()
}

function leaveRoom() {
  window.xechat && window.xechat.roomLeave()
}
</script>

<style scoped>
.room-panel {
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
  display: flex;
  align-items: center;
  gap: 8px;
}

.room-id {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 400;
}

.room-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
}

.player-list {
  flex: 1;
  overflow-y: auto;
}

.player-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 6px;
  background: var(--bg-secondary);
  margin-bottom: 6px;
}

.player-name {
  font-size: 13px;
}

.room-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.empty-hint {
  color: var(--text-muted);
  text-align: center;
  padding: 40px 0;
  font-size: 13px;
}
</style>
