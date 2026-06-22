<template>
  <div class="profile-container">
    <el-row :gutter="24">
      <!-- ═══ 左侧：用户名片卡 ═══ -->
      <el-col :xs="24" :md="8">
        <el-card class="user-card" shadow="never" v-loading="loading">
          <div class="card-bg"></div>
          <div class="avatar-container">
            <el-upload
              class="avatar-uploader"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleAvatarChange"
              accept="image/*"
            >
              <div class="avatar-wrapper" v-loading="uploading">
                <el-avatar :size="100" :src="profileForm.avatarUrl" class="user-avatar">
                  <img src="https://cube.elemecdn.com/e/fd/0fc769396203ba652971805f60932png.png" />
                </el-avatar>
                <div class="avatar-mask">
                  <el-icon :size="24"><Camera /></el-icon>
                </div>
              </div>
            </el-upload>
          </div>

          <div class="user-info-center">
            <div class="main-name">{{ profileForm.realName || profileForm.username || '未命名用户' }}</div>
            <div class="sub-name">@{{ profileForm.username }}</div>
            <div class="role-tags">
              <el-tag v-for="role in displayRoles" :key="role" size="small" effect="light" class="custom-tag">
                {{ role }}
              </el-tag>
              <el-tag v-if="!displayRoles.length" size="small" type="info">暂无角色</el-tag>
            </div>
          </div>

          <el-divider border-style="dashed" />

          <!-- 账号状态 & 当前角色 -->
          <div class="user-stats">
            <div class="stat-item">
              <div class="stat-label">账号状态</div>
              <!-- app_user.status: 1=正常 0=禁用 -->
              <div class="stat-value" :class="userStatus === 1 ? 'text-seafoam' : 'text-danger'">
                {{ userStatus === 1 ? '正常' : '已禁用' }}
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-label">当前角色</div>
              <div class="stat-value">{{ primaryRole }}</div>
            </div>
          </div>

          <el-divider border-style="dashed" />

          <!-- 核心数据概览 -->
          <div class="user-stats core-stats">
            <div class="stat-item">
              <div class="stat-label">可用积分</div>
              <!-- DB: user_point_account.available_points -->
              <!-- API: TODO GET /points/account -->
              <div class="stat-value text-primary">{{ coreOverview.availablePoints }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">学习等级</div>
              <!-- DB: user_learning_profile.level -->
              <!-- API: TODO GET /learning/profile -->
              <div class="stat-value text-primary">Lv.{{ coreOverview.level }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">已获勋章</div>
              <!-- DB: user_badge COUNT WHERE user_id = ? -->
              <!-- API: TODO GET /badge/list -->
              <div class="stat-value text-primary">{{ coreOverview.badgeCount }} 枚</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- ═══ 右侧：多 Tab 内容区 ═══ -->
      <el-col :xs="24" :md="16">
        <el-card class="settings-card" shadow="never">
          <el-tabs v-model="activeTab" class="profile-tabs">

            <!-- ========== Tab 1：基本资料（保留原有） ========== -->
            <el-tab-pane label="基本资料" name="basic">
              <div class="tab-content">
                <!--
                  API 已完成：
                  GET  /sys-user/profile     → 获取个人资料
                  PUT  /sys-user/profile     → 更新个人资料
                  POST /sys-user/upload/avatar → 上传头像
                  DB: app_user (username, real_name, email, phone, avatar_url, status)
                -->
                <el-form ref="formRef" :model="profileForm" :rules="rules" label-width="80px" label-position="top">
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="账号" prop="username">
                        <el-input v-model="profileForm.username" disabled />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="用户名" prop="realName">
                        <el-input v-model="profileForm.realName" placeholder="请输入用户名" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="邮箱" prop="email">
                        <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="手机号" prop="phone">
                        <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item style="margin-top: 12px;">
                    <el-button type="primary" @click="submitProfile" :loading="submitting">保存修改</el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>

            <!-- ========== Tab 2：安全设置（保留原有） ========== -->
            <el-tab-pane label="安全设置" name="security">
              <div class="tab-content">
                <!--
                  API 已完成：PUT /sys-user/password
                -->
                <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px" label-position="top" class="security-form">
                  <el-form-item label="原密码" prop="oldPassword">
                    <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password />
                  </el-form-item>
                  <el-form-item label="新密码" prop="newPassword">
                    <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（至少6位）" show-password />
                  </el-form-item>
                  <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
                  </el-form-item>
                  <el-form-item style="margin-top: 12px;">
                    <el-button type="primary" @click="submitPassword" :loading="changingPassword">更新密码</el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>

            <!-- ========== Tab 3：我的学习（P0 核心模块） ========== -->
            <el-tab-pane label="我的学习" name="learning">
              <div class="tab-content">
                <!--
                  DB 依赖：
                  - user_learning_profile (user_id, level, total_questions, correct_count, correct_rate, weak_points)
                  - quiz_wrong_bookmark  (user_id, question_id, wrong_count, mastered)
                  - quiz_attempt          (user_id, question_id, user_answer_json, is_correct, time_spent_seconds, attempted_at)
                  - quiz_question         (stem, question_type)
                  - conversation_message  (user_id, session_id)
                  API 状态：
                  ✅ GET  /learning/profile         → 学习画像统计 + 错题数
                  ✅ GET  /learning/answer-history?pageNum=&pageSize= → 答题记录分页
                  ✅ GET  /learning/ai-session-count → AI 会话数
                  ✅ GET  /learning/wrong-book?pageNum=&pageSize= → 错题本详情
                -->
                <!-- 数据统计卡片 -->
                <el-row :gutter="16" class="learning-stats-row" v-loading="learningLoading">
                  <el-col :xs="12" :md="6" v-for="stat in learningStats" :key="stat.label">
                    <div class="stat-card">
                      <div class="stat-card-num">{{ stat.value }}</div>
                      <div class="stat-card-label">{{ stat.label }}</div>
                    </div>
                  </el-col>
                </el-row>

                <!-- 快捷入口 -->
                <el-row :gutter="16" class="quick-entry-row">
                  <el-col :span="12">
                    <div class="quick-entry-card">
                      <div class="quick-entry-info">
                        <el-icon :size="22" color="var(--theme-coral)"><Reading /></el-icon>
                        <span class="quick-entry-title">错题本</span>
                        <!-- DB: quiz_wrong_bookmark COUNT WHERE user_id=? AND mastered=0 -->
                        <span class="quick-entry-desc">共 {{ wrongBookCount }} 道错题待复习</span>
                      </div>
                      <el-button type="primary" size="small" round @click="goToWrongBook">去复习</el-button>
                    </div>
                  </el-col>
                </el-row>

                <!-- 最近答题记录：quiz_attempt JOIN quiz_question -->
                <div class="section-title">最近答题记录</div>
                <el-table :data="recentAnswerList" stripe style="width: 100%" v-loading="answerLoading" size="default">
                  <el-table-column prop="stem" label="题目摘要" min-width="160" show-overflow-tooltip />
                  <el-table-column prop="questionType" label="题型" width="80" align="center">
                    <template #default="{ row }">
                      <el-tag size="small" type="info" effect="plain">{{ row.questionType }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="isCorrect" label="是否正确" width="100" align="center">
                    <template #default="{ row }">
                      <!-- DB: quiz_attempt.is_correct TINYINT 1=正确 0=错误 -->
                      <el-tag size="small" :class="row.isCorrect ? 'tag-success' : 'tag-danger'">
                        {{ row.isCorrect ? '正确' : '错误' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="timeSpent" label="用时" width="80" align="center" />
                  <el-table-column prop="attemptedAt" label="答题时间" width="160" />
                </el-table>
                <div class="pagination-wrapper">
                  <el-pagination
                    v-model:current-page="answerPage.pageNum"
                    :page-size="answerPage.pageSize"
                    :total="answerPage.total"
                    layout="prev, pager, next"
                    background
                    size="small"
                    @current-change="onAnswerPageChange"
                  />
                </div>
              </div>
            </el-tab-pane>

            <!-- ========== Tab 4：我的积分（P0 核心模块） ========== -->
            <el-tab-pane label="我的积分" name="points">
              <div class="tab-content">
                <!--
                  DB 依赖：user_point_account / point_transaction / point_exchange_order / point_shop_item
                  API 状态：
                  ✅ GET  /points/account                → 积分余额
                  ✅ GET  /points/transactions?pageNum=&pageSize= → 积分流水
                  ✅ GET  /points/exchange-orders?pageNum=&pageSize= → 兑换记录
                -->
                <!-- 积分余额卡片 -->
                <div class="points-balance-card" v-loading="pointsLoading">
                  <div class="balance-main">
                    <!-- DB: user_point_account.available_points -->
                    <div class="balance-label">可用积分</div>
                    <div class="balance-num">{{ pointsAccount.availablePoints }}</div>
                    <div class="balance-sub">
                      <!-- DB: user_point_account.total_earned_points / total_spent_points -->
                      <span>累计获得 <b>{{ pointsAccount.totalEarned }}</b></span>
                      <el-divider direction="vertical" />
                      <span>累计消耗 <b>{{ pointsAccount.totalSpent }}</b></span>
                    </div>
                  </div>
                  <div class="balance-actions">
                    <el-button type="primary" size="default" @click="goToPointsDetail">积分明细</el-button>
                    <el-button size="default" @click="goToPointsShop">去积分商店</el-button>
                  </div>
                </div>

                <!-- 积分子 Tab -->
                <el-tabs v-model="pointsSubTab" class="points-sub-tabs">
                  <!-- 积分流水：point_transaction -->
                  <el-tab-pane label="积分流水" name="flow">
                    <el-table :data="pointsFlowList" stripe style="width: 100%" size="default" v-loading="flowLoading">
                      <el-table-column prop="createdAt" label="时间" width="170" />
                      <el-table-column prop="bizType" label="业务类型" min-width="120">
                        <template #default="{ row }">
                          <el-tag size="small" effect="plain" type="info">{{ row.bizTypeLabel }}</el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column prop="points" label="积分变动" width="110" align="center">
                        <template #default="{ row }">
                          <span :class="row.points > 0 ? 'text-income' : 'text-expense'">
                            {{ row.points > 0 ? '+' : '' }}{{ row.points }}
                          </span>
                        </template>
                      </el-table-column>
                      <el-table-column prop="description" label="备注说明" min-width="150" show-overflow-tooltip />
                    </el-table>
                    <div class="pagination-wrapper">
                      <el-pagination
                        v-model:current-page="pointsFlowPage.pageNum"
                        :page-size="pointsFlowPage.pageSize"
                        :total="pointsFlowPage.total"
                        layout="prev, pager, next"
                        background
                        size="small"
                        @current-change="onFlowPageChange"
                      />
                    </div>
                  </el-tab-pane>

                  <!-- 兑换记录：point_exchange_order JOIN point_shop_item -->
                  <el-tab-pane label="兑换记录" name="exchange">
                    <el-table :data="exchangeList" stripe style="width: 100%" size="default" v-loading="exchangeLoading">
                      <el-table-column prop="goodsName" label="商品名称" min-width="140" show-overflow-tooltip />
                      <el-table-column prop="pointsCost" label="消耗积分" width="100" align="center" />
                      <el-table-column prop="createdAt" label="兑换时间" width="170" />
                      <el-table-column prop="orderStatus" label="订单状态" width="100" align="center">
                        <template #default="{ row }">
                          <el-tag size="small" :class="statusTagClass(row.orderStatus)">
                            {{ row.orderStatus === "SUCCESS" ? "已完成" : row.orderStatus === "PROCESSING" ? "处理中" : row.orderStatus }}
                          </el-tag>
                        </template>
                      </el-table-column>
                    </el-table>
                    <div class="pagination-wrapper">
                      <el-pagination
                        v-model:current-page="exchangePage.pageNum"
                        :page-size="exchangePage.pageSize"
                        :total="exchangePage.total"
                        layout="prev, pager, next"
                        background
                        size="small"
                        @current-change="onExchangePageChange"
                      />
                    </div>
                  </el-tab-pane>
                </el-tabs>
              </div>
            </el-tab-pane>

            <!-- ========== Tab 5：我的成就（P0 核心模块） ========== -->
            <el-tab-pane label="我的成就" name="achievement">
              <div class="tab-content">
                <!--
                  DB 依赖：user_badge / learning_task / user_task_record
                  API 状态：
                  ✅ GET  /achievement/badges        → 已获得勋章
                  ✅ GET  /achievement/daily-tasks    → 每日任务 + 进度
                  ✅ POST /achievement/claim/{taskId} → 领取奖励
                -->
                <!-- 勋章墙 -->
                <div class="section-title">勋章墙</div>
                <el-row :gutter="16" class="badge-grid" v-loading="badgeLoading">
                  <el-col :xs="12" :md="6" v-for="badge in badgeList" :key="badge.badgeCode">
                    <!-- DB: user_badge.earned_at 不为空 → 已获得；未获得来自 badge 定义表差值 -->
                    <div class="badge-card" :class="{ 'badge-locked': !badge.earned }">
                      <div class="badge-icon">
                        <el-icon :size="36">
                          <component :is="badge.icon" />
                        </el-icon>
                      </div>
                      <div class="badge-name">{{ badge.badgeName }}</div>
                      <div class="badge-desc">
                        {{ badge.earned ? badge.earnedAt : badge.unlockCondition }}
                      </div>
                    </div>
                  </el-col>
                </el-row>

                <el-divider border-style="dashed" />

                <!-- 每日任务 -->
                <div class="section-title">每日任务</div>
                <div class="task-list" v-loading="taskLoading">
                  <!-- DB: learning_task JOIN user_task_record -->
                  <div class="task-item" v-for="task in dailyTaskList" :key="task.taskId">
                    <div class="task-info">
                      <div class="task-name">{{ task.title }}</div>
                      <!-- DB: learning_task.reward_points -->
                      <div class="task-reward">+{{ task.rewardPoints }} 积分</div>
                    </div>
                    <div class="task-progress">
                      <el-progress
                        :percentage="task.progressPercent"
                        :status="task.completed ? 'success' : undefined"
                        :stroke-width="8"
                        :show-text="false"
                      />
                      <span class="task-progress-text">{{ task.progressValue }}/{{ task.targetValue }}</span>
                    </div>
                    <div class="task-action">
                      <!-- DB: user_task_record.reward_claimed: 0=可领取 1=已领取 -->
                      <el-tag v-if="task.rewardClaimed" size="small" class="tag-success">已领取</el-tag>
                      <el-tag v-else-if="task.completed && !task.rewardClaimed" size="small" type="warning" style="cursor: pointer;" @click="claimReward(task)">待领取</el-tag>
                      <el-button v-else size="small" type="primary" round @click="goDoTask(task)">去完成</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <!-- ========== Tab 6：我的收藏（P2 可选模块） ========== -->
            <el-tab-pane label="我的收藏" name="favorites">
              <div class="tab-content">
                <!--
                  DB 依赖：
                  - user_bookmark (user_id, target_type, target_id, created_at)
                    target_type ∈ ('species', 'ecosystem', 'kb_document', 'quiz_question')
                    JOIN marine_species / marine_ecosystem / kb_document / quiz_question 获取详情
                  待建 API：
                  - GET    /bookmark/list?type=&page=  → 收藏分页
                  - POST   /bookmark                   → 添加收藏
                  - DELETE /bookmark/{targetType}/{targetId} → 取消收藏
                -->
                <el-tabs v-model="favSubTab" class="fav-sub-tabs">
                  <!-- target_type 对应 DB user_bookmark.target_type 枚举值 -->
                  <el-tab-pane label="物种" name="species" />
                  <el-tab-pane label="生态系统" name="ecosystem" />
                  <el-tab-pane label="知识库" name="kb_document" />
                  <el-tab-pane label="题目" name="quiz_question" />
                </el-tabs>

                <template v-if="currentFavList.length">
                  <el-row :gutter="16" class="fav-grid">
                    <el-col :xs="24" :sm="12" :md="8" v-for="item in currentFavList" :key="item.bookmarkId">
                      <div class="fav-card">
                        <div class="fav-thumb">
                          <img :src="item.thumbnail" :alt="item.title" />
                        </div>
                        <div class="fav-info">
                          <div class="fav-title">{{ item.title }}</div>
                          <!-- DB: user_bookmark.created_at -->
                          <div class="fav-time">{{ item.createdAt }}</div>
                        </div>
                        <el-button
                          class="fav-remove-btn"
                          size="small"
                          type="danger"
                          text
                          @click="removeBookmark(item)"
                        >
                          取消收藏
                        </el-button>
                      </div>
                    </el-col>
                  </el-row>
                  <div class="pagination-wrapper">
                    <el-pagination
                      v-model:current-page="favPage.pageNum"
                      :page-size="favPage.pageSize"
                      :total="favPage.total"
                      layout="prev, pager, next"
                      background
                      size="small"
                    />
                  </div>
                </template>
                <el-empty v-else description="暂无收藏内容" :image-size="120" />
              </div>
            </el-tab-pane>

            <!-- ========== Tab 7：我的观察（P2 可选模块） ========== -->
            <el-tab-pane label="我的观察" name="observation">
              <div class="tab-content">
                <!--
                  DB 依赖：
                  - user_observation (id, user_id, species_id, title, description,
                                       latitude, longitude, location_name, observed_at,
                                       photo_media_id, ai_identified, ai_confidence, status)
                    JOIN marine_species ON species_id → chinese_name
                    关联 media_asset    ON photo_media_id → url
                  待建 API：
                  - GET  /observation/my?page=  → 我的观察分页
                  - POST /observation            → 发布观察
                  - GET  /observation/{id}       → 观察详情
                -->
                <div class="obs-header">
                  <el-button type="primary" @click="publishObservation">
                    <el-icon><Plus /></el-icon> 发布观察
                  </el-button>
                </div>

                <template v-if="observationList.length">
                  <div class="obs-list">
                    <div class="obs-item" v-for="obs in observationList" :key="obs.id">
                      <div class="obs-image">
                        <!-- DB: media_asset.url (via photo_media_id) -->
                        <img :src="obs.photoUrl" :alt="obs.title" />
                      </div>
                      <div class="obs-content">
                        <div class="obs-title">{{ obs.title }}</div>
                        <div class="obs-meta">
                          <el-icon :size="14"><Location /></el-icon>
                          <!-- DB: user_observation.location_name -->
                          <span>{{ obs.locationName }}</span>
                          <el-divider direction="vertical" />
                          <el-icon :size="14"><Clock /></el-icon>
                          <!-- DB: user_observation.observed_at -->
                          <span>{{ obs.observedAt }}</span>
                        </div>
                        <div class="obs-tags">
                          <!-- DB: user_observation.ai_identified / ai_confidence
                               关联 marine_species.chinese_name -->
                          <el-tag size="small" effect="plain" class="custom-tag">
                            AI识别：{{ obs.aiSpeciesName }} ({{ obs.aiConfidence }}%)
                          </el-tag>
                          <!-- DB: user_observation.status: 0=隐藏 1=公开 2=审核中 -->
                          <el-tag size="small" :class="obsStatusClass(obs.status)">
                            {{ obsStatusLabel(obs.status) }}
                          </el-tag>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="pagination-wrapper">
                    <el-pagination
                      v-model:current-page="obsPage.pageNum"
                      :page-size="obsPage.pageSize"
                      :total="obsPage.total"
                      layout="prev, pager, next"
                      background
                      size="small"
                    />
                  </div>
                </template>
                <el-empty v-else description="暂无观察记录" :image-size="120" />
              </div>
            </el-tab-pane>

          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Camera, Reading, ChatDotRound,
  Medal, TrophyBase, StarFilled, Present,
  Location, Clock, Plus
} from "@element-plus/icons-vue";
import { getUserProfile, updatePasswordApi, updateUserProfile, uploadAvatarApi } from "@/api/sysUser";
import { getLearningProfile, getAnswerHistory, getAiSessionCount } from "@/api/learning";
import { getPointsAccount, getPointsTransactions, getExchangeOrders } from "@/api/points";
import { getBadges, getDailyTasks, claimTaskReward, dailyCheckin } from "@/api/achievement";
import { useAuthStore } from "@/store/auth";

const authStore = useAuthStore();
const router = useRouter();
const activeTab = ref("basic");

// ═══ 角色映射（app_role.role_code → 中文名） ═══
const ROLE_MAP = { ADMIN: "系统管理员", MANAGER: "管理人员", VISITOR: "普通用户" };
const displayRoles = computed(() => {
  return authStore.roles.map(code => ROLE_MAP[code] || code);
});
const primaryRole = computed(() => {
  if (!authStore.roles.length) return "未分配";
  return ROLE_MAP[authStore.roles[0]] || authStore.roles[0];
});

// ═══ 通用状态 ═══
const loading = ref(false);
const uploading = ref(false);
const submitting = ref(false);
const formRef = ref(null);
// DB: app_user.status TINYINT 1=enabled 0=disabled
const userStatus = ref(1);

// ═══ Tab1 基本资料：app_user 表 ═══
const profileForm = reactive({
  username: "",   // app_user.username
  realName: "",   // app_user.real_name
  email: "",      // app_user.email
  phone: "",      // app_user.phone
  avatarUrl: ""   // app_user.avatar_url
});

const rules = {
  realName: [{ required: true, message: "用户名不能为空", trigger: "blur" }],
  email: [
    { required: true, message: "邮箱不能为空", trigger: "blur" },
    { type: "email", message: "请输入正确的邮箱格式", trigger: "blur" }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: "请输入正确的手机号格式", trigger: "blur" }
  ]
};

const formatAvatarUrl = (url) => {
  if (!url) return "";
  if (url.startsWith("http") || url.startsWith("/api")) return url;
  return `/api${url}`;
};

// GET /sys-user/profile ✅ 已对接
const fetchProfile = async () => {
  loading.value = true;
  try {
    const res = await getUserProfile();
    Object.assign(profileForm, res.data);
    userStatus.value = res.data.status ?? 1;
    profileForm.avatarUrl = formatAvatarUrl(profileForm.avatarUrl);
    authStore.avatarUrl = profileForm.avatarUrl;
  } catch (err) {
    ElMessage.error("获取个人资料失败");
  } finally {
    loading.value = false;
  }
};

// POST /sys-user/upload/avatar ✅ 已对接
const handleAvatarChange = async (uploadFile) => {
  const file = uploadFile.raw;
  if (file.size / 1024 / 1024 > 2) {
    ElMessage.error("头像图片大小不能超过 2MB!");
    return;
  }
  uploading.value = true;
  try {
    const res = await uploadAvatarApi(file);
    profileForm.avatarUrl = formatAvatarUrl(res.data.avatarUrl);
    authStore.avatarUrl = profileForm.avatarUrl;
    ElMessage.success("头像上传成功");
  } catch (err) {
    ElMessage.error("头像上传失败");
  } finally {
    uploading.value = false;
  }
};

// PUT /sys-user/profile ✅ 已对接
const submitProfile = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        await updateUserProfile({
          realName: profileForm.realName,
          email: profileForm.email,
          phone: profileForm.phone
        });
        ElMessage.success("资料更新成功");
      } catch (err) {
        ElMessage.error(err.response?.data || "更新失败");
      } finally {
        submitting.value = false;
      }
    }
  });
};

