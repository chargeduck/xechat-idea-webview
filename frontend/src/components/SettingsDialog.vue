<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="设置"
    width="320px"
    :close-on-click-modal="false"
  >
    <div class="settings-body">
      <div class="setting-row">
        <span>主题</span>
        <el-switch
          :model-value="isDark"
          @change="toggleTheme()"
          active-text="暗色"
          inactive-text="亮色"
        />
      </div>
      <el-form-item label="通信方式">
        <el-select v-model="localTransportMode" @change="onTransportModeChange" style="width: 100%" size="small">
          <el-option label="自动检测" value="auto" />
          <el-option label="JSBridge（插件内）" value="jsbridge" />
          <el-option label="WebSocket（直连）" value="websocket" />
        </el-select>
        <div style="font-size: 11px; color: var(--el-text-color-secondary); margin-top: 4px">
          当前：{{ currentTransportMode }} | 切换后需刷新页面生效
        </div>
      </el-form-item>
      <div class="setting-row">
        <span>在线状态</span>
        <el-tag :type="state.online ? 'success' : 'info'" size="small">
          {{ state.online ? '已连接' : '未连接' }}
        </el-tag>
      </div>
      <div v-if="state.username" class="setting-row">
        <span>用户名</span>
        <span style="color:var(--text-secondary)">{{ state.username }}</span>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { state } from '../store.js'
import { isDark, toggleTheme } from '../composables/useTheme.js'
import { getMode } from '../api.js'

defineProps({
  modelValue: { type: Boolean, default: false }
})

defineEmits(['update:modelValue'])

const currentTransportMode = ref(getMode())
const localTransportMode = ref(localStorage.getItem('xechat_transport_mode') || getMode())

function onTransportModeChange(val) {
  localStorage.setItem('xechat_transport_mode', val)
  currentTransportMode.value = val
}
</script>

<style scoped>
.settings-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}
</style>
