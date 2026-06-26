<template>
  <div class="post-detail-page">
    <!-- ===== 顶部导航栏 ===== -->
    <div class="detail-topbar">
      <button class="back-btn" @click="goBack">
        <el-icon :size="20"><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <span class="topbar-title">帖子详情</span>
    </div>

    <!-- 加载态 -->
    <div v-if="loading" class="glass-pill">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else-if="data">
      <!-- 用户信息 + 帖子内容区块 -->
      <div class="glass-pill">
        <div class="detail-user">
          <div class="detail-avatar-frame clickable-avatar" :class="'frame-' + (data.avatarFrame || 'default')" @click.stop="openUserProfile(data.userId)">
            <el-avatar :size="54" :src="formatAvatar(data.avatarUrl)">
              <el-icon :size="22"><User /></el-icon>
            </el-avatar>
          </div>
          <div class="detail-user-info">
            <div class="detail-user-row">
              <span class="detail-username">{{ data.username }}</span>
              <span v-if="data.userTitle" class="detail-usertitle">{{ data.userTitle }}</span>
            </div>
            <div class="detail-time">
              <el-icon :size="12"><Clock /></el-icon>{{ data.createdAt }}
            </div>
          </div>
        </div>

        <!-- 有图：标准图文排版 -->
        <template v-if="data.photoUrl">
          <h2 class="detail-title">{{ data.title }}</h2>
          <p v-if="data.description" class="detail-desc">{{ data.description }}</p>
          <div class="detail-img-wrapper">
            <el-image
                :src="data.photoUrl"
                fit="contain"
                :preview-src-list="[data.photoUrl]"
                preview-teleported
                style="width:100%;max-height:420px;border-radius:8px"
            />
          </div>
          <div class="detail-tags">
            <span v-if="data.speciesName" class="dtag sp">🐟 {{ data.speciesName }}</span>
            <span v-if="data.locationName" class="dtag loc">📍 {{ data.locationName }}</span>
            <span v-if="data.observedAt" class="dtag dt">📅 {{ data.observedAt }}</span>
          </div>
          <div class="detail-actions">
            <button class="daction-btn" :class="{ liked: data.liked }" @click="toggleDetailLike">
              <el-icon :size="18"><CircleCheck /></el-icon>
              <span>{{ data.likeCount || 0 }}</span>
            </button>
            <button class="daction-btn" :class="{ bookmarked: data.bookmarked }" @click="toggleDetailBookmark">
              <el-icon :size="18"><Star /></el-icon>
              <span>{{ data.bookmarked ? '已收藏' : '收藏' }}</span>
            </button>
          </div>
        </template>

        <!-- 无图：纯文字紧凑排版 -->
        <div v-else class="text-only-detail">
          <h2 class="detail-title text-only-title">{{ data.title }}</h2>
          <p v-if="data.description" class="text-only-desc">{{ data.description }}</p>
          <div class="detail-tags">
            <span v-if="data.speciesName" class="dtag sp">🐟 {{ data.speciesName }}</span>
            <span v-if="data.locationName" class="dtag loc">📍 {{ data.locationName }}</span>
            <span v-if="data.observedAt" class="dtag dt">📅 {{ data.observedAt }}</span>
          </div>
          <div class="detail-actions text-only-actions">
            <button class="daction-btn" :class="{ liked: data.liked }" @click="toggleDetailLike">
              <el-icon :size="18"><CircleCheck /></el-icon>
              <span>{{ data.likeCount || 0 }}</span>
            </button>
            <button class="daction-btn" :class="{ bookmarked: data.bookmarked }" @click="toggleDetailBookmark">
              <el-icon :size="18"><Star /></el-icon>
              <span>{{ data.bookmarked ? '已收藏' : '收藏' }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- ===== 评论区区块 ===== -->
      <div class="glass-pill">
        <div id="comments" class="detail-comments">
          <div class="comment-header-bar">
            <span class="comment-heading">评论 ({{ data.commentCount || 0 }})</span>
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

          <div v-if="commentsLoading" class="cloading"><el-skeleton :rows="2" animated /></div>

          <template v-else-if="detailComments.length">
            <div class="clist">
              <div v-for="c in detailComments" :key="c.id" class="citem">
                <div class="c-avatar-frame clickable-avatar" :class="'frame-' + (c.avatarFrame || 'default')" @click.stop="openUserProfile(c.userId)">
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

                  <div v-if="c.replyCount > 0 && detailReplies[c.id]" class="creplies">
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
  </div>

  <el-dialog v-model="userProfileVisible" title="用户信息" width="380px" class="glass-dialog">
    <div v-loading="userProfileLoading" class="user-pop-profile">
      <div class="up-header">
        <div class="detail-avatar-frame" :class="'frame-' + (userProfileData.avatarFrame || 'default')">
          <el-avatar :size="64" :src="formatAvatar(userProfileData.avatarUrl)">
            <el-icon :size="24"><User /></el-icon>
          </el-avatar>
        </div>
        <div class="up-info">
          <div class="up-name">
            {{ userProfileData.username }}
            <span class="up-level">Lv.{{ userProfileData.level }}</span>
          </div>

          <div class="up-tags">
            <span v-if="userProfileData.userTitle && userProfileData.userTitle !== '__none__'" class="up-title">
              {{ userProfileData.userTitle }}
            </span>
            <span class="up-likes">
               🔥 获赞 {{ userProfileData.totalLikes || 0 }}
            </span>
          </div>

        </div>
      </div>

      <div class="up-divider">TA 的过往发帖</div>

      <div class="up-post-list">
        <div
            v-for="post in userProfileData.posts"
            :key="post.id"
            class="up-post-item"
            @click="goToUserPost(post.id)"
        >
          <el-icon class="post-icon"><Document /></el-icon>
          <span class="up-post-title">{{ post.title }}</span>

          <div class="up-post-stats">
            <span title="点赞数"><el-icon><Pointer /></el-icon> {{ post.like_count || 0 }}</span>
            <span title="评论数"><el-icon><ChatDotRound /></el-icon> {{ post.comment_count || 0 }}</span>
            <span title="收藏数"><el-icon><Star /></el-icon> {{ post.bookmark_count || 0 }}</span>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuthStore } from "@/store/auth";
