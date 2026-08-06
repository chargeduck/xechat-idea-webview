<template>
  <el-container class="app-container">
    <el-aside width="48px" class="app-sidebar">
      <div class="nav-buttons">
        <el-tooltip content="聊天" placement="right" :show-after="500">
          <div
            class="nav-btn"
            :class="{ active: currentRoute === 'chat' }"
            @click="state.currentRoute = 'chat'"
          >
            <el-icon :size="20"><ChatDotRound /></el-icon>
          </div>
        </el-tooltip>
        <el-tooltip content="工具" placement="right" :show-after="500">
          <div
            class="nav-btn"
            :class="{ active: currentRoute === 'tools' }"
            @click="state.currentRoute = 'tools'"
          >
            <el-icon :size="20"><Tools /></el-icon>
          </div>
        </el-tooltip>
        <el-tooltip content="游戏" placement="right" :show-after="500">
          <div
            class="nav-btn"
            :class="{ active: currentRoute === 'games' || currentRoute === 'room' }"
            @click="state.currentRoute = 'games'"
          >
            <el-icon :size="20"><GameController /></el-icon>
          </div>
        </el-tooltip>
      </div>
      <div class="nav-bottom">
        <el-tooltip content="设置" placement="right" :show-after="500">
          <div class="nav-btn" @click="state.settingsOpen = !state.settingsOpen">
            <el-icon :size="18"><Setting /></el-icon>
          </div>
        </el-tooltip>
      </div>
    </el-aside>

    <el-main class="app-main">
      <ChatPanel v-if="currentRoute === 'chat'" />
      <ToolPanel v-else-if="currentRoute === 'tools'" />
      <GamePanel v-else-if="currentRoute === 'games'" />
      <RoomPanel v-else-if="currentRoute === 'room'" />

      <SettingsDialog v-model="state.settingsOpen" />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { state } from './store.js'
import { isDark } from './composables/useTheme.js'
import ChatPanel from './components/ChatPanel.vue'
import ToolPanel from './components/ToolPanel.vue'
import GamePanel from './components/GamePanel.vue'
import RoomPanel from './components/RoomPanel.vue'
import SettingsDialog from './components/SettingsDialog.vue'

const currentRoute = computed(() => state.currentRoute)
</script>

<style lang="scss" scoped>
.app-container {
  height: 100%;
  background: var(--bg-primary);
}

.app-sidebar {
  width: 48px !important;
  min-width: 48px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 8px 0;
  overflow: hidden;
}

.nav-buttons {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.nav-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.nav-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.15s;

  &:hover {
    background: var(--bg-tertiary);
    color: var(--text-primary);
  }

  &.active {
    background: var(--bg-tertiary);
    color: var(--accent-color);
  }
}

.app-main {
  flex: 1;
  padding: 0;
  overflow: hidden;
}
</style>
