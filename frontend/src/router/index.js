import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/store/auth";
import { ElMessage } from "element-plus";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // B端 — 管理后台  (/admin)
    {
      path: "/admin",
      component: () => import("@/layout/AdminLayout.vue"),
      redirect: "/admin/dashboard",
      meta: { requiresAuth: true, roles: ["ADMIN", "MANAGER"] },
      children: [
        {
          path: "dashboard",
          name: "AdminDashboard",
          meta: { title: "数据看板" },
          component: () => import("@/views/admin/dashboard/Index.vue"),
        },
        {
          path: "species",
          name: "AdminSpecies",
          meta: { title: "物种管理" },
          component: () => import("@/views/admin/species/Index.vue"),
        },
        {
          path: "species/:id",
          name: "AdminSpeciesDetail",
          meta: { title: "物种详情" },
          component: () => import("@/views/admin/species/Detail.vue"),
        },
        {
          path: "ecosystem",
          name: "AdminEcosystem",
          meta: { title: "生态系统管理" },
          component: () => import("@/views/admin/ecosystem/Index.vue"),
        },
        {
          path: "knowledge",
          name: "AdminKnowledge",
          meta: { title: "RAG 知识库" },
          component: () => import("@/views/admin/knowledge/Index.vue"),
        },
        {
          path: "quiz-manage",
          name: "AdminQuizManage",
          meta: { title: "题库管理" },
          component: () => import("@/views/admin/quiz-manage/Index.vue"),
        },
        {
          path: "visual/species-count",
          name: "AdminSpeciesCount",
          meta: { title: "物种数量可视化" },
          component: () => import("@/views/admin/visual/SpeciesCountView.vue"),
        },
        {
          path: "visual/geo-analysis",
          name: "AdminGeoAnalysis",
          meta: { title: "地理分布可视化" },
          component: () => import("@/views/admin/visual/GeoAnalysisView.vue"),
        },
        {
          path: "system/users",
          name: "AdminUsers",
          meta: { title: "用户管理", roles: ["ADMIN"] },
          component: () => import("@/views/admin/system/UserManage.vue"),
        },
        {
          path: "system/roles",
          name: "AdminRoles",
          meta: { title: "角色管理", roles: ["ADMIN"] },
          component: () => import("@/views/admin/system/RoleManage.vue"),
        },
        {
          path: "system/operation-log",
          name: "AdminOperationLog",
          meta: { title: "操作日志", roles: ["ADMIN"] },
          component: () => import("@/views/admin/system/OperationLog.vue"),
        },
        {
          path: "system/shop-manage",
          name: "AdminShopManage",
          meta: { title: "积分商店管理", roles: ["ADMIN", "MANAGER"] },
          component: () => import("@/views/admin/system/ShopManage.vue"),
        },
      ],
    },
    // C端 — 教育互动平台
    {
      path: "/",
      component: () => import("@/layout/EduLayout.vue"),
      redirect: "/home",
      children: [
        {
          path: "home",
          name: "EduHome",
          meta: { title: "首页" },
          component: () => import("@/views/edu/home/Index.vue"),
        },
        {
          path: "encyclopedia",
          name: "EduEncyclopedia",
          meta: { title: "海洋百科", requiresAuth: true },
          component: () => import("@/views/edu/encyclopedia/Index.vue"),
        },
        {
          path: "ai-assistant",
          name: "EduAiAssistant",
          meta: { title: "AI 导师", requiresAuth: true },
          component: () => import("@/views/edu/ai-assistant/Index.vue"),
        },
        {
          path: "map-explore",
          name: "EduMapExplore",
          meta: { title: "探索地图", requiresAuth: true },
          component: () => import("@/views/edu/map-explore/Index.vue"),
        },
        {
          path: "quiz",
          name: "EduQuiz",
          meta: { title: "答题闯关", requiresAuth: true },
          component: () => import("@/views/edu/quiz/Index.vue"),
        },
        {
          path: "profile",
          name: "EduProfile",
          meta: { title: "个人中心", requiresAuth: true },
          component: () => import("@/views/edu/profile/Index.vue"),
        },
        {
          path: "learning/wrong-book",
          name: "EduWrongBook",
          meta: { title: "错题本", requiresAuth: true },
          component: () => import("@/views/edu/learning/WrongBook.vue"),
        },
        {
          path: "points/shop",
          name: "EduPointsShop",
          meta: { title: "积分商店", requiresAuth: true },
          component: () => import("@/views/edu/points/Shop.vue"),
        },
        {
          path: "observation/publish",
          name: "EduObservationPublish",
          meta: { title: "发布观察", requiresAuth: true },
          component: () => import("@/views/edu/observation/Publish.vue"),
        },
      ],
    },
    // 公共路由 — 无布局
    {
      path: "/login",
      name: "Login",
      meta: { title: "登录" },
      component: () => import("@/views/common/login/LoginView.vue"),
    },
    {
      path: "/403",
      name: "Forbidden",
      meta: { title: "无权限" },
      component: () => import("@/views/common/403.vue"),
    },
    {
      path: "/:pathMatch(.*)*",
      name: "NotFound",
      meta: { title: "页面不存在" },
      component: () => import("@/views/common/404.vue"),
    },
  ],
});

// 全局导航守卫
router.beforeEach(async (to, from, next) => {
  document.title = `${to.meta?.title || '首页'} - 海洋学堂`;
  const authStore = useAuthStore();

  // 已登录但头像为空 → 自动拉取用户信息（解决刷新/首次登录头像不显示）
  if (authStore.isLoggedIn && !authStore.avatarUrl) {
    await authStore.fetchUserInfo();
  }

  // 1. 路由需要登录但未登录 → 带 redirect 参数跳到登录页
  const needAuth = to.matched.some((r) => r.meta?.requiresAuth);
  if (needAuth && !authStore.isLoggedIn) {
    return next({ path: "/login", query: { redirect: to.fullPath } });
  }

  // 2. 已登录访问 /login → 按角色跳转（优先使用 redirect 参数）
  if (to.path === "/login" && authStore.isLoggedIn) {
    const redirect = to.query?.redirect;
    if (redirect && typeof redirect === "string" && redirect !== "/login") {
      return next(redirect);
    }
    const roles = authStore.roles ?? [];
    const isAdmin = roles.some((r) => ["ADMIN", "MANAGER"].includes(r));
    return next(isAdmin ? "/admin/dashboard" : "/home");
  }

  // 3. 角色鉴权（子路由 meta.roles 覆盖父路由）
  const requiredRoles = to.meta?.roles;
  if (Array.isArray(requiredRoles) && requiredRoles.length > 0) {
    const hasRole = requiredRoles.some((r) => authStore.hasRole(r));
    if (!hasRole) {
      ElMessage.error("权限不足，无法访问该页面");
      return next({ path: "/403", replace: true });
    }
  }

  next();
});

export default router;