import { ElMessage } from "element-plus";
import { ArrowLeft, User, Clock, CircleCheck, Star, Pointer ,ChatDotRound, Document } from "@element-plus/icons-vue";
import {getCommunityObservationDetail, getPublicProfile} from "@/api/observation";
import { getComments, getReplies, createComment, deleteComment as deleteCommentApi } from "@/api/comment";
import { toggleLike as toggleLikeApi } from "@/api/like";
import { addBookmark, removeBookmark } from "@/api/bookmark";


const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const loading = ref(false);
const data = ref(null);

const detailComments = ref([]);
const commentsLoading = ref(false);
const commentSort = ref("latest");
const detailCommentInput = ref("");
const commentSubmitting = ref(false);
const detailReplying = ref(null);
const detailReplies = reactive({});
const detailRepliesShowAll = reactive({});

const formatAvatar = (url) => {
  if (!url) return "";
  if (url.startsWith("http") || url.startsWith("/api")) return url;
  return `/api${url}`;
};

const goBack = () => {
  // 优先返回上一页；若无历史记录则回社区列表
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push("/map-explore");
  }
};

const loadDetail = async (obsId) => {
  loading.value = true;
  window.scrollTo(0, 0);
  try {
    const res = await getCommunityObservationDetail(obsId);
    if (res.data.success) {
      data.value = res.data.data;
    } else {
      ElMessage.warning("记录不存在或已下架");
      data.value = null;
    }
  } catch (err) {
    ElMessage.error("加载帖子详情失败");
    data.value = null;
  } finally {
    loading.value = false;
  }

  if (data.value) {
    await loadDetailComments();
    // 等评论区渲染完成后定位页面位置
    await nextTick();
    if (route.hash === '#comments') {
      const el = document.getElementById('comments');
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
      window.scrollTo(0, 0);
    }
  }
};

