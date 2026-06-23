<template>
  <div class="depth-tracker" :class="{ 'is-hidden': activeIndex === 0 }">
    <div class="tracker-line"></div>
    <div class="tracker-item">DEPTH <span class="tracker-value">{{ currentEnv?.depth || '0000m' }}</span></div>
    <div class="tracker-item" v-if="currentEnv?.light">LIGHT <span class="tracker-value">{{ currentEnv.light }}</span></div>
    <div class="tracker-item" v-if="currentEnv?.pressure">PRESS <span class="tracker-value">{{ currentEnv.pressure }}</span></div>
    <div class="tracker-item" v-if="currentEnv?.status">STAT <span class="tracker-value warning">{{ currentEnv.status }}</span></div>
    <div class="tracker-item">ZONE <span class="tracker-value">{{ currentEnv?.zone || 'UNKNOWN' }}</span></div>
  </div>

  <div class="edu-home-scroll-container">

    <section v-reveal class="snap-screen carousel-screen" data-index="0">
      <div class="carousel-wrapper">
        <el-carousel :interval="5000" arrow="always" indicator-position="outside" height="65vh" class="home-carousel">
          <el-carousel-item v-for="(slide, i) in slides" :key="i">
            <div class="slide-card" :style="{ backgroundImage: `url('${slide.bg}')` }">
              <div class="overlay"></div>
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

    <section v-reveal class="snap-screen hero-screen" data-index="1">
      <div class="scene-decorations rays-container">
        <div class="light-ray ray-1"></div>
        <div class="light-ray ray-2"></div>
        <div class="light-ray ray-3"></div>
      </div>
      <div class="content-wrapper">
        <h1 v-reveal class="hero-title fluid-text reveal-up">深潜蔚蓝<br>探索未知</h1>
        <p v-reveal class="hero-subtitle reveal-up delay-100">带你潜入 10,000 米的深海知识宇宙</p>
        <div v-reveal class="scroll-down-indicator reveal-up delay-200">
          <span>继续下潜</span>
          <el-icon class="bounce-arrow"><ArrowDown /></el-icon>
        </div>
      </div>
    </section>

    <section v-reveal class="snap-screen feature-screen layout-left" data-index="2">
      <div class="scene-decorations bubbles-container">
        <div class="bubble b-1"></div>
        <div class="bubble b-2"></div>
        <div class="bubble b-3"></div>
        <div class="bubble b-4"></div>
      </div>
      <div class="content-wrapper">
        <h2 v-reveal class="section-title reveal-slide-right">
          <span class="wave-text" v-split>浅海漫游</span> <span>/ Discovery</span>
        </h2>
        <p v-reveal class="section-desc reveal-slide-right delay-100">
          翻开生命的图鉴，定位每一片珊瑚的坐标。<br>从微小的浮游生物到庞大的座头鲸，每一处海域都藏着惊喜。
        </p>
        <div v-reveal class="action-buttons reveal-slide-right delay-200">
          <el-button class="glass-btn outline-btn" @click="goOrLogin('/encyclopedia')">翻阅海洋百科</el-button>
          <el-button class="glass-btn outline-btn" @click="goOrLogin('/map-explore')">展开探索地图</el-button>
        </div>
      </div>
    </section>

    <section v-reveal class="snap-screen feature-screen layout-right" data-index="3">
      <div class="scene-decorations radar-container">
        <div class="radar-circle rc-1"></div>
        <div class="radar-circle rc-2"></div>
        <div class="radar-scanner"></div>
      </div>
      <div class="content-wrapper">
        <h2 v-reveal class="section-title reveal-slide-left">
          <span class="decode-text" data-text="深海智光">深海智光</span> <span>/ AI Assistant</span>
        </h2>
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

    <section v-reveal class="snap-screen feature-screen layout-center" data-index="4">
      <div class="scene-decorations rings-container">
        <div class="pulse-ring pr-1"></div>
        <div class="pulse-ring pr-2"></div>
        <div class="pulse-ring pr-3"></div>
      </div>
      <div class="content-wrapper relative-z">
        <h2 v-reveal class="section-title reveal-zoom-in">
          <span class="wave-text" v-split>海底遗迹</span> <span>/ Challenge</span>
        </h2>
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
import { ref, onUnmounted, markRaw } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/store/auth";
import { ArrowDown, MagicStick, Search, TrophyBase } from "@element-plus/icons-vue";