// ═══ Tab2 安全设置 ═══
const passwordFormRef = ref(null);
const changingPassword = ref(false);
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};

const passwordRules = {
  oldPassword: [{ required: true, message: "请输入原密码", trigger: "blur" }],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, message: "新密码长度不能少于6位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认新密码", trigger: "blur" },
    { validator: validateConfirmPassword, trigger: "blur" },
  ],
};

// PUT /sys-user/password ✅ 已对接
const submitPassword = async () => {
  if (!passwordFormRef.value) return;
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return;
    changingPassword.value = true;
    try {
      await updatePasswordApi({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
      });
      passwordForm.oldPassword = "";
      passwordForm.newPassword = "";
      passwordForm.confirmPassword = "";
      ElMessage.success("密码修改成功，即将返回登录页");
      setTimeout(async () => {
        await authStore.logoutAction();
      }, 1500);
    } catch (err) {
      ElMessage.error(err.response?.data || "密码修改失败");
    } finally {
      changingPassword.value = false;
    }
  });
};

// ══════════════════════════════════════════════════════════════
// 以下为新增 C 端模块 —— 全部使用 Mock 数据，字段对齐 marine_db.sql
// 每个模块注释标注了对应 DB 表与待建 API
// ══════════════════════════════════════════════════════════════

