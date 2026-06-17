<template>
  <div id="app-container">
    <!-- 鼠标轨迹拖影容器 -->
    <div class="mouse-trail-container" ref="trailContainerRef"></div>
    <router-view />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue';

// ═══════════════════════════════════════════
// 1. 核心缓动系统（目标值 + 插值）
// ═══════════════════════════════════════════
const targetX = ref(0);
const targetY = ref(0);
const currentX = ref(0);
const currentY = ref(0);
// 缓动系数（越小越丝滑，0.05-0.2之间最佳）
const easeFactor = 0.12;

// ═══════════════════════════════════════════
// 2. 拖拽按压状态
// ═══════════════════════════════════════════
const isMouseDown = ref(false);
const isTouchActive = ref(false);

// ═══════════════════════════════════════════
// 3. 轨迹拖影系统
// ═══════════════════════════════════════════
const trailContainerRef = ref(null);
const MAX_TRAIL_DOTS = 15;
const trailDots = [];

//  深海拖影调色盘
const trailPalette = [
  // 深海蓝系（主）
  { color: '#165dff', glow: 'rgba(22, 93, 255, 0.5)' },
  { color: '#4080ff', glow: 'rgba(64, 128, 255, 0.45)' },
  // 海沫绿系
  { color: '#36cfc9', glow: 'rgba(54, 207, 201, 0.45)' },
  { color: '#2dd4bf', glow: 'rgba(45, 212, 191, 0.4)' },
  // 珍珠白+电光青（点缀）
  { color: '#ffffff', glow: 'rgba(255, 255, 255, 0.7)' },
  { color: '#00f2fe', glow: 'rgba(0, 242, 254, 0.5)' }
];

function spawnTrailDot(clientX, clientY) {
  const container = trailContainerRef.value;
  if (!container) return;

  const dot = document.createElement('div');
  dot.className = 'trail-dot';
  dot.style.left = clientX + 'px';
  dot.style.top = clientY + 'px';

  // 随机大小 3–7px
  const size = (Math.random() * 4 + 3).toFixed(1);
  dot.style.setProperty('--trail-size', size + 'px');



  // 随机抽取一种颜色
  const randomStyle = trailPalette[Math.floor(Math.random() * trailPalette.length)];

  dot.style.setProperty('--trail-color', randomStyle.color);
  dot.style.setProperty('--trail-glow', randomStyle.glow);

  // 随机动画延迟，错落感
  dot.style.setProperty('--trail-delay', (Math.random() * 0.15).toFixed(2) + 's');

  container.appendChild(dot);

  // 下一帧触发入场动画
  requestAnimationFrame(() => {
    dot.classList.add('trail-dot-enter');
  });

  // 400ms 后开始淡出
  const fadeTimer = setTimeout(() => {
    dot.classList.add('trail-dot-leave');
  }, 400);

  // 1000ms 后从 DOM 移除，避免内存泄漏
  const removeTimer = setTimeout(() => {
    if (dot.parentNode) dot.parentNode.removeChild(dot);
  }, 1000);

  // 限制轨迹点数量，超出则移除最早的
  const record = { dot, fadeTimer, removeTimer };
  trailDots.push(record);
  if (trailDots.length > MAX_TRAIL_DOTS) {
    const oldest = trailDots.shift();
    clearTimeout(oldest.fadeTimer);
    clearTimeout(oldest.removeTimer);
    if (oldest.dot.parentNode) oldest.dot.parentNode.removeChild(oldest.dot);
  }
}

// ═══════════════════════════════════════════
// 4. 节流控制（~60fps）
// ═══════════════════════════════════════════
let lastEventTime = 0;
const THROTTLE_INTERVAL = 16;

// ═══════════════════════════════════════════
// 5. 事件处理器
// ═══════════════════════════════════════════
function handleMouseMove(e) {
  const now = performance.now();
  if (now - lastEventTime < THROTTLE_INTERVAL) return;
  lastEventTime = now;

  targetX.value = (e.clientX / window.innerWidth) - 0.5;
  targetY.value = (e.clientY / window.innerHeight) - 0.5;

  // 核心修改：无条件生成轨迹拖影
  spawnTrailDot(e.clientX, e.clientY);
}

function handleMouseDown(e) {
  isMouseDown.value = true;
  document.documentElement.style.setProperty('--mouse-active', '1');
  document.body.classList.add('mouse-active');
  spawnClickBurst(e.clientX, e.clientY);
}

function handleMouseUp() {
  isMouseDown.value = false;
  document.documentElement.style.setProperty('--mouse-active', '0');
  document.body.classList.remove('mouse-active');
}

// 鼠标离开窗口时重置按压状态，避免状态卡住
function handleMouseLeave() {
  if (isMouseDown.value) {
    isMouseDown.value = false;
    document.documentElement.style.setProperty('--mouse-active', '0');
    document.body.classList.remove('mouse-active');
  }
}

// ═══ 移动端触摸兼容 ═══
function handleTouchMove(e) {
  if (!e.touches.length) return;
  const touch = e.touches[0];
  const now = performance.now();
  if (now - lastEventTime < THROTTLE_INTERVAL) return;
  lastEventTime = now;

  targetX.value = (touch.clientX / window.innerWidth) - 0.5;
  targetY.value = (touch.clientY / window.innerHeight) - 0.5;

  // 核心修改：无条件生成轨迹拖影
  spawnTrailDot(touch.clientX, touch.clientY);
}

