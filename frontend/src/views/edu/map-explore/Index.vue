<template>
  <div class="community-page">
    <!-- ═══ 页面头部 ═══ -->
    <div class="community-header">
      <div class="header-top-row">
        <h2 class="page-title">
          <span class="title-icon">🌊</span>
          观察社区
        </h2>
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索帖子标题..."
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
            class="search-input"
          />
          <el-button type="primary" class="search-btn" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
      </div>
      <div class="header-bottom-row">
        <span class="page-subtitle">分享你的海洋观察发现</span>
        <div class="header-tools">
          <button class="sort-toggle" @click="switchSort(sortMode === 'latest' ? 'hot' : 'latest')">
            <el-icon :size="15"><component :is="sortMode === 'latest' ? Clock : Lightning" /></el-icon>
            {{ sortMode === 'latest' ? '最新' : '最热' }}
          </button>
          <span class="stat-badge">共 {{ totalCount }} 条</span>
          <el-button type="primary" class="publish-btn" @click="goPublish">
            <el-icon><Plus /></el-icon> 发布
          </el-button>
        </div>
      </div>
    </div>

    <!-- ═══ 加载状态 ═══ -->
    <div v-if="loading && posts.length === 0" class="loading-grid">
      <div v-for="i in 6" :key="i" class="skeleton-card">
        <el-skeleton :rows="3" animated />
      </div>
    </div>

    <!-- ═══ 空状态 ═══ -->
    <el-empty v-else-if="!loading && posts.length === 0" description="暂无观察分享" :image-size="120">
      <template #description><span>还没有人分享观察记录，快来发布第一条吧！</span></template>
      <el-button type="primary" @click="goPublish">发布观察</el-button>
    </el-empty>

    <!-- ═══ 卡片网格 ═══ -->
    <div v-else class="post-grid">
      <div
        v-for="post in sortedPosts"
        :key="post.id"
        class="post-card"
        @click="openDetail(post)"
      >
        <!-- 缩略图 -->
        <div class="card-thumb">
          <img
            v-if="post.photoUrl"
            :src="post.photoUrl"
            :alt="post.title"
          />
          <div v-else class="card-no-img">
            <el-icon :size="32"><Picture /></el-icon>
          </div>
          <!-- 交互量浮层 -->
          <div class="card-stats-overlay">
            <span><el-icon :size="12"><CircleCheck /></el-icon> {{ post.likeCount || 0 }}</span>
            <span><el-icon :size="12"><ChatDotSquare /></el-icon> {{ post.commentCount || 0 }}</span>
          </div>
        </div>

        <!-- 卡片信息 -->
        <div class="card-body">
          <h3 class="card-title">{{ post.title }}</h3>
          <div class="card-user">
            <div class="card-avatar-frame" :class="'frame-' + (post.avatarFrame || 'default')">
              <el-avatar :size="40" :src="formatAvatar(post.avatarUrl)">
                <el-icon :size="16"><User /></el-icon>
              </el-avatar>
            </div>
            <span class="card-username">{{ post.username }}</span>
            <span v-if="post.userTitle" class="card-badge">{{ post.userTitle }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══ 分页 ═══ -->
    <div v-if="totalCount > pageSize" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="totalCount"
        layout="prev, pager, next"
        background
        size="small"
        @current-change="loadPosts"
      />
    </div>

    <!-- ════════════════════════════════════════════════════════════
         帖子详情弹窗（含评论区）
         ════════════════════════════════════════════════════════════ -->
    <el-dialog
      v-model="detail.visible"
      title=""
      width="640px"
      top="3vh"
      class="detail-dialog"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div v-if="detail.loading" class="detail-loading">
        <el-skeleton :rows="6" animated />
      </div>

      <template v-else-if="detail.data">
        <div class="detail-scroll">
          <!-- ── 用户信息头部 ── -->
          <div class="detail-user">
            <div class="detail-avatar-frame" :class="'frame-' + (detail.data.avatarFrame || 'default')">
              <el-avatar :size="54" :src="formatAvatar(detail.data.avatarUrl)">
                <el-icon :size="22"><User /></el-icon>
              </el-avatar>
            </div>
            <div class="detail-user-info">
              <div class="detail-user-row">
                <span class="detail-username">{{ detail.data.username }}</span>
                <span v-if="detail.data.userTitle" class="detail-usertitle">{{ detail.data.userTitle }}</span>
              </div>
              <div class="detail-time"><el-icon :size="12"><Clock /></el-icon>{{ detail.data.createdAt }}</div>
            </div>
          </div>

          <!-- ── 正文 ── -->
          <h2 class="detail-title">{{ detail.data.title }}</h2>
          <p v-if="detail.data.description" class="detail-desc">{{ detail.data.description }}</p>

          <div v-if="detail.data.photoUrl" class="detail-img-wrapper">
            <el-image
              :src="detail.data.photoUrl"
              fit="contain"
              :preview-src-list="[detail.data.photoUrl]"
              preview-teleported
              style="width:100%;max-height:420px;border-radius:8px"
            />
          </div>

          <div class="detail-tags">
            <span v-if="detail.data.speciesName" class="dtag sp">🐟 {{ detail.data.speciesName }}</span>
            <span v-if="detail.data.locationName" class="dtag loc">📍 {{ detail.data.locationName }}</span>
            <span v-if="detail.data.observedAt" class="dtag dt">📅 {{ detail.data.observedAt }}</span>
          </div>

          <!-- ── 操作栏 ── -->
          <div class="detail-actions">
            <button class="daction-btn" :class="{ liked: detail.data.liked }" @click="toggleDetailLike">
              <el-icon :size="18"><CircleCheck /></el-icon>
              <span>{{ detail.data.likeCount || 0 }}</span>
            </button>
            <button class="daction-btn" :class="{ bookmarked: detail.data.bookmarked }" @click="toggleDetailBookmark">
              <el-icon :size="18"><Star /></el-icon>
              <span>{{ detail.data.bookmarked ? '已收藏' : '收藏' }}</span>
            </button>
          </div>

          <el-divider style="border-color:#e8e8e8;margin:12px 0" />

          <!-- ── 评论区 ── -->
          <div class="detail-comments">
            <div class="comment-header-bar">
              <span class="comment-heading">评论 ({{ detail.data.commentCount || 0 }})</span>
              <div class="comment-sort-tabs">
                <button
                  class="csort-btn"
                  :class="{ active: commentSort === 'latest' }"
                  @click="switchCommentSort('latest')"
                >最新</button>
                <button
                  class="csort-btn"
                  :class="{ active: commentSort === 'hot' }"
                  @click="switchCommentSort('hot')"
                >最热</button>
              </div>
            </div>

            <!-- 评论列表 -->
            <div v-if="commentsLoading" class="cloading"><el-skeleton :rows="2" animated /></div>

            <template v-else-if="detailComments.length">
              <div class="clist">
                <div v-for="c in detailComments" :key="c.id" class="citem">
                  <div class="c-avatar-frame" :class="'frame-' + (c.avatarFrame || 'default')">
                    <el-avatar :size="42" :src="formatAvatar(c.avatarUrl)">
                      <el-icon :size="14"><User /></el-icon>
                    </el-avatar>
                  </div>
                  <div class="cbody">
                    <div class="cmeta">
                      <span class="cuser">{{ c.username }}</span>
                      <span v-if="c.title" class="ctitle">{{ c.title }}</span>
                    </div>
                    <div class="ctext">{{ c.content }}</div>
                    <div class="cbar">
                      <span class="ctime">{{ c.createdAt }}</span>
                      <button class="cbar-btn" @click="startDetailReply(c)">
                        {{ detailReplying?.id === c.id ? '取消' : '回复' }}
                      </button>
                      <button class="cbar-btn like-btn" :class="{ liked: c.liked }" @click="toggleCommentLike(c)">
                        <el-icon :size="12"><CircleCheck /></el-icon> {{ c.likeCount || 0 }}
                      </button>
                      <button v-if="c.isOwner" class="cbar-btn danger" @click="deleteDetailComment(c)">删除</button>
                    </div>

                    <!-- 回复列表（自动展开最佳回复，其余折叠） -->
                    <div v-if="c.replyCount > 0 && detailReplies[c.id]" class="creplies">
                      <!-- 置顶回复：点赞最高（无点赞则最早） -->
                      <div class="creply top-reply">
                        <div class="c-avatar-frame mini" :class="'frame-' + (topReply(c).avatarFrame || 'default')">
                          <el-avatar :size="32" :src="formatAvatar(topReply(c).avatarUrl)">
                            <el-icon :size="12"><User /></el-icon>
                          </el-avatar>
                        </div>
                        <div class="cbody">
                          <div class="cmeta">
                            <span class="cuser">{{ topReply(c).username }}</span>
                            <span v-if="topReply(c).title" class="ctitle">{{ topReply(c).title }}</span>
                          </div>
                          <div class="ctext">
                            <span class="reply-to">回复 <em>@{{ c.username }}</em>：</span>
                            {{ topReply(c).content }}
                          </div>
                          <div class="cbar">
                            <span class="ctime">{{ topReply(c).createdAt }}</span>
                            <button class="cbar-btn like-btn" :class="{ liked: topReply(c).liked }" @click="toggleCommentLike(topReply(c))">
                              <el-icon :size="12"><CircleCheck /></el-icon> {{ topReply(c).likeCount || 0 }}
                            </button>
                            <button v-if="topReply(c).isOwner" class="cbar-btn danger" @click="deleteDetailReply(c, topReply(c))">删除</button>
                          </div>
                        </div>
                      </div>
                      <!-- 其余回复折叠 -->
                      <div v-if="detailRepliesShowAll[c.id]" class="more-replies">
                        <div v-for="r in detailReplies[c.id].slice(1)" :key="r.id" class="creply">
                          <div class="c-avatar-frame mini" :class="'frame-' + (r.avatarFrame || 'default')">
                            <el-avatar :size="32" :src="formatAvatar(r.avatarUrl)">
                              <el-icon :size="12"><User /></el-icon>
                            </el-avatar>
                          </div>
                          <div class="cbody">
                            <div class="cmeta">
                              <span class="cuser">{{ r.username }}</span>
                              <span v-if="r.title" class="ctitle">{{ r.title }}</span>
                            </div>
                            <div class="ctext">
                              <span class="reply-to">回复 <em>@{{ c.username }}</em>：</span>
                              {{ r.content }}
                            </div>
                            <div class="cbar">
                              <span class="ctime">{{ r.createdAt }}</span>
                              <button class="cbar-btn like-btn" :class="{ liked: r.liked }" @click="toggleCommentLike(r)">
                                <el-icon :size="12"><CircleCheck /></el-icon> {{ r.likeCount || 0 }}
                              </button>
                              <button v-if="r.isOwner" class="cbar-btn danger" @click="deleteDetailReply(c, r)">删除</button>
                            </div>
                          </div>
                        </div>
                      </div>
                      <div
                        v-if="detailReplies[c.id].length > 1"
                        class="load-replies"
                        @click="detailRepliesShowAll[c.id] = !detailRepliesShowAll[c.id]"
                      >
                        {{ detailRepliesShowAll[c.id] ? '收起回复' : `查看全部 ${c.replyCount} 条回复` }}
                      </div>
                    </div>
                    <div
                      v-else-if="c.replyCount > 0 && !detailReplies[c.id]"
                      class="load-replies"
                      @click="loadDetailReplies(c)"
                    >加载 {{ c.replyCount }} 条回复</div>
                  </div>
                </div>
              </div>
            </template>

            <div v-else class="no-cmt">暂无评论，来说两句吧～</div>

            <!-- 评论输入 -->
            <div class="cinput-area">
              <div v-if="detailReplying" class="cinput-hint">
                回复 <strong>{{ detailReplying.username }}</strong>
                <a class="cancel-reply" @click="cancelDetailReply">取消</a>
              </div>
              <div class="cinput-row">
                <el-input
                  v-model="detailCommentInput"
                  type="textarea"
                  :rows="2"
                  :placeholder="detailReplying ? `回复 ${detailReplying.username}...` : '写下你的评论...'"
                  maxlength="1000"
                  show-word-limit
                  resize="none"
                  class="cinput"
                />
                <el-button
                  type="primary"
                  :loading="commentSubmitting"
                  :disabled="!detailCommentInput.trim()"
                  @click="submitDetailComment"
                >发送</el-button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <el-empty v-else description="暂无数据" :image-size="80" />
    </el-dialog>

    <!-- ═══ 回到顶部 ═══ -->
    <el-backtop :right="30" :bottom="80" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuthStore } from "@/store/auth";