// ═══ 左侧名片：核心数据概览 ═══
// DB: user_point_account + user_learning_profile + user_badge
const coreOverview = reactive({
  // user_point_account.available_points ← TODO: GET /points/account 待建
  availablePoints: 2580,
  // user_learning_profile.level ← GET /learning/profile ✅
  level: 1,
  // COUNT(user_badge) ← TODO: GET /badge/list 待建
  badgeCount: 8,
});

// ═══ Tab 3：我的学习 ═══
const learningLoading = ref(false);
const answerLoading = ref(false);

// 学习画像统计 ← GET /learning/profile ✅
const learningStats = reactive([
  { label: "累计答题数", value: 0 },
  { label: "答题正确率", value: "0%" },
  { label: "当前等级", value: "Lv.1" },
  { label: "错题总数", value: 0 },
]);

// 错题本数量 ← GET /learning/profile.wrongQuestionCount ✅
const wrongBookCount = ref(0);

// 答题记录 ← GET /learning/answer-history ✅
const answerPage = reactive({ pageNum: 1, pageSize: 6, total: 0 });
const recentAnswerList = ref([]);

/** 获取学习画像 + 错题统计 */
const fetchLearningProfile = async () => {
  learningLoading.value = true;
  try {
    const res = await getLearningProfile();
    const d = res.data.data;
    // 更新统计卡片
    learningStats[0].value = d.totalQuestions ?? 0;
    learningStats[1].value = (d.correctRate ?? 0) + "%";
    learningStats[2].value = "Lv." + (d.level ?? 1);
    learningStats[3].value = d.wrongQuestionCount ?? 0;
    // 更新快捷入口
    wrongBookCount.value = d.wrongQuestionCount ?? 0;
    // 更新左侧名片等级
    coreOverview.level = d.level ?? 1;
  } catch (err) {
    // 接口失败时保持上一次数据，不覆盖
    console.error("获取学习画像失败", err);
  } finally {
    learningLoading.value = false;
  }
};