const router = useRouter();
const authStore = useAuthStore();

const goOrLogin = (path) => {
  if (authStore.isLoggedIn) {
    router.push(path);
  } else {
    router.push({ path: "/login", query: { redirect: path } });
  }
};

const slides = [
  {
    title: "探索海底两万里",
    desc: "从珊瑚礁到深海热泉，认识海洋生物的家园",
    btn: "开始探索",
    icon: markRaw(Search),
    bg: "http://tgig77s29.hn-bkt.clouddn.com/carousel/百科（鲸鱼）.png",
    path: "/encyclopedia"
  },
  {
    title: "AI 智能识图",
    desc: "拍张照，让 AI 导师告诉你这是什么海洋生物",
    btn: "试试识图",
    icon: markRaw(MagicStick),
    bg: "http://tgig77s29.hn-bkt.clouddn.com/carousel/AI（水母）.png",
    path: "/ai-assistant"
  },
  {
    title: "答题闯关挑战",
    desc: "闯关答题，检验你的海洋知识，赢取成就徽章",
    btn: "开始答题",
    icon: markRaw(TrophyBase),
    bg: "http://tgig77s29.hn-bkt.clouddn.com/carousel/问答（遗迹）.png",
    path: "/quiz"
  },
];

const activeIndex = ref(0);
const envConfig = [
  { depth: "0000m", zone: "SURFACE" },
  { depth: "-0200m", light: "15%", zone: "EPIPELAGIC" },
  { depth: "-1000m", light: "1%", pressure: "HIGH", zone: "MESOPELAGIC" },
  { depth: "-4000m", pressure: "CRITICAL", status: "AI ONLINE", zone: "BATHYAL" },
  { depth: "-10000m", status: "DETECTED", zone: "TRENCH" },
];
const currentEnv = ref(envConfig[0]);

const playDecodeAnimation = (el) => {
  if (!el) return;
  const text = el.getAttribute('data-text') || "";
  const letters = "01010101ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  let iterations = 0;
  if (el._decodeInterval) clearInterval(el._decodeInterval);

  el._decodeInterval = setInterval(() => {
    el.innerText = text.split("").map((letter, index) => {
      if (index < iterations) return text[index];
      return letters[Math.floor(Math.random() * 34)];
    }).join("");

    if (iterations >= text.length) clearInterval(el._decodeInterval);
    iterations += 1 / 3;
  }, 40);
};

let globalObserver = null;

const initObserver = () => {
  if (globalObserver) return; // 如果已经创建了，就不再重复创建

  globalObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      const el = entry.target;
      if (entry.isIntersecting) {
        el.classList.add("is-visible");

        if (el.classList.contains('snap-screen')) {
          const rawIndex = el.getAttribute('data-index');
          const index = rawIndex ? parseInt(rawIndex) : 0;
          activeIndex.value = isNaN(index) ? 0 : index;
          currentEnv.value = envConfig[activeIndex.value] || envConfig[0];

          const decors = el.querySelector('.scene-decorations');
          if (decors) decors.classList.add('is-visible');
        }

        const decodeEl = el.querySelector('.decode-text');
        if (decodeEl && !decodeEl._isDecoded) {
          decodeEl._isDecoded = true;
          playDecodeAnimation(decodeEl);
        }
      } else {
        el.classList.remove("is-visible");

        if (el.classList.contains('snap-screen')) {
          const decors = el.querySelector('.scene-decorations');
          if (decors) decors.classList.remove('is-visible');
        }

        const decodeEl = el.querySelector('.decode-text');
        if (decodeEl) decodeEl._isDecoded = false;
      }
    });
  }, { threshold: 0.35, rootMargin: "0px" });
};

// 组件销毁时断开监听
onUnmounted(() => {
  if (globalObserver) globalObserver.disconnect();
});

// 🌟 修复指令：在绑定时直接调用懒加载初始化函数
const vReveal = {
  mounted(el) {
    initObserver(); // 确保此时 Observer 一定存在！
    globalObserver.observe(el);
  },
  unmounted(el) {
    if (globalObserver) globalObserver.unobserve(el);
  }
};

