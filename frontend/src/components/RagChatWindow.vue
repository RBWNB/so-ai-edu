<template>
  <Transition name="panel">
    <div v-if="visible" class="rag-panel-overlay" @click.self="minimize">
      <div class="rag-panel" :class="{ maximized: isMaximized }">
        <!-- 标题栏 -->
        <div class="panel-header" @mousedown="startDrag">
          <div class="panel-title">
            <el-icon :size="18"><ChatDotRound /></el-icon>
            <span>海洋知识智能问答</span>
            <el-tag size="small" type="success" effect="dark">知识库增强</el-tag>
          </div>
          <div class="panel-actions">
            <el-button link :icon="isMaximized ? CopyDocument : FullScreen" @click="isMaximized = !isMaximized" />
            <el-button link :icon="Minus" @click="minimize" />
            <el-button link :icon="Close" @click="close" />
          </div>
        </div>

        <!-- 消息列表 -->
        <div ref="msgListRef" class="panel-messages">
          <div v-if="messages.length === 0" class="welcome-area">
            <div class="welcome-icon">
              <el-icon :size="48"><ChatDotRound /></el-icon>
            </div>
            <h3>海洋知识 RAG 问答</h3>
            <p>基于知识库文档的智能检索增强生成</p>
            <div class="suggest-list">
              <el-tag
                v-for="(q, i) in suggestQuestions"
                :key="i"
                class="suggest-tag"
                @click="useSuggest(q)"
              >
                {{ q }}
              </el-tag>
            </div>
          </div>

          <div
            v-for="msg in messages"
            :key="msg.id"
            class="msg-row"
            :class="msg.role"
          >
            <div class="msg-avatar">
              <el-avatar :size="28" :icon="msg.role === 'user' ? UserFilled : ChatDotRound" />
            </div>
            <div class="msg-bubble">
              <div class="msg-role-label">{{ msg.role === "user" ? "你" : "海洋小助手" }}</div>
              <div
                v-if="msg.role === 'assistant'"
                class="msg-content markdown-body"
                v-html="msg.displayHtml || msg.content"
              ></div>
              <div v-else class="msg-content">{{ msg.content }}</div>
            </div>
          </div>

          <!-- 加载指示器 -->
          <div v-if="streaming" class="msg-row assistant">
            <div class="msg-avatar">
              <el-avatar :size="28" :icon="ChatDotRound" />
            </div>
            <div class="msg-bubble">
              <div class="typing-dots"><span></span><span></span><span></span></div>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="panel-input">
          <el-input
            v-model="inputText"
            placeholder="基于知识库提问..."
            :disabled="streaming"
            @keyup.enter.exact="handleSend"
          >
            <template #append>
              <el-button
                v-if="streaming"
                type="warning"
                @click="handleStop"
              >
                <el-icon><VideoPause /></el-icon>
              </el-button>
              <el-button
                v-else
                type="primary"
                :disabled="!inputText.trim()"
                @click="handleSend"
              >
                <el-icon><Promotion /></el-icon>
              </el-button>
            </template>
          </el-input>
          <div class="input-hint">基于知识库增强检索回答 · Enter 发送</div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { nextTick, onMounted, ref } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";
import { ElMessage } from "element-plus";
import {
  ChatDotRound,
  Close,
  CopyDocument,
  FullScreen,
  Minus,
  Promotion,
  UserFilled,
  VideoPause,
} from "@element-plus/icons-vue";
import { askRag, askRagStream, getRagHistory } from "@/api/rag";

marked.setOptions({
  breaks: true,
  gfm: true,
  mangle: false,
  headerIds: false
});

const STORAGE_KEY = "rag_session_id";

// ========== Props / Emits ==========
const props = defineProps({
  modelValue: { type: Boolean, default: false },
});
const emit = defineEmits(["update:modelValue"]);

// ========== 状态 ==========
const visible = ref(false);
const isMaximized = ref(false);
const messages = ref([]);
const inputText = ref("");
const streaming = ref(false);
const msgListRef = ref(null);
let controller = null;

