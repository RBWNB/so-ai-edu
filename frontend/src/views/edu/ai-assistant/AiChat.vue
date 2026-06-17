<template>
  <div class="chat-page">
    <el-card class="chat-card" shadow="hover">
      <template #header>
        <div class="chat-title">
          <span>智能对话助手</span>
          <div class="chat-actions">
            <el-select
              v-model="modelProvider"
              size="small"
              style="width: 180px"
              @change="handleModelChange"
            >
              <el-option label="gemma4-uncensored【本地】" value="ollama" />
              <el-option label="glm-4v-flash【智谱】" value="zhipu" />
              <el-option label="glm-5【阿里云百炼】" value="bailian" />
            </el-select>
            <el-tag type="success">SSE 实时输出</el-tag>
            <el-button
              v-if="messages.length > 0"
              type="danger"
              size="small"
              plain
              text
              @click="handleClear"
            >
              清空对话
            </el-button>
          </div>
        </div>
      </template>

      <div ref="messageListRef" class="message-list">
        <template v-for="item in messages" :key="item.id">
          <div class="bubble-row" :class="item.role">
            <div class="avatar">
              <el-avatar :size="32" :icon="item.role === 'user' ? UserFilled : Promotion" />
            </div>
            <div class="bubble">
              <div class="bubble-role">{{ item.role === "user" ? "你" : "AI助手" }}</div>
              <div v-if="item.role === 'assistant'">
                <div v-if="item.reasoning" class="reasoning-section">
                  <div class="reasoning-header" @click="item.reasoningCollapsed = !item.reasoningCollapsed">
                    <span class="reasoning-toggle">{{ item.reasoningCollapsed ? '▶' : '▼' }}</span>
                    <span>思考过程</span>
                  </div>
                  <div v-show="!item.reasoningCollapsed" class="reasoning-content">{{ item.reasoning }}</div>
                </div>
                <div
                  class="bubble-content markdown-body"
                  v-html="renderMarkdown(item.content)"
                />
              </div>
              <div v-else class="bubble-content">{{ item.content }}</div>
            </div>
          </div>
        </template>

        <div v-if="messages.length === 0" class="empty-state">
          <el-empty description="开始提问吧，例如：">
            <div class="suggest-list">
              <el-tag
                v-for="(q, i) in suggestQuestions"
                :key="i"
                class="suggest-tag"
                effect="plain"
                @click="useSuggest(q)"
              >
                {{ q }}
              </el-tag>
            </div>
          </el-empty>
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="请输入你的问题，Shift+Enter 换行，Enter 发送"
          @keydown.enter.exact.prevent="handleSend"
        />
        <div class="input-actions">
          <span class="word-count">{{ inputText.length }} / 500</span>
          <el-button v-if="streaming" type="warning" @click="handleStop">
            <el-icon class="icon"><VideoPause /></el-icon> 停止生成
          </el-button>
          <el-button type="primary" :loading="streaming" @click="handleSend">
            <el-icon class="icon"><Promotion /></el-icon> 发送
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { marked } from "marked";
import { nextTick, ref, onUnmounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Promotion, UserFilled, VideoPause } from "@element-plus/icons-vue";
import { chatWithAI, chatWithAIStream } from "@/api/ai";

marked.setOptions({
  breaks: true,
  gfm: true,
});

// 简单 LaTeX 渲染：将 $...$ 内联数学转为可读格式
const renderLatex = (text) => {
  return text.replace(/\$([^$]+)\$/g, (_, expr) => {
    let clean = expr.replace(/\\text\{([^}]*)\}/g, "$1");
    clean = clean.replace(/\\([a-zA-Z]+)\{([^}]*)\}/g, "$2");
    clean = clean.replace(/\\([a-zA-Z]+)(?:\s|$)/g, "");
    return `<span class="latex-inline">${clean}</span>`;
  });
};

const suggestQuestions = [
  "南海常见濒危海洋生物有哪些？",
  "海龟的生活习性是什么？",
  "珊瑚白化的主要原因是什么？",
  "如何保护海洋生物多样性？",
];

let seed = 0;
const genId = () => `${Date.now()}_${seed++}`;

const inputText = ref("");
const messages = ref([]);
const streaming = ref(false);
const messageListRef = ref(null);
const modelProvider = ref(localStorage.getItem("ai-model-provider") || "zhipu");
let controller = null;

