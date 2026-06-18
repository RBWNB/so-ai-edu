<template>
  <Transition name="panel">
    <div v-if="visible" class="rag-panel-overlay" @click.self="minimize">
      <div class="rag-panel" :class="{ maximized: isMaximized }">
        <!-- 标题栏 -->
        <div class="panel-header" @mousedown="startDrag">
          <div class="panel-title">
            <el-icon :size="18"><ChatDotRound /></el-icon>
            <span>RAG 智能问答</span>
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
              <div class="msg-role-label">{{ msg.role === "user" ? "你" : "RAG 助手" }}</div>
              <div
                v-if="msg.role === 'assistant'"
                class="msg-content markdown-body"
                v-html="renderMarkdown(msg.content)"
              />
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

marked.setOptions({ breaks: true, gfm: true });

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
      messages.value = data.map((m) => ({
        id: m.id,
        role: m.role,
        content: m.content,
      }));
      await scrollToBottom();
    }
  } catch {
    // 首次使用无历史
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
  messages.value.push({ id: aiMsgId, role: "assistant", content: "" });
  const aiMsg = messages.value[messages.value.length - 1];

  let gotStreamChunk = false;

  await askRagStream({
    question,
    sessionId,
    signal: controller.signal,
    onMessage: async (chunk) => {
      gotStreamChunk = true;
      aiMsg.content += chunk;
      await scrollToBottom();
    },
    onDone: async () => {
      streaming.value = false;
      controller = null;
      if (!aiMsg.content.trim()) {
        aiMsg.content = "（AI 暂无输出）";
      }
      // 刷新 sessionId（后端可能更新了）
      await scrollToBottom();
    },
    onError: async (err) => {
      streaming.value = false;
      controller = null;
      console.warn("RAG 流式失败:", err);
      if (!gotStreamChunk) {
        // 回退到非流式
        try {
          const resp = await askRag({ question, sessionId });
          const payload = resp?.data?.data || resp?.data;
          aiMsg.content =
            typeof payload === "string" ? payload : payload?.answer || payload?.data || "（暂无输出）";
        } catch (fallbackErr) {
          console.error("RAG 非流式也失败:", fallbackErr);
          aiMsg.content = "请求失败，请稍后重试。";
        }
      } else if (err.name !== "AbortError") {
        ElMessage.error("问答服务暂不可用");
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

const renderMarkdown = (text) => {
  if (!text) return "";
  try {
    return marked.parse(text);
  } catch {
    return text;
  }
};

// ========== 暴露给父组件 ==========
defineExpose({ open, close });

onMounted(() => {
  // 如果需要在挂载时加载历史
});
</script>

<style scoped>
/* ── 遮罩 ── */
.rag-panel-overlay {
  position: fixed;
  inset: 0;
  z-index: 9998;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ── 面板 ── */
.rag-panel {
  position: absolute;
  bottom: 24px;
  right: 24px;
  width: 440px;
  height: 600px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.18), 0 0 0 1px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.25s ease, height 0.25s ease;
}

.rag-panel.maximized {
  width: 720px;
  height: 85vh;
  bottom: 5vh;
  right: calc((100vw - 720px) / 2);
}

/* ── Header ── */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, var(--theme-primary, #0052d9), #3a7bd5);
  color: #fff;
  cursor: move;
  user-select: none;
  flex-shrink: 0;
}
.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}
.panel-actions {
  display: flex;
  gap: 4px;
}
.panel-actions :deep(.el-button) {
  color: rgba(255, 255, 255, 0.85);
}
.panel-actions :deep(.el-button:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
}

/* ── Messages ── */
.panel-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f7f8fa;
}

.welcome-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: var(--theme-text-muted);
}
.welcome-icon {
  color: var(--theme-primary, #0052d9);
  opacity: 0.4;
  margin-bottom: 12px;
}
.welcome-area h3 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: var(--theme-text-primary);
}
.welcome-area p {
  margin: 0 0 16px;
  font-size: 13px;
}
.suggest-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.suggest-tag {
  cursor: pointer;
  padding: 6px 14px;
  font-size: 13px;
  border-radius: 8px;
  background: #fff !important;
  border: 1px solid var(--theme-border-light) !important;
  transition: all 0.2s;
  justify-content: center;
}
.suggest-tag:hover {
  border-color: var(--theme-primary, #0052d9) !important;
  color: var(--theme-primary, #0052d9) !important;
  background: rgba(0, 82, 217, 0.04) !important;
}

/* ── 消息行 ── */
.msg-row {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
.msg-row.user {
  flex-direction: row-reverse;
}
.msg-bubble {
  max-width: 80%;
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
}
.msg-row.user .msg-bubble {
  background: var(--theme-primary, #0052d9);
  color: #fff;
}
.msg-row.assistant .msg-bubble {
  background: #fff;
  border: 1px solid #e8ecf1;
  color: var(--theme-text-primary);
}
.msg-role-label {
  font-size: 11px;
  opacity: 0.7;
  margin-bottom: 4px;
}
.msg-content {
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-content.markdown-body :deep(p) {
  margin: 4px 0;
}
.msg-content.markdown-body :deep(ul),
.msg-content.markdown-body :deep(ol) {
  padding-left: 18px;
  margin: 4px 0;
}
.msg-content.markdown-body :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 0.92em;
}
.msg-content.markdown-body :deep(pre) {
  background: #f0f2f5;
  border-radius: 6px;
  padding: 10px;
  overflow-x: auto;
  margin: 6px 0;
}
.msg-content.markdown-body :deep(strong) {
  font-weight: 600;
}

/* ── 打字动画 ── */
.typing-dots {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}
.typing-dots span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #aaa;
  animation: dotBounce 1.4s ease-in-out infinite both;
}
.typing-dots span:nth-child(1) {
  animation-delay: 0s;
}
.typing-dots span:nth-child(2) {
  animation-delay: 0.2s;
}
.typing-dots span:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes dotBounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* ── Input ── */
.panel-input {
  padding: 12px 16px;
  border-top: 1px solid #eee;
  background: #fff;
  flex-shrink: 0;
}
.input-hint {
  margin-top: 6px;
  font-size: 11px;
  color: var(--theme-text-muted);
  text-align: center;
}

/* ── Transition ── */
.panel-enter-active,
.panel-leave-active {
  transition: opacity 0.25s ease;
}
.panel-enter-active .rag-panel,
.panel-leave-active .rag-panel {
  transition: transform 0.25s ease, opacity 0.25s ease;
}
.panel-enter-from,
.panel-leave-to {
  opacity: 0;
}
.panel-enter-from .rag-panel,
.panel-leave-to .rag-panel {
  transform: translateY(20px) scale(0.96);
}

@media (max-width: 768px) {
  .rag-panel {
    width: 100vw;
    height: 100vh;
    bottom: 0;
    right: 0;
    border-radius: 0;
  }
  .rag-panel.maximized {
    width: 100vw;
    height: 100vh;
    right: 0;
  }
}
</style>