// 从 localStorage 获取或生成 sessionId
const getSessionId = () => {
  let sid = localStorage.getItem(STORAGE_KEY);
  if (!sid) {
    sid = "rag_" + Date.now() + "_" + Math.random().toString(36).slice(2, 8);
    localStorage.setItem(STORAGE_KEY, sid);
  }
  return sid;
};

const suggestQuestions = [
  "中华白海豚的栖息地和保护现状是什么？",
  "珊瑚礁生态系统面临哪些威胁？",
  "红树林有哪些生态功能？",
  "如何保护海洋生物多样性？",
];

// ========== 窗口控制 ==========
const open = () => {
  visible.value = true;
  emit("update:modelValue", true);
  loadHistory();
};

const minimize = () => {
  visible.value = false;
  emit("update:modelValue", false);
};

const close = () => {
  visible.value = false;
  emit("update:modelValue", false);
  messages.value = [];
  localStorage.removeItem(STORAGE_KEY);
};

// 简易拖拽
let dragging = false;
let dragOffsetX = 0;
let dragOffsetY = 0;

const startDrag = (e) => {
  if (e.target.tagName === "BUTTON" || e.target.closest("button")) return;
  dragging = true;
  const panel = e.currentTarget.closest(".rag-panel");
  if (!panel) return;

  panel.style.transition = "none";

  const rect = panel.getBoundingClientRect();
  dragOffsetX = e.clientX - Math.max(16, rect.left);
  dragOffsetY = e.clientY - Math.max(16, rect.top);

  const onMove = (ev) => {
    if (!dragging) return;
    const x = Math.max(0, Math.min(window.innerWidth - 60, ev.clientX - dragOffsetX));
    const y = Math.max(0, Math.min(window.innerHeight - 60, ev.clientY - dragOffsetY));
    panel.style.left = x + "px";
    panel.style.top = y + "px";
    panel.style.right = "auto";
    panel.style.bottom = "auto";
  };

  const onUp = () => {
    dragging = false;

    panel.style.transition = "";
    document.removeEventListener("mousemove", onMove);
    document.removeEventListener("mouseup", onUp);
  };

  document.addEventListener("mousemove", onMove);
  document.addEventListener("mouseup", onUp);
};

// ========== 对话 ==========
const loadHistory = async () => {
  try {
    const sid = getSessionId();
    const res = await getRagHistory(sid);
    const data = res?.data?.data || res?.data;
    if (Array.isArray(data) && data.length > 0) {
      // ★ loadHistory 的做法：全新数组 + 每个对象预先算好 displayHtml
      messages.value = data.map((m) => ({
        id: m.id,
        role: m.role,
        content: m.content,
        streaming: false,
        displayHtml: safeParseMarkdown(m.content || ""),
      }));
      await scrollToBottom();
    }
  } catch {
    // 首次使用无历史
  }
};

/**
 * ★ 流式 Markdown 规范化
 *
 * 【根因说明】
 * SSE 流式返回的 markdown 可能缺少换行符与标题空格，例如：
 *   "###问题解答...内容。### 参考依据..."
 * CommonMark 规范要求：
 *   1. ### 必须在行首（前面是换行或字符串开头）
 *   2. ### 后面必须有空格（"###text" 不是标题，"### text" 才是）
 * 缺失换行/空格时，marked 无法识别标题/列表等语法，会原样输出纯文本。
 *
 * 本函数在 marked.parse() 之前做无损规范化，对已规范的文本是幂等的。
 */