function handleTouchStart(e) {
  isTouchActive.value = true;
  isMouseDown.value = true;
  document.documentElement.style.setProperty('--mouse-active', '1');
  document.body.classList.add('mouse-active');
  if (e.touches.length) {
    spawnClickBurst(e.touches[0].clientX, e.touches[0].clientY);
  }
}

function handleTouchEnd() {
  isTouchActive.value = false;
  isMouseDown.value = false;
  document.documentElement.style.setProperty('--mouse-active', '0');
  document.body.classList.remove('mouse-active');
}

// ═══════════════════════════════════════════
// 6. requestAnimationFrame 动画循环
// ═══════════════════════════════════════════
let animationFrameId = null;

const updateMouseVars = () => {
  // 缓动公式：当前值 = 当前值 + (目标值 - 当前值) * 缓动系数
  currentX.value += (targetX.value - currentX.value) * easeFactor;
  currentY.value += (targetY.value - currentY.value) * easeFactor;

  const cx = currentX.value;
  const cy = currentY.value;

  // 仅当差值大于0.001时更新CSS变量（减少DOM操作）
  if (Math.abs(targetX.value - cx) > 0.001 || Math.abs(targetY.value - cy) > 0.001) {
    const root = document.documentElement;

    root.style.setProperty('--mouse-x', cx.toFixed(4));
    root.style.setProperty('--mouse-y', cy.toFixed(4));

    // ═══ 分层视差变量（带 px 单位的偏移量，由 CSS 中的速率乘数换算） ═══
    root.style.setProperty('--parallax-bg-x',   (cx * 30).toFixed(2) + 'px');
    root.style.setProperty('--parallax-bg-y',   (cy * 30).toFixed(2) + 'px');
    root.style.setProperty('--parallax-light-x',(cx * 80).toFixed(2) + 'px');
    root.style.setProperty('--parallax-light-y',(cy * 80).toFixed(2) + 'px');
    root.style.setProperty('--parallax-glow-x', (cx * 25).toFixed(2) + 'px');
    root.style.setProperty('--parallax-glow-y', (cy * 25).toFixed(2) + 'px');
    root.style.setProperty('--parallax-card-x', (cx * 8).toFixed(2) + 'px');
    root.style.setProperty('--parallax-card-y', (cy * 8).toFixed(2) + 'px');
    root.style.setProperty('--parallax-btn-x',  (cx * 10).toFixed(2) + 'px');
    root.style.setProperty('--parallax-btn-y',  (cy * 10).toFixed(2) + 'px');
  }

  animationFrameId = requestAnimationFrame(updateMouseVars);
};

// ═══════════════════════════════════════════
// 7. 生命周期
// ═══════════════════════════════════════════
onMounted(() => {
  // 鼠标事件
  window.addEventListener('mousemove', handleMouseMove, { passive: true });
  window.addEventListener('mousedown', handleMouseDown);
  window.addEventListener('mouseup', handleMouseUp);
  window.addEventListener('mouseleave', handleMouseLeave);

  // 触摸事件（移动端兼容）
  window.addEventListener('touchmove', handleTouchMove, { passive: true });
  window.addEventListener('touchstart', handleTouchStart);
  window.addEventListener('touchend', handleTouchEnd);

  // 初始按压状态
  document.documentElement.style.setProperty('--mouse-active', '0');

  // 启动动画循环
  animationFrameId = requestAnimationFrame(updateMouseVars);
});

onUnmounted(() => {
  // 移除鼠标事件
  window.removeEventListener('mousemove', handleMouseMove);
  window.removeEventListener('mousedown', handleMouseDown);
  window.removeEventListener('mouseup', handleMouseUp);
  window.removeEventListener('mouseleave', handleMouseLeave);

  // 移除触摸事件
  window.removeEventListener('touchmove', handleTouchMove);
  window.removeEventListener('touchstart', handleTouchStart);
  window.removeEventListener('touchend', handleTouchEnd);

  // 销毁动画帧，避免内存泄漏
  cancelAnimationFrame(animationFrameId);

  // 清理所有轨迹点
  trailDots.forEach((r) => {
    clearTimeout(r.fadeTimer);
    clearTimeout(r.removeTimer);
    if (r.dot.parentNode) r.dot.parentNode.removeChild(r.dot);
  });
  trailDots.length = 0;
});

// 点击水花粒子
function spawnClickBurst(x, y) {
  const container = trailContainerRef.value;
  if (!container) return;
  const count = 8;
  for (let i = 0; i < count; i++) {
    const angle = (i / count) * Math.PI * 2;
    const dist = Math.random() * 25 + 15;
    const size = (Math.random() * 4 + 2).toFixed(1);
    const color = trailPalette[Math.floor(Math.random() * trailPalette.length)].color;

    const p = document.createElement('div');
    p.className = 'click-particle';
    p.style.cssText = `
      left:${x}px;top:${y}px;
      width:${size}px;height:${size}px;
      --p-angle:${angle}rad;
      --p-dist:${dist}px;
      --p-color:${color};
    `;
    container.appendChild(p);
    setTimeout(() => p.remove(), 600);
  }
}
</script>

<style scoped>
#app-container {
  width: 100%;
  height: 100%;
  min-height: 100vh;
  background: transparent !important;
}

/* 轨迹拖影容器（全局指针穿透层） */
.mouse-trail-container {
  position: fixed;
  inset: 0;
  z-index: 9999;
  pointer-events: none;
  overflow: hidden;
}
</style>