const vSplit = {
  mounted(el) {
    if (!el) return;
    const text = el.textContent || '';
    el.innerHTML = text.trim().split('').map((char, i) =>
        `<span class="wave-char" style="transition-delay: ${i * 60}ms;">${char}</span>`
    ).join('');
  }
};
</script>

<style scoped>
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
.carousel-wrapper { width: 100%; max-width: 1440px; padding: 0 24px; display: flex; flex-direction: column; align-items: center; gap: 30px; }
.home-carousel { width: 100%; border-radius: 24px; box-shadow: 0 12px 40px rgba(0, 47, 167, 0.15); overflow: hidden; }
.home-carousel :deep(.el-carousel__indicators) { bottom: 16px; }
.slide-card { height: 100%; display: flex; align-items: center; justify-content: space-between; padding: 0 100px; color: #fff; position: relative; background-size: cover; background-position: center; background-repeat: no-repeat;}
.slide-card .overlay { position: absolute; inset: 0; background: linear-gradient(to right, rgba(11, 43, 94, 0.85) 0%, rgba(11, 43, 94, 0.4) 50%, rgba(0, 0, 0, 0.1) 100%); z-index: 0;}
.slide-text, .slide-icon { position: relative; z-index: 1; }
.slide-text { z-index: 1; max-width: 650px; }
.slide-text h2 { font-size: 4rem; font-weight: 800; margin: 0 0 12px; text-shadow: 0 2px 12px rgba(0,0,0,0.3); }
.slide-text p { font-size: 1.25rem; opacity: 0.9; margin: 0 0 28px; line-height: 1.6; }
.slide-icon { z-index: 1; opacity: 0.2; transform: scale(1.2);}
.scroll-down-hint { display: flex; flex-direction: column; align-items: center; gap: 8px; color: #165dff; font-size: 14px; font-weight: 500; opacity: 0.8; }

/* 字体与按钮样式 */
.hero-title {
  font-size: 5.5vw; font-weight: 900; line-height: 1.15; margin-bottom: 24px;
  text-shadow: 0 4px 20px rgba(22, 93, 255, 0.15);
}

/* 流体渐变文字 */
.fluid-text {
  background: linear-gradient(270deg, #0a2647, #165dff, #00d2ff, #165dff);
  background-size: 300% 300%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: fluidGradient 6s ease infinite;
}
@keyframes fluidGradient {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.hero-subtitle { font-size: 1.4rem; color: #2b4b7c; font-weight: 500; margin-bottom: 60px; letter-spacing: 2px; }
.scroll-down-indicator { display: flex; flex-direction: column; align-items: center; gap: 12px; color: #165dff; font-size: 13px; font-weight: 600; letter-spacing: 1px; }

.section-title {
  /* 使用 clamp 函数：最小 3rem，推荐 4.5vw，最大 5.5rem，随屏幕无限放大 */
  font-size: clamp(3rem, 4.5vw, 5.5rem);
  font-weight: 900; /* 极致加粗 */
  color: #0b1a30;
  margin-bottom: 30px;
  letter-spacing: 4px; /* 拉大字间距，占领更多横向空间 */
  /* 给文字加一层极淡的阴影，让字体显得更厚实 */
  text-shadow: 0 4px 15px rgba(11, 26, 48, 0.1);
}

/* 英文副标题同步放大加粗 */
.section-title span {
  font-size: clamp(1.5rem, 2vw, 2.5rem);
  color: #165dff;
  font-weight: 800;
  font-family: 'Courier New', Courier, monospace;
}

/* 正文描述加粗加大 */
.section-desc {
  font-size: clamp(1.25rem, 1.4vw, 1.6rem); /* 放大正文 */
  font-weight: 600; /* 提升正文字重，避免在浅色背景上发虚 */
  color: #2b3a52; /* 加深颜色，增加视觉重量 */
  line-height: 2.2; /* 增加行高，让文字块更庞大 */
  margin-bottom: 50px;
  letter-spacing: 1.5px;
  max-width: 700px;
}

/* 针对居中的第五屏（海底遗迹），让它的正文块居中对齐 */
.layout-center .section-desc {
  margin-left: auto;
  margin-right: auto;
}
/* 海浪逐字出场 */
:deep(.wave-char) {
  display: inline-block;
  opacity: 0;
  transform: translateY(30px) scale(0.9);
  transition: opacity 0.6s cubic-bezier(0.2, 0.8, 0.2, 1), transform 0.6s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.is-visible :deep(.wave-char) {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.section-desc { font-size: 1.25rem; color: #4e5969; line-height: 1.8; margin-bottom: 40px; }

.action-buttons { display: flex; gap: 24px; }
.layout-right .action-buttons { justify-content: flex-end; }
.layout-center .action-buttons { justify-content: center; }

glass-btn {
  border-radius: 40px;
  padding: 16px 42px;
  height: auto;
  font-size: 1.15rem;
  font-weight: 800;
  letter-spacing: 2px;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  backdrop-filter: blur(12px);
}
.outline-btn { background: rgba(22, 93, 255, 0.05); border: 2px solid rgba(22, 93, 255, 0.4); color: #165dff; }
.outline-btn:hover { background: rgba(22, 93, 255, 0.15); border-color: #165dff; color: #0b2b5e; transform: translateY(-4px); box-shadow: 0 10px 24px rgba(22, 93, 255, 0.15); }
.primary-glow { background: linear-gradient(135deg, #165dff, #00d2ff); border: none; color: #fff; box-shadow: 0 4px 15px rgba(22, 93, 255, 0.2); }
.primary-glow:hover { box-shadow: 0 10px 30px rgba(22, 93, 255, 0.5); transform: translateY(-4px); }
.hero-btn { padding: 18px 56px; font-size: 1.25rem; background: #0b1a30; color: #fff; border: none; }
.hero-btn:hover { background: #165dff; box-shadow: 0 12px 36px rgba(22, 93, 255, 0.35); transform: translateY(-4px); }
.btn-icon { margin-right: 8px; font-size: 1.4rem; transform: translateY(2px); display: inline-block; }

.depth-tracker {
  position: fixed;
  left: 30px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 100;
  background: rgba(11, 26, 48, 0.05);
  backdrop-filter: blur(8px);
  padding: 24px 20px;
  border-radius: 16px;
  border: 1px solid rgba(22, 93, 255, 0.15);
  color: #4e5969;
  font-family: 'Courier New', Courier, monospace;
  display: flex;
  flex-direction: column;
  gap: 12px;
  pointer-events: none;
  transition: opacity 0.5s ease, transform 0.5s ease;
}
.depth-tracker.is-hidden { opacity: 0; transform: translateY(-50%) translateX(-20px); }
.tracker-line { width: 2px; height: 40px; background: #165dff; margin-bottom: 8px; margin-left: 4px; }
.tracker-item { font-size: 13px; font-weight: 700; letter-spacing: 1.5px; display: flex; align-items: center; gap: 12px; }
.tracker-value { color: #165dff; font-weight: 900; font-size: 16px; min-width: 60px; text-align: left; text-shadow: 0 2px 8px rgba(22, 93, 255, 0.2); }
.tracker-value.warning { color: #f53f3f; animation: pulseWarning 1.5s infinite; }
@keyframes pulseWarning { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

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

/* 氛围动画容器 */
.scene-decorations {
  position: absolute; inset: 0; pointer-events: none; z-index: 0;
  opacity: 0; transition: opacity 1.2s ease;
}
.scene-decorations.is-visible { opacity: 1; }

/* 丁达尔光线 */
.rays-container {
  background: radial-gradient(circle at 50% -20%, rgba(22, 93, 255, 0.15), transparent 60%);
}
.light-ray {
  position: absolute; top: -10%; height: 120%;
  /* 加深光线的蓝色，并利用 mix-blend-mode 让光线在浅色背景上显影 */
  background: linear-gradient(180deg, rgba(22, 93, 255, 0.25) 0%, rgba(22, 93, 255, 0.05) 60%, transparent 100%);
  transform-origin: top; filter: blur(15px); will-change: transform;
  mix-blend-mode: multiply;
}
.ray-1 { left: 20%; width: 15%; --base-rotation: 15deg; animation: sway 6s infinite alternate ease-in-out; }
.ray-2 { left: 50%; width: 25%; --base-rotation: -10deg; animation: sway 8s infinite alternate-reverse ease-in-out; }
.ray-3 { left: 80%; width: 10%; --base-rotation: 25deg; animation: sway 5s infinite alternate ease-in-out; }
@keyframes sway {
  0% { transform: rotate(var(--base-rotation)) skewX(2deg); opacity: 0.6; }
  100% { transform: rotate(calc(var(--base-rotation) + 5deg)) skewX(-2deg); opacity: 1; }
}

/* 上升气泡 */
.bubbles-container { width: 40%; height: 100%; left: 55%; position: absolute; }
.bubble {
  position: absolute; bottom: -10%; border-radius: 50%;
  /* 加粗边框，改为深蓝色 */
  border: 2px solid rgba(22, 93, 255, 0.4);
  /* 内部加入蓝色渐变和一点点白色高光 */
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.8), rgba(22, 93, 255, 0.15));
  /* 增加外阴影和内阴影，塑造立体感 */
  box-shadow: 0 4px 15px rgba(22, 93, 255, 0.15), inset 0 -4px 10px rgba(22, 93, 255, 0.1);
  backdrop-filter: blur(2px);
  will-change: transform;
}
.bubbles-container.is-visible .b-1 { width: 45px; height: 45px; left: 20%; animation: floatUp 8s infinite linear; }
.bubbles-container.is-visible .b-2 { width: 25px; height: 25px; left: 50%; animation: floatUp 5s infinite linear 2s; }
.bubbles-container.is-visible .b-3 { width: 70px; height: 70px; left: 70%; animation: floatUp 10s infinite linear 1s; }
.bubbles-container.is-visible .b-4 { width: 18px; height: 18px; left: 30%; animation: floatUp 4s infinite linear 3s; }
@keyframes floatUp {
  0% { transform: translateY(0) scale(1); opacity: 0; }
  20% { opacity: 1; }
  80% { opacity: 0.8; }
  100% { transform: translateY(-100vh) scale(1.5); opacity: 0; }
}

/* 赛博雷达 */
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
.radar-container { width: 50%; height: 100%; left: 0; position: absolute; display: flex; align-items: center; justify-content: center; }
.radar-circle {
  position: absolute; border-radius: 50%;
  border: 1.5px solid rgba(22, 93, 255, 0.2);
  box-shadow: 0 0 20px rgba(22, 93, 255, 0.05);
}
.radar-container.is-visible .rc-1 {
  width: 300px; height: 300px;
  /* 虚线加粗加深 */
  border: 2px dashed rgba(22, 93, 255, 0.6);
  animation: spin 20s infinite linear;
}
.radar-container.is-visible .rc-2 {
  width: 450px; height: 450px;
  /* 外环加粗加深 */
  border-top: 3px solid rgba(22, 93, 255, 0.8);
  animation: spin 15s infinite linear reverse;
}
.radar-container.is-visible .radar-scanner {
  position: absolute; width: 225px; height: 225px;
  /* 扫描扇形加深不透明度 */
  background: conic-gradient(from 0deg, transparent 60%, rgba(22, 93, 255, 0.35) 100%);
  border-radius: 50%; transform-origin: center; animation: spin 4s infinite linear;
}

/* 脉冲光环 */
.rings-container { display: flex; align-items: center; justify-content: center; }
.pulse-ring {
  position: absolute; border-radius: 50%;
  /* 提升初始线条粗细和颜色浓度 */
  border: 3px solid rgba(22, 93, 255, 0.7);
  /* 增加明显的蓝色发光效果 */
  box-shadow: 0 0 20px rgba(22, 93, 255, 0.2), inset 0 0 20px rgba(22, 93, 255, 0.2);
  top: 50%; left: 50%; transform: translate(-50%, -50%);
}
.rings-container.is-visible .pr-1 { animation: pulseOut 3s infinite cubic-bezier(0.21, 0.53, 0.56, 0.8); }
.rings-container.is-visible .pr-2 { animation: pulseOut 3s infinite cubic-bezier(0.21, 0.53, 0.56, 0.8) 1s; }
.rings-container.is-visible .pr-3 { animation: pulseOut 3s infinite cubic-bezier(0.21, 0.53, 0.56, 0.8) 2s; }
@keyframes pulseOut {
  0% { width: 100px; height: 100px; opacity: 1; border-width: 4px; }
  100% { width: 800px; height: 800px; opacity: 0; border-width: 0px; }
}
</style>