const normalizeMarkdown = (text) => {
  if (!text) return "";
  let result = text;
  // ① 确保标题标记后有空格：###text → ### text（不破坏已正确的 ### text）
  result = result.replace(/(#{1,6})([^\s#])/gm, "$1 $2");
  // ② 在非行首的标题前插入换行
  //    [^\n#] 排除 # 号，防止 "### " 被拆成 "#\n## "（贪心匹配会把 ### 的第一个 # 当作前导字符）
  result = result.replace(/([^\n#])(#{1,6}\s)/g, "$1\n$2");
  // ③ 在非行首的列表标记前插入换行（排除 ** 等加粗标记被误拆分）
  result = result.replace(/([^\n])(?<![*-])([-*]\s)/g, "$1\n$2");
  // ④ 在 `1. ` 有序列表前插入换行
  result = result.replace(/([^\n])(\d+\.\s)/g, "$1\n$2");
  // ⑤ 压缩多余空行
  result = result.replace(/\n{3,}/g, "\n\n");
  return result;
};

/**
 * ★ 统一 Markdown 解析函数
 * 先规范化再解析。增加 console.error 以便排查静默失败。
 */
const safeParseMarkdown = (text) => {
  if (!text) return "";
  try {
    const normalized = normalizeMarkdown(text);
    const raw = marked.parse(normalized);
    const clean = DOMPurify.sanitize(raw);
    return clean;
  } catch (e) {
    console.error("[safeParseMarkdown] 解析失败:", e, "输入:", text.slice(0, 100));
    return text;
  }
};

const handleSend = async () => {
  const question = inputText.value.trim();
  if (!question || streaming.value) return;

  messages.value.push({ id: Date.now(), role: "user", content: question });
  inputText.value = "";
  await scrollToBottom();

  const sessionId = getSessionId();
  streaming.value = true;
  controller = new AbortController();

  const aiMsgId = Date.now() + 1;
  messages.value.push({
    id: aiMsgId,
    role: "assistant",
    content: "",
    streaming: true,
    displayHtml: "",
  });

  let gotStreamChunk = false;

  await askRagStream({
    question,
    sessionId,
    signal: controller.signal,
    onMessage: async (chunk) => {
      gotStreamChunk = true;
      // 通过 findIndex 每次定位到数组中的实际位置，避免闭包引用过期
      const idx = messages.value.findIndex((m) => m.id === aiMsgId);
      if (idx === -1) return;
      const target = messages.value[idx];
      target.content += chunk;
      target.displayHtml = safeParseMarkdown(target.content);
      await nextTick();
      await scrollToBottom();
    },

    onDone: async () => {
      streaming.value = false;
      controller = null;

      const idx = messages.value.findIndex((m) => m.id === aiMsgId);
      if (idx === -1) return;

      const current = messages.value[idx];
      if (!current.content.trim()) {
        current.content = "（AI 暂无输出）";
      }
      const finalHtml = safeParseMarkdown(current.content);

      // ★★★ 核心修复 ★★★
      // 采用与 loadHistory 完全一致的策略：全量替换整个 messages 数组。
      // 这会让 Vue 的 v-for 拿到一个全新的数组引用，触发完整的 diff + patch，
      // 所有 v-html 指令都会从零初始化，彻底规避增量更新时的缓存/复用问题。
      messages.value = messages.value.map((m) => {
        if (m.id !== aiMsgId) return m;
        return {
          ...m,
          streaming: false,
          displayHtml: finalHtml,
        };
      });

      await scrollToBottom();
    },

    onError: async (err) => {
      streaming.value = false;
      controller = null;
      console.warn("RAG 流式失败:", err);

      const idx = messages.value.findIndex((m) => m.id === aiMsgId);
      if (idx === -1) return;

      const current = messages.value[idx];

      if (gotStreamChunk) {
        // ★ 已收到部分内容：全量替换数组
        const finalHtml = safeParseMarkdown(current.content);
        messages.value = messages.value.map((m) => {
          if (m.id !== aiMsgId) return m;
          return { ...m, streaming: false, displayHtml: finalHtml };
        });

        if (err.name !== "AbortError") {
          ElMessage.error("问答服务连接中断，已展示已接收内容");
        }
      } else {
        // 完全没收到内容：回退到非流式请求
        try {
          const resp = await askRag({ question, sessionId });
          const payload = resp?.data?.data || resp?.data;
          const fallbackContent =
            typeof payload === "string" ? payload : payload?.answer || payload?.data || "（暂无输出）";
          const finalHtml = safeParseMarkdown(fallbackContent);
          messages.value = messages.value.map((m) => {
            if (m.id !== aiMsgId) return m;
            return { ...m, content: fallbackContent, streaming: false, displayHtml: finalHtml };
          });
        } catch (fallbackErr) {
          console.error("RAG 非流式也失败:", fallbackErr);
          const errContent = "请求失败，请稍后重试。";
          messages.value = messages.value.map((m) => {
            if (m.id !== aiMsgId) return m;
            return { ...m, content: errContent, streaming: false, displayHtml: errContent };
          });
        }
      }

      await scrollToBottom();
    },
  });
};

const handleStop = () => {
  if (controller) {
    controller.abort();
  }
  streaming.value = false;
  controller = null;
};

const useSuggest = (q) => {
  inputText.value = q;
  handleSend();
};

const scrollToBottom = async () => {
  await nextTick();
  if (msgListRef.value) {
    msgListRef.value.scrollTop = msgListRef.value.scrollHeight;
  }
};

//暴露给父组件
defineExpose({ open, close });

onMounted(() => {
  // 如果需要在挂载时加载历史
});
</script>

<style scoped>
/* ── 遮罩层 (柔和的浅色遮罩) ── */
.rag-panel-overlay {
  position: fixed;
  inset: 0;
  z-index: 9998;
  background: rgba(240, 248, 255, 0.3);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ── 面板本体 (高透白毛玻璃) ── */
.rag-panel {
  position: absolute;
  bottom: 24px;
  right: 24px;
  width: 440px;
  height: 640px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(28px) saturate(120%);
  -webkit-backdrop-filter: blur(28px) saturate(120%);
  border: 1px solid rgba(255, 255, 255, 1);
  border-radius: 24px;
  /* 清新的弥散蓝阴影 */
  box-shadow:
      0 24px 48px rgba(0, 50, 150, 0.08),
      0 4px 16px rgba(0, 100, 255, 0.04);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all 0.35s cubic-bezier(0.25, 1, 0.5, 1);
}

.rag-panel.maximized {
  width: 800px;
  height: 85vh;
  bottom: 5vh;
  right: calc((100vw - 800px) / 2);
  border-radius: 32px;
}

/* ── 顶部栏 (干净、无边框感) ── */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  background: transparent;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  color: #0b1a30; /* 深藏青色 */
  cursor: move;
  user-select: none;
  flex-shrink: 0;
}
.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.panel-title :deep(.el-icon) {
  color: #165dff;
}
.panel-title :deep(.el-tag) {
  background: rgba(22, 93, 255, 0.1);
  border: none;
  color: #165dff;
  border-radius: 6px;
  font-weight: 600;
}
.panel-actions {
  display: flex;
  gap: 4px;
}
.panel-actions :deep(.el-button) {
  color: #86909c;
  font-size: 17px;
}
.panel-actions :deep(.el-button:hover) {
  color: #165dff;
  background: rgba(22, 93, 255, 0.08);
}

/* ── 消息列表区 ── */
.panel-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: transparent;
  scrollbar-width: thin;
  scrollbar-color: rgba(0,0,0,0.1) transparent;
}
.panel-messages::-webkit-scrollbar {
  width: 6px;
}
.panel-messages::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,0.1);
  border-radius: 10px;
}

/* ── 欢迎页 (清爽质感) ── */
.welcome-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: #4e5969;
  animation: fadeIn 0.6s ease;
}
.welcome-icon {
  color: #165dff;
  opacity: 0.8;
  margin-bottom: 16px;
  animation: floatUp 3s ease-in-out infinite;
}
.welcome-area h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
}
.welcome-area p {
  margin: 0 0 24px;
  font-size: 13px;
  color: #86909c;
}
.suggest-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  max-width: 320px;
}
.suggest-tag {
  cursor: pointer;
  padding: 10px 16px;
  font-size: 13px;
  border-radius: 12px;
  background: #fff !important;
  border: 1px solid rgba(0,0,0,0.06) !important;
  color: #4e5969 !important;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  justify-content: center;
  white-space: normal;
  height: auto;
  line-height: 1.4;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}
