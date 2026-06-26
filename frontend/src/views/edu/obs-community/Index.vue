<template>
  <!-- Feed流（经典单列布局） -->
  <CommunityFeedList v-if="isFeedStyle" />
  <!-- 瀑布流（多列瀑布布局） -->
  <CommunityMasonryList v-else />

  <!-- 视图切换按钮 -->
  <button
      class="community-style-toggle"
      :class="{ 'is-feed-style': isFeedStyle }"
      @click="toggleStyle"
      :title="isFeedStyle ? '切换至瀑布流布局' : '切换至经典信息流布局'"
      :aria-label="isFeedStyle ? '切换至瀑布流布局' : '切换至经典信息流布局'"
      :aria-pressed="isFeedStyle"
  >
    <span class="toggle-icon" ref="iconRef">
      <el-icon :size="16">
        <List v-if="isFeedStyle" />
        <Grid v-else />
      </el-icon>
    </span>
    <span class="toggle-label">{{ isFeedStyle ? '经典款' : '瀑布流' }}</span>
  </button>
</template>

<script setup>
import { ref } from 'vue'
import CommunityMasonryList from './CommunityMasonryList.vue'
import CommunityFeedList from './CommunityFeedList.vue'
import { useStyleMode } from '@/composables/useStyleMode'
import { Grid, List } from '@element-plus/icons-vue'

const {
  isOldStyle: isFeedStyle,
  toggleStyle: originalToggleStyle
} = useStyleMode()

// 图标 DOM 引用，替代原生 querySelector
const iconRef = ref(null)

// 扩展切换方法：保留原有核心逻辑 + 点击旋转动画
const toggleStyle = () => {
  // 执行原有的样式切换逻辑
  originalToggleStyle()

  // 执行图标旋转动画
  if (iconRef.value) {
    iconRef.value.style.transform = 'rotate(180deg)'
    setTimeout(() => {
      iconRef.value.style.transform = 'rotate(0deg)'
    }, 500)
  }
}
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
  box-shadow:
      0 4px 12px rgba(0, 0, 0, 0.08),
      0 1px 2px rgba(22, 93, 255, 0.1);
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

.community-style-toggle:focus {
  outline: 2px solid #165dff;
  outline-offset: 2px;
}

.community-style-toggle:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

/* Feed流模式下的按钮深色样式 */
.community-style-toggle.is-feed-style {
  background: rgba(11, 26, 48, 0.85);
  backdrop-filter: blur(12px);
  border-color: rgba(0, 210, 255, 0.3);
  color: #00d2ff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
}

.community-style-toggle.is-feed-style:hover {
  background: rgba(11, 26, 48, 0.95);
  border-color: #00d2ff;
  box-shadow: 0 6px 24px rgba(0, 210, 255, 0.2);
}

.community-style-toggle.is-feed-style:focus {
  outline-color: #00d2ff;
}

/* 图标容器动画 */
.toggle-icon {
  display: flex;
  align-items: center;
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.toggle-label {
  white-space: nowrap;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .community-style-toggle {
    top: auto;
    bottom: 20px;
    right: 20px;
    padding: 6px 10px;
    font-size: 12px;
  }
}
</style>