const toggleDetailLike = async () => {
  if (!data.value) return;
  try {
    const res = await toggleLikeApi("user_observation", data.value.id);
    if (res.data.success) {
      data.value.liked = res.data.data.liked;
      data.value.likeCount = res.data.data.count;
    }
  } catch (err) { console.error(err); }
};

const toggleDetailBookmark = async () => {
  if (!data.value) return;
  try {
    if (data.value.bookmarked) {
      const res = await removeBookmark("user_observation", data.value.id);
      if (res.data.success) {
        data.value.bookmarked = false;
        ElMessage.success("已取消收藏");
      }
    } else {
      const res = await addBookmark("user_observation", data.value.id);
      if (res.data.success) {
        data.value.bookmarked = true;
        ElMessage.success("收藏成功");
      }
    }
  } catch (err) { console.error(err); }
};

const topReply = (comment) => {
  const replies = detailReplies[comment.id];
  if (!replies || replies.length === 0) return null;
  return replies.reduce((best, r) => {
    const bestScore = best.likeCount || 0;
    const rScore = r.likeCount || 0;
    if (rScore > bestScore) return r;
    if (rScore === bestScore) return best;
    return best;
  });
};

const loadDetailComments = async () => {
  if (!data.value) return;
  commentsLoading.value = true;
  try {
    const res = await getComments("user_observation", data.value.id, {
      params: { sort: commentSort.value },
    });
    if (res.data.success) {
      detailComments.value = res.data.data || [];
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

const toggleCommentLike = async (comment) => {
  try {
    const res = await toggleLikeApi("comment", comment.id);
    if (res.data.success) {
      comment.liked = res.data.data.liked;
      comment.likeCount = res.data.data.count;
    }
  } catch (err) { console.error(err); }
};

const submitDetailComment = async () => {
  const content = detailCommentInput.value.trim();
  if (!content || !data.value) return;

  commentSubmitting.value = true;
  try {
    const payload = {
      targetType: "user_observation",
      targetId: data.value.id,
      content,
    };
    if (detailReplying.value) {
      payload.parentId = detailReplying.value.id;
    }

    const res = await createComment(payload);
    if (res.data.success) {
      detailCommentInput.value = "";
      ElMessage.success("评论成功");
      data.value.commentCount = (data.value.commentCount || 0) + 1;
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

const startDetailReply = (comment) => {
  if (detailReplying.value?.id === comment.id) {
    cancelDetailReply();
    return;
  }
  detailReplying.value = { id: comment.id, username: comment.username };
  loadDetailReplies(comment);
  nextTick(() => {
    document.querySelector(".cinput textarea")?.focus();
  });
};

const cancelDetailReply = () => {
  detailReplying.value = null;
};

const loadDetailReplies = async (comment) => {
  if (detailReplies[comment.id]) return;
  try {
    const res = await getReplies(comment.id);
    if (res.data.success) {
      detailReplies[comment.id] = res.data.data || [];
      detailReplies[comment.id].sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0));
      detailRepliesShowAll[comment.id] = false;
    }
  } catch (err) {
    console.error("加载回复失败", err);
  }
};

const deleteDetailComment = async (comment) => {
  try {
    const res = await deleteCommentApi(comment.id);
    if (res.data.success) {
      ElMessage.success("已删除");
      const idx = detailComments.value.findIndex((c) => c.id === comment.id);
      if (idx !== -1) detailComments.value.splice(idx, 1);
      if (data.value) data.value.commentCount = Math.max(0, (data.value.commentCount || 0) - 1);
    }
  } catch (err) { ElMessage.error("删除失败"); }
};

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
      if (data.value) data.value.commentCount = Math.max(0, (data.value.commentCount || 0) - 1);
    }
  } catch (err) { ElMessage.error("删除失败"); }
};

onMounted(() => {
  const obsId = route.params.id;
  if (obsId) {
    loadDetail(obsId);
  }
});

// 路由参数变化时重新加载（解决从详情A返回再进详情B时组件复用的问题）
watch(() => route.params.id, (newId) => {
  if (newId) {
    // 重置评论状态
    detailComments.value = [];
    detailReplying.value = null;
    detailCommentInput.value = "";
    Object.keys(detailReplies).forEach(k => delete detailReplies[k]);
    Object.keys(detailRepliesShowAll).forEach(k => delete detailRepliesShowAll[k]);
    loadDetail(newId);
  }
});

// ================= 用户微名片逻辑 =================
const userProfileVisible = ref(false);
const userProfileLoading = ref(false);
const userProfileData = ref({
  username: "",
  avatarUrl: "",
  avatarFrame: "default",
  userTitle: "",
  level: 1,
  totalLikes: 0,
  posts: []
});

// 打开用户主页
const openUserProfile = async (userId) => {
  if (!userId) return;
  userProfileVisible.value = true;
  userProfileLoading.value = true;
  try {
    const res = await getPublicProfile(userId);
    if (res.data.success) {
      userProfileData.value = res.data.data;
    }
  } catch (err) {
    ElMessage.error("获取用户信息失败");
  } finally {
    userProfileLoading.value = false;
  }
};

// 跳转到该用户的帖子
const goToUserPost = (postId) => {
  userProfileVisible.value = false;
  // 跳转到对应的详情页
  router.push(`/obs-community/detail/${postId}`);
};

// 🌟 核心修复：监听路由变化，解决从帖子A跳转到帖子B时页面数据不刷新的问题
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    loadDetail(newId);
    window.scrollTo({ top: 0, behavior: 'smooth' }); // 回到页面顶部
  }
});
</script>

