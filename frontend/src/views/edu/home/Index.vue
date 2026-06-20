<template>
  <div class="edu-home-scroll-container">

    <section class="snap-screen carousel-screen">
      <div class="carousel-wrapper">
        <el-carousel :interval="5000" arrow="always" indicator-position="outside" height="420px" class="home-carousel">
          <el-carousel-item v-for="(slide, i) in slides" :key="i">
            <div class="slide-card" :style="{ background: slide.bg }">
              <div class="slide-text">
                <h2>{{ slide.title }}</h2>
                <p>{{ slide.desc }}</p>
                <el-button type="primary" size="large" round @click="goOrLogin(slide.path)">
                  {{ slide.btn }}
                </el-button>
              </div>
              <div class="slide-icon">
                <el-icon :size="120"><component :is="slide.icon" /></el-icon>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
        <div class="scroll-down-hint">
          <span>向下滑动 开始下潜</span>
          <el-icon class="bounce-arrow"><ArrowDown /></el-icon>
        </div>
      </div>
    </section>

    <section class="snap-screen hero-screen">
      <div v-reveal class="scene-decorations rays-container">
        <div class="light-ray ray-1"></div>
        <div class="light-ray ray-2"></div>
        <div class="light-ray ray-3"></div>
      </div>

      <div class="content-wrapper">
        <h1 v-reveal class="hero-title reveal-up">深潜蔚蓝<br>探索未知</h1>
        <p v-reveal class="hero-subtitle reveal-up delay-100">带你潜入 10,000 米的深海知识宇宙</p>
        <div v-reveal class="scroll-down-indicator reveal-up delay-200">
          <span>继续下潜</span>
          <el-icon class="bounce-arrow"><ArrowDown /></el-icon>
        </div>
      </div>
    </section>

    <section class="snap-screen feature-screen layout-left">
      <div v-reveal class="scene-decorations bubbles-container">
        <div class="bubble b-1"></div>
        <div class="bubble b-2"></div>
        <div class="bubble b-3"></div>
        <div class="bubble b-4"></div>
      </div>

      <div class="content-wrapper">
        <h2 v-reveal class="section-title reveal-slide-right">浅海漫游 <span>/ Discovery</span></h2>
        <p v-reveal class="section-desc reveal-slide-right delay-100">
          翻开生命的图鉴，定位每一片珊瑚的坐标。<br>从微小的浮游生物到庞大的座头鲸，每一处海域都藏着惊喜。
        </p>
        <div v-reveal class="action-buttons reveal-slide-right delay-200">
          <el-button class="glass-btn outline-btn" @click="goOrLogin('/encyclopedia')">翻阅海洋百科</el-button>
          <el-button class="glass-btn outline-btn" @click="goOrLogin('/map-explore')">展开探索地图</el-button>
        </div>
      </div>
    </section>

    <section class="snap-screen feature-screen layout-right">
      <div v-reveal class="scene-decorations radar-container">
        <div class="radar-circle rc-1"></div>
        <div class="radar-circle rc-2"></div>
        <div class="radar-scanner"></div>
      </div>

      <div class="content-wrapper">
        <h2 v-reveal class="section-title reveal-slide-left">深海智光 <span>/ AI Assistant</span></h2>
        <p v-reveal class="section-desc reveal-slide-left delay-100">
          遇见未知的发光生物？<br>连接你的 AI 导师，智能识图，即刻解答你的每一个海洋疑惑。
        </p>
        <div v-reveal class="action-buttons reveal-slide-left delay-200">
          <el-button class="glass-btn primary-glow" @click="goOrLogin('/ai-assistant')">
            <el-icon class="btn-icon"><MagicStick /></el-icon> 唤醒 AI 导师
          </el-button>
        </div>
      </div>
    </section>

    <section class="snap-screen feature-screen layout-center">
      <div v-reveal class="scene-decorations rings-container">
        <div class="pulse-ring pr-1"></div>
        <div class="pulse-ring pr-2"></div>
        <div class="pulse-ring pr-3"></div>
      </div>

      <div class="content-wrapper relative-z">
        <h2 v-reveal class="section-title reveal-zoom-in">海底遗迹 <span>/ Challenge</span></h2>
        <p v-reveal class="section-desc reveal-zoom-in delay-100">
          知识的试炼场已经开启。<br>闯关答题，挑战你的知识极限，赢取属于航海者的无上勋章。
        </p>
        <div v-reveal class="action-buttons reveal-zoom-in delay-200">
          <el-button class="glass-btn hero-btn" @click="goOrLogin('/quiz')">接受试炼</el-button>
        </div>
      </div>
    </section>

  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import { useAuthStore } from "@/store/auth";