.suggest-tag:hover {
  border-color: #165dff !important;
  color: #165dff !important;
  background: rgba(22, 93, 255, 0.04) !important;
  box-shadow: 0 6px 16px rgba(22, 93, 255, 0.1);
  transform: translateY(-2px);
}

/* ── 消息气泡基础 ── */
.msg-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: slideUp 0.4s ease forwards;
}
.msg-row.user {
  flex-direction: row-reverse;
}
.msg-avatar :deep(.el-avatar) {
  background: #fff;
  border: 1px solid rgba(0,0,0,0.05);
  color: #165dff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.04);
}
.msg-bubble {
  max-width: 82%;
  border-radius: 16px;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
}

/* 用户气泡 (清爽海蓝渐变) */
.msg-row.user .msg-bubble {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
  border-top-right-radius: 4px;
  box-shadow: 0 6px 16px rgba(79, 172, 254, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* AI 气泡 (纯白实体+弥散阴影) */
.msg-row.assistant .msg-bubble {
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.04);
  color: #1d2129;
  border-top-left-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.msg-role-label {
  font-size: 11px;
  color: #86909c;
  margin-bottom: 6px;
  font-weight: 500;
}
.msg-row.user .msg-role-label {
  text-align: right;
  color: #86909c;
}

/* Markdown 优化 (适应亮色主题) */
.msg-content {
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-content.markdown-body :deep(p) { margin: 4px 0; }
.msg-content.markdown-body :deep(a) { color: #165dff; text-decoration: none; }
.msg-content.markdown-body :deep(ul),
.msg-content.markdown-body :deep(ol) { padding-left: 20px; margin: 6px 0; }
.msg-content.markdown-body :deep(code) {
  background: rgba(0, 0, 0, 0.04);
  color: #165dff;
  padding: 2px 6px;
  border-radius: 6px;
  font-family: monospace;
}
.msg-content.markdown-body :deep(pre) {
  background: #f7f8fa;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  padding: 12px;
  overflow-x: auto;
  margin: 10px 0;
}

/* ── 打字动画 (清新蓝) ── */
.typing-dots { display: flex; gap: 5px; padding: 6px 4px; }
.typing-dots span {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #4facfe;
  animation: dotBounce 1.4s ease-in-out infinite both;
}
.typing-dots span:nth-child(1) { animation-delay: 0s; }
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }

/* ── 底部输入区── */
.panel-input {
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.6);
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
}

.panel-input :deep(.el-input-group) {
  background: #fff;
  border-radius: 30px;
  border: 1px solid #e5e6eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  transition: all 0.3s;
  display: flex;
  align-items: center;
  padding: 4px;
}
.panel-input :deep(.el-input-group:focus-within) {
  border-color: #4facfe;
  box-shadow: 0 4px 12px rgba(79, 172, 254, 0.15);
}


.panel-input :deep(.el-input__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  border: none;
  padding-left: 16px;
}
.panel-input :deep(.el-input__inner) {
  color: #1d2129;
  height: 38px;
}
.panel-input :deep(.el-input__inner::placeholder) {
  color: #c9cdd4;
}

.panel-input :deep(.el-input-group__append) {
  background: transparent;
  border: none;
  box-shadow: none !important;
  padding: 0 4px; /* 控制按钮和右边缘的距离 */
}


.panel-input :deep(.el-input-group__append .el-button) {
  border-radius: 50% !important;
  width: 38px !important;
  height: 38px !important;
  margin: 0 !important;
  padding: 0 !important;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border: none;
  color: #fff;
  box-shadow: 0 4px 10px rgba(79, 172, 254, 0.3);
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
}

.panel-input :deep(.el-input-group__append .el-button:hover) {
  box-shadow: 0 6px 16px rgba(79, 172, 254, 0.5);
  transform: scale(1.06);
}

.input-hint {
  margin-top: 10px;
  font-size: 11px;
  color: #86909c;
  text-align: center;
  letter-spacing: 0.5px;
}

/* ── 动画 Keyframes ── */
@keyframes floatUp {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
@keyframes fadeIn {
  from { opacity: 0; transform: scale(0.97); }
  to { opacity: 1; transform: scale(1); }
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes dotBounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1.1); opacity: 1; }
}

/* ── Transition 组件动画增强 ── */
.panel-enter-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.panel-leave-active { transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1); }
.panel-enter-from, .panel-leave-to { opacity: 0; }
.panel-enter-from .rag-panel { transform: translateY(40px) scale(0.95); opacity: 0; }
.panel-leave-to .rag-panel { transform: translateY(20px) scale(0.98); opacity: 0; }

@media (max-width: 768px) {
  .rag-panel {
    width: 100vw; height: 100vh; bottom: 0; right: 0; border-radius: 0;
    background: rgba(255, 255, 255, 0.95);
  }
}
</style>