<style scoped>
/* ═══ 页面布局 ═══ */
.post-detail-page {
  max-width: 960px;
  margin: 0 auto;
  min-height: 100vh;
  background: transparent;
  padding: 0 12px 40px;
}

/* 顶部栏 — 液态玻璃药丸框风格 */
.detail-topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  position: sticky;
  top: 12px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
  width: fit-content;
  min-width: 200px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(0, 0, 0, 0.10);
  border-radius: 20px;
  padding: 6px 16px;
  color: #334155;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.25s;
}
.back-btn:hover {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(0, 0, 0, 0.18);
  color: #1a1a2e;
}

.topbar-title {
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(135deg, #48cae4, #0077b6, #023e8a);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
}

/* 液态玻璃药丸框 */
.glass-pill {
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 24px;
  padding: 28px 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}
.glass-pill:last-child {
  margin-bottom: 0;
}

.detail-loading { padding: 40px 20px; }

/* 用户信息 */
.detail-user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.detail-avatar-frame {
  display: inline-flex;
  border-radius: 50%;
  padding: 3px;
}
.detail-avatar-frame .el-avatar {
  display: block;
  border-radius: 50%;
  box-sizing: content-box;
}
.detail-user-info { flex: 1; min-width: 0; }
.detail-user-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.detail-username { font-size: 15px; font-weight: 600; color: #1a1a2e; }
.detail-usertitle { font-size: 11px; color: #b8860b; background: rgba(255, 215, 0, 0.15); padding: 1px 8px; border-radius: 10px; }
.detail-time { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #999; margin-top: 2px; }

/* 帖子内容 */
.detail-title { font-size: 20px; font-weight: 600; color: #1a1a2e; margin: 0 0 10px; }
.detail-desc { font-size: 14px; color: #444; line-height: 1.7; margin: 0 0 14px; white-space: pre-wrap; word-break: break-word; }
.detail-img-wrapper { margin-bottom: 12px; display: flex; justify-content: center; background: rgba(0, 0, 0, 0.03); border-radius: 8px; overflow: hidden; }

/* 无图纯文字详情 — 居中排版 + 限制阅读宽度 */
.text-only-detail {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 8px 0 0;
}

.text-only-title {
  max-width: 600px;
  margin-bottom: 14px !important;
  font-size: 22px;
}

.text-only-desc {
  max-width: 600px;
  width: 100%;
  margin: 0 auto 8px;
  font-size: 15px;
  color: #444;
  line-height: 1.85;
  white-space: pre-wrap;
  word-break: break-word;
  text-align: left;
}

.text-only-actions {
  margin-top: 0;
}

/* 标签 */
.detail-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.text-only-detail .detail-tags { margin-bottom: 10px; }
.dtag { font-size: 12px; padding: 3px 10px; border-radius: 14px; white-space: nowrap; }
.dtag.sp { color: #1a9bc4; background: rgba(26, 155, 196, 0.08); }
.dtag.loc { color: #67c23a; background: rgba(103, 194, 58, 0.08); }
.dtag.dt { color: #999; background: rgba(0, 0, 0, 0.04); }

/* 操作按钮 */
.detail-actions { display: flex; gap: 8px; }
.daction-btn {
  display: flex; align-items: center; gap: 5px; padding: 6px 16px;
  border: 1px solid #e8e8e8; border-radius: 8px; background: #fafafa;
  color: #666; font-size: 13px; cursor: pointer; transition: all 0.2s;
}
.daction-btn:hover { background: #f0f0f0; color: #333; }
.daction-btn.liked { color: #409eff; background: rgba(64, 158, 255, 0.06); border-color: #409eff; }
.daction-btn.bookmarked { color: #e6a23c; background: rgba(230, 162, 60, 0.06); border-color: #e6a23c; }

/* ═══ 评论区 ═══ */
.comment-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.comment-heading { font-size: 15px; font-weight: 600; color: #1a1a2e; }
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
.csort-btn.active { background: #fff; color: #409eff; font-weight: 600; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08); }
.csort-btn:hover { color: #666; }

.cloading { padding: 16px 0; }
.clist { display: flex; flex-direction: column; gap: 14px; margin-bottom: 14px; }
.citem { display: flex; gap: 10px; }

.c-avatar-frame {
  display: inline-flex;
  border-radius: 50%;
  padding: 3px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  align-self: flex-start;
}
.c-avatar-frame .el-avatar {
  display: block;
  border-radius: 50%;
  box-sizing: content-box;
}
.c-avatar-frame.mini { padding: 2px; }
.c-avatar-frame.mini .el-avatar { width: 32px; height: 32px; }

.cbody { flex: 1; min-width: 0; }
.cmeta { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-bottom: 2px; }
.cuser { font-size: 13px; font-weight: 600; color: #2c3e50; }
.ctitle { font-size: 10px; color: #b8860b; background: rgba(255, 215, 0, 0.15); padding: 0 6px; border-radius: 8px; }
.ctext { font-size: 13px; color: #333; line-height: 1.5; word-break: break-word; }
.cbar { display: flex; align-items: center; gap: 10px; margin-top: 3px; }
.ctime { font-size: 11px; color: #bbb; }
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

.reply-to { font-size: 12px; color: #999; }
.reply-to em { font-style: normal; color: #409eff; }

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
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
}
.creply:hover { background: rgba(0, 0, 0, 0.035); }
.top-reply { background: rgba(64, 158, 255, 0.04); border: 1px solid rgba(64, 158, 255, 0.1); }
.top-reply:hover { background: rgba(64, 158, 255, 0.07); }
.more-replies { display: flex; flex-direction: column; gap: 8px; }
.load-replies { font-size: 12px; color: #409eff; cursor: pointer; margin-top: 4px; }
.load-replies:hover { color: #66b1ff; }
.no-cmt { text-align: center; padding: 20px 0; color: #bbb; font-size: 13px; }

.cinput-area { margin-top: 12px; border-top: 1px solid #eee; padding-top: 12px; }
.cinput-hint { font-size: 12px; color: #999; margin-bottom: 8px; }
.cancel-reply { color: #409eff; cursor: pointer; margin-left: 4px; font-size: 12px; }
.cancel-reply:hover { color: #66b1ff; }
.cinput-row { display: flex; gap: 10px; align-items: flex-start; }
.cinput { flex: 1; }
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

/* ================== 用户微名片样式 ================== */
.clickable-avatar {
  cursor: pointer;
  transition: transform 0.2s ease;
}
.clickable-avatar:hover {
  transform: scale(1.08);
}

.user-pop-profile {
  padding: 5px 0;
}
.up-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.up-info { flex: 1; }
.up-name {
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
  display: flex;
  align-items: center;
  gap: 8px;
}
/* 等级标签样式 */
.up-level {
  font-size: 12px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, #ff7a00, #ff004d);
  padding: 2px 8px;
  border-radius: 10px;
}
.up-title {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  color: #b8860b;
  background: rgba(255, 215, 0, 0.15);
  padding: 2px 10px;
  border-radius: 12px;
  border: 1px solid rgba(255, 215, 0, 0.3);
}

.up-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.up-likes {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 600;
  color: #ff5722;
  background: rgba(255, 87, 34, 0.1);
  padding: 2px 10px;
  border-radius: 12px;
  border: 1px solid rgba(255, 87, 34, 0.2);
}
.up-divider {
  font-size: 13px;
  font-weight: 600;
  color: #86909c;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}
.up-divider::before, .up-divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 0 10px;
}

.up-post-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 260px;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 6px 12px 4px;
}

.up-post-list::-webkit-scrollbar { width: 4px; }
.up-post-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}
.up-post-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}

/* 单个帖子卡片样式 */
.up-post-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #f8fafc; /* 替换掉原本生硬的纯灰，改为清爽的蓝灰色系 */
  border: 1px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1); /* 更顺滑的过渡动画 */
  color: #475569;
  overflow: hidden;
}

/* 前置小图标颜色 */
.up-post-item .el-icon {
  font-size: 16px;
  color: #94a3b8;
  transition: color 0.3s;
}



.up-post-item:hover {
  background: #ffffff;
  color: #165dff;
  border-color: rgba(22, 93, 255, 0.15);
  box-shadow: 0 6px 16px rgba(22, 93, 255, 0.08);
  transform: translateY(-2px);
}

.up-post-item::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #165dff;
  opacity: 0;
  transform: scaleY(0);
  transition: all 0.3s ease;
}
.up-post-item:hover::before {
  opacity: 1;
  transform: scaleY(1);
}
.up-post-item:hover .el-icon {
  color: #165dff;
}


.up-post-title {
  font-size: 14px;
  font-weight: 500;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 16px;
}

.up-post-stats {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: #94a3b8;
  flex-shrink: 0;
  transition: color 0.3s ease;
}

.up-post-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: monospace;
}

.up-post-item:hover .up-post-stats {
  color: #64748b;
}

.up-post-item:hover .up-post-stats .el-icon {
  color: #165dff;
  transform: scale(1.1);
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), color 0.2s;
}
/* 头像框 */
.frame-default { background: #dcdfe6; }
.frame-gold {
  background: linear-gradient(135deg, #f6d365, #fda085);
  box-shadow: 0 0 14px rgba(246, 211, 101, 0.6);
}
.frame-ocean {
  background: linear-gradient(135deg, #00d2ff, #165dff);
  box-shadow: 0 0 16px rgba(0, 210, 255, 0.6);
}
.frame-rainbow {
  background: linear-gradient(90deg, #ff6b6b, #feca57, #48dbfb, #ff9ff3);
  background-size: 200% 100%;
  animation: frame-rainbow-spin 3s linear infinite;
}
@keyframes frame-rainbow-spin {
  0% { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}
.frame-flame {
  background: linear-gradient(135deg, #ff4500, #ff8c00, #ffd700);
  box-shadow: 0 0 18px rgba(255, 69, 0, 0.6);
}
.frame-dashed {
  background: repeating-conic-gradient(#fd7000 0deg 18deg, transparent 18deg 36deg);
  box-shadow: 0 0 14px rgba(237, 35, 76, 0.5);
  animation: frame-dash-glow 2s ease-in-out infinite;
}
@keyframes frame-dash-glow {
  0%, 100% { box-shadow: 0 0 14px rgba(237, 35, 76, 0.5); }
  50% { box-shadow: 0 0 26px rgba(237, 35, 76, 0.85); }
}
.frame-neon {
  background: linear-gradient(135deg, #00fff5, #ff00e4);
  box-shadow: 0 0 14px rgba(0, 255, 245, 0.7), 0 0 28px rgba(255, 0, 228, 0.4);
  animation: frame-neon-pulse 3s ease-in-out infinite;
}
@keyframes frame-neon-pulse {
  0%, 100% { box-shadow: 0 0 14px rgba(0, 255, 245, 0.7), 0 0 28px rgba(255, 0, 228, 0.4); }
  50% { box-shadow: 0 0 24px rgba(0, 255, 245, 0.9), 0 0 44px rgba(255, 0, 228, 0.6); }
}
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
.frame-crystal {
  background: linear-gradient(135deg, #00f5ff, #ff00e5);
  box-shadow: 0 0 16px rgba(0, 245, 255, 0.5), 0 0 32px rgba(255, 0, 229, 0.3), inset 0 0 8px rgba(255, 255, 255, 0.4);
  animation: crystal-breath 2s ease-in-out infinite;
}
@keyframes crystal-breath {
  0%, 100% { box-shadow: 0 0 16px rgba(0, 245, 255, 0.5), 0 0 32px rgba(255, 0, 229, 0.3); }
  50% { box-shadow: 0 0 28px rgba(0, 245, 255, 0.8), 0 0 56px rgba(255, 0, 229, 0.5); }
}
.frame-royal {
  background: linear-gradient(135deg, #6c3cc7, #9b59b6, #f1c40f);
  box-shadow: 0 0 16px rgba(108, 60, 199, 0.6);
}
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

/* 响应式 */
@media (max-width: 768px) {
  .post-detail-page { padding: 0 8px 20px; }
  .glass-pill { padding: 20px 16px; }
  .detail-topbar { padding: 12px 0; margin-bottom: 12px; }

  .text-only-detail { padding-top: 4px; }
  .text-only-title { max-width: 100%; font-size: 20px; }
  .text-only-desc { max-width: 100%; font-size: 14px; line-height: 1.75; }
}

/* ═══ 微交互动效 (详情页) ═══ */
/* 1. 点赞 - Q弹放大特效 (同时作用于正文点赞和评论区点赞) */
@keyframes heart-pop {
  0% { transform: scale(1); }
  40% { transform: scale(1.5); }
  70% { transform: scale(0.9); }
  100% { transform: scale(1); }
}

.daction-btn.liked .el-icon,
.cbar-btn.liked .el-icon {
  animation: heart-pop 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
}

/* 2. 收藏 - 星星旋转点亮特效 */
@keyframes star-spin {
  0% { transform: scale(1) rotate(0deg); }
  50% { transform: scale(1.4) rotate(144deg); }
  100% { transform: scale(1) rotate(360deg); }
}

.daction-btn.bookmarked .el-icon {
  animation: star-spin 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}
</style>