/** 获取 AI 会话数 */
const fetchAiSessionCount = async () => {
  try {
    const res = await getAiSessionCount();
    aiSessionCount.value = res.data.data?.sessionCount ?? 0;
  } catch (err) {
    console.error("获取AI会话统计失败", err);
  }
};

/** 获取最近答题记录（分页） */
const fetchAnswerHistory = async () => {
  answerLoading.value = true;
  try {
    const res = await getAnswerHistory(answerPage.pageNum, answerPage.pageSize);
    const page = res.data.data;
    recentAnswerList.value = page.records ?? [];
    answerPage.total = page.total ?? 0;
  } catch (err) {
    console.error("获取答题记录失败", err);
    recentAnswerList.value = [];
  } finally {
    answerLoading.value = false;
  }
};

/** 答题记录分页切换 */
const onAnswerPageChange = (pageNum) => {
  answerPage.pageNum = pageNum;
  fetchAnswerHistory();
};

const goToWrongBook = () => {
  router.push("/learning/wrong-book");
};

const goToAiHistory = () => {
  router.push("/ai-assistant");
};

// ═══ Tab 4：我的积分 ═══
// API: GET /points/account ✅  /points/transactions ✅  /points/exchange-orders ✅
const pointsSubTab = ref("flow");
const pointsLoading = ref(false);
const flowLoading = ref(false);
const exchangeLoading = ref(false);