import { ElMessage } from "element-plus";
import {
  Plus, User, Clock, Location, Calendar,
  CircleCheck, ChatDotSquare, Star,
  Picture, Lightning, Search
} from "@element-plus/icons-vue";
import { getCommunityObservations, getCommunityObservationDetail } from "@/api/observation";
import { getComments, getReplies, createComment, deleteComment as deleteCommentApi } from "@/api/comment";
import { toggleLike as toggleLikeApi } from "@/api/like";
import { addBookmark, removeBookmark } from "@/api/bookmark";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

// ═══════════════════════════════════════════════════════════════
//  社区列表
// ═══════════════════════════════════════════════════════════════

const posts = ref([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(24);
const totalCount = ref(0);
const sortMode = ref("latest"); // latest | hot
const searchKeyword = ref('');

/** loadPosts 必须先定义，后续 handleSearch / switchSort 才能调用 */
const loadPosts = async () => {
  loading.value = true;
  try {
    const res = await getCommunityObservations({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sort: sortMode.value,
      keyword: searchKeyword.value.trim() || undefined,
    });
    if (res.data.success) {
      const d = res.data.data;
      posts.value = d.records || [];
      totalCount.value = d.total || 0;
    } else {
      posts.value = [];
      totalCount.value = 0;
    }
  } catch (err) {
    console.error("加载社区观察失败", err);
    ElMessage.error("加载失败");
    posts.value = [];
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pageNum.value = 1;
  loadPosts();
};

const switchSort = (mode) => {
  if (sortMode.value === mode) return;
  sortMode.value = mode;
  pageNum.value = 1;
  loadPosts();
};

/** 前端二次排序：最新按 createdAt（后端已排好），最热按 likeCount+commentCount */
const sortedPosts = computed(() => {
  if (sortMode.value === "hot") {
    return [...posts.value].sort(
      (a, b) => (b.likeCount + b.commentCount * 2) - (a.likeCount + a.commentCount * 2)
    );
  }
  return posts.value;
});

const formatAvatar = (url) => {
  if (!url) return "";
  if (url.startsWith("http") || url.startsWith("/api")) return url;
  return `/api${url}`;
};

const goPublish = () => router.push("/observation/publish");

// ═══════════════════════════════════════════════════════════════
//  详情弹窗
// ═══════════════════════════════════════════════════════════════

const detail = reactive({
  visible: false,
  loading: false,
  data: null,
});

const detailComments = ref([]);
const commentsLoading = ref(false);
const commentSort = ref("latest");
const detailCommentInput = ref("");
const commentSubmitting = ref(false);
const detailReplying = ref(null); // { id, username }
const detailReplies = reactive({});
const detailRepliesShowAll = reactive({});

const openDetail = async (post) => {
  detail.visible = true;
  detail.loading = true;
  detail.data = null;
  detailComments.value = [];
  detailReplying.value = null;
  detailCommentInput.value = "";
  cancelDetailReply();

  try {
    // 获取完整详情
    const res = await getCommunityObservationDetail(post.id);
    if (res.data.success) {
      detail.data = res.data.data;
    } else {
      // 降级使用列表数据
      detail.data = { ...post };
    }
  } catch {
    detail.data = { ...post };
  } finally {
    detail.loading = false;
  }

  // 加载评论
  await loadDetailComments();
};

// ── 详情内点赞 ──
const toggleDetailLike = async () => {
  if (!detail.data) return;
  try {
    const res = await toggleLikeApi("user_observation", detail.data.id);
    if (res.data.success) {
      detail.data.liked = res.data.data.liked;
      detail.data.likeCount = res.data.data.count;
    }
  } catch (err) { console.error(err); }
};

// ── 详情内收藏 ──
const toggleDetailBookmark = async () => {
  if (!detail.data) return;
  try {
    if (detail.data.bookmarked) {
      const res = await removeBookmark("user_observation", detail.data.id);
      if (res.data.success) {
        detail.data.bookmarked = false;
        ElMessage.success("已取消收藏");
      }
    } else {
      const res = await addBookmark("user_observation", detail.data.id);
      if (res.data.success) {
        detail.data.bookmarked = true;
        ElMessage.success("收藏成功");
      }
    }
  } catch (err) { console.error(err); }
};

// ═══════════════════════════════════════════════════════════════
//  评论
// ═══════════════════════════════════════════════════════════════

/** 取评论的最佳回复（点赞最高，无点赞则取最早） */
const topReply = (comment) => {
  const replies = detailReplies[comment.id];
  if (!replies || replies.length === 0) return null;
  return replies.reduce((best, r) => {
    const bestScore = best.likeCount || 0;
    const rScore = r.likeCount || 0;
    if (rScore > bestScore) return r;
    if (rScore === bestScore) return best; // 保持原有顺序（最早优先）
    return best;
  });
};

const loadDetailComments = async () => {
  if (!detail.data) return;
  commentsLoading.value = true;
  try {
    const res = await getComments("user_observation", detail.data.id, {
      params: { sort: commentSort.value },
    });
    if (res.data.success) {
      detailComments.value = res.data.data || [];
      // 有回复的评论自动加载其回复
      detailComments.value.forEach((c) => {
        if (c.replyCount > 0 && !detailReplies[c.id]) {
          loadDetailReplies(c);
        }
      });
    }
  } catch (err) {
    console.error("加载评论失败", err);
    detailComments.value = [];
  } finally {
    commentsLoading.value = false;
  }
};

const switchCommentSort = (mode) => {
  if (commentSort.value === mode) return;
  commentSort.value = mode;
  loadDetailComments();
  cancelDetailReply();
};

// ── 评论点赞 ──
const toggleCommentLike = async (comment) => {
  try {
    const res = await toggleLikeApi("comment", comment.id);
    if (res.data.success) {
      comment.liked = res.data.data.liked;
      comment.likeCount = res.data.data.count;
    }
  } catch (err) { console.error(err); }
};

// ── 提交评论 ──
const submitDetailComment = async () => {
  const content = detailCommentInput.value.trim();
  if (!content) return;

  commentSubmitting.value = true;
  try {
    const data = {
      targetType: "user_observation",
      targetId: detail.data.id,
      content,
    };
    if (detailReplying.value) {
      data.parentId = detailReplying.value.id;
    }

    const res = await createComment(data);
    if (res.data.success) {
      detailCommentInput.value = "";
      ElMessage.success("评论成功");
      detail.data.commentCount = (detail.data.commentCount || 0) + 1;
      cancelDetailReply();
      await loadDetailComments();
    } else {
      ElMessage.warning(res.data.message || "评论失败");
    }
  } catch (err) {
    ElMessage.error("评论失败");
  } finally {
    commentSubmitting.value = false;
  }
};

// ── 回复 ──
const startDetailReply = (comment) => {
  if (detailReplying.value?.id === comment.id) {
    cancelDetailReply();
    return;
  }
  detailReplying.value = { id: comment.id, username: comment.username };
  // 加载回复列表
  loadDetailReplies(comment);
  nextTick(() => {
    document.querySelector(".detail-dialog .cinput textarea")?.focus();
  });
};

const cancelDetailReply = () => {
  detailReplying.value = null;
};

// ── 加载回复（已加载则跳过） ──
const loadDetailReplies = async (comment) => {
  if (detailReplies[comment.id]) return;
  try {
    const res = await getReplies(comment.id);
    if (res.data.success) {
      detailReplies[comment.id] = res.data.data || [];
      // 按点赞数降序，无点赞按创建时间升序
      detailReplies[comment.id].sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0));
      detailRepliesShowAll[comment.id] = false; // 默认只展开最佳回复
    }
  } catch (err) {
    console.error("加载回复失败", err);
  }
};

// ── 删除评论 ──
const deleteDetailComment = async (comment) => {
  try {
    const res = await deleteCommentApi(comment.id);
    if (res.data.success) {
      ElMessage.success("已删除");
      const idx = detailComments.value.findIndex((c) => c.id === comment.id);
      if (idx !== -1) detailComments.value.splice(idx, 1);
      detail.data.commentCount = Math.max(0, (detail.data.commentCount || 0) - 1);
    }
  } catch (err) { ElMessage.error("删除失败"); }
};

// ── 删除回复 ──
const deleteDetailReply = async (comment, reply) => {
  try {
    const res = await deleteCommentApi(reply.id);
    if (res.data.success) {
      ElMessage.success("已删除");
      const arr = detailReplies[comment.id];
      if (arr) {
        const idx = arr.findIndex((r) => r.id === reply.id);
        if (idx !== -1) arr.splice(idx, 1);
      }
      detail.data.commentCount = Math.max(0, (detail.data.commentCount || 0) - 1);
    }
  } catch (err) { ElMessage.error("删除失败"); }
};

// ═══ 初始化 ═══
/** 打开指定 ID 的帖子详情（支持从收藏点击跳转） */
const openDetailById = async (obsId) => {
  // 先看看列表中是否已加载
  const found = posts.value.find((p) => p.id === Number(obsId));
  if (found) {
    await openDetail(found);
  } else {
    // 不在当前页，直接用 ID 获取详情
    detail.visible = true;
    detail.loading = true;
    detail.data = null;
    try {
      const res = await getCommunityObservationDetail(obsId);
      if (res.data.success) {
        detail.data = res.data.data;
        await loadDetailComments();
      } else {
        ElMessage.warning("记录不存在或已下架");
        detail.visible = false;
      }
    } catch (err) {
      ElMessage.error("加载失败");
      detail.visible = false;
    } finally {
      detail.loading = false;
    }
  }
};

onMounted(async () => {
  await loadPosts();
  // 检查 URL 参数：从收藏页跳转过来
  const detailId = route.query.detail;
  if (detailId) {
    await nextTick();
    openDetailById(detailId);
  }
});
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════════
   页面整体 — 亮色文字
   ═══════════════════════════════════════════════════════════ */
.community-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 16px 0 40px;
}