import { ArrowDown, MagicStick, Search, TrophyBase } from "@element-plus/icons-vue";

const $router = useRouter();
const authStore = useAuthStore();

const goOrLogin = (path) => {
  if (authStore.isLoggedIn) {
    $router.push(path);
  } else {
    $router.push({ path: "/login", query: { redirect: path } });
  }
};

const slides = [
  { title: "探索海底两万里", desc: "从珊瑚礁到深海热泉，认识海洋生物的家园", btn: "开始探索", icon: "Search", bg: "linear-gradient(135deg, #0b2b5e 0%, #0d4f8b 40%, #0077b6 100%)", path: "/encyclopedia" },
  { title: "AI 智能识图", desc: "拍张照，让 AI 导师告诉你这是什么海洋生物", btn: "试试识图", icon: "MagicStick", bg: "linear-gradient(135deg, #0a2647 0%, #144272 40%, #205295 100%)", path: "/ai-assistant" },
  { title: "答题闯关挑战", desc: "闯关答题，检验你的海洋知识，赢取成就徽章", btn: "开始答题", icon: "TrophyBase", bg: "linear-gradient(135deg, #06283d 0%, #1363df 50%, #47b5ff 100%)", path: "/quiz" },
];

const vReveal = {
  mounted(el) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          el.classList.add("is-visible");
        } else {
          el.classList.remove("is-visible");
        }
      });
    }, { threshold: 0.15, rootMargin: "0px 0px -50px 0px" });
    observer.observe(el);
    el._revealObserver = observer;
  },
  unmounted(el) {
    if (el._revealObserver) el._revealObserver.disconnect();
  }
};
</script>

<style scoped>
/* =======================================================
   1. 基础骨架与排版 (同前)
   ======================================================= */
.edu-home-scroll-container {
  height: calc(100vh - 56px);
  overflow-y: scroll;
  scroll-snap-type: y mandatory;
  scroll-behavior: smooth;
  scrollbar-width: none;
}
.edu-home-scroll-container::-webkit-scrollbar { display: none; }

.snap-screen {
  height: 100%; width: 100%;
  scroll-snap-align: start; scroll-snap-stop: always;
  display: flex; align-items: center; justify-content: center;
  position: relative; background: transparent; overflow: hidden;
}

.content-wrapper { max-width: 1200px; width: 100%; margin: 0 auto; padding: 0 40px; z-index: 10; }
.relative-z { position: relative; z-index: 10; }

.hero-screen { justify-content: center; text-align: center; }
.layout-left .content-wrapper { text-align: left; padding-right: 45%; }
.layout-right .content-wrapper { text-align: right; padding-left: 45%; }
.layout-center .content-wrapper { text-align: center; }