// 打字机效果
const contentBuffer = ref("");
const reasoningBuffer = ref("");
const currentAiMsg = ref(null);
let typewriterTimer = null;
const TYPING_INTERVAL = 30; // 每30ms输出一个字符

const startTypewriter = () => {
  if (typewriterTimer) return;
  typewriterTimer = setInterval(() => {
    if (!currentAiMsg.value) return;
    let didWork = false;
    // 优先输出正式回答，再输出思考过程
    if (contentBuffer.value.length > 0) {
      currentAiMsg.value.content += contentBuffer.value[0];
      contentBuffer.value = contentBuffer.value.slice(1);
      didWork = true;
    } else if (reasoningBuffer.value.length > 0) {
      currentAiMsg.value.reasoning += reasoningBuffer.value[0];
      reasoningBuffer.value = reasoningBuffer.value.slice(1);
      didWork = true;
    }
    if (didWork) scrollToBottom();
  }, TYPING_INTERVAL);
};

const flushTypewriter = () => {
  if (typewriterTimer) {
    clearInterval(typewriterTimer);
    typewriterTimer = null;
  }
  if (!currentAiMsg.value) return;
  if (contentBuffer.value.length > 0) {
    currentAiMsg.value.content += contentBuffer.value;
    contentBuffer.value = "";
  }
  if (reasoningBuffer.value.length > 0) {
    currentAiMsg.value.reasoning += reasoningBuffer.value;
    reasoningBuffer.value = "";
  }
  // 正式回答开始输出后，自动折叠思考过程
  if (currentAiMsg.value.content.trim()) {
    currentAiMsg.value.reasoningCollapsed = true;
  }
  scrollToBottom();
};

onUnmounted(() => {
  if (typewriterTimer) clearInterval(typewriterTimer);
});

const handleModelChange = (val) => {
  localStorage.setItem("ai-model-provider", val);
};

const scrollToBottom = async () => {
  await nextTick();
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
  }
};

const renderMarkdown = (text) => {
  if (!text) return "";
  try {
    let html = marked.parse(text);
    html = renderLatex(html);
    return html;
  } catch {
    return text;
  }
};

const appendMessage = (role, content) => {
  const message = { id: genId(), role, content, reasoning: "", reasoningCollapsed: false };
  messages.value.push(message);
  return message;
};

const handleStop = () => {
  flushTypewriter();
  if (controller) {
    controller.abort();
  }
  streaming.value = false;
  controller = null;
  currentAiMsg.value = null;
};

const handleClear = async () => {
  try {
    await ElMessageBox.confirm("确定清空所有对话记录吗？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    });
    if (streaming.value) {
      flushTypewriter();
      if (controller) controller.abort();
      streaming.value = false;
      controller = null;
      currentAiMsg.value = null;
    }
    messages.value = [];
  } catch {
    // 取消操作
  }
};

const useSuggest = (question) => {
  inputText.value = question;
  handleSend();
};

const handleSend = async () => {
  const question = inputText.value.trim();
  if (!question || streaming.value) {
    return;
  }

  if (question.length > 500) {
    ElMessage.warning("问题不能超过500字");
    return;
  }

  appendMessage("user", question);
  inputText.value = "";

  const aiMsg = appendMessage("assistant", "");
  currentAiMsg.value = aiMsg;
  await scrollToBottom();

  streaming.value = true;
  controller = new AbortController();

  let gotStreamChunk = false;
  startTypewriter();

  await chatWithAIStream({
    question,
    signal: controller.signal,
    provider: modelProvider.value,
    onMessage: async (chunk) => {
      gotStreamChunk = true;
      contentBuffer.value += chunk;
    },
    onReasoning: async (text) => {
      reasoningBuffer.value += text;
    },
    onDone: async () => {
      flushTypewriter();
      streaming.value = false;
      controller = null;
      currentAiMsg.value = null;
      if (!aiMsg.content.trim()) {
        aiMsg.content = "（AI暂无输出）";
      }
      await scrollToBottom();
    },
    onError: async (err) => {
      flushTypewriter();
      if (!controller?.signal?.aborted && !gotStreamChunk) {
        console.warn("SSE流式响应失败，回退到非流式接口:", err);
        try {
          const resp = await chatWithAI({ query: question, question, provider: modelProvider.value });
          const payload = resp?.data?.data || resp?.data?.result || resp?.data;
          aiMsg.content =
            (typeof payload === "string" ? payload : payload?.answer || payload?.content || payload?.text) ||
            "（AI暂无输出）";
        } catch (fallbackErr) {
          console.error(fallbackErr);
          aiMsg.content = "请求失败，请稍后重试。";
          ElMessage.error("AI服务暂不可用");
        }
      } else if (controller?.signal?.aborted) {
        aiMsg.content = aiMsg.content || "已停止生成。";
      } else if (!aiMsg.content.trim()) {
        aiMsg.content = "请求失败，请稍后重试。";
      }

      streaming.value = false;
      controller = null;
      currentAiMsg.value = null;
      await scrollToBottom();
      if (err?.name !== "AbortError") {
        console.error(err);
      }
    },
  });
};
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 120px);
  max-width: 900px;
  margin: 0 auto;
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-bottom: 0;
}

