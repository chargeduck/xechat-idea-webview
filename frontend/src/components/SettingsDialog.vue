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
import { state } from '../store.js'
import { isDark, toggleTheme } from '../composables/useTheme.js'

defineProps({
  modelValue: { type: Boolean, default: false }
})

defineEmits(['update:modelValue'])
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
