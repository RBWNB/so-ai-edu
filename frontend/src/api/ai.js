import axios from "axios";
import { getStoredToken } from "@/store/auth";

const api = axios.create({
  baseURL: "/api",
  timeout: 20000,
});

api.interceptors.request.use((config) => {
  const token = getStoredToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * 图像识别：上传图片文件
 */
export const identifySpeciesByImage = (file) => {
  const formData = new FormData();
  formData.append("file", file);
  // 兼容部分后端字段名
  formData.append("image", file);
  return api.post("/ai/identify", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
};

/**
 * 图像识别：普通 JSON 模式（兼容现有后端）
 */
export const identifySpecies = (data) => api.post("/ai/identify", data);

/**
 * 非流式聊天
 */
export const chatWithAI = (data) => api.post("/ai/chat", data);

/**
 * 本地 ComfyUI 文生图
 */
export const generateImageByPrompt = (data) => api.post("/ai/image/generate", data, {
  timeout: 180000,
});

/**
 * SSE 流式聊天（打字机效果）
 * onMessage: 每次增量回调
 * onDone: 完成回调
 * onError: 错误回调
 * onReasoning: 推理内容回调（ollama 模型思考过程）
 */
export const chatWithAIStream = async ({
  question,
  onMessage,
  onDone,
  onError,
  signal,
  provider = "zhipu",
  onReasoning,
}) => {
  try {
    const token = getStoredToken();
    const headers = {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
    };
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    const response = await fetch("/api/ai/chat/stream", {
      method: "POST",
      headers,
      body: JSON.stringify({ query: question, question, provider }),
      signal,
    });

    if (!response.ok || !response.body) {
      throw new Error(`SSE请求失败: ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let pending = "";

    while (true) {
      const { value, done } = await reader.read();
      if (done) {
        break;
      }
      pending += decoder.decode(value, { stream: true });

      let splitIndex = pending.indexOf("\n");
      while (splitIndex !== -1) {
        const line = pending.slice(0, splitIndex).trim();
        pending = pending.slice(splitIndex + 1);
        splitIndex = pending.indexOf("\n");

        if (!line) {
          continue;
        }

        let chunk = line;
        if (line.startsWith("data:")) {
          chunk = line.slice(5).trim();
        } else if (line.startsWith("event:") || line.startsWith("id:") || line.startsWith(":")) {
          // SSE 事件元数据行，跳过
          continue;
        }

        if (!chunk || chunk === "[DONE]") {
          continue;
        }

        try {
          const parsed = JSON.parse(chunk);
          if (parsed.reasoning) {
            onReasoning?.(String(parsed.reasoning));
          }
          const text =
            parsed.delta ||
            parsed.content ||
            parsed.text ||
            parsed.data ||
            parsed.answer ||
            "";
          if (text) {
            onMessage?.(String(text));
            continue;
          }
          if (parsed.reasoning) {
            continue;
          }
        } catch {
          // 非 JSON 文本，按原始分片处理
        }

        onMessage?.(chunk);
      }
    }

    if (pending.trim()) {
      const tail = pending.replace(/^data:\s*/, "").trim();
      if (tail && tail !== "[DONE]") {
        onMessage?.(tail);
      }
    }
    onDone?.();
  } catch (error) {
    onError?.(error);
  }
};

/**
 * 智能搜索建议：根据关键词返回匹配的物种列表
 */
export const suggestSpecies = (keyword, limit = 10) => api.get("/species/suggest", {
  params: { keyword, limit }
});

export default api;