/* ═══ 头部 ═══ */
.community-header {
  margin-bottom: 20px;
  padding: 0 4px;
}

.header-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.page-title {
  font-size: 24px;
  font-weight: 800;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, #48cae4, #0077b6, #023e8a);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  white-space: nowrap;
  letter-spacing: 1px;
}

.title-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: rgba(0, 180, 216, 0.12);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(72, 202, 228, 0.25);
  font-size: 20px;
  -webkit-text-fill-color: initial;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-box .search-input {
  width: 240px;
}

.search-btn {
  border-radius: 20px !important;
  padding: 8px 20px !important;
  background: linear-gradient(135deg, #0077b6, #00b4d8) !important;
  border: none !important;
  font-weight: 600;
}

.search-btn:hover {
  background: linear-gradient(135deg, #005f8a, #0096c7) !important;
}

:deep(.search-input .el-input__wrapper) {
  background: rgba(255,255,255,0.92) !important;
  border: 1px solid rgba(0, 119, 182, 0.2) !important;
  border-radius: 20px !important;
  box-shadow: 0 1px 4px rgba(0, 119, 182, 0.06) !important;
}

:deep(.search-input .el-input__inner) {
  color: #1e293b;
}

:deep(.search-input .el-input__inner::placeholder) {
  color: #94a3b8;
}

:deep(.search-input .el-input__prefix .el-icon) {
  color: #94a3b8;
}

.header-bottom-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.page-subtitle {
  font-size: 13px;
  color: #64748b;
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.stat-badge {
  font-size: 13px;
  color: #334155;
  background: rgba(255,255,255,0.85);
  padding: 4px 12px;
  border-radius: 20px;
  white-space: nowrap;
  border: 1px solid rgba(0,0,0,0.06);
}
.publish-btn { border-radius: 20px; }

/* ═══ 排序切换 — 单按钮 ═══ */
.sort-toggle {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 16px;
  border: 1px solid rgba(0,0,0,0.10);
  border-radius: 20px;
  background: rgba(255,255,255,0.85);
  color: #334155;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s;
  white-space: nowrap;
}
.sort-toggle:hover {
  background: #ffffff;
  color: #1e293b;
  border-color: rgba(0,119,182,0.3);
}

/* ═══ 加载骨架 ═══ */
.loading-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 16px;
}
.skeleton-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid rgba(0,0,0,0.06);
}

/* ═══════════════════════════════════════════════════════════
   卡片网格
   ═══════════════════════════════════════════════════════════ */
.post-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 16px;
}