// 积分余额 ← GET /points/account
const pointsAccount = reactive({
  availablePoints: 0,
  totalEarned: 0,
  totalSpent: 0,
});

// 积分流水 ← GET /points/transactions
const pointsFlowPage = reactive({ pageNum: 1, pageSize: 6, total: 0 });
const pointsFlowList = ref([]);

// 兑换记录 ← GET /points/exchange-orders
const exchangePage = reactive({ pageNum: 1, pageSize: 6, total: 0 });
const exchangeList = ref([]);

const fetchPointsAccount = async () => {
  pointsLoading.value = true;
  try {
    const res = await getPointsAccount();
    const d = res.data.data;
    pointsAccount.availablePoints = d.availablePoints ?? 0;
    pointsAccount.totalEarned = d.totalEarned ?? 0;
    pointsAccount.totalSpent = d.totalSpent ?? 0;
    // 同步到左侧名片
    coreOverview.availablePoints = d.availablePoints ?? 0;
  } catch (err) {
    console.error("获取积分余额失败", err);
  } finally {
    pointsLoading.value = false;
  }
};

const fetchPointsFlow = async () => {
  flowLoading.value = true;
  try {
    const res = await getPointsTransactions(pointsFlowPage.pageNum, pointsFlowPage.pageSize);
    const page = res.data.data;
    pointsFlowList.value = page.records ?? [];
    pointsFlowPage.total = page.total ?? 0;
  } catch (err) {
    console.error("获取积分流水失败", err);
    pointsFlowList.value = [];
  } finally {
    flowLoading.value = false;
  }
};

const fetchExchangeOrders = async () => {
  exchangeLoading.value = true;
  try {
    const res = await getExchangeOrders(exchangePage.pageNum, exchangePage.pageSize);
    const page = res.data.data;
    exchangeList.value = page.records ?? [];
    exchangePage.total = page.total ?? 0;
  } catch (err) {
    console.error("获取兑换记录失败", err);
    exchangeList.value = [];
  } finally {
    exchangeLoading.value = false;
  }
};

const onFlowPageChange = (pageNum) => {
  pointsFlowPage.pageNum = pageNum;
  fetchPointsFlow();
};

const onExchangePageChange = (pageNum) => {
  exchangePage.pageNum = pageNum;
  fetchExchangeOrders();
};

const goToPointsDetail = () => {
  pointsSubTab.value = "flow";
};

const goToPointsShop = () => {
  // TODO: 路由 → /points/shop (商品来自 point_shop_item 表)
  ElMessage.info("跳转至积分商店（待对接路由）");
};

const statusTagClass = (status) => {
  if (status === "SUCCESS") return "tag-success";
  if (status === "PROCESSING") return "tag-warning";
  return "tag-danger";
};

// ═══ Tab 5：我的成就 ═══
// API: GET /achievement/badges ✅  /achievement/daily-tasks ✅  POST /achievement/claim/{taskId} ✅
const badgeLoading = ref(false);
const taskLoading = ref(false);

// ---- 全量勋章定义（前端静态，后端只回已获得列表）----
const ALL_BADGE_DEFS = [
  { badgeCode: "first_quiz", badgeName: "初识海洋", icon: Medal, unlockCondition: "首次完成答题" },
  { badgeCode: "quiz_master", badgeName: "答题达人", icon: StarFilled, unlockCondition: "累计答对100题" },
  { badgeCode: "eco_guardian", badgeName: "生态卫士", icon: Present, unlockCondition: "浏览50个物种详情" },
  { badgeCode: "perfect_streak", badgeName: "十连答对", icon: TrophyBase, unlockCondition: "连续答对10题" },
  { badgeCode: "persistence", badgeName: "持之以恒", icon: Medal, unlockCondition: "连续签到7天" },
  { badgeCode: "ai_explorer", badgeName: "AI探索者", icon: StarFilled, unlockCondition: "完成20次AI问答" },
  { badgeCode: "collector", badgeName: "收藏达人", icon: Present, unlockCondition: "收藏30个物种或知识" },
  { badgeCode: "knowledge_star", badgeName: "知识之星", icon: TrophyBase, unlockCondition: "达到Lv.10等级" },
  { badgeCode: "encyclopedia", badgeName: "百科全书", icon: Medal, unlockCondition: "累计答对500题" },
  { badgeCode: "eco_expert", badgeName: "生态专家", icon: TrophyBase, unlockCondition: "识别100种物种" },
  { badgeCode: "social_star", badgeName: "社交达人", icon: StarFilled, unlockCondition: "发布50条观察记录" },
  { badgeCode: "supreme_king", badgeName: "至尊王者", icon: Present, unlockCondition: "达到Lv.30等级" },
];

const badgeList = ref([]);   // 合并后：已获得在前，未获得在后
const dailyTaskList = ref([]);

const fetchBadges = async () => {
  badgeLoading.value = true;
  try {
    const res = await getBadges();
    const earnedList = res.data.data ?? [];
    const earnedCodes = new Set(earnedList.map(b => b.badgeCode));
    // 合并全量定义 + 已获得数据
    badgeList.value = ALL_BADGE_DEFS.map(def => {
      const earned = earnedList.find(b => b.badgeCode === def.badgeCode);
      return {
        ...def,
        earned: !!earned,
        earnedAt: earned?.earnedAt || "",
      };
    });
    // 更新左侧名片勋章数
    coreOverview.badgeCount = earnedList.length;
    // 新获得的勋章弹窗恭喜
    const newlyAwarded = res.data.newlyAwarded;
    if (newlyAwarded && newlyAwarded.length > 0) {
      const names = newlyAwarded.map(b => b.badgeName).join('、');
      ElMessage.success(`🎉 恭喜获得新勋章：${names}`);
    }
  } catch (err) {
    console.error("获取勋章失败", err);
  } finally {
    badgeLoading.value = false;
  }
};

const fetchDailyTasks = async () => {
  taskLoading.value = true;
  try {
    const res = await getDailyTasks();
    dailyTaskList.value = res.data.data ?? [];
  } catch (err) {
    console.error("获取每日任务失败", err);
  } finally {
    taskLoading.value = false;
  }
};

const claimReward = async (task) => {
  try {
    const res = await claimTaskReward(task.taskId);
    if (res.data.success) {
      ElMessage.success(res.data.message || `已领取 +${task.rewardPoints} 积分`);
      // 刷新任务列表 + 积分余额
      fetchDailyTasks();
      fetchPointsAccount();
    } else {
      ElMessage.warning(res.data.message || "领取失败");
    }
  } catch (err) {
    ElMessage.error("领取奖励失败");
  }
};