/* 轮播图样式 */
.carousel-wrapper { width: 100%; max-width: 1200px; padding: 0 24px; display: flex; flex-direction: column; align-items: center; gap: 30px; }
.home-carousel { width: 100%; border-radius: 20px; box-shadow: 0 12px 40px rgba(0, 47, 167, 0.15); overflow: hidden; }
.home-carousel :deep(.el-carousel__indicators) { bottom: 16px; }
.slide-card { height: 100%; display: flex; align-items: center; justify-content: space-between; padding: 0 80px; color: #fff; position: relative; }
.slide-text { z-index: 1; max-width: 480px; }
.slide-text h2 { font-size: 36px; font-weight: 800; margin: 0 0 12px; text-shadow: 0 2px 12px rgba(0,0,0,0.3); }
.slide-text p { font-size: 16px; opacity: 0.9; margin: 0 0 28px; line-height: 1.6; }
.slide-icon { z-index: 1; opacity: 0.2; }
.scroll-down-hint { display: flex; flex-direction: column; align-items: center; gap: 8px; color: #165dff; font-size: 14px; font-weight: 500; opacity: 0.8; }

/* 字体与按钮样式 */
.hero-title { font-size: 5.5vw; font-weight: 900; line-height: 1.15; margin-bottom: 24px; background: linear-gradient(135deg, #0a2647 0%, #165dff 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; text-shadow: 0 4px 20px rgba(22, 93, 255, 0.15); }
.hero-subtitle { font-size: 1.4rem; color: #2b4b7c; font-weight: 500; margin-bottom: 60px; letter-spacing: 2px; }
.scroll-down-indicator { display: flex; flex-direction: column; align-items: center; gap: 12px; color: #165dff; font-size: 13px; font-weight: 600; letter-spacing: 1px; }

.section-title { font-size: 3.5rem; font-weight: 800; color: #0b1a30; margin-bottom: 24px; text-shadow: 0 2px 10px rgba(0, 0, 0, 0.05); }
.section-title span { font-size: 1.5rem; color: #165dff; font-weight: 600; font-family: 'Courier New', Courier, monospace; }
.section-desc { font-size: 1.25rem; color: #4e5969; line-height: 1.8; margin-bottom: 40px; }

.action-buttons { display: flex; gap: 20px; }
.layout-right .action-buttons { justify-content: flex-end; }
.layout-center .action-buttons { justify-content: center; }

.glass-btn { border-radius: 30px; padding: 12px 32px; height: auto; font-size: 1rem; font-weight: 600; transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1); backdrop-filter: blur(12px); }
.outline-btn { background: rgba(22, 93, 255, 0.05); border: 1px solid rgba(22, 93, 255, 0.3); color: #165dff; }
.outline-btn:hover { background: rgba(22, 93, 255, 0.15); border-color: #165dff; color: #0b2b5e; transform: translateY(-3px); box-shadow: 0 8px 20px rgba(22, 93, 255, 0.15); }
.primary-glow { background: linear-gradient(135deg, #165dff, #00d2ff); border: none; color: #fff; }
.primary-glow:hover { box-shadow: 0 8px 24px rgba(22, 93, 255, 0.4); transform: translateY(-3px); }
.hero-btn { padding: 14px 48px; font-size: 1.1rem; background: #0b1a30; color: #fff; border: none; }
.hero-btn:hover { background: #165dff; box-shadow: 0 10px 30px rgba(22, 93, 255, 0.3); transform: translateY(-3px); }
.btn-icon { margin-right: 6px; font-size: 1.2rem; }

/* 入场动画配置 */
.reveal-up { opacity: 0; transform: translateY(60px); }
.reveal-slide-right { opacity: 0; transform: translateX(-80px); }
.reveal-slide-left { opacity: 0; transform: translateX(80px); }
.reveal-zoom-in { opacity: 0; transform: scale(0.85); }
.reveal-up, .reveal-slide-right, .reveal-slide-left, .reveal-zoom-in { transition: opacity 0.8s cubic-bezier(0.2, 0.8, 0.2, 1), transform 0.8s cubic-bezier(0.2, 0.8, 0.2, 1); }
.delay-100 { transition-delay: 100ms; }
.delay-200 { transition-delay: 200ms; }
.is-visible { opacity: 1; transform: translate(0, 0) scale(1); }

@keyframes bounce { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(10px); } }
.bounce-arrow { font-size: 24px; animation: bounce 2s infinite ease-in-out; }

/* =======================================================
   2. 屏显场景专属氛围动画 (The Magic)
   ======================================================= */
.scene-decorations {
  position: absolute;
  inset: 0;
  pointer-events: none; /* 防止挡住鼠标点击 */
  z-index: 0;
  opacity: 0; /* 默认隐藏，被 v-reveal 触发时显示 */
  transition: opacity 1s ease;
}
.scene-decorations.is-visible {
  opacity: 1;
}

/* ── 第一屏：丁达尔光线 (Light Rays) ── */
.rays-container {
  overflow: hidden;
  background: radial-gradient(circle at 50% -20%, rgba(22, 93, 255, 0.05), transparent 60%);
}
.light-ray {
  position: absolute;
  top: -10%;
  width: 15%;
  height: 120%;
  background: linear-gradient(180deg, rgba(22, 93, 255, 0.08) 0%, transparent 100%);
  transform-origin: top;
  filter: blur(20px);
}

/* 使用 CSS 变量 --base-rotation 存储初始角度 */
.ray-1 { left: 20%; --base-rotation: 15deg; animation: sway 6s infinite alternate ease-in-out; }
.ray-2 { left: 50%; --base-rotation: -10deg; width: 25%; animation: sway 8s infinite alternate-reverse ease-in-out; }
.ray-3 { left: 80%; --base-rotation: 25deg; width: 10%; animation: sway 5s infinite alternate ease-in-out; }

@keyframes sway {
  /* 调用 var(--base-rotation) 进行计算 */
  0% { transform: rotate(var(--base-rotation)) skewX(2deg); opacity: 0.5; }
  100% { transform: rotate(calc(var(--base-rotation) + 5deg)) skewX(-2deg); opacity: 1; }
}

/* ── 第二屏：深海上升气泡 (Rising Bubbles) ── */
.bubbles-container {
  /* 放置在右侧空白区域 */
  width: 40%;
  height: 100%;
  left: 55%;
  position: absolute;
}
.bubble {
  position: absolute;
  bottom: -10%;
  border-radius: 50%;
  border: 1px solid rgba(22, 93, 255, 0.2);
  background: radial-gradient(circle at 30% 30%, rgba(255,255,255,0.4), transparent);
}
/* 必须配合 .is-visible 才能触发气泡上升 */
.bubbles-container.is-visible .b-1 { width: 40px; height: 40px; left: 20%; animation: floatUp 8s infinite linear; }
.bubbles-container.is-visible .b-2 { width: 20px; height: 20px; left: 50%; animation: floatUp 5s infinite linear 2s; }
.bubbles-container.is-visible .b-3 { width: 60px; height: 60px; left: 70%; animation: floatUp 10s infinite linear 1s; }
.bubbles-container.is-visible .b-4 { width: 15px; height: 15px; left: 30%; animation: floatUp 4s infinite linear 3s; }

@keyframes floatUp {
  0% { transform: translateY(0) scale(1); opacity: 0; }
  20% { opacity: 0.8; }
  80% { opacity: 0.5; }
  100% { transform: translateY(-100vh) scale(1.5); opacity: 0; }
}

/* ── 第三屏：赛博雷达扫描 (Cyber Radar) ── */
.radar-container {
  width: 50%; height: 100%; left: 0; position: absolute;
  display: flex; align-items: center; justify-content: center;
}
.radar-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(0, 210, 255, 0.1);
}
.radar-container.is-visible .rc-1 { width: 300px; height: 300px; border: 1px dashed rgba(0, 210, 255, 0.3); animation: spin 20s infinite linear; }
.radar-container.is-visible .rc-2 { width: 450px; height: 450px; border-top: 2px solid rgba(0, 210, 255, 0.4); animation: spin 15s infinite linear reverse; }
.radar-container.is-visible .radar-scanner {
  position: absolute;
  width: 225px; height: 225px;
  background: conic-gradient(from 0deg, transparent 70%, rgba(0, 210, 255, 0.15) 100%);
  border-radius: 50%;
  transform-origin: center;
  animation: spin 4s infinite linear;
}

@keyframes spin { 100% { transform: rotate(360deg); } }

/* ── 第四屏：试炼脉冲光环 (Pulse Rings) ── */
.rings-container {
  display: flex; align-items: center; justify-content: center;
}
.pulse-ring {
  position: absolute;
  border-radius: 50%;
  border: 2px solid rgba(22, 93, 255, 0.4);
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
}
.rings-container.is-visible .pr-1 { animation: pulseOut 3s infinite cubic-bezier(0.21, 0.53, 0.56, 0.8); }
.rings-container.is-visible .pr-2 { animation: pulseOut 3s infinite cubic-bezier(0.21, 0.53, 0.56, 0.8) 1s; }
.rings-container.is-visible .pr-3 { animation: pulseOut 3s infinite cubic-bezier(0.21, 0.53, 0.56, 0.8) 2s; }

@keyframes pulseOut {
  0% { width: 100px; height: 100px; opacity: 1; }
  100% { width: 800px; height: 800px; opacity: 0; border-width: 0px; }
}

@media (max-width: 960px) {
  .slide-card { padding: 0 40px; }
  .slide-icon { display: none; }
  .hero-title, .section-title { font-size: 2.5rem; }
  /* 移动端隐藏部分复杂背景以保持性能 */
  .radar-container, .bubbles-container { display: none; }
}
</style>
