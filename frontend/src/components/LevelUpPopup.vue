<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="level-up-overlay">
        <div class="sonar-ring ring-1"></div>
        <div class="sonar-ring ring-2"></div>
        <div class="sonar-ring ring-3"></div>

        <div class="particles">
          <span v-for="i in 12" :key="i" class="particle" :style="getParticleStyle(i)"></span>
        </div>

        <div class="level-card-wrapper" :class="{ 'is-entering': isEntering }">
          <div class="level-card">
            <div class="card-glow"></div>

            <div class="card-header">
              <el-icon class="star-icon left"><StarFilled /></el-icon>
              <h2 class="title-text">LEVEL UP</h2>
              <el-icon class="star-icon right"><StarFilled /></el-icon>
            </div>

            <div class="level-number-box">
              <span class="level-prefix">Lv.</span>
              <span class="level-num">{{ displayLevel }}</span>
            </div>

            <div class="unlock-info">
              <p>恭喜！你已解锁更深海域的探索权限</p>
              <div class="rewards-tags" v-if="rewards.length">
                <span v-for="(reward, idx) in rewards" :key="idx" class="reward-tag">
                  {{ reward }}
                </span>
              </div>
            </div>

            <button class="continue-btn" @click="close">
              继续探索
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue';
import { StarFilled } from '@element-plus/icons-vue';

const visible = ref(false);
const isEntering = ref(false);
const displayLevel = ref(1);
const rewards = ref([]);

// 随机生成粒子位置
const getParticleStyle = (i) => {
  const size = Math.random() * 6 + 2;
  const left = Math.random() * 100;
  const top = Math.random() * 100;
  const delay = Math.random() * 2;
  const duration = Math.random() * 2 + 2;
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  };
};

// 暴露给父组件的触发方法
const show = (newLevel, unlockRewards = ['+ 50 积分', '解锁新图鉴']) => {
  displayLevel.value = newLevel - 1; // 为了做数字跳动效果，先设为旧等级
  rewards.value = unlockRewards;
  visible.value = true;
  isEntering.value = true;

  // 延迟播放数字跳动动画
  setTimeout(() => {
    displayLevel.value = newLevel;
  }, 600);
};

const close = () => {
  isEntering.value = false;
  setTimeout(() => {
    visible.value = false;
  }, 300); // 等待退场动画结束
};

defineExpose({ show, close });
</script>

<style scoped>
/* ── 全局遮罩层 ── */
.level-up-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(5, 10, 20, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  perspective: 1000px; /* 开启 3D 透视 */
}

/* ── 声呐波纹动画 (Sonar Ping) ── */
.sonar-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  border: 2px solid rgba(0, 210, 255, 0.6);
  box-shadow: 0 0 30px rgba(0, 210, 255, 0.4), inset 0 0 20px rgba(0, 210, 255, 0.2);
  opacity: 0;
  pointer-events: none;
}
.ring-1 { animation: sonar 3s cubic-bezier(0.2, 0.8, 0.2, 1) infinite; }
.ring-2 { animation: sonar 3s cubic-bezier(0.2, 0.8, 0.2, 1) infinite 0.8s; }
.ring-3 { animation: sonar 3s cubic-bezier(0.2, 0.8, 0.2, 1) infinite 1.6s; }

@keyframes sonar {
  0% { width: 0; height: 0; opacity: 1; border-width: 8px; }
  100% { width: 800px; height: 800px; opacity: 0; border-width: 1px; }
}

/* ── 悬浮发光粒子 ── */
.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.particle {
  position: absolute;
  background: #00d2ff;
  border-radius: 50%;
  box-shadow: 0 0 10px #00d2ff, 0 0 20px #00d2ff;
  opacity: 0;
  animation: floatUp ease-in-out infinite;
}
@keyframes floatUp {
  0% { transform: translateY(50px) scale(0); opacity: 0; }
  50% { opacity: 0.8; }
  100% { transform: translateY(-100px) scale(1.5); opacity: 0; }
}

/* ── 全息水晶卡片 ── */
.level-card-wrapper {
  position: relative;
  z-index: 10;
  opacity: 0;
  transform: rotateX(-30deg) translateY(50px) scale(0.8);
  transition: all 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.level-card-wrapper.is-entering {
  opacity: 1;
  transform: rotateX(0deg) translateY(0) scale(1);
}

.level-card {
  width: 400px;
  background: rgba(10, 18, 38, 0.6);
  backdrop-filter: blur(24px) saturate(150%);
  border: 1px solid rgba(0, 210, 255, 0.3);
  border-radius: 24px;
  padding: 40px;
  text-align: center;
  box-shadow:
      0 20px 50px rgba(0, 0, 0, 0.5),
      inset 0 0 40px rgba(0, 210, 255, 0.1),
      0 0 0 1px rgba(255, 255, 255, 0.05);
  position: relative;
  overflow: hidden;
}

/* 扫光特效 */
.card-glow {
  position: absolute;
  top: 0; left: -100%;
  width: 50%; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transform: skewX(-20deg);
  animation: sweepGlow 3s infinite 1s;
}
@keyframes sweepGlow {
  0% { left: -100%; }
  20%, 100% { left: 200%; }
}

/* ── 文字与内容排版 ── */
.card-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
}
.star-icon {
  font-size: 24px;
  color: #00d2ff;
  filter: drop-shadow(0 0 8px #00d2ff);
}
.star-icon.left { animation: spin 4s linear infinite; }
.star-icon.right { animation: spin 4s linear infinite reverse; }

.title-text {
  margin: 0;
  font-size: 28px;
  font-weight: 900;
  letter-spacing: 4px;
  color: #fff;
  text-shadow: 0 0 15px rgba(0, 210, 255, 0.8), 0 0 5px rgba(255, 255, 255, 0.8);
}

.level-number-box {
  margin: 20px 0 30px;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 8px;
}
.level-prefix {
  font-size: 32px;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 700;
  font-style: italic;
}
.level-num {
  font-size: 100px;
  font-weight: 900;
  line-height: 1;
  background: linear-gradient(180deg, #ffffff 0%, #00d2ff 50%, #0055ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 10px 20px rgba(0, 210, 255, 0.5));
  transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.unlock-info p {
  color: rgba(235, 245, 255, 0.8);
  font-size: 15px;
  margin: 0 0 16px;
}

.rewards-tags {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 32px;
}
.reward-tag {
  background: rgba(0, 210, 255, 0.1);
  border: 1px solid rgba(0, 210, 255, 0.3);
  padding: 6px 16px;
  border-radius: 20px;
  color: #00d2ff;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 0 10px rgba(0, 210, 255, 0.1);
}

/* ── 按钮 ── */
.continue-btn {
  width: 100%;
  padding: 14px;
  border-radius: 16px;
  background: linear-gradient(135deg, #165dff, #00d2ff);
  border: none;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  letter-spacing: 2px;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 210, 255, 0.3);
  transition: all 0.3s;
}
.continue-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(0, 210, 255, 0.5);
  background: linear-gradient(135deg, #00d2ff, #165dff);
}

/* Vue 过渡动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.4s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

@keyframes spin { 100% { transform: rotate(360deg); } }
</style>