.post-card {
  background: #fff;
  border: 1px solid rgba(0,0,0,0.08);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s ease;
}
.post-card:hover {
  border-color: rgba(22,119,255,0.35);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.10);
}

.card-thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: rgba(0,0,0,0.3);
}
.card-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.post-card:hover .card-thumb img {
  transform: scale(1.06);
}
.card-no-img {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255,255,255,0.15);
}

.card-stats-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 6px 10px;
  background: linear-gradient(transparent, rgba(0,0,0,0.65));
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: rgba(255,255,255,0.85);
}
.card-stats-overlay span {
  display: flex;
  align-items: center;
  gap: 3px;
}

.card-body {
  padding: 12px 14px 14px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-user {
  display: flex;
  align-items: center;
  gap: 6px;
}
.card-avatar-frame {
  display: inline-flex;
  border-radius: 50%;
  padding: 3px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  align-self: flex-start;
}
.card-avatar-frame .el-avatar {
  display: block;
  border-radius: 50%;
  box-sizing: content-box;
}
.card-username {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-badge {
  font-size: 10px;
  color: #b8860b;
  background: rgba(255,215,0,0.15);
  padding: 0 6px;
  border-radius: 8px;
  white-space: nowrap;
}

/* ═══ 分页 ═══ */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

/* ═══════════════════════════════════════════════════════════
   详情弹窗
   ═══════════════════════════════════════════════════════════ */
.detail-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
  max-height: 80vh;
  overflow-y: auto;
}
.detail-loading { padding: 40px 0; }
.detail-scroll { max-height: 70vh; overflow-y: auto; padding-right: 4px; }

.detail-user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.detail-avatar-frame {
  display: inline-flex;
  border-radius: 50%;
  padding: 3px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  align-self: flex-start;
}
.detail-avatar-frame .el-avatar {
  display: block;
  border-radius: 50%;
  box-sizing: content-box;
}
.detail-user-info { flex:1; min-width:0; }
.detail-user-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.detail-username { font-size:15px; font-weight:600; color:#1a1a2e; }
.detail-usertitle { font-size:11px; color:#b8860b; background:rgba(255,215,0,0.15); padding:1px 8px; border-radius:10px; }
.detail-time { display:flex; align-items:center; gap:4px; font-size:12px; color:#999; margin-top:2px; }

.detail-title { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 0 10px; }
.detail-desc { font-size:14px; color:#444; line-height:1.7; margin:0 0 14px; white-space:pre-wrap; word-break:break-word; }
.detail-img-wrapper { margin-bottom:12px; display:flex; justify-content:center; background:rgba(0,0,0,0.03); border-radius:8px; overflow:hidden; }

.detail-tags { display:flex; flex-wrap:wrap; gap:8px; margin-bottom:12px; }
.dtag { font-size:12px; padding:3px 10px; border-radius:14px; white-space:nowrap; }
.dtag.sp { color:#1a9bc4; background:rgba(26,155,196,0.08); }
.dtag.loc { color:#67c23a; background:rgba(103,194,58,0.08); }
.dtag.dt { color:#999; background:rgba(0,0,0,0.04); }

.detail-actions { display:flex; gap:8px; }
.daction-btn {
  display:flex; align-items:center; gap:5px; padding:6px 16px;
  border:1px solid #e8e8e8; border-radius:8px; background:#fafafa;
  color:#666; font-size:13px; cursor:pointer; transition:all 0.2s;
}
.daction-btn:hover { background:#f0f0f0; color:#333; }
.daction-btn.liked { color:#409eff; background:rgba(64,158,255,0.06); border-color:#409eff; }
.daction-btn.bookmarked { color:#e6a23c; background:rgba(230,162,60,0.06); border-color:#e6a23c; }

/* ═══ 评论区(弹窗内) ═══ */
.comment-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.comment-heading { font-size:15px; font-weight:600; color:#1a1a2e; }
.comment-sort-tabs {
  display: flex;
  background: #f0f2f5;
  border-radius: 6px;
  padding: 2px;
}
.csort-btn {
  padding: 3px 10px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #999;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.csort-btn.active { background:#fff; color:#409eff; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,0.08); }
.csort-btn:hover { color:#666; }

.cloading { padding: 16px 0; }
.clist { display:flex; flex-direction:column; gap:14px; margin-bottom:14px; }
.citem { display:flex; gap:10px; }
/* ── 头像框 — 与顶栏完全一致 ── */
.c-avatar-frame {
  display: inline-flex;
  border-radius: 50%;
  padding: 3px;
  flex-shrink: 0;
  transition: all 0.3s ease;
  align-items: center;
  justify-content: center;
  align-self: flex-start;
}
.c-avatar-frame .el-avatar {
  display: block;
  border-radius: 50%;
  box-sizing: content-box;
}
.c-avatar-frame.mini {
  padding: 2px;
}
.c-avatar-frame.mini .el-avatar { width:32px; height:32px; }
.cbody { flex:1; min-width:0; }
.cmeta { display:flex; align-items:center; gap:6px; flex-wrap:wrap; margin-bottom:2px; }
.cuser { font-size:13px; font-weight:600; color:#2c3e50; }
.ctitle { font-size:10px; color:#b8860b; background:rgba(255,215,0,0.15); padding:0 6px; border-radius:8px; }
.ctext { font-size:13px; color:#333; line-height:1.5; word-break:break-word; }
.cbar { display:flex; align-items:center; gap:10px; margin-top:3px; }
.ctime { font-size:11px; color:#bbb; }
.cbar-btn {
  font-size: 11px;
  color: #999;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  gap: 2px;
  transition: color 0.2s;
}
.cbar-btn:hover { color: #409eff; }
.cbar-btn.danger:hover { color: #f56c6c; }
.cbar-btn.liked { color: #409eff; }
.cbar-btn.like-btn:hover { color: #409eff; }

.reply-to { font-size:12px; color:#999; }
.reply-to em { font-style:normal; color:#409eff; }

.creplies {
  margin-top: 10px;
  padding-left: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.creply {
  display: flex;
  gap: 8px;
  padding: 8px 10px;
  background: rgba(0,0,0,0.02);
  border-radius: 8px;
}
.creply:hover { background: rgba(0,0,0,0.035); }
.top-reply { background: rgba(64,158,255,0.04); border:1px solid rgba(64,158,255,0.1); }
.top-reply:hover { background: rgba(64,158,255,0.07); }
.more-replies { display:flex; flex-direction:column; gap:8px; }
.load-replies { font-size:12px; color:#409eff; cursor:pointer; margin-top:4px; }
.load-replies:hover { color:#66b1ff; }
.no-cmt { text-align:center; padding:20px 0; color:#bbb; font-size:13px; }

.cinput-area { margin-top:12px; border-top:1px solid #eee; padding-top:12px; }
.cinput-hint { font-size:12px; color:#999; margin-bottom:8px; }
.cancel-reply { color:#409eff; cursor:pointer; margin-left:4px; font-size:12px; }
.cancel-reply:hover { color:#66b1ff; }
.cinput-row { display:flex; gap:10px; align-items:flex-start; }
.cinput { flex:1; }
.cinput :deep(.el-textarea__inner) {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  color: #333;
  border-radius: 8px;
  font-size: 13px;
}
.cinput :deep(.el-textarea__inner:focus) {
  border-color: #409eff;
  background: #fff;
}
.cinput :deep(.el-input__count) {
  color: #bbb;
  background: transparent;
}

/* ═══ 头像框（同步个人中心精良版） ═══ */

/* 默认边框 */
.frame-default {
  background: #dcdfe6;
}

/* 黄金边框 */
.frame-gold {
  background: linear-gradient(135deg, #f6d365, #fda085);
  box-shadow: 0 0 14px rgba(246, 211, 101, 0.6);
}

/* 深海边框 */
.frame-ocean {
  background: linear-gradient(135deg, #00d2ff, #165dff);
  box-shadow: 0 0 16px rgba(0, 210, 255, 0.6);
}

/* 彩虹边框 */
.frame-rainbow {
  background: linear-gradient(90deg, #ff6b6b, #feca57, #48dbfb, #ff9ff3);
  background-size: 200% 100%;
  animation: frame-rainbow-spin 3s linear infinite;
}
@keyframes frame-rainbow-spin {
  0% { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}

/* 火焰边框 */
.frame-flame {
  background: linear-gradient(135deg, #ff4500, #ff8c00, #ffd700);
  box-shadow: 0 0 18px rgba(255, 69, 0, 0.6);
}

/* 烈焰虚线 — 橙红渐变 + 脉冲光晕 */
.frame-dashed {
  background: repeating-conic-gradient(
    #fd7000 0deg 18deg,
    transparent 18deg 36deg
  );
  box-shadow: 0 0 14px rgba(237, 35, 76, 0.5);
  animation: frame-dash-glow 2s ease-in-out infinite;
}
@keyframes frame-dash-glow {
  0%, 100% { box-shadow: 0 0 14px rgba(237, 35, 76, 0.5); }
  50% { box-shadow: 0 0 26px rgba(237, 35, 76, 0.85); }
}

/* 霓虹光效 — 青 → 品红渐变色 + 双重光晕 */
.frame-neon {
  background: linear-gradient(135deg, #00fff5, #ff00e4);
  box-shadow:
    0 0 14px rgba(0, 255, 245, 0.7),
    0 0 28px rgba(255, 0, 228, 0.4);
  animation: frame-neon-pulse 3s ease-in-out infinite;
}
@keyframes frame-neon-pulse {
  0%, 100% { box-shadow: 0 0 14px rgba(0, 255, 245, 0.7), 0 0 28px rgba(255, 0, 228, 0.4); }
  50% { box-shadow: 0 0 24px rgba(0, 255, 245, 0.9), 0 0 44px rgba(255, 0, 228, 0.6); }
}

/* 极光幻彩 — 绿 → 蓝 → 紫 流动渐变 */
.frame-aurora {
  background: linear-gradient(135deg, #00f260, #0575e6, #a855f7);
  background-size: 200% 200%;
  animation: frame-aurora-shift 4s ease infinite;
  box-shadow: 0 0 16px rgba(5, 117, 230, 0.5);
}
@keyframes frame-aurora-shift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* 赛博光轮 — 青紫脉冲 */
.frame-crystal {
  background: linear-gradient(135deg, #00f5ff, #ff00e5);
  box-shadow:
    0 0 16px rgba(0, 245, 255, 0.5),
    0 0 32px rgba(255, 0, 229, 0.3),
    inset 0 0 8px rgba(255, 255, 255, 0.4);
  animation: crystal-breath 2s ease-in-out infinite;
}
@keyframes crystal-breath {
  0%, 100% { box-shadow: 0 0 16px rgba(0, 245, 255, 0.5), 0 0 32px rgba(255, 0, 229, 0.3); }
  50% { box-shadow: 0 0 28px rgba(0, 245, 255, 0.8), 0 0 56px rgba(255, 0, 229, 0.5); }
}

/* 紫金皇冠 — 深紫 + 金线点缀 */
.frame-royal {
  background: linear-gradient(135deg, #6c3cc7, #9b59b6, #f1c40f);
  box-shadow: 0 0 16px rgba(108, 60, 199, 0.6);
}

/* 所有非默认框：头像加 3px 白边（box-sizing:content-box 保持内容区不缩） */
.frame-gold .el-avatar,
.frame-ocean .el-avatar,
.frame-rainbow .el-avatar,
.frame-flame .el-avatar,
.frame-dashed .el-avatar,
.frame-neon .el-avatar,
.frame-aurora .el-avatar,
.frame-royal .el-avatar {
  border: 3px solid #fff;
  border-radius: 50%;
}
.frame-crystal .el-avatar {
  border: 3px solid rgba(255, 255, 255, 0.9);
  border-radius: 50%;
}
.frame-default .el-avatar {
  border: 3px solid rgba(255, 255, 255, 0.7);
  border-radius: 50%;
}

/* ═══ 滚动条（弹窗内） ═══ */
.detail-scroll::-webkit-scrollbar,
.detail-dialog :deep(.el-dialog__body)::-webkit-scrollbar {
  width: 4px;
}
.detail-scroll::-webkit-scrollbar-track,
.detail-dialog :deep(.el-dialog__body)::-webkit-scrollbar-track {
  background: transparent;
}
.detail-scroll::-webkit-scrollbar-thumb,
.detail-dialog :deep(.el-dialog__body)::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.15);
  border-radius: 4px;
}

/* ═══ 响应式 ═══ */
@media (max-width: 768px) {
  .community-page { padding: 12px 8px; }
  .post-grid { grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 10px; }
  .header-top-row {
    flex-wrap: wrap;
  }
  .search-box {
    width: 100%;
  }
  .search-box .search-input {
    flex: 1;
  }
  .search-btn {
    white-space: nowrap;
  }
  .header-bottom-row {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  .page-subtitle { display: none; }
  .header-tools {
    justify-content: space-between;
    width: 100%;
  }
  .detail-dialog :deep(.el-dialog__body) { padding: 16px; }
}
</style>
