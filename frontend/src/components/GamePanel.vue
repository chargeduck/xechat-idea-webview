<template>
  <div class="game-panel">
    <div class="panel-header">游戏</div>
    <div class="game-grid">
      <div
        v-for="(game, idx) in state.games"
        :key="idx"
        class="game-card"
        :class="{ active: state.gamePlaying && state.currentGameIndex === idx }"
        @click="startGame(idx)"
      >
        <div class="game-icon">{{ game.name?.charAt(0) || 'G' }}</div>
        <div class="game-name">{{ game.name }}</div>
        <div v-if="game.desc" class="game-desc">{{ game.desc }}</div>
      </div>
      <div v-if="state.games.length === 0" class="empty-hint">暂无可用游戏</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { state, loadGames } from '../store.js'

onMounted(() => loadGames())

function startGame(idx) {
  window.xechat && window.xechat.gameAction(idx, 'start')
}
</script>

<style scoped>
.game-panel {
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

.game-grid {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
  align-content: start;
}

.game-card {
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
    border-color: var(--success-color);
    background: var(--bg-tertiary);
  }
}

.game-icon {
  width: 40px;
  height: 40px;
  margin: 0 auto 8px;
  border-radius: 8px;
  background: var(--success-color);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}

.game-name {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 2px;
}

.game-desc {
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