// 每日任务路由映射（对齐 seed_data 6 个任务，以后不变）
const TASK_ROUTE_MAP = {
  daily_quiz:         "/quiz",
  ask_ai:             "/ai-assistant",
  read_species:       "/encyclopedia",
  bookmark:           "/encyclopedia",
};

const goDoTask = async (task) => {
  // 签到 → 就地签到
  if (task.taskType === "daily_checkin") {
    try {
      const res = await dailyCheckin();
      if (res.data.success) {
        ElMessage.success(res.data.message);
        fetchDailyTasks();            // 刷新任务列表
        fetchPointsAccount();         // 刷新积分
      } else {
        ElMessage.warning(res.data.message);
      }
    } catch (err) {
      ElMessage.error("签到失败");
    }
    return;
  }
  // 其他任务 → 跳转
  const route = TASK_ROUTE_MAP[task.taskType];
  if (route) {
    router.push(route);
  } else {
    ElMessage.info(`请前往对应页面完成「${task.title}」`);
  }
};

// ═══ Tab 6：我的收藏 ═══
// DB: user_bookmark (target_type, target_id) JOIN 对应业务表
const favSubTab = ref("species");
const favPage = reactive({ pageNum: 1, pageSize: 9, total: 15 });

const favDataMap = {
  // target_type = 'species' → JOIN marine_species (chinese_name, image_url)
  species: [
    { bookmarkId: 1, title: "中华白海豚", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-20", targetType: "species", targetId: 1 },
    { bookmarkId: 2, title: "蓝鲸", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-18", targetType: "species", targetId: 2 },
    { bookmarkId: 3, title: "珊瑚", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-15", targetType: "species", targetId: 3 },
    { bookmarkId: 4, title: "绿海龟", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-12", targetType: "species", targetId: 4 },
    { bookmarkId: 5, title: "座头鲸", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-10", targetType: "species", targetId: 5 },
    { bookmarkId: 6, title: "企鹅", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-08", targetType: "species", targetId: 6 },
  ],
  // target_type = 'ecosystem' → JOIN marine_ecosystem (name, cover_media_id)
  ecosystem: [
    { bookmarkId: 7, title: "珊瑚礁生态系统", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-16", targetType: "ecosystem", targetId: 1 },
    { bookmarkId: 8, title: "红树林湿地", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-05", targetType: "ecosystem", targetId: 2 },
  ],
  // target_type = 'kb_document' → JOIN kb_document (title)
  kb_document: [
    { bookmarkId: 9, title: "海洋酸化对生物的影响", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-19", targetType: "kb_document", targetId: 1 },
    { bookmarkId: 10, title: "食物链与能量流动", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-14", targetType: "kb_document", targetId: 2 },
    { bookmarkId: 11, title: "生物多样性保护策略", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-11", targetType: "kb_document", targetId: 3 },
  ],
  // target_type = 'quiz_question' → JOIN quiz_question (stem)
  quiz_question: [
    { bookmarkId: 12, title: "哺乳动物分类题", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-17", targetType: "quiz_question", targetId: 1 },
    { bookmarkId: 13, title: "生态系统综合题", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-13", targetType: "quiz_question", targetId: 2 },
    { bookmarkId: 14, title: "物种识别挑战题", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-09", targetType: "quiz_question", targetId: 3 },
    { bookmarkId: 15, title: "海洋生物专项练习", thumbnail: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", createdAt: "2026-06-07", targetType: "quiz_question", targetId: 4 },
  ],
};

const currentFavList = computed(() => {
  return favDataMap[favSubTab.value] || [];
});

const removeBookmark = (item) => {
  // TODO: DELETE /bookmark/{item.targetType}/{item.targetId}
  ElMessageBox.confirm(`确定取消收藏「${item.title}」？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  }).then(() => {
    ElMessage.success("已取消收藏");
  }).catch(() => {});
};

// ═══ Tab 7：我的观察 ═══
// DB: user_observation (title, location_name, observed_at, photo_media_id,
//     ai_identified, ai_confidence, status)
//     JOIN marine_species ON species_id → chinese_name
//     JOIN media_asset ON photo_media_id → url
// Seed Data: user_id=2, species_id=1(中华白海豚), ai_identified=1, ai_confidence=98.50
const obsPage = reactive({ pageNum: 1, pageSize: 6, total: 8 });

const observationList = ref([
  {
    id: 1,
    title: "深圳湾偶遇粉红小精灵！",            // user_observation.title
    photoUrl: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg", // media_asset.url
    locationName: "深圳湾公园",                  // user_observation.location_name
    observedAt: "2026-06-19 15:30",             // user_observation.observed_at
    aiSpeciesName: "中华白海豚",                  // marine_species.chinese_name (via species_id=1)
    aiConfidence: 98.50,                         // user_observation.ai_confidence
    status: 1,                                   // user_observation.status: 1=公开
    speciesId: 1,                                // user_observation.species_id
  },
  {
    id: 2,
    title: "红树林湿地候鸟种群记录",
    photoUrl: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg",
    locationName: "海南·东寨港",
    observedAt: "2026-06-18 09:15",
    aiSpeciesName: "黑脸琵鹭",
    aiConfidence: 92.30,
    status: 1,
    speciesId: null,
  },
  {
    id: 3,
    title: "近海水母爆发记录",
    photoUrl: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg",
    locationName: "青岛·石老人海域",
    observedAt: "2026-06-16 11:42",
    aiSpeciesName: "海月水母",
    aiConfidence: 85.10,
    status: 2,                                   // user_observation.status: 2=审核中
    speciesId: null,
  },
  {
    id: 4,
    title: "海龟产卵地调查",
    photoUrl: "https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg",
    locationName: "广东·惠东海龟湾",
    observedAt: "2026-06-14 07:00",
    aiSpeciesName: "绿海龟",
    aiConfidence: 96.80,
    status: 1,
    speciesId: null,
  },
]);

// user_observation.status 映射
const obsStatusClass = (status) => {
  // status: 0=隐藏 1=公开 2=审核中
  if (status === 1) return "tag-success";
  if (status === 2) return "tag-warning";
  return "tag-danger";
};

const obsStatusLabel = (status) => {
  if (status === 1) return "审核通过";
  if (status === 2) return "审核中";
  return "已隐藏";
};

const publishObservation = () => {
  // TODO: 路由 → /observation/publish
  // POST /observation body: { title, description, species_id, latitude, longitude, location_name, observed_at, photo_media_id }
  ElMessage.info("打开发布观察（待对接路由）");
};

// ═══ 初始化 ═══
onMounted(() => {
  fetchProfile();
  fetchLearningData();
});

/** 加载 Tab3 所有数据 */
const fetchLearningData = () => {
  fetchLearningProfile();
  fetchAiSessionCount();
  fetchAnswerHistory();
};

/** 加载 Tab4 所有数据 */
const fetchPointsData = () => {
  fetchPointsAccount();
  fetchPointsFlow();
  fetchExchangeOrders();
};

/** 加载 Tab5 所有数据 */
const fetchAchievementData = () => {
  fetchBadges();
  fetchDailyTasks();
};

// 切换 Tab 拉取数据
const dataFetchedFlags = { learning: false, points: false, achievement: false };
watch(activeTab, (tab) => {
  if (tab === "learning") {
    fetchLearningData();
    dataFetchedFlags.learning = true;
  }
  if (tab === "points") {
    fetchPointsData();
    dataFetchedFlags.points = true;
  }
  if (tab === "achievement") {
    fetchAchievementData();
    dataFetchedFlags.achievement = true;
  }
});
</script>

<style scoped>
/* ════════════════════════════════════════════════════════════════════
   全局容器与基础毛玻璃材质
   ════════════════════════════════════════════════════════════════════ */
.profile-container {
  padding: 0;
  overflow-x: hidden;
  color: #1d2129;
}

/* 统一卡片毛玻璃基底 */
.user-card, .settings-card {
  background: rgba(255, 255, 255, 0.65) !important;
  backdrop-filter: blur(24px) saturate(120%);
  -webkit-backdrop-filter: blur(24px) saturate(120%);
  border: 1px solid rgba(255, 255, 255, 0.9) !important;
  border-radius: 24px !important;
  box-shadow:
      0 12px 32px rgba(0, 50, 150, 0.04),
      inset 0 1px 1px rgba(255, 255, 255, 0.5) !important;
  overflow: hidden;
}

:deep(.el-card__body) {
  padding: 0 !important;
}

/* ════════════════════════════════════════════════════════════════════
   左侧：用户名片卡
   ════════════════════════════════════════════════════════════════════ */
.card-bg {
  height: 130px;
  /* 清新浅海渐变 */
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  position: relative;
}
.card-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='100' height='100' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0 100 C 20 0 50 0 100 100 Z' fill='rgba(255,255,255,0.1)'/%3E%3C/svg%3E") repeat-x;
  background-size: cover;
}

.avatar-container {
  display: flex;
  justify-content: center;
  margin-top: -55px;
  position: relative;
  z-index: 2;
}

.avatar-wrapper {
  position: relative;
  border-radius: 50%;
  padding: 6px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(0, 150, 255, 0.15);
  transition: transform 0.3s ease;
}
.avatar-wrapper:hover {
  transform: translateY(-4px) scale(1.02);
}

.user-avatar {
  display: block;
  border-radius: 50%;
}

.avatar-mask {
  position: absolute;
  inset: 6px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s ease;
}
.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.user-info-center {
  text-align: center;
  padding: 16px 20px 8px;
}
.main-name {
  font-size: 22px;
  font-weight: 700;
  color: #1d2129;
  margin-bottom: 4px;
}
.sub-name {
  font-size: 13px;
  color: #86909c;
  margin-bottom: 14px;
}

.custom-tag {
  background: rgba(22, 93, 255, 0.06) !important;
  border: 1px solid rgba(22, 93, 255, 0.2) !important;
  color: #165dff !important;
  border-radius: 12px;
  padding: 0 14px;
  font-weight: 500;
}

.user-stats {
  display: flex;
  padding: 0 20px 24px;
  justify-content: space-around;
}
.core-stats {
  padding-top: 0;
}
.stat-item {
  text-align: center;
}
.stat-label {
  font-size: 12px;
  color: #86909c;
  margin-bottom: 6px;
}
.stat-value {
  font-size: 15px;
  font-weight: 700;
}

/* 文本高亮颜色 */
.text-primary   { color: #165dff; }
.text-seafoam   { color: #00b42a; }
.text-danger    { color: #f53f3f; }
.text-income    { color: #00b42a; font-weight: 700; }
.text-expense   { color: #f53f3f; font-weight: 700; }

/* 虚线分割线 */
:deep(.el-divider--horizontal) {
  margin: 16px 0;
  border-top: 1px dashed rgba(0, 0, 0, 0.08);
}

/* ════════════════════════════════════════════════════════════════════
   右侧：多 Tab 区域面板
   ════════════════════════════════════════════════════════════════════ */
.settings-card {
  min-height: 600px;
  padding: 20px 24px !important;
}

/* 安全设置表单用适中的宽度，和基本资料对齐 */
.security-form {
  max-width: 420px;
}

.tab-content {
  padding: 16px 4px 24px;
  animation: fadeIn 0.4s ease;
}

/* 个人资料表单更紧凑 */
.tab-content :deep(.el-form-item) {
  margin-bottom: 16px;
}

.tab-content :deep(.el-form-item__label) {
  padding-bottom: 4px !important;
  font-size: 13px;
}

.tab-content :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

/* 深度定制 Element Plus Tabs */
:deep(.profile-tabs .el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: rgba(0, 0, 0, 0.05);
}
:deep(.profile-tabs .el-tabs__item) {
  font-size: 15px;
  color: #86909c;
  transition: all 0.3s;
}
:deep(.profile-tabs .el-tabs__item.is-active) {
  font-weight: 700;
  color: #165dff;
}
:deep(.profile-tabs .el-tabs__active-bar) {
  background-color: #165dff;
  height: 3px;
  border-radius: 3px 3px 0 0;
}

/* 标签状态色 */
.tag-success { background: rgba(0, 180, 42, 0.1) !important; border-color: transparent !important; color: #00b42a !important; }
.tag-danger { background: rgba(245, 63, 63, 0.1) !important; border-color: transparent !important; color: #f53f3f !important; }
.tag-warning { background: rgba(255, 125, 0, 0.1) !important; border-color: transparent !important; color: #ff7d00 !important; }

/* ════════════════════════════════════════════════════════════════════
   表格透明化 (Glass Table) - 极其重要
   ════════════════════════════════════════════════════════════════════ */
:deep(.el-table), :deep(.el-table tr), :deep(.el-table th.el-table__cell), :deep(.el-table td.el-table__cell) {
  background: transparent !important;
  border-bottom: 1px solid rgba(0,0,0,0.04) !important;
}
:deep(.el-table th.el-table__cell) {
  color: #86909c;
  font-weight: 600;
}
:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: rgba(255, 255, 255, 0.4) !important;
}
:deep(.el-table::before), :deep(.el-table::after) {
  display: none;
}
:deep(.el-table tbody tr:hover > td) {
  background-color: rgba(22, 93, 255, 0.04) !important;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

/* 区块标题 */
.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #1d2129;
  margin-bottom: 16px;
  margin-top: 8px;
  display: flex;
  align-items: center;
}
.section-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  background: #165dff;
  border-radius: 2px;
  margin-right: 8px;
}

/* ════════════════════════════════════════════════════════════════════
   内嵌玻璃卡片通用样式 (Glass Item)
   ════════════════════════════════════════════════════════════════════ */
.stat-card, .quick-entry-card, .badge-card, .task-item, .fav-card, .obs-item {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.02);
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  backdrop-filter: blur(10px);
}
.stat-card:hover, .quick-entry-card:hover, .badge-card:hover, .task-item:hover, .fav-card:hover, .obs-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(22, 93, 255, 0.1);
  border-color: rgba(22, 93, 255, 0.2);
  background: rgba(255, 255, 255, 0.8);
}

/* ── Tab 3：我的学习 ── */
.learning-stats-row { margin-bottom: 24px; }
.stat-card {
  padding: 24px 16px;
  text-align: center;
}
.stat-card-num {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #165dff, #00d2ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 8px;
  line-height: 1.2;
}
.stat-card-label { font-size: 13px; color: #86909c; font-weight: 500; }

.quick-entry-row { margin-bottom: 24px; }
.quick-entry-card {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px;
}
.quick-entry-info { display: flex; align-items: center; gap: 12px; }
.quick-entry-title { font-size: 16px; font-weight: 600; color: #1d2129; }
.quick-entry-desc { font-size: 13px; color: #86909c; }

/* ── Tab 4：我的积分 ── */
.points-balance-card {
  display: flex; align-items: center; justify-content: space-between;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border-radius: 20px;
  padding: 32px 40px;
  margin-bottom: 28px;
  color: #fff;
  box-shadow: 0 12px 24px rgba(79, 172, 254, 0.3);
}
.balance-main { display: flex; flex-direction: column; gap: 8px; }
.balance-label { font-size: 15px; opacity: 0.9; font-weight: 500;}
.balance-num { font-size: 46px; font-weight: 800; line-height: 1; text-shadow: 0 4px 12px rgba(0,0,0,0.1);}
.balance-sub { font-size: 13px; opacity: 0.9; display: flex; align-items: center; gap: 10px; margin-top: 4px;}
.balance-sub b { font-weight: 700; opacity: 1; font-size: 15px;}
.balance-sub :deep(.el-divider--vertical) { height: 16px; border-color: rgba(255, 255, 255, 0.5); }
.balance-actions { display: flex; flex-direction: column; gap: 12px; flex-shrink: 0; }
.balance-actions .el-button { min-width: 130px; border-radius: 20px; font-weight: 600; }
.balance-actions .el-button--primary { background: rgba(255,255,255,0.2); border: 1px solid rgba(255,255,255,0.4); color: #fff; }
.balance-actions .el-button--primary:hover { background: #fff; color: #165dff; }
.balance-actions .el-button--default { color: #165dff; border: none; }

.points-sub-tabs { margin-top: 8px; }
:deep(.points-sub-tabs .el-tabs__header) { margin-bottom: 20px; }

/* ── Tab 5：我的成就 ── */
.badge-grid { margin-bottom: 12px; }
.badge-card {
  text-align: center;
  padding: 24px 12px;
  margin-bottom: 16px;
}
.badge-locked { opacity: 0.5; filter: grayscale(1); background: rgba(0,0,0,0.02); }
.badge-icon { color: #165dff; margin-bottom: 12px; filter: drop-shadow(0 4px 8px rgba(22, 93, 255, 0.2));}
.badge-locked .badge-icon { color: #86909c; filter: none;}
.badge-name { font-size: 15px; font-weight: 700; color: #1d2129; margin-bottom: 6px; }
.badge-desc { font-size: 12px; color: #86909c; line-height: 1.4;}

.task-list { display: flex; flex-direction: column; gap: 14px; }
.task-item {
  display: flex; align-items: center; gap: 16px;
  padding: 16px 20px;
}
.task-info { width: 180px; flex-shrink: 0; }
.task-name { font-size: 15px; font-weight: 600; color: #1d2129; margin-bottom: 4px; }
.task-reward { font-size: 13px; color: #ff7d00; font-weight: 500;}
.task-progress { flex: 1; display: flex; align-items: center; gap: 12px; }
.task-progress .el-progress { flex: 1; }
.task-progress-text { font-size: 13px; color: #86909c; min-width: 45px; font-weight: 500;}
.task-action { flex-shrink: 0; min-width: 80px; text-align: right; }

/* ── Tab 6：我的收藏 ── */
.fav-sub-tabs { margin-bottom: 8px; }
:deep(.fav-sub-tabs .el-tabs__header) { margin-bottom: 20px; }
.fav-grid { row-gap: 16px; }
.fav-card {
  position: relative;
  overflow: hidden;
  margin-bottom: 16px;
}
.fav-thumb { width: 100%; height: 140px; overflow: hidden; background: #f2f3f5; }
.fav-thumb img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s;}
.fav-card:hover .fav-thumb img { transform: scale(1.05); }
.fav-info { padding: 16px; }
.fav-title { font-size: 15px; font-weight: 600; color: #1d2129; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fav-time { font-size: 12px; color: #86909c; margin-top: 6px; }
.fav-remove-btn { position: absolute; top: 10px; right: 10px; background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(4px); border-radius: 8px; padding: 4px 10px; font-size: 12px; opacity: 0; transition: opacity 0.3s;}
.fav-card:hover .fav-remove-btn { opacity: 1; }

/* ── Tab 7：我的观察 ── */
.obs-header { display: flex; justify-content: flex-end; margin-bottom: 20px; }
.obs-list { display: flex; flex-direction: column; gap: 16px; }
.obs-item {
  display: flex; gap: 20px;
  padding: 16px;
}
.obs-image { width: 160px; height: 110px; border-radius: 10px; overflow: hidden; flex-shrink: 0; background: #f2f3f5; }
.obs-image img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s;}
.obs-item:hover .obs-image img { transform: scale(1.05);}
.obs-content { flex: 1; display: flex; flex-direction: column; justify-content: space-between; padding: 4px 0;}
.obs-title { font-size: 16px; font-weight: 700; color: #1d2129; margin-bottom: 8px; }
.obs-meta { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #86909c; margin-bottom: 12px; }
.obs-meta .el-divider--vertical { height: 12px; margin: 0 6px; border-color: rgba(0,0,0,0.1);}
.obs-tags { display: flex; gap: 10px; }

/* 动画 Keyframes */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 响应式微调 */
@media (max-width: 768px) {
  .user-card, .settings-card { border-radius: 16px !important; }
  .settings-card { padding: 16px !important; min-height: auto; }
  .tab-content { padding: 12px 0 16px; }

  /* 小屏下表单字段改为单列 */
  .tab-content :deep(.el-row) {
    flex-direction: column;
  }
  .tab-content :deep(.el-col) {
    width: 100% !important;
    max-width: 100% !important;
  }
  .tab-content :deep(.el-form-item) {
    margin-bottom: 14px;
  }

  .security-form {
    max-width: 100%;
  }

  .points-balance-card { flex-direction: column; align-items: flex-start; gap: 24px; padding: 24px; }
  .balance-num { font-size: 36px; }
  .balance-actions { flex-direction: row; width: 100%; }
  .balance-actions .el-button { flex: 1; }
  .task-item { flex-wrap: wrap; }
  .task-info { width: 100%; }
  .task-progress { flex: 1; min-width: 140px; }
  .obs-item { flex-direction: column; }
  .obs-image { width: 100%; height: 180px; }
  .fav-thumb { height: 120px; }
}
</style>
