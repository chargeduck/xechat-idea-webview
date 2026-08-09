<template>
  <el-container class="app-container">
    <el-aside width="48px" class="app-sidebar">
      <div class="nav-buttons">
        <div
          class="nav-btn"
          :class="{ active: currentRoute === 'chat' }"
          @click="state.currentRoute = 'chat'"
          title="聊天"
        >
          <el-icon :size="20"><ChatDotRound /></el-icon>
        </div>
        <div
          class="nav-btn"
          :class="{ active: currentRoute === 'tools' }"
          @click="state.currentRoute = 'tools'"
          title="工具"
        >
          <el-icon :size="20"><Tools /></el-icon>
        </div>
        <div
          class="nav-btn"
          :class="{ active: currentRoute === 'games' || currentRoute === 'room' }"
          @click="state.currentRoute = 'games'"
          title="游戏"
        >
          <el-icon :size="20"><Trophy /></el-icon>
        </div>
        <div
          class="nav-btn"
          :class="{ active: currentRoute === 'users' }"
          @click="state.currentRoute = 'users'"
          title="在线列表"
        >
          <el-icon :size="20"><UserFilled /></el-icon>
        </div>
      </div>
      <div class="nav-bottom">
        <div class="nav-btn" @click="state.settingsOpen = !state.settingsOpen" title="设置">
          <el-icon :size="18"><Setting /></el-icon>
        </div>
      </div>
    </el-aside>

    <el-main class="app-main">
      <ChatPanel v-if="currentRoute === 'chat'" />
      <ToolPanel v-else-if="currentRoute === 'tools'" />
      <GamePanel v-else-if="currentRoute === 'games'" />
      <RoomPanel v-else-if="currentRoute === 'room'" />
      <OnlineUsersPanel v-else-if="currentRoute === 'users'" />
      <StyleEditor v-else-if="currentRoute === 'style-editor'" />

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
import OnlineUsersPanel from './components/OnlineUsersPanel.vue'
import StyleEditor from './components/StyleEditor.vue'
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
