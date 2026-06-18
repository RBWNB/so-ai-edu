import axios from "axios";
import { getStoredToken } from "@/store/auth";

const api = axios.create({
  baseURL: "/api",
  timeout: 120000,
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
 * 非流式 RAG 问答
 */
export const askRag = (data) => api.post("/rag/ask", data);

/**
 * 获取会话历史
 */
export const getRagHistory = (sessionId) =>
  api.get(`/rag/history/${sessionId}`);

/**
 * SSE 流式 RAG 问答
 * @param {Object} options
 * @param {string} options.question - 用户问题
 * @param {string} options.sessionId - 会话ID
 * @param {Function} options.onMessage - 每次增量回调
 * @param {Function} options.onDone - 完成回调
 * @param {Function} options.onError - 错误回调
 * @param {AbortSignal} options.signal - 取消信号
 */
export const askRagStream = async ({
  question,
  sessionId,
  onMessage,
  onDone,
  onError,
  signal,
}) => {
  try {
    const token = getStoredToken();
    const headers = { Accept: "text/event-stream" };
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    const params = new URLSearchParams({ question, sessionId });
    const response = await fetch(`/api/rag/ask/stream?${params}`, {
      method: "GET",
      headers,
      signal,
    });

    if (!response.ok || !response.body) {
      throw new Error(`SSE request failed: ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let pending = "";

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;

      pending += decoder.decode(value, { stream: true });
      let idx = pending.indexOf("\n");
      while (idx !== -1) {
        const line = pending.slice(0, idx).trim();
        pending = pending.slice(idx + 1);
        idx = pending.indexOf("\n");

        if (!line) continue;

        let chunk = line;
        if (line.startsWith("data:")) {
          chunk = line.slice(5).trim();
        } else if (line.startsWith("event:") || line.startsWith("id:") || line.startsWith(":")) {
          continue;
        }

        if (!chunk || chunk === "[DONE]") continue;

        try {
          const parsed = JSON.parse(chunk);
          const text = parsed.delta || parsed.content || parsed.data || "";
          if (text) {
            onMessage?.(String(text));
          }
        } catch {
          onMessage?.(chunk);
        }
      }
    }

    if (pending.trim()) {
      const tail = pending.replace(/^data:\s*/, "").trim();
      if (tail && tail !== "[DONE]") onMessage?.(tail);
    }
    onDone?.();
  } catch (error) {
    if (error.name === "AbortError") {
      onDone?.();
    } else {
      onError?.(error);
    }
  }
};

export default api;
