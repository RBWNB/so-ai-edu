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
        <h1 v-reveal class="hero-title fluid-text reveal-fade-scale">深潜蔚蓝<br>探索未知</h1>
        <p v-reveal class="hero-subtitle reveal-fade-scale delay-100">带你潜入 10,000 米的深海知识宇宙</p>
        <div v-reveal class="scroll-down-indicator reveal-fade-scale delay-200">
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
          <span class="blur-reveal-text">浅海漫游</span> <span>/ Discovery</span>
        </h2>
        <p v-reveal class="section-desc reveal-slide-right delay-100">
          翻开生命的图鉴，定位每一片珊瑚的坐标。<br>从微小的浮游生物到庞大的座头鲸，每一处海域都藏着惊喜。
        </p>
        <div v-reveal class="action-buttons reveal-slide-right delay-200">
          <el-button class="glass-btn outline-btn" @click="goOrLogin('/encyclopedia')">翻阅海洋百科</el-button>
        </div>
      </div>
    </section>

    <section v-reveal class="snap-screen feature-screen layout-right" data-index="3">
      <div class="scene-decorations community-nodes-container">
        <div class="connection-glow"></div>
        <div class="c-node cn-1"><div class="node-core"></div><div class="node-ring"></div></div>
        <div class="c-node cn-2"><div class="node-core"></div><div class="node-ring"></div></div>
        <div class="c-node cn-3"><div class="node-core"></div><div class="node-ring"></div></div>
        <div class="c-node cn-4"><div class="node-core"></div><div class="node-ring"></div></div>
        <div class="c-node cn-5"><div class="node-core"></div><div class="node-ring"></div></div>
        <div class="c-spark cs-1"></div>
        <div class="c-spark cs-2"></div>
        <div class="c-spark cs-3"></div>
      </div>
      <div class="content-wrapper">
        <h2 v-reveal class="section-title reveal-slide-left">
          <span class="sweep-reveal-text" data-text="蔚蓝圈子">蔚蓝圈子</span> <span>/ Community</span>
        </h2>
        <p v-reveal class="section-desc reveal-slide-left delay-100">
          告别孤单的航行，在这里遇见志同道合的探索者。<br>发布你的海洋观测记录，分享每一次深海奇遇。
        </p>
        <div v-reveal class="action-buttons reveal-slide-left delay-200">
          <el-button class="glass-btn outline-btn" @click="goOrLogin('/obs-community')">进入海友社区</el-button>
        </div>
      </div>
    </section>

    <section v-reveal class="snap-screen feature-screen layout-left" data-index="4">
      <div class="scene-decorations radar-container" style="left: auto; right: 0;">
        <div class="radar-circle rc-1"></div>
        <div class="radar-circle rc-2"></div>
        <div class="radar-scanner"></div>
      </div>
      <div class="content-wrapper">
        <h2 v-reveal class="section-title reveal-slide-right">
          <span class="decode-text" data-text="智识海物">智识海物</span> <span>/ AI Assistant</span>
        </h2>
        <p v-reveal class="section-desc reveal-slide-right delay-100">
          遇见未知的发光生物？<br>连接智识海物，智能识图，即刻解答你的每一个海洋疑惑。
        </p>
        <div v-reveal class="action-buttons reveal-slide-right delay-200">
          <el-button class="glass-btn primary-glow" @click="goOrLogin('/ai-assistant')">
            <el-icon class="btn-icon"><MagicStick /></el-icon> 唤醒智识海物
          </el-button>
        </div>
      </div>
    </section>

    <section v-reveal class="snap-screen feature-screen layout-center" data-index="5">
      <div class="scene-decorations rings-container">
        <div class="pulse-ring pr-1"></div>
        <div class="pulse-ring pr-2"></div>
        <div class="pulse-ring pr-3"></div>
      </div>
      <div class="content-wrapper relative-z">
        <h2 v-reveal class="section-title reveal-zoom-in">
          <span class="tracking-reveal-text">海底遗迹</span> <span>/ Challenge</span>
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
import { ArrowDown, MagicStick, Search, TrophyBase, ChatDotRound } from "@element-plus/icons-vue";

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
    title: "智识海物",
    desc: "拍张照，让智识海物告诉你这是什么海洋生物",
    btn: "试试识图",
    icon: markRaw(MagicStick),
    bg: "http://tgig77s29.hn-bkt.clouddn.com/carousel/AI（水母）.png",
    path: "/ai-assistant"
  },
  {
    title: "海友社区",
    desc: "发布观测记录，与全球探索者分享蔚蓝奇遇",
    btn: "进入社区",
    icon: markRaw(ChatDotRound),
    bg: "http://tgig77s29.hn-bkt.clouddn.com/carousel/社区（海龟）.png",
    path: "/obs-community"
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
  { depth: "-3000m", pressure: "ELEVATED", zone: "BATHYPELAGIC" },
  { depth: "-6000m", pressure: "CRITICAL", status: "AI ONLINE", zone: "ABYSSAL" },
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
  if (globalObserver) return;

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

onUnmounted(() => {
  if (globalObserver) globalObserver.disconnect();
});

const vReveal = {
  mounted(el) {
    initObserver();
    globalObserver.observe(el);
  },
  unmounted(el) {
    if (globalObserver) globalObserver.unobserve(el);
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
  font-size: clamp(3rem, 4.5vw, 5.5rem);
  font-weight: 900;
  color: #0b1a30;
  margin-bottom: 30px;
  letter-spacing: 4px;
  text-shadow: 0 4px 15px rgba(11, 26, 48, 0.1);
}

.section-title span {
  font-size: clamp(1.5rem, 2vw, 2.5rem);
  color: #165dff;
  font-weight: 800;
  font-family: 'Courier New', Courier, monospace;
}

.section-desc {
  font-size: clamp(1.25rem, 1.4vw, 1.6rem);
  font-weight: 600;
  color: #2b3a52;
  line-height: 2.2;
  margin-bottom: 50px;
  letter-spacing: 1.5px;
  max-width: 700px;
}

.layout-center .section-desc { margin-left: auto; margin-right: auto; }


/* 1. 浅海漫游：深水浮现 (Blur Focus) */
.blur-reveal-text {
  display: inline-block;
  opacity: 0;
  filter: blur(12px);
  transform: scale(1.05);
  transition: filter 1.2s cubic-bezier(0.2, 0.8, 0.2, 1),
  opacity 1.2s cubic-bezier(0.2, 0.8, 0.2, 1),
  transform 1.2s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.is-visible .blur-reveal-text {
  opacity: 1;
  filter: blur(0);
  transform: scale(1);
}

/* 2. 蔚蓝圈子：流光擦除 (Sweep Wipe) */
.sweep-reveal-text {
  position: relative;
  display: inline-block;
  color: rgba(11, 26, 48, 0.12); /* 骨架底色 */
}
.sweep-reveal-text::after {
  content: attr(data-text);
  position: absolute;
  left: 0;
  top: 0;
  color: #0b1a30; /* 实色填充 */
  white-space: nowrap;
  clip-path: polygon(0 0, 0 0, 0 100%, 0% 100%);
  transition: clip-path 1.2s cubic-bezier(0.77, 0, 0.175, 1) 0.3s;
}
.is-visible .sweep-reveal-text::after {
  clip-path: polygon(0 0, 100% 0, 100% 100%, 0 100%);
}

/* 3. 海底遗迹：遗迹重组 (Letter Tracking In) */
.tracking-reveal-text {
  display: inline-block;
  opacity: 0;
  letter-spacing: 24px;
  transform: scale(0.9);
  transition: letter-spacing 1.2s cubic-bezier(0.2, 0.8, 0.2, 1) 0.1s,
  opacity 1.2s ease-out 0.1s,
  transform 1.2s cubic-bezier(0.2, 0.8, 0.2, 1) 0.1s;
}
.is-visible .tracking-reveal-text {
  opacity: 1;
  letter-spacing: 4px; /* 恢复原生间距 */
  transform: scale(1);
}
/* =============================== */

.action-buttons { display: flex; gap: 24px; }
.layout-right .action-buttons { justify-content: flex-end; }
.layout-center .action-buttons { justify-content: center; }

.glass-btn {
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
  position: fixed; left: 30px; top: 50%; transform: translateY(-50%); z-index: 100;
  background: rgba(11, 26, 48, 0.05); backdrop-filter: blur(8px); padding: 24px 20px;
  border-radius: 16px; border: 1px solid rgba(22, 93, 255, 0.15); color: #4e5969;
  font-family: 'Courier New', Courier, monospace; display: flex; flex-direction: column; gap: 12px; pointer-events: none; transition: opacity 0.5s ease, transform 0.5s ease;
}
.depth-tracker.is-hidden { opacity: 0; transform: translateY(-50%) translateX(-20px); }
.tracker-line { width: 2px; height: 40px; background: #165dff; margin-bottom: 8px; margin-left: 4px; }
.tracker-item { font-size: 13px; font-weight: 700; letter-spacing: 1.5px; display: flex; align-items: center; gap: 12px; }
.tracker-value { color: #165dff; font-weight: 900; font-size: 16px; min-width: 60px; text-align: left; text-shadow: 0 2px 8px rgba(22, 93, 255, 0.2); }
.tracker-value.warning { color: #f53f3f; animation: pulseWarning 1.5s infinite; }
@keyframes pulseWarning { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

/* 全局进场控制 */
.reveal-fade-scale { opacity: 0; transform: scale(0.9); }
.reveal-slide-right { opacity: 0; transform: translateX(-80px); }
.reveal-slide-left { opacity: 0; transform: translateX(80px); }
.reveal-zoom-in { opacity: 0; transform: scale(0.85); }
.reveal-fade-scale, .reveal-slide-right, .reveal-slide-left, .reveal-zoom-in {
  transition: opacity 0.8s cubic-bezier(0.2, 0.8, 0.2, 1), transform 0.8s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.delay-100 { transition-delay: 100ms; }
.delay-200 { transition-delay: 200ms; }
.is-visible { opacity: 1; transform: translate(0, 0) scale(1); }

@keyframes bounce { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(10px); } }
.bounce-arrow { font-size: 24px; animation: bounce 2s infinite ease-in-out; }

.scene-decorations { position: absolute; inset: 0; pointer-events: none; z-index: 0; opacity: 0; transition: opacity 1.2s ease; }
.scene-decorations.is-visible { opacity: 1; }

/* 光线 */
.rays-container { background: radial-gradient(circle at 50% -20%, rgba(22, 93, 255, 0.15), transparent 60%); }
.light-ray {
  position: absolute; top: -10%; height: 120%;
  background: linear-gradient(180deg, rgba(22, 93, 255, 0.25) 0%, rgba(22, 93, 255, 0.05) 60%, transparent 100%);
  transform-origin: top; filter: blur(15px); will-change: transform; mix-blend-mode: multiply;
}
.ray-1 { left: 20%; width: 15%; --base-rotation: 15deg; animation: sway 6s infinite alternate ease-in-out; }
.ray-2 { left: 50%; width: 25%; --base-rotation: -10deg; animation: sway 8s infinite alternate-reverse ease-in-out; }
.ray-3 { left: 80%; width: 10%; --base-rotation: 25deg; animation: sway 5s infinite alternate ease-in-out; }
@keyframes sway { 0% { transform: rotate(var(--base-rotation)) skewX(2deg); opacity: 0.6; } 100% { transform: rotate(calc(var(--base-rotation) + 5deg)) skewX(-2deg); opacity: 1; } }

/* 气泡 */
.bubbles-container { width: 40%; height: 100%; left: 55%; position: absolute; }
.bubble {
  position: absolute; bottom: -10%; border-radius: 50%; border: 2px solid rgba(22, 93, 255, 0.4);
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.8), rgba(22, 93, 255, 0.15));
  box-shadow: 0 4px 15px rgba(22, 93, 255, 0.15), inset 0 -4px 10px rgba(22, 93, 255, 0.1); backdrop-filter: blur(2px); will-change: transform;
}
.bubbles-container.is-visible .b-1 { width: 45px; height: 45px; left: 20%; animation: floatUp 8s infinite linear; }
.bubbles-container.is-visible .b-2 { width: 25px; height: 25px; left: 50%; animation: floatUp 5s infinite linear 2s; }
.bubbles-container.is-visible .b-3 { width: 70px; height: 70px; left: 70%; animation: floatUp 10s infinite linear 1s; }
.bubbles-container.is-visible .b-4 { width: 18px; height: 18px; left: 30%; animation: floatUp 4s infinite linear 3s; }
@keyframes floatUp { 0% { transform: translateY(0) scale(1); opacity: 0; } 20% { opacity: 1; } 80% { opacity: 0.8; } 100% { transform: translateY(-100vh) scale(1.5); opacity: 0; } }

/* 海友社区通信网络特效 */
.community-nodes-container {
  width: 45%; height: 80%; left: 2%; top: 10%; position: absolute;
}
.connection-glow {
  position: absolute; top: 30%; left: 30%; width: 40%; height: 40%;
  background: radial-gradient(circle, rgba(0, 210, 255, 0.15) 0%, transparent 70%);
  filter: blur(30px); animation: pulseGlow 4s infinite alternate ease-in-out;
}
.c-node {
  position: absolute; display: flex; align-items: center; justify-content: center;
  will-change: transform;
}
.node-core {
  width: 8px; height: 8px; border-radius: 50%;
  background: #00d2ff;
  box-shadow: 0 0 12px #00d2ff, 0 0 24px #165dff;
}
.node-ring {
  position: absolute; width: 24px; height: 24px;
  border: 1px solid rgba(0, 210, 255, 0.6); border-radius: 50%;
}

.community-nodes-container.is-visible .c-node { animation: floatNode 6s infinite alternate ease-in-out; }
.community-nodes-container.is-visible .node-ring { animation: rippleNode 2.5s infinite cubic-bezier(0.1, 0.7, 0.3, 1); }

.cn-1 { left: 20%; top: 25%; animation-delay: 0s !important; }
.cn-2 { left: 65%; top: 15%; animation-delay: -1.5s !important; }
.cn-3 { left: 80%; top: 55%; animation-delay: -3s !important; }
.cn-4 { left: 45%; top: 80%; animation-delay: -0.5s !important; }
.cn-5 { left: 15%; top: 60%; animation-delay: -2s !important; }

.cn-1 .node-ring { animation-delay: 0s !important; }
.cn-2 .node-ring { animation-delay: 0.6s !important; }
.cn-3 .node-ring { animation-delay: 1.2s !important; }
.cn-4 .node-ring { animation-delay: 1.8s !important; }
.cn-5 .node-ring { animation-delay: 2.4s !important; }

.c-spark {
  position: absolute; width: 3px; height: 3px; border-radius: 50%;
  background: #fff; box-shadow: 0 0 8px #fff, 0 0 16px #00d2ff;
  opacity: 0;
}
.community-nodes-container.is-visible .cs-1 { animation: sparkMove1 4s infinite ease-in-out; }
.community-nodes-container.is-visible .cs-2 { animation: sparkMove2 5s infinite ease-in-out 1.5s; }
.community-nodes-container.is-visible .cs-3 { animation: sparkMove3 4.5s infinite ease-in-out 3s; }

@keyframes pulseGlow {
  0% { transform: scale(1); opacity: 0.5; }
  100% { transform: scale(1.3); opacity: 1; }
}
@keyframes floatNode {
  0% { transform: translate(0, 0); }
  100% { transform: translate(20px, -20px); }
}
@keyframes rippleNode {
  0% { transform: scale(0.8); opacity: 1; border-color: rgba(0, 210, 255, 0.9); }
  100% { transform: scale(4); opacity: 0; border-color: rgba(22, 93, 255, 0); }
}
@keyframes sparkMove1 {
  0% { left: 20%; top: 25%; opacity: 0; transform: scale(0.5); }
  20% { opacity: 1; transform: scale(1); }
  80% { opacity: 1; transform: scale(1); }
  100% { left: 65%; top: 15%; opacity: 0; transform: scale(0.5); }
}
@keyframes sparkMove2 {
  0% { left: 65%; top: 15%; opacity: 0; transform: scale(0.5); }
  20% { opacity: 1; transform: scale(1); }
  80% { opacity: 1; transform: scale(1); }
  100% { left: 80%; top: 55%; opacity: 0; transform: scale(0.5); }
}
@keyframes sparkMove3 {
  0% { left: 80%; top: 55%; opacity: 0; transform: scale(0.5); }
  20% { opacity: 1; transform: scale(1); }
  80% { opacity: 1; transform: scale(1); }
  100% { left: 45%; top: 80%; opacity: 0; transform: scale(0.5); }
}

/* 雷达 */
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
.radar-container { width: 50%; height: 100%; left: 0; position: absolute; display: flex; align-items: center; justify-content: center; }
.radar-circle { position: absolute; border-radius: 50%; border: 1.5px solid rgba(22, 93, 255, 0.2); box-shadow: 0 0 20px rgba(22, 93, 255, 0.05); }
.radar-container.is-visible .rc-1 { width: 300px; height: 300px; border: 2px dashed rgba(22, 93, 255, 0.6); animation: spin 20s infinite linear; }
.radar-container.is-visible .rc-2 { width: 450px; height: 450px; border-top: 3px solid rgba(22, 93, 255, 0.8); animation: spin 15s infinite linear reverse; }
.radar-container.is-visible .radar-scanner { position: absolute; width: 225px; height: 225px; background: conic-gradient(from 0deg, transparent 60%, rgba(22, 93, 255, 0.35) 100%); border-radius: 50%; transform-origin: center; animation: spin 4s infinite linear; }

/* 脉冲环 */
.rings-container { display: flex; align-items: center; justify-content: center; }
.pulse-ring {
  position: absolute; border-radius: 50%; border: 3px solid rgba(22, 93, 255, 0.7); box-shadow: 0 0 20px rgba(22, 93, 255, 0.2), inset 0 0 20px rgba(22, 93, 255, 0.2);
  top: 50%; left: 50%; transform: translate(-50%, -50%);
}
.rings-container.is-visible .pr-1 { animation: pulseOut 3s infinite cubic-bezier(0.21, 0.53, 0.56, 0.8); }
.rings-container.is-visible .pr-2 { animation: pulseOut 3s infinite cubic-bezier(0.21, 0.53, 0.56, 0.8) 1s; }
.rings-container.is-visible .pr-3 { animation: pulseOut 3s infinite cubic-bezier(0.21, 0.53, 0.56, 0.8) 2s; }
@keyframes pulseOut { 0% { width: 100px; height: 100px; opacity: 1; border-width: 4px; } 100% { width: 800px; height: 800px; opacity: 0; border-width: 0px; } }
</style>