.chat-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: var(--theme-text-primary);
}

.chat-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 2px 8px;
}

.bubble-row {
  display: flex;
  margin: 14px 0;
  gap: 8px;
}

.bubble-row.user {
  flex-direction: row-reverse;
}

.avatar {
  flex-shrink: 0;
}

.bubble {
  max-width: 80%;
  border-radius: 12px;
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0.25);
}

.bubble-row.user .bubble {
  background: var(--color-dark-green);
  color: #fff;
  border-color: var(--color-dark-green);
}

.bubble-row.assistant .bubble {
  background: rgba(255, 255, 255, 0.15);
  color: var(--theme-text-primary);
  border-color: rgba(255, 255, 255, 0.25);
}

.bubble-role {
  font-size: 12px;
  opacity: 0.8;
  margin-bottom: 4px;
  color: inherit;
}

.bubble-content {
  line-height: 1.35;
  white-space: pre-wrap;
  word-break: break-word;
}

:deep(.bubble-content.markdown-body p) {
  margin: 2px 0;
}

:deep(.bubble-content.markdown-body h1,
.bubble-content.markdown-body h2,
.bubble-content.markdown-body h3,
.bubble-content.markdown-body h4) {
  margin: 4px 0 2px;
  font-size: inherit;
  font-weight: 600;
}

:deep(.bubble-content.markdown-body ul,
.bubble-content.markdown-body ol) {
  padding-left: 18px;
  margin: 2px 0;
}

:deep(.bubble-content.markdown-body li) {
  margin: 0;
}

:deep(.bubble-content.markdown-body code) {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  padding: 1px 4px;
  font-size: 0.9em;
  color: var(--color-khaki);
}

:deep(.bubble-content.markdown-body pre) {
  background: rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  padding: 10px;
  overflow-x: auto;
  margin: 6px 0;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

:deep(.bubble-content.markdown-body pre code) {
  background: none;
  padding: 0;
}

:deep(.bubble-content.markdown-body strong) {
  font-weight: 600;
  color: var(--color-khaki);
}

:deep(.bubble-content.markdown-body blockquote) {
  border-left: 3px solid var(--color-khaki);
  padding-left: 10px;
  margin: 4px 0;
  color: var(--theme-text-secondary);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--theme-text-muted);
}

.suggest-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.suggest-tag {
  cursor: pointer;
  padding: 4px 12px;
  font-size: 13px;
  transition: all 0.2s;
  background: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  color: var(--theme-text-secondary) !important;
}

.suggest-tag:hover {
  background: var(--color-dark-green) !important;
  color: #fff !important;
  border-color: var(--color-dark-green) !important;
}

.input-area {
  margin-top: 10px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
}

.input-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.word-count {
  font-size: 12px;
  color: var(--theme-text-muted);
  margin-right: auto;
}

.icon {
  margin-right: 2px;
}

.reasoning-section {
  margin-bottom: 8px;
  border-left: 3px solid var(--color-khaki, #b8a56a);
  padding-left: 10px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 0 4px 4px 0;
}

.reasoning-header {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 12px;
  color: var(--color-khaki, #b8a56a);
  padding: 4px 0;
  user-select: none;
}

.reasoning-header:hover {
  opacity: 0.8;
}

.reasoning-toggle {
  font-size: 10px;
  transition: transform 0.2s;
}

.reasoning-content {
  font-size: 13px;
  color: var(--theme-text-muted, #999);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
  padding-bottom: 6px;
}

.latex-inline {
  font-style: italic;
  font-family: "Times New Roman", Times, serif;
  padding: 0 2px;
  color: var(--color-khaki, #b8a56a);
}
</style>
