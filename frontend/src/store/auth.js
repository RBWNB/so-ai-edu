import { defineStore } from "pinia";
import { logoutApi } from "@/api/auth";
import { getUserProfile } from '@/api/sysUser'

const TOKEN_KEY = "marine_token";
const USERNAME_KEY = "marine_username";
const ROLES_KEY = "marine_roles";
const AVATAR_KEY = "marine_avatar";
const AVATAR_FRAME_KEY = "marine_avatar_frame";

export const getStoredToken = () => localStorage.getItem(TOKEN_KEY) || "";
export const getStoredUsername = () => localStorage.getItem(USERNAME_KEY) || "";
export const getStoredAvatar = () => localStorage.getItem(AVATAR_KEY) || "";
export const getStoredAvatarFrame = () => localStorage.getItem(AVATAR_FRAME_KEY) || "default";

const getStoredJSON = (key) => {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

export const getStoredRoles = () => getStoredJSON(ROLES_KEY) || [];

/** 静态菜单定义：根据角色返回固定菜单树（B端侧边栏） */
export const STATIC_MENUS = {
  ADMIN: [
    { id: 1,  name: "数据看板",       code: "dashboard",      type: 2, path: "/admin/dashboard",            icon: "DataBoard" },
    { id: 2,  name: "物种数量可视化",  code: "species-count",  type: 2, path: "/admin/visual/species-count",  icon: "PieChart" },
    { id: 3,  name: "地理分布可视化",  code: "geo-analysis",   type: 2, path: "/admin/visual/geo-analysis",   icon: "MapLocation" },
    { id: 4,  name: "物种管理",        code: "species",        type: 2, path: "/admin/species",               icon: "Collection" },
    { id: 5,  name: "生态系统管理",    code: "ecosystem",      type: 2, path: "/admin/ecosystem",             icon: "Odometer" },
    { id: 6,  name: "RAG 知识库",      code: "knowledge",      type: 2, path: "/admin/knowledge",             icon: "Document" },
    { id: 7,  name: "题库管理",        code: "quiz-manage",    type: 2, path: "/admin/quiz-manage",           icon: "Edit" },
    { id: 8,  name: "用户管理",        code: "sys-users",      type: 2, path: "/admin/system/users",          icon: "User" },
    { id: 9,  name: "角色管理",        code: "sys-roles",      type: 2, path: "/admin/system/roles",          icon: "Avatar" },
    { id: 10, name: "操作日志",        code: "operation-log",  type: 2, path: "/admin/system/operation-log",  icon: "Document" },
    { id: 11, name: "积分商店管理",    code: "shop-manage",    type: 2, path: "/admin/system/shop-manage",    icon: "Setting" },
    { id: 12, name: "观察审核",        code: "observation",    type: 2, path: "/admin/observation",           icon: "Camera" },
  ],
  MANAGER: [
    { id: 1, name: "数据看板",       code: "dashboard",      type: 2, path: "/admin/dashboard",            icon: "DataBoard" },
    { id: 2, name: "物种数量可视化",  code: "species-count",  type: 2, path: "/admin/visual/species-count",  icon: "PieChart" },
    { id: 3, name: "地理分布可视化",  code: "geo-analysis",   type: 2, path: "/admin/visual/geo-analysis",   icon: "MapLocation" },
    { id: 4, name: "物种管理",        code: "species",        type: 2, path: "/admin/species",               icon: "Collection" },
    { id: 5, name: "生态系统管理",    code: "ecosystem",      type: 2, path: "/admin/ecosystem",             icon: "Odometer" },
    { id: 6, name: "RAG 知识库",      code: "knowledge",      type: 2, path: "/admin/knowledge",             icon: "Document" },
    { id: 7, name: "题库管理",        code: "quiz-manage",    type: 2, path: "/admin/quiz-manage",           icon: "Edit" },
    { id: 8, name: "积分商店管理",    code: "shop-manage",    type: 2, path: "/admin/system/shop-manage",    icon: "Setting" },
  ],
  VISITOR: [
    { id: 1, name: "数据看板",       code: "dashboard",      type: 2, path: "/admin/dashboard",            icon: "DataBoard" },
    { id: 2, name: "物种数量可视化",  code: "species-count",  type: 2, path: "/admin/visual/species-count",  icon: "PieChart" },
    { id: 3, name: "地理分布可视化",  code: "geo-analysis",   type: 2, path: "/admin/visual/geo-analysis",   icon: "MapLocation" },
  ],
};

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: getStoredToken(),
    username: getStoredUsername(),
    roles: getStoredRoles(),
    avatarUrl: getStoredAvatar(),
    avatarFrame: getStoredAvatarFrame(),
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    /** 根据角色返回静态菜单 */
    menus: (state) => {
      if (!state.roles || state.roles.length === 0) return [];
      // 按优先级：ADMIN > MANAGER > VISITOR
      if (state.roles.includes("ADMIN")) return STATIC_MENUS.ADMIN;
      if (state.roles.includes("MANAGER")) return STATIC_MENUS.MANAGER;
      return STATIC_MENUS.VISITOR;
    },
  },
  actions: {
    /** 统一格式化头像URL（全局复用，避免路径不一致） */
    formatAvatarUrl(url) {
      if (!url) return "";
      if (url.startsWith("http") || url.startsWith("/api")) return url;
      return `/api${url}`;
    },

    /** 自动拉取用户信息（含头像、角色），解决刷新/首次登录数据缺失 */
    async fetchUserInfo() {
      if (!this.token) return;
      try {
        const res = await getUserProfile();
        const data = res.data;

        // 同步基础信息
        this.username = data.username || this.username;
        this.roles = data.roles || this.roles;
        // 格式化头像后同步
        this.avatarUrl = this.formatAvatarUrl(data.avatarUrl);
        this.avatarFrame = data.avatarFrame || "default";

        // 持久化到本地存储，刷新不丢失
        localStorage.setItem(USERNAME_KEY, this.username);
        localStorage.setItem(ROLES_KEY, JSON.stringify(this.roles));
        localStorage.setItem(AVATAR_KEY, this.avatarUrl);
      } catch (err) {
        console.error("拉取用户信息失败:", err);
        // token 失效时自动清理登录状态
        if (err.response?.status === 401) {
          this.clearAuth();
        }
      }
    },

    setAuth(token, username, roles = [], avatarUrl = "", avatarFrame = "default") {
      this.token = token || "";
      this.username = username || "";
      this.roles = roles;
      this.avatarUrl = this.formatAvatarUrl(avatarUrl);
      this.avatarFrame = avatarFrame || "default";

      if (this.token) {
        localStorage.setItem(TOKEN_KEY, this.token);
      } else {
        localStorage.removeItem(TOKEN_KEY);
      }
      if (this.username) {
        localStorage.setItem(USERNAME_KEY, this.username);
      } else {
        localStorage.removeItem(USERNAME_KEY);
      }
      if (this.avatarUrl) {
        localStorage.setItem(AVATAR_KEY, this.avatarUrl);
      } else {
        localStorage.removeItem(AVATAR_KEY);
      }
      if (this.roles && this.roles.length > 0) {
        localStorage.setItem(ROLES_KEY, JSON.stringify(this.roles));
      } else {
        localStorage.removeItem(ROLES_KEY);
      }
      if (this.avatarFrame) {
        localStorage.setItem(AVATAR_FRAME_KEY, this.avatarFrame);
      } else {
        localStorage.removeItem(AVATAR_FRAME_KEY);
      }
    },

    updateUserInfo(username, avatarUrl) {
      if (username) {
        this.username = username;
        localStorage.setItem(USERNAME_KEY, this.username);
      }
      if (avatarUrl !== undefined) {
        this.avatarUrl = this.formatAvatarUrl(avatarUrl);
        localStorage.setItem(AVATAR_KEY, this.avatarUrl);
      }
    },

    async logoutAction() {
      try {
        await logoutApi();
      } catch {
        // ignore
      } finally {
        this.clearAuth();
      }
    },

    clearAuth() {
      this.token = "";
      this.username = "";
      this.roles = [];
      this.avatarUrl = "";
      this.avatarFrame = "default";

      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USERNAME_KEY);
      localStorage.removeItem(ROLES_KEY);
      localStorage.removeItem(AVATAR_KEY);
      localStorage.removeItem(AVATAR_FRAME_KEY);
    },

    hasRole(role) {
      return this.roles && this.roles.includes(role);
    },
  },
});
