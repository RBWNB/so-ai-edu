<template>
  <!-- ═══ 旧版 B站风格信息流 ═══ -->
  <OldCommunityList v-if="isOldStyle" />

  <!-- ═══ 新版瀑布流 ═══ -->
  <OceanCommunityList v-else />

  <!-- ═══ 样式切换按钮 ═══ -->
  <button
    class="community-style-toggle"
    :class="{ 'is-old': isOldStyle }"
    @click="toggleStyle"
    :title="isOldStyle ? '切换至瀑布流' : '切换至经典版'"
  >
    <span class="toggle-icon">
      <el-icon :size="16"><Refresh /></el-icon>
    </span>
    <span class="toggle-label">{{ isOldStyle ? '瀑布流' : '经典版' }}</span>
  </button>
</template>

<script setup>
import OceanCommunityList from './OceanCommunityList.vue'
import OldCommunityList from './OldCommunityList.vue'
import { useStyleMode } from '@/composables/useStyleMode'
import { Refresh } from '@element-plus/icons-vue'

const { isOldStyle, toggleStyle } = useStyleMode()
</script>

<style scoped>
.community-style-toggle {
  position: fixed;
  top: 80px;
  right: 20px;
  z-index: 999;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid rgba(22, 93, 255, 0.3);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #165dff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  font-family: inherit;
  letter-spacing: 0.5px;
}
.community-style-toggle:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(22, 93, 255, 0.2);
  border-color: #165dff;
  background: rgba(255, 255, 255, 0.95);
}
.community-style-toggle:active {
  transform: scale(0.96);
}
.community-style-toggle.is-old {
  background: rgba(11, 26, 48, 0.85);
  backdrop-filter: blur(12px);
  border-color: rgba(0, 210, 255, 0.3);
  color: #00d2ff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
}
.community-style-toggle.is-old:hover {
  background: rgba(11, 26, 48, 0.95);
  border-color: #00d2ff;
  box-shadow: 0 6px 24px rgba(0, 210, 255, 0.2);
}
.toggle-icon {
  display: flex;
  align-items: center;
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.community-style-toggle:hover .toggle-icon {
  transform: rotate(180deg);
}
.toggle-label {
  white-space: nowrap;
}
</style>
