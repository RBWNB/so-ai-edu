<template>
  <div class="profile-container">
    <el-row :gutter="24">
      <!-- ═══ 左侧：用户名片卡 ═══ -->
      <el-col :xs="24" :md="8">
        <el-card class="user-card" shadow="never" v-loading="loading">
          <div class="card-bg"></div>
          <div class="avatar-container">
            <!-- 隐藏的文件上传（通过下拉菜单触发） -->
            <el-upload
              ref="uploadRef"
              class="avatar-uploader"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleAvatarChange"
              accept="image/*"
              style="display:none"
            />
            <el-dropdown trigger="click" @command="handleAvatarCommand" placement="bottom" popper-class="avatar-glass-popper">
              <div class="avatar-wrapper" v-loading="uploading">
                <div class="avatar-frame-box" :class="'frame-' + (profileForm.avatarFrame || 'default')">
                  <el-avatar :size="96" :src="profileForm.avatarUrl" class="user-avatar">
                    <img src="https://cube.elemecdn.com/e/fd/0fc769396203ba652971805f60932png.png" />
                  </el-avatar>
                </div>
                <div class="avatar-mask">
                  <el-icon :size="18"><Camera /></el-icon>
                  <span class="avatar-mask-text">更换</span>
                </div>
              </div>
              <template #dropdown>
                <el-dropdown-menu class="avatar-dropdown-menu">
                  <el-dropdown-item command="upload">
                    <el-icon><Camera /></el-icon> 修改头像
                  </el-dropdown-item>
                  <el-dropdown-item command="frame">
                    <el-icon><Picture /></el-icon> 更换头像框
                  </el-dropdown-item>
                  <el-dropdown-item command="title">
                    <el-icon><Medal /></el-icon> 更换称号
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <div class="user-info-center">
            <div class="main-name">{{ profileForm.realName || profileForm.username || '未命名用户' }}</div>
            <div class="sub-name">@{{ profileForm.username }}</div>
            <div v-if="profileForm.userTitle && profileForm.userTitle !== '__none__'" class="user-title-tag">{{ profileForm.userTitle }}</div>
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
              <!-- API: GET /points/account ✅ -->
              <div class="stat-value text-primary">{{ coreOverview.availablePoints }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">学习等级</div>
              <!-- DB: user_learning_profile.level -->
              <!-- API: GET /learning/profile ✅ -->
              <div class="stat-value text-primary">Lv.{{ coreOverview.level }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">已获勋章</div>
              <!-- DB: user_badge COUNT WHERE user_id = ? -->
              <!-- API: GET /achievement/badges ✅ -->
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
                <el-form ref="formRef" :model="profileForm" :rules="rules" label-width="80px" label-position="top" class="profile-form">
                  <el-row :gutter="24">
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
                  <el-row :gutter="24">
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
                  <div class="form-actions">
                    <el-button class="submit-btn" @click="submitProfile" :loading="submitting">保存修改</el-button>
                  </div>
                </el-form>
              </div>
            </el-tab-pane>

            <!-- ========== Tab 2：安全设置（保留原有） ========== -->
            <el-tab-pane label="安全设置" name="security">
              <div class="tab-content">
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
                  <div class="form-actions">
                    <el-button class="submit-btn" @click="submitPassword" :loading="changingPassword">确认更新</el-button>
                  </div>
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
                <!-- 双倍经验倒计时横幅 -->
                <div v-if="doubleXpActive" class="xp-banner">
                  <span class="xp-banner-icon">⚡</span>
                  <span>双倍经验生效中</span>
                  <span class="xp-banner-countdown">剩余 {{ xpCountdown }}</span>
                </div>

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
                  API 状态：
                  ✅ GET    /bookmark/list            → 收藏列表（按类型分组）
                  ✅ POST   /bookmark/{type}/{id}     → 添加收藏
                  ✅ DELETE /bookmark/{type}/{id}     → 取消收藏
                -->
                <el-tabs v-model="favSubTab" class="fav-sub-tabs">
                  <!-- target_type 对应 DB user_bookmark.target_type 枚举值 -->
                  <el-tab-pane label="物种" name="species" />
                  <el-tab-pane label="生态系统" name="ecosystem" />
                  <el-tab-pane label="知识库" name="kb_document" />
                  <el-tab-pane label="题目" name="quiz_question" />
                  <el-tab-pane label="观察帖子" name="user_observation" />
                </el-tabs>

                <template v-if="currentFavList.length">
                  <el-row :gutter="16" class="fav-grid">
                    <el-col :xs="24" :sm="12" :md="8" v-for="item in currentFavList" :key="item.bookmarkId">
                      <div
                        class="fav-card"
                        :class="{ 'fav-card-clickable': favSubTab === 'user_observation' }"
                        @click="favSubTab === 'user_observation' && goToObservation(item)"
                      >
                        <div class="fav-thumb">
                          <img :src="getFavImageUrl(item.thumbnail)" :alt="item.title" @error="handleFavImgError" />
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
                          @click.stop="removeBookmark(item)"
                        >
                          取消收藏
                        </el-button>
                      </div>
                    </el-col>
                </el-row>
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
                  API 状态：
                  ✅ GET  /observation/list → 我的观察列表
                  ✅ POST /observation       → 发布观察
                -->
                <div class="obs-header">
                  <el-button type="primary" @click="publishObservation">
                    <el-icon><Plus /></el-icon> 发布观察
                  </el-button>
                </div>

                <template v-if="observationList.length">
                  <div class="obs-list">
                    <div class="obs-item" v-for="obs in observationList" :key="obs.id">
                      <div class="obs-image" @click="viewObservation(obs)">
                        <!-- DB: media_asset.url (via photo_media_id) -->
                        <img v-if="obs.photoUrl" :src="obs.photoUrl" :alt="obs.title" />
                        <div v-else class="obs-no-image">
                          <el-icon :size="32"><Picture /></el-icon>
                          <span>暂无图片</span>
                        </div>
                      </div>
                      <div class="obs-content">
                        <div class="obs-title-row">
                          <div class="obs-title" @click="viewObservation(obs)">{{ obs.title }}</div>
                          <div class="obs-actions">
                            <el-button size="small" text type="primary" @click="viewObservation(obs)">
                              <el-icon :size="14"><View /></el-icon> 详情
                            </el-button>
                            <el-button size="small" text type="warning" @click="editObservation(obs)">
                              <el-icon :size="14"><Edit /></el-icon> 编辑
                            </el-button>
                            <el-popconfirm title="确定删除此观察记录？" confirmButtonText="确定" cancelButtonText="取消"
                                              width="220" @confirm="removeObservation(obs)">
                              <template #reference>
                                <el-button size="small" text type="danger">
                                  <el-icon :size="14"><Delete /></el-icon> 删除
                                </el-button>
                              </template>
                            </el-popconfirm>
                          </div>
                        </div>
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
                          <el-tag v-if="obs.aiSpeciesName" size="small" effect="plain" class="custom-tag">
                            AI识别：{{ obs.aiSpeciesName }} ({{ obs.aiConfidence }}%)
                          </el-tag>
                          <!-- DB: user_observation.status: 0=隐藏 1=公开 2=审核中 -->
                          <el-tag size="small" :class="obsStatusClass(obs.status)">
                            {{ obsStatusLabel(obs.status) }}
                          </el-tag>
                          <div v-if="obs.status === 0 && obs.auditRemark" class="obs-reject-reason">
                            <el-icon :size="14"><WarningFilled /></el-icon>
                            下架原因：{{ obs.auditRemark }}
                          </div>
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

  <!-- ═══ 头像框选择弹窗 ═══ -->
  <el-dialog v-model="frameDialogVisible" title="选择头像框" width="640px" :close-on-click-modal="false" class="frame-dialog">
    <div class="frame-grid">
      <div
        v-for="f in frameList"
        :key="f.code"
        class="frame-card"
        :class="{
          'frame-card-active': pendingFrame === f.code,
          'frame-card-locked': !ownedFrames.has(f.code)
        }"
        @click="ownedFrames.has(f.code) && selectFrame(f.code)"
      >
        <div class="frame-card-preview">
          <div class="frame-card-ring" :class="'frame-' + f.code">
            <el-avatar :size="56" :src="profileForm.avatarUrl" class="user-avatar" />
          </div>
          <div v-if="!ownedFrames.has(f.code)" class="frame-card-lock">
            <el-icon :size="16"><Lock /></el-icon>
          </div>
          <div v-if="f.code === 'default' || (profileForm.avatarFrame === f.code && ownedFrames.has(f.code) && pendingFrame !== f.code)" class="frame-card-badge">
            {{ f.code === 'default' ? '默认' : '已装备' }}
          </div>
          <div v-if="pendingFrame === f.code" class="frame-card-check">
            <el-icon :size="16"><Check /></el-icon>
          </div>
        </div>
        <div class="frame-card-name">{{ f.name }}</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="frameDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="frameSaving" @click="saveFrame">保存</el-button>
    </template>
  </el-dialog>

  <!-- ═══ 称号选择弹窗 ═══ -->
  <el-dialog v-model="titleDialogVisible" title="选择称号" width="500px" :close-on-click-modal="false" class="frame-dialog">
    <div class="title-grid">
      <!-- 不佩戴称号 -->
      <div class="title-card" :class="{ 'title-card-active': pendingTitle === '__none__' }" @click="selectTitle('__none__')">
        <div class="title-card-icon">🚫</div>
        <div class="title-card-name">不佩戴称号</div>
      </div>
      <div
        v-for="t in allTitleOptions"
        :key="t.code"
        class="title-card"
        :class="{
          'title-card-active': pendingTitle === t.code,
          'title-card-locked': !t.fromBadge && badgeCount < (t.needBadges || 999)
        }"
        @click="(t.fromBadge || badgeCount >= (t.needBadges || 999)) && selectTitle(t.code)"
      >
        <div class="title-card-icon">{{ t.icon }}</div>
        <div class="title-card-name">{{ t.label }}</div>
        <div class="title-card-need" v-if="!t.fromBadge && badgeCount < (t.needBadges || 999)">🔒 需{{ t.needBadges }}勋章</div>
      </div>
      <!-- 自定义称号 -->
      <div class="title-card title-custom" :class="{
        'title-card-active': pendingTitle === '__custom__',
        'title-card-locked': !ownedCustomTitle
      }" @click="ownedCustomTitle && startCustomTitle()">
        <div class="title-card-icon">✏️</div>
        <div class="title-card-name">自定义</div>
        <div class="title-card-need" v-if="!ownedCustomTitle">🔒 商店购买</div>
      </div>
    </div>
    <div v-if="customTitleMode && ownedCustomTitle" class="custom-title-input" style="margin-top:12px">
      <el-input v-model="customTitleValue" placeholder="输入你的自定义称号（最长10字）" maxlength="10" show-word-limit />
    </div>
    <template #footer>
      <el-button @click="titleDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="titleSaving" @click="saveTitle">保存</el-button>
    </template>
  </el-dialog>

  <!-- ═══ 观察记录详情弹窗 ═══ -->
  <el-dialog v-model="obsDetailVisible" title="观察记录详情" width="600px" class="obs-detail-dialog">
    <div v-loading="obsDetailLoading" class="obs-detail-body">
      <template v-if="obsDetail.id">
        <!-- 图片区域 -->
        <div class="detail-image" v-if="obsDetail.photoUrl">
          <img :src="obsDetail.photoUrl" :alt="obsDetail.title" />
        </div>
        <div v-else class="detail-no-image">
          <el-icon :size="48"><Picture /></el-icon>
          <span>暂无图片</span>
        </div>

        <!-- 信息区域 -->
        <el-descriptions :column="1" border size="default" class="detail-desc">
          <el-descriptions-item label="标题">{{ obsDetail.title }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ obsDetail.description || '暂无描述' }}</el-descriptions-item>
          <el-descriptions-item label="物种">
            {{ obsDetail.speciesName || '未指定' }}
          </el-descriptions-item>
          <el-descriptions-item label="地点">
            <span>{{ obsDetail.locationName }}</span>
            <span v-if="obsDetail.latitude && obsDetail.longitude">
              ({{ obsDetail.latitude }}, {{ obsDetail.longitude }})
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="观察时间">{{ obsDetail.observedAt }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :class="obsStatusClass(obsDetail.status)">
              {{ obsStatusLabel(obsDetail.status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </div>
    <template #footer>
      <el-button @click="obsDetailVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- ═══ 观察记录编辑弹窗 ═══ -->
  <el-dialog v-model="obsEditVisible" title="编辑观察记录" width="620px"
             :close-on-click-modal="false" class="obs-edit-dialog">
    <el-form ref="obsEditFormRef" :model="obsEditForm" :rules="obsEditRules"
             label-width="90px" label-position="top" v-loading="obsEditSaving">
      <el-form-item label="标题" prop="title">
        <el-input v-model="obsEditForm.title" placeholder="给你的观察取个名字" maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="obsEditForm.description" type="textarea" :rows="3"
                  placeholder="描述你观察到的内容..." maxlength="500" show-word-limit />
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="观察地点" prop="locationName">
            <el-input v-model="obsEditForm.locationName" placeholder="如：深圳湾公园" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="观察时间" prop="observedAt">
            <el-date-picker
              v-model="obsEditForm.observedAt"
              type="datetime"
              placeholder="选择观察时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="纬度">
            <el-input v-model.number="obsEditForm.latitude" placeholder="如：22.5264" type="number" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="经度">
            <el-input v-model.number="obsEditForm.longitude" placeholder="如：113.9558" type="number" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="物种（可选）">
        <el-select
          v-model="obsEditForm.speciesId"
          placeholder="搜索选择物种"
          filterable remote
          :remote-method="editSearchSpecies"
          :loading="editSpeciesLoading"
          clearable
          style="width: 100%"
        >
          <el-option v-for="s in editSpeciesOptions" :key="s.id" :label="s.chineseName" :value="s.id" />
        </el-select>
      </el-form-item>

      <!-- 照片编辑区域 -->
      <el-form-item label="照片">
        <div class="edit-photo-area">
          <!-- 已有照片预览 -->
          <div v-if="obsEditForm.photoUrl || obsEditPhotoPreview" class="current-photo-preview">
            <img :src="obsEditPhotoPreview || obsEditForm.photoUrl" alt="观察照片" />
            <div class="photo-mask">
              <el-button type="danger" size="small" circle @click="removeEditPhoto">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
          <!-- 上传新照片（无照片或已删除时显示） -->
          <el-upload
            v-else
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleEditPhotoChange"
            accept="image/*"
            class="edit-photo-upload"
            v-loading="obsEditPhotoUploading"
          >
            <div class="upload-trigger">
              <el-icon :size="28"><Plus /></el-icon>
              <span>上传照片</span>
            </div>
          </el-upload>
          <div v-if="obsEditPhotoUploading" class="upload-tip">正在上传到云端...</div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="obsEditVisible = false">取消</el-button>
      <el-button type="primary" :loading="obsEditSaving" @click="submitEditObservation">保存修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Camera, Picture, Check, Reading, ChatDotRound, Lock,
  Medal, TrophyBase, StarFilled, Present,
  Location, Clock, Plus, View, Edit, Delete, WarningFilled
} from "@element-plus/icons-vue";
import { getUserProfile, updatePasswordApi, updateUserProfile, uploadAvatarApi, updateAvatarFrameApi, updateUserTitleApi } from "@/api/sysUser";
import { getLearningProfile, getAnswerHistory, getAiSessionCount } from "@/api/learning";
import { getBadgeCount } from "@/api/achievement";
import { getOwnedTitle } from "@/api/points";
import { getPointsAccount, getPointsTransactions, getExchangeOrders, getOwnedFrames } from "@/api/points";
import { getBadges, getDailyTasks, claimTaskReward, dailyCheckin } from "@/api/achievement";
import { removeBookmark as removeBookmarkApi, getBookmarkList } from "@/api/bookmark";
import { getObservationList, getObservationDetail, updateObservation, deleteObservation } from "@/api/observation";
import { useAuthStore } from "@/store/auth";

const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();
const activeTab = ref(route.query.tab || "basic");

// ═══ 角色映射（app_role.role_code → 中文名） ═══
const ROLE_MAP = { ADMIN: "系统管理员", MANAGER: "内容管理员", VISITOR: "普通用户" };
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
  avatarUrl: "",  // app_user.avatar_url
  avatarFrame: "default",  // app_user.avatar_frame
  userTitle: "",  // app_user.user_title
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
    authStore.avatarFrame = profileForm.avatarFrame || "default";
    localStorage.setItem('marine_avatar_frame', profileForm.avatarFrame || "default");
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
  // user_point_account.available_points ← GET /points/account ✅
  availablePoints: 0,
  // user_learning_profile.level ← GET /learning/profile ✅
  level: 1,
  // COUNT(user_badge) ← GET /achievement/badges ✅
  badgeCount: 0,
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

// 双倍经验卡状态 ← GET /learning/profile ✅
const doubleXpActive = ref(false);
const xpCountdown = ref("");
const xpExpireAt = ref(null);  // 后端返回的过期时间字符串

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
    // 双倍经验状态
    doubleXpActive.value = d.doubleXp ?? false;
    xpExpireAt.value = d.xpExpireAt ?? null;
    if (doubleXpActive.value) startXpCountdown();
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

let xpTimer = null;
const startXpCountdown = () => {
  clearInterval(xpTimer);
  const expireTime = xpExpireAt.value ? new Date(xpExpireAt.value.replace(" ", "T")) : null;
  const tick = () => {
    const diff = expireTime ? expireTime - Date.now() : 0;
    if (diff <= 0) {
      doubleXpActive.value = false;
      xpCountdown.value = "";
      clearInterval(xpTimer);
      return;
    }
    const h = Math.floor(diff / 3600000);
    const m = Math.floor((diff % 3600000) / 60000);
    const s = Math.floor((diff % 60000) / 1000);
    xpCountdown.value = `${h}时${m}分${s}秒`;
  };
  tick();
  xpTimer = setInterval(tick, 1000);
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
  router.push("/points/shop");
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
    // 追加商店隐藏勋章（不在静态定义内，从 API 补充）
    const staticCodes = new Set(ALL_BADGE_DEFS.map(d => d.badgeCode));
    const hiddenBadges = earnedList
      .filter(b => !staticCodes.has(b.badgeCode))
      .map(b => ({
        badgeCode: b.badgeCode,
        badgeName: b.badgeName,
        icon: Present,         // 隐藏勋章默认图标
        unlockCondition: "积分商店兑换获得",
        earned: true,
        earnedAt: b.earnedAt || "",
      }));
    if (hiddenBadges.length > 0) {
      badgeList.value = [...badgeList.value, ...hiddenBadges];
    }
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

// ═══ 头像框 ═══
const uploadRef = ref(null);
const frameDialogVisible = ref(false);
const frameSaving = ref(false);
const pendingFrame = ref("default");
const ownedFrames = ref(new Set(["default"]));
const frameList = [
  { code: "default", name: "默认" },
  { code: "gold",    name: "黄金边框" },
  { code: "ocean",   name: "深海边框" },
  { code: "rainbow", name: "彩虹边框" },
  { code: "flame",   name: "火焰边框" },
  { code: "dashed",  name: "烈焰虚线" },
  { code: "neon",    name: "霓虹光效" },
  { code: "aurora",  name: "极光幻彩" },
  { code: "crystal", name: "冰晶之辉" },
  { code: "royal",   name: "紫金皇冠" },
];

const openFrameDialog = async () => {
  pendingFrame.value = profileForm.avatarFrame || "default";
  frameDialogVisible.value = true;
  // 拉取已拥有的头像框
  try {
    const res = await getOwnedFrames();
    if (res.data.success) {
      ownedFrames.value = new Set(res.data.data || ["default"]);
    }
  } catch { /* 离线也能用默认框 */ }
};

const selectFrame = (code) => {
  pendingFrame.value = code;
};

/** 头像下拉菜单命令 */
const handleAvatarCommand = (cmd) => {
  if (cmd === 'upload') {
    // 触发隐藏的 file input
    const input = uploadRef.value?.$el?.querySelector('input[type="file"]');
    if (input) input.click();
  } else if (cmd === 'frame') {
    openFrameDialog();
  } else if (cmd === 'title') {
    openTitleDialog();
  }
};

const saveFrame = async () => {
  frameSaving.value = true;
  try {
    const res = await updateAvatarFrameApi(pendingFrame.value);
    if (res.data.success || res.status === 200) {
      profileForm.avatarFrame = pendingFrame.value;
      authStore.avatarFrame = pendingFrame.value;  // 同步到全局
      localStorage.setItem('marine_avatar_frame', pendingFrame.value); // 持久化
      frameDialogVisible.value = false;
      ElMessage.success("头像框已更换");
    }
  } catch (err) {
    ElMessage.error("更换头像框失败");
  } finally {
    frameSaving.value = false;
  }
};

// ═══ 称号 ═══
const titleDialogVisible = ref(false);
const titleSaving = ref(false);
const pendingTitle = ref('');
const customTitleMode = ref(false);
const customTitleValue = ref('');
const badgeCount = ref(0);
const ownedCustomTitle = ref(false);

const titleList = [
  { code: 'ocean_explorer', icon: '🌊', label: '海洋探索者', needBadges: 1 },
  { code: 'marine_scholar', icon: '📚', label: '海洋学者', needBadges: 2 },
  { code: 'coral_guardian', icon: '🪸', label: '珊瑚守护者', needBadges: 3 },
  { code: 'whale_friend', icon: '🐋', label: '鲸鱼之友', needBadges: 4 },
  { code: 'turtle_hero', icon: '🐢', label: '海龟英雄', needBadges: 5 },
  { code: 'starfish_collector', icon: '⭐', label: '海星收藏家', needBadges: 6 },
  { code: 'tide_pooler', icon: '🏖️', label: '潮间带行者', needBadges: 7 },
  { code: 'deep_diver', icon: '🤿', label: '深海潜水员', needBadges: 8 },
  { code: 'plankton_lover', icon: '🦐', label: '浮游生物爱好者', needBadges: 9 },
  { code: 'kelp_guardian', icon: '🌿', label: '海藻守护者', needBadges: 10 },
];

const earnedBadgeTitles = ref([]); // 商城购买的隐藏称号
const allTitleOptions = computed(() => [...titleList, ...earnedBadgeTitles.value]);

const openTitleDialog = async () => {
  customTitleMode.value = false;
  customTitleValue.value = '';
  earnedBadgeTitles.value = [];
  // 拉取勋章数量、自定义称号拥有状态、已获勋章列表
  try {
    const [bcRes, otRes, badgeRes] = await Promise.all([
      getBadgeCount(),
      getOwnedTitle(),
      getBadges()
    ]);
    badgeCount.value = bcRes.data?.data ?? 0;
    ownedCustomTitle.value = otRes.data?.data ?? false;
    // 筛选商城购买的隐藏称号（badgeName 以「隐藏称号」开头）
    const badges = badgeRes.data?.data ?? [];
    earnedBadgeTitles.value = badges
      .filter(b => b.badgeName && b.badgeName.includes('隐藏称号'))
      .map(b => ({
        code: 'badge_' + b.badgeCode,
        icon: '🏅',
        label: b.badgeName.replace(/^🏅\s*/, ''),
        fromBadge: true
      }));
  } catch { /* 降级处理 */ }
  // 判断当前称号
  const current = profileForm.userTitle || '';
  if (!current) {
    pendingTitle.value = '__none__';
  } else {
    const allTitles = [...titleList, ...earnedBadgeTitles.value];
    const found = allTitles.find(t => t.code === current || t.label === current);
    if (found) {
      pendingTitle.value = found.code;
    } else if (ownedCustomTitle.value) {
      pendingTitle.value = '__custom__';
      customTitleValue.value = current;
      customTitleMode.value = true;
    } else {
      const firstUnlocked = titleList.find(t => badgeCount.value >= t.needBadges);
      pendingTitle.value = firstUnlocked ? firstUnlocked.code : '__none__';
    }
  }
  titleDialogVisible.value = true;
};

const selectTitle = (code) => {
  customTitleMode.value = false;
  customTitleValue.value = '';
  pendingTitle.value = code;
};

const startCustomTitle = () => {
  pendingTitle.value = '__custom__';
  customTitleMode.value = true;
  customTitleValue.value = '';
};

const saveTitle = async () => {
  let title = '';
  if (pendingTitle.value === '__none__') {
    title = '__none__'; // 不佩戴称号 → 保存特殊标记
  } else if (pendingTitle.value === '__custom__') {
    title = customTitleValue.value.trim();
    if (!title) {
      ElMessage.warning('请输入自定义称号');
      return;
    }
  } else {
    // 同时查找 titleList 和商城购买的隐藏称号
    const allTitles = [...titleList, ...earnedBadgeTitles.value];
    const found = allTitles.find(t => t.code === pendingTitle.value);
    title = found ? found.label : '';
  }

  titleSaving.value = true;
  try {
    const res = await updateUserTitleApi(title);
    if (res.data.success || res.status === 200) {
      profileForm.userTitle = title;
      titleDialogVisible.value = false;
      ElMessage.success("称号已更换");
    }
  } catch (err) {
    ElMessage.error("更换称号失败");
  } finally {
    titleSaving.value = false;
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
// API: GET /bookmark/list ✅  DELETE /bookmark/{type}/{id} ✅  POST /bookmark/{type}/{id} ✅
const favSubTab = ref("species");
const favLoading = ref(false);
const favDataMap = reactive({
  species: [],
  ecosystem: [],
  kb_document: [],
  quiz_question: [],
  user_observation: [],
});

/** 处理收藏列表中图片URL（兼容七牛云完整URL和本地相对路径） */
const getFavImageUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http')) return url;
  if (url.startsWith('/uploads/')) return `/api${url}`;
  if (url.startsWith('/')) return `/api/uploads${url}`;
  return `/api/uploads/${url}`;
};

/** 收藏图片加载失败时隐藏 */
const handleFavImgError = (e) => {
  e.target.style.display = 'none';
};

const fetchBookmarks = async () => {
  favLoading.value = true;
  try {
    const res = await getBookmarkList();
    if (res.data.success) {
      const d = res.data.data;
      favDataMap.species = d.species ?? [];
      favDataMap.ecosystem = d.ecosystem ?? [];
      favDataMap.kb_document = d.kb_document ?? [];
      favDataMap.quiz_question = d.quiz_question ?? [];
      favDataMap.user_observation = d.user_observation ?? [];
    }
  } catch (err) {
    console.error("获取收藏列表失败", err);
  } finally {
    favLoading.value = false;
  }
};

const currentFavList = computed(() => {
  return favDataMap[favSubTab.value] || [];
});

/** 点击收藏的观察帖子 → 跳到独立详情页，保存滚动位置 */
const goToObservation = (item) => {
  sessionStorage.setItem('profile_scroll_top', window.scrollY);
  sessionStorage.setItem('profile_tab', activeTab.value);
  sessionStorage.setItem('profile_fav_subtab', favSubTab.value);
  router.push({ name: 'EduObservationDetail', params: { id: item.targetId } });
};

const removeBookmark = async (item) => {
  // DELETE /bookmark/{targetType}/{targetId} ✅
  ElMessageBox.confirm(`确定取消收藏「${item.title}」？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  }).then(async () => {
    try {
      const res = await removeBookmarkApi(item.targetType, item.targetId);
      if (res.data.success) {
        ElMessage.success("已取消收藏");
        // 从本地列表移除
        const list = favDataMap[favSubTab.value];
        if (list) {
          const idx = list.findIndex(f => f.targetType === item.targetType && f.targetId === item.targetId);
          if (idx !== -1) list.splice(idx, 1);
        }
      } else {
        ElMessage.warning(res.data.message || "取消收藏失败");
      }
    } catch (err) {
      ElMessage.error("取消收藏失败");
    }
  }).catch(() => {});
};

// ═══ Tab 7：我的观察 ═══
// DB: user_observation (title, location_name, observed_at, photo_media_id,
//     ai_identified, ai_confidence, status)
// API: GET /observation/list ✅  POST /observation ✅  GET /observation/{id} ✅
//      PUT /observation/{id} ✅  DELETE /observation/{id} ✅
const obsLoading = ref(false);
const obsPage = reactive({ pageNum: 1, pageSize: 6, total: 0 });

const observationList = ref([]);

const fetchObservations = async () => {
  obsLoading.value = true;
  try {
    const res = await getObservationList();
    if (res.data.success) {
      observationList.value = res.data.data ?? [];
      obsPage.total = observationList.value.length;
    }
  } catch (err) {
    console.error("获取观察记录失败", err);
    observationList.value = [];
  } finally {
    obsLoading.value = false;
  }
};

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

// ═══ 详情弹窗 ═══
const obsDetailVisible = ref(false);
const obsDetailLoading = ref(false);
const obsDetail = reactive({
  id: null, title: "", description: "", locationName: "",
  observedAt: "", latitude: null, longitude: null,
  speciesName: "", speciesId: null, photoUrl: "",
  photoMediaId: null, status: null, createdAt: ""
});

const viewObservation = async (obs) => {
  obsDetailVisible.value = true;
  obsDetailLoading.value = true;
  try {
    const res = await getObservationDetail(obs.id);
    if (res.data.success) {
      Object.assign(obsDetail, res.data.data);
    } else {
      ElMessage.warning(res.data.message || "获取详情失败");
    }
  } catch (err) {
    ElMessage.error("获取观察详情失败");
  } finally {
    obsDetailLoading.value = false;
  }
};

// ═══ 编辑弹窗 ═══
const obsEditVisible = ref(false);
const obsEditSaving = ref(false);
const obsEditFormRef = ref(null);
const editSpeciesLoading = ref(false);
const editSpeciesOptions = ref([]);

const obsEditForm = reactive({
  id: null, title: "", description: "", locationName: "",
  observedAt: "", latitude: null, longitude: null, speciesId: null, photoMediaId: null, photoUrl: "",
});

// 编辑弹窗照片相关状态
const obsEditPhotoPreview = ref("");
const obsEditPhotoUploading = ref(false);
const obsEditNewPhotoFile = ref(null);
const obsEditNewMediaId = ref(null);

const obsEditRules = {
  title: [{ required: true, message: "请输入标题", trigger: "blur" }],
  locationName: [{ required: true, message: "请输入观察地点", trigger: "blur" }],
  observedAt: [{ required: true, message: "请选择观察时间", trigger: "change" }],
};

const editObservation = async (obs) => {
  // 先拉取完整数据（列表可能字段不全），加载完再打开弹窗
  obsEditSaving.value = true;
  // 重置照片相关状态
  obsEditPhotoPreview.value = "";
  obsEditNewPhotoFile.value = null;
  obsEditNewMediaId.value = null;
  try {
    const res = await getObservationDetail(obs.id);
    if (res.data.success) {
      const d = res.data.data;
      // 重置表单并填充数据
      Object.assign(obsEditForm, {
        id: d.id,
        title: d.title || "",
        description: d.description || "",
        locationName: d.locationName || "",
        observedAt: d.observedAt || "",
        latitude: d.latitude ?? null,
        longitude: d.longitude ?? null,
        speciesId: d.speciesId ?? null,
        photoMediaId: d.photoMediaId ?? null,
        photoUrl: d.photoUrl || "",
      });
      // 回显物种名到搜索选项中
      if (d.speciesName && !editSpeciesOptions.value.some(s => s.id === d.speciesId)) {
        editSpeciesOptions.value.push({ id: d.speciesId, chineseName: d.speciesName });
      }
      // 数据就绪后再打开弹窗
      obsEditVisible.value = true;
    } else {
      ElMessage.warning(res.data.message || "获取数据失败");
    }
  } catch (err) {
    ElMessage.error("获取数据失败");
  } finally {
    obsEditSaving.value = false;
  }
};

const editSearchSpecies = async (query) => {
  if (!query) { editSpeciesOptions.value = []; return; }
  editSpeciesLoading.value = true;
  try {
    const { suggestSpecies } = await import("@/api/species");
    const res = await suggestSpecies(query);
    const list = res.data?.data ?? res.data ?? [];
    editSpeciesOptions.value = Array.isArray(list) ? list : [];
  } catch {
    editSpeciesOptions.value = [];
  } finally {
    editSpeciesLoading.value = false;
  }
};

const submitEditObservation = async () => {
  if (!obsEditFormRef.value) return;
  await obsEditFormRef.value.validate(async (valid) => {
    if (!valid) return;
    // 如果正在上传照片，提示等待
    if (obsEditNewPhotoFile.value && !obsEditNewMediaId.value) {
      ElMessage.warning("图片正在上传中，请稍候...");
      return;
    }
    obsEditSaving.value = true;
    try {
      const payload = {
        title: obsEditForm.title,
        description: obsEditForm.description,
        locationName: obsEditForm.locationName,
        observedAt: obsEditForm.observedAt,
        latitude: obsEditForm.latitude,
        longitude: obsEditForm.longitude,
        speciesId: obsEditForm.speciesId,
        // 如果有新上传的照片使用新的 mediaId，否则保持原有的
        photoMediaId: obsEditNewMediaId.value || obsEditForm.photoMediaId || null,
      };
      const res = await updateObservation(obsEditForm.id, payload);
      if (res.data.success) {
        ElMessage.success(res.data.message || "更新成功，将重新审核");
        obsEditVisible.value = false;
        fetchObservations(); // 刷新列表
      } else {
        ElMessage.warning(res.data.message || "更新失败");
      }
    } catch (err) {
      ElMessage.error("更新失败，请重试");
    } finally {
      obsEditSaving.value = false;
    }
  });
};

/** 编辑弹窗 - 处理照片选择 */
const handleEditPhotoChange = async (uploadFile) => {
  const file = uploadFile.raw;
  if (!file) return;

  // 文件大小限制 10MB
  if (file.size / 1024 / 1024 > 10) {
    ElMessage.error("图片大小不能超过 10MB");
    return;
  }

  // 本地预览
  obsEditNewPhotoFile.value = file;
  obsEditPhotoPreview.value = URL.createObjectURL(file);

  // 上传到云端
  obsEditPhotoUploading.value = true;
  try {
    const { uploadObservationPhoto } = await import("@/api/observation");
    const res = await uploadObservationPhoto(file);
    if (res.data.success) {
      obsEditNewMediaId.value = res.data.data.mediaId;
      ElMessage.success("图片上传成功");
    } else {
      ElMessage.warning(res.data.message || "图片上传失败");
      // 上传失败则回滚
      obsEditNewPhotoFile.value = null;
      obsEditPhotoPreview.value = "";
      obsEditNewMediaId.value = null;
    }
  } catch (err) {
    ElMessage.error("图片上传失败，请重试");
    obsEditNewPhotoFile.value = null;
    obsEditPhotoPreview.value = "";
    obsEditNewMediaId.value = null;
  } finally {
    obsEditPhotoUploading.value = false;
  }
};

/** 编辑弹窗 - 移除照片 */
const removeEditPhoto = () => {
  obsEditForm.photoUrl = "";
  obsEditForm.photoMediaId = null;
  obsEditPhotoPreview.value = "";
  obsEditNewPhotoFile.value = null;
  obsEditNewMediaId.value = null;
};

// ═══ 删除 ═══
const removeObservation = async (obs) => {
  try {
    const res = await deleteObservation(obs.id);
    if (res.data.success) {
      ElMessage.success("删除成功");
      // 从本地列表移除
      const idx = observationList.value.findIndex(o => o.id === obs.id);
      if (idx !== -1) observationList.value.splice(idx, 1);
      obsPage.total = observationList.value.length;
    } else {
      ElMessage.warning(res.data.message || "删除失败");
    }
  } catch (err) {
    ElMessage.error("删除失败，请重试");
  }
};

const publishObservation = () => {
  // 路由 → /observation/publish ✅
  // POST /observation body: { title, description, species_id, latitude, longitude, location_name, observed_at, photo_media_id }
  router.push("/observation/publish");
};

// ═══ 初始化 ═══
onMounted(() => {
  fetchProfile();
  fetchLearningData();
  // 预加载左侧名片核心数据（积分余额 + 勋章数量）
  fetchPointsAccount();
  fetchBadges();
  // 从 sessionStorage 恢复状态（从帖子详情页返回时）
  const savedTab = sessionStorage.getItem('profile_tab');
  const savedFavSubtab = sessionStorage.getItem('profile_fav_subtab');
  if (savedTab) {
    sessionStorage.removeItem('profile_tab');
    activeTab.value = savedTab;
  }
  if (savedFavSubtab) {
    sessionStorage.removeItem('profile_fav_subtab');
    favSubTab.value = savedFavSubtab;
  }

  // 根据路由参数 tab 加载对应 Tab 数据（如从发布观察页返回时定位到"我的观察"）
  const initTab = route.query.tab;
  const effectiveTab = savedTab || initTab;
  if (effectiveTab === "observation") {
    fetchObservations();
    dataFetchedFlags.observation = true;
  } else if (effectiveTab === "learning") {
    fetchLearningData();
    dataFetchedFlags.learning = true;
  } else if (effectiveTab === "points") {
    fetchPointsData();
    dataFetchedFlags.points = true;
  } else if (effectiveTab === "achievement") {
    fetchAchievementData();
    dataFetchedFlags.achievement = true;
  } else if (effectiveTab === "favorites") {
    fetchBookmarks();
    dataFetchedFlags.bookmark = true;
  }

  // 从帖子详情页返回时恢复滚动位置
  const savedScroll = sessionStorage.getItem('profile_scroll_top');
  if (savedScroll) {
    sessionStorage.removeItem('profile_scroll_top');
    nextTick(() => {
      window.scrollTo(0, parseInt(savedScroll));
    });
  }
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
const dataFetchedFlags = { learning: false, points: false, achievement: false, bookmark: false, observation: false };
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
  if (tab === "favorites") {
    fetchBookmarks();
    dataFetchedFlags.bookmark = true;
  }
  if (tab === "observation") {
    fetchObservations();
    dataFetchedFlags.observation = true;
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
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s ease;
}
.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}
.avatar-mask-text {
  font-size: 11px;
  font-weight: 500;
  text-shadow: 0 1px 3px rgba(0,0,0,0.4);
}

/* 头像下拉菜单样式 */
:deep(.avatar-dropdown-menu) {
  min-width: 140px;
  border-radius: 12px;
  padding: 6px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}
:deep(.avatar-dropdown-menu .el-dropdown-menu__item) {
  border-radius: 8px;
  padding: 8px 14px;
  font-size: 14px;
  gap: 8px;
}
:deep(.avatar-dropdown-menu .el-dropdown-menu__item .el-icon) {
  margin-right: 6px;
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
  margin-bottom: 8px;
}

.user-title-tag {
  display: inline-block;
  font-size: 12px;
  color: #b8860b;
  background: linear-gradient(135deg, #fef9e7, #fdebd0);
  padding: 2px 12px;
  border-radius: 12px;
  margin-bottom: 10px;
  font-weight: 500;
  border: 1px solid rgba(255, 215, 0, 0.3);
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

/* 多 Tab 区域面板 */
.settings-card {
  min-height: 600px;
  padding: 20px 24px !important;
}

/* 安全设置表单用适中的宽度，和基本资料对齐 */
.security-form {
  max-width: 420px;
  margin-top: 8px;
}

.profile-form {
  max-width: 600px;
  margin-top: 8px;
}

.tab-content {
  padding: 16px 4px 24px;
  animation: fadeIn 0.4s ease;
}

.form-actions {
  margin-top: 28px;
  display: flex;
  justify-content: flex-start; /* 靠左对齐，也可以改成 center */
}

/* 个人资料表单更紧凑 */
.tab-content :deep(.el-form-item) {
  margin-bottom: 16px;
}

.tab-content :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  padding-bottom: 8px !important;
}
.tab-content :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.6);
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.06) inset;
  border-radius: 12px;
  padding: 6px 16px;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
}
.tab-content :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px #165dff inset !important;
  background: #fff;
}
.tab-content :deep(.el-input__inner) {
  color: #1d2129;
  font-weight: 600;
  height: 38px;
}
.tab-content :deep(.el-input__inner::placeholder) {
  color: #c9cdd4;
  font-weight: 400;
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

/* 按钮样式 */
.submit-btn {
  min-width: 140px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #165dff, #00d2ff);
  border: none;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  box-shadow: 0 6px 16px rgba(22, 93, 255, 0.25);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(22, 93, 255, 0.4);
  color: #fff;
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
.xp-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  margin-bottom: 20px;
  border-radius: 12px;
  background: linear-gradient(135deg, #fff7e6, #ffe7ba);
  border: 1px solid #ffd591;
  color: #ad6800;
  font-size: 15px;
  font-weight: 600;
  animation: xpPulse 2s ease-in-out infinite;
}
@keyframes xpPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(250, 140, 22, 0.3); }
  50% { box-shadow: 0 0 16px 4px rgba(250, 140, 22, 0.15); }
}
.xp-banner-icon {
  font-size: 22px;
  animation: xpBounce 0.6s ease-in-out infinite alternate;
}
@keyframes xpBounce {
  from { transform: translateY(0); }
  to { transform: translateY(-4px); }
}
.xp-banner-countdown {
  margin-left: auto;
  font-size: 14px;
  font-weight: 700;
  color: #d46b08;
}

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

/* ── Tab 4：我的积分 (深海黑金风格) ── */
.points-balance-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  /* 极深蓝灰色底，沉稳不刺眼 */
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  border: 1px solid rgba(212, 175, 55, 0.15); /* 极弱的金边 */
  border-radius: 20px;
  padding: 32px 40px;
  margin-bottom: 28px;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.25);
  transition: transform 0.4s ease, box-shadow 0.4s ease;
  position: relative;
  overflow: hidden;
}

/* 扫光特效保留，在黑底上更显高级 */
.points-balance-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 50%;
  height: 100%;
  background: linear-gradient(to right, rgba(255,255,255,0) 0%, rgba(255,255,255,0.1) 50%, rgba(255,255,255,0) 100%);
  transform: skewX(-25deg);
  transition: all 0.75s ease;
}
.points-balance-card:hover::before {
  left: 200%;
}

.points-balance-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.4);
}

.balance-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
  z-index: 1;
}

.balance-label { font-size: 15px; color: rgba(255, 255, 255, 0.5); font-weight: 500;}

/* 核心积分数字：铂金色渐变 */
.balance-num {
  font-size: 48px;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(135deg, #fde08b 0%, #d4af37 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 4px 12px rgba(212, 175, 55, 0.15));
}

.balance-sub { font-size: 13px; color: rgba(255, 255, 255, 0.5); display: flex; align-items: center; gap: 10px; margin-top: 4px;}
.balance-sub b { font-weight: 700; color: #fde08b; font-size: 15px;}
.balance-sub :deep(.el-divider--vertical) { height: 16px; border-color: rgba(255, 255, 255, 0.15); }

.balance-actions {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}
.balance-actions .el-button {
  min-width: 120px;
  border-radius: 20px;
  font-weight: 600;
  transition: all 0.3s ease;
  height: 38px;
}

/* 去积分商店（主行动点：哑光金底黑字） */
.balance-actions .el-button--default {
  background: linear-gradient(135deg, #d4af37 0%, #fde08b 100%);
  border: none;
  color: #0f172a;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}
.balance-actions .el-button--default:hover {
  background: #fde08b;
  box-shadow: 0 6px 16px rgba(212, 175, 55, 0.3);
  transform: translateY(-2px);
}

/* 积分明细（次行动点：金丝线框） */
.balance-actions .el-button--primary {
  background: rgba(212, 175, 55, 0.05);
  border: 1px solid rgba(212, 175, 55, 0.4);
  color: #d4af37;
  box-shadow: none;
}
.balance-actions .el-button--primary:hover {
  background: rgba(212, 175, 55, 0.15);
  border-color: #fde08b;
  color: #fde08b;
  transform: translateY(-2px);
}

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
.fav-card-clickable { cursor: pointer; }
.fav-card-clickable:hover { border-color: #409eff; }

/* ── Tab 7：我的观察 ── */
.obs-header { display: flex; justify-content: flex-end; margin-bottom: 20px; }
.obs-list { display: flex; flex-direction: column; gap: 16px; }
.obs-item {
  display: flex; gap: 20px;
  padding: 16px;
}
.obs-image { width: 160px; height: 110px; border-radius: 10px; overflow: hidden; flex-shrink: 0; background: #f2f3f5; cursor: pointer; }
.obs-image img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s;}
.obs-no-image { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #86909c; font-size: 13px; gap: 4px; background: #f2f3f5; }
.obs-item:hover .obs-image img { transform: scale(1.05);}
.obs-content { flex: 1; display: flex; flex-direction: column; justify-content: space-between; padding: 4px 0;}
.obs-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.obs-title { font-size: 16px; font-weight: 700; color: #1d2129; cursor: pointer; transition: color 0.2s; flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.obs-title:hover { color: #165dff; }
.obs-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.obs-actions .el-button { font-size: 12px; }

/* 删除确认气泡弹窗按钮对齐优化 */
:global(.obs-list .el-popconfirm) { --el-popconfirm-padding: 14px 16px 10px; }
:global(.obs-list .el-popconfirm__action) {
  display: flex !important;
  justify-content: center;
  gap: 8px;
  margin-top: 6px !important;
}
:global(.obs-list .el-popconfirm__action .el-button) {
  padding: 5px 16px !important;
}
.obs-meta { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #86909c; margin-bottom: 12px; }
.obs-meta .el-divider--vertical { height: 12px; margin: 0 6px; border-color: rgba(0,0,0,0.1);}
.obs-tags { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.obs-reject-reason {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.08);
  padding: 2px 10px;
  border-radius: 6px;
  line-height: 1.5;
  max-width: 100%;
  word-break: break-word;
}

/* ── 观察详情弹窗 ── */
.obs-detail-dialog :deep(.el-dialog__body) { padding: 16px 24px 8px; }
.obs-detail-body { min-height: 80px; }
.detail-image { width: 100%; max-height: 320px; border-radius: 12px; overflow: hidden; margin-bottom: 20px; background: #f2f3f5; }
.detail-image img { width: 100%; height: auto; max-height: 320px; object-fit: cover; display: block; }
.detail-no-image { width: 100%; height: 160px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #c0c4cc; font-size: 14px; gap: 8px; background: #fafafa; border-radius: 12px; margin-bottom: 20px; }
.detail-desc :deep(.el-descriptions__label) { width: 90px; font-weight: 600; color: #606266; }

/* ── 观察编辑弹窗 ── */
.obs-edit-dialog :deep(.el-dialog__body) { padding: 16px 24px 8px; }

/* 编辑弹窗照片区域 */
.edit-photo-area { width: 100%; }
.current-photo-preview {
  position: relative;
  width: 100%;
  max-width: 280px;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid #e4e7ed;
}
.current-photo-preview img {
  width: 100%;
  height: auto;
  display: block;
  max-height: 200px;
  object-fit: cover;
}
.current-photo-preview .photo-mask {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  gap: 8px;
}
.current-photo-preview:hover .photo-mask { opacity: 1; }
.edit-photo-upload { width: 100%; }
.edit-photo-upload :deep(.el-upload) {
  width: 100%;
  max-width: 280px;
}
.upload-trigger {
  width: 160px;
  height: 120px;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #86909c;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fafafa;
}
.upload-trigger:hover {
  border-color: #165dff;
  color: #165dff;
  background: rgba(22, 93, 255, 0.04);
}
.upload-trigger span {
  font-size: 13px;
  font-weight: 500;
}

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

/* ═══ 头像框样式 ═══ */
.avatar-frame-selector {
  text-align: center;
  margin-top: 4px;
}

.avatar-frame-box {
  display: inline-flex;
  border-radius: 50%;
  padding: 5px;
  transition: all 0.3s ease;
}
.avatar-frame-box .user-avatar {
  display: block;
  border-radius: 50%;
}

/* 默认边框 */
.frame-default {
  background: #dcdfe6;
}

/* 黄金边框 */
.frame-gold {
  background: linear-gradient(135deg, #f6d365, #fda085);
  box-shadow: 0 0 14px rgba(246, 211, 101, 0.6);
}
.frame-gold .user-avatar { border: 3px solid #fff; border-radius: 50%; }

/* 深海边框 */
.frame-ocean {
  background: linear-gradient(135deg, #00d2ff, #165dff);
  box-shadow: 0 0 16px rgba(0, 210, 255, 0.6);
}
.frame-ocean .user-avatar { border: 3px solid #fff; border-radius: 50%; }

/* 彩虹边框 */
.frame-rainbow {
  background: linear-gradient(90deg, #ff6b6b, #feca57, #48dbfb, #ff9ff3);
  background-size: 200% 100%;
  animation: rainbow-spin 3s linear infinite;
}
.frame-rainbow .user-avatar { border: 3px solid #fff; border-radius: 50%; }
@keyframes rainbow-spin {
  0% { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}

/* 火焰边框 */
.frame-flame {
  background: linear-gradient(135deg, #ff4500, #ff8c00, #ffd700);
  box-shadow: 0 0 18px rgba(255, 69, 0, 0.6);
}
.frame-flame .user-avatar { border: 3px solid #fff; border-radius: 50%; }

/* ═══ 头像框选择弹窗 — 卡片式布局 ═══ */
.frame-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}

.frame-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding: 12px 8px;
}
.dialog-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}
.glass-cancel-btn {
  background: rgba(0, 0, 0, 0.04);
  border: none;
  color: #4e5969;
  font-weight: 600;
  border-radius: 12px;
  min-width: 90px;
  height: 38px;
  transition: all 0.3s;
}
.glass-cancel-btn:hover {
  background: rgba(0, 0, 0, 0.08);
  color: #1d2129;
}
.submit-btn.mini {
  height: 38px;
  min-width: 100px;
  border-radius: 12px;
}

.frame-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 6px 12px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 2px solid transparent;
  background: rgba(255, 255, 255, 0.4);
  position: relative;
}
.frame-card:hover {
  border-color: rgba(64, 158, 255, 0.3);
  background: rgba(64, 158, 255, 0.05);
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
}

.frame-card-active {
  border-color: #409eff !important;
  background: rgba(64, 158, 255, 0.08) !important;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.15), 0 6px 20px rgba(64, 158, 255, 0.1) !important;
}

.frame-card-locked {
  opacity: 0.45;
  cursor: not-allowed;
  filter: grayscale(0.6);
}
.frame-card-locked:hover {
  border-color: transparent !important;
  background: rgba(255, 255, 255, 0.4) !important;
  transform: none !important;
  box-shadow: none !important;
}

.frame-card-preview {
  position: relative;
  display: inline-flex;
}

.frame-card-ring {
  display: inline-flex;
  border-radius: 50%;
  padding: 4px;
}
.frame-card-ring .user-avatar {
  border-radius: 50%;
}

.frame-card-lock {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.35);
  border-radius: 50%;
  color: #fff;
}

.frame-card-badge {
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  padding: 0 8px;
  height: 18px;
  line-height: 18px;
  border-radius: 9px;
  background: rgba(0, 180, 42, 0.85);
  color: #fff;
  white-space: nowrap;
  font-weight: 600;
  backdrop-filter: blur(4px);
}

.frame-card-check {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.4);
}

.frame-card-name {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
  text-align: center;
  line-height: 1.3;
}

@media (max-width: 540px) {
  .frame-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* ═══ 新头像框 ═══ */

/* 烈焰虚线 — 橙红渐变 + 脉冲光晕 */
.frame-dashed {
  background: repeating-conic-gradient(
    #fd7000 0deg 18deg,
    transparent 18deg 36deg
  );
  box-shadow: 0 0 14px rgba(237, 35, 76, 0.5);
  animation: frame-dash-glow 2s ease-in-out infinite;
}
.frame-dashed .user-avatar { border: 3px solid #fff; border-radius: 50%; }
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
.frame-neon .user-avatar { border: 3px solid rgba(255,255,255,0.9); border-radius: 50%; }
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
.frame-aurora .user-avatar { border: 3px solid rgba(255,255,255,0.9); border-radius: 50%; }
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
.frame-crystal .user-avatar {
  border: 3px solid rgba(255, 255, 255, 0.9);
  border-radius: 50%;
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
.frame-royal .user-avatar { border: 3px solid #fff; border-radius: 50%; }

/* ═══ 称号选择弹窗 ═══ */
.title-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  padding: 8px 4px;
}
.title-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 2px solid transparent;
  background: rgba(255,255,255,0.5);
}
.title-card:hover {
  border-color: rgba(64,158,255,0.3);
  background: rgba(64,158,255,0.05);
  transform: translateY(-2px);
}
.title-card-active {
  border-color: #409eff !important;
  background: rgba(64,158,255,0.08) !important;
  box-shadow: 0 0 0 3px rgba(64,158,255,0.15) !important;
}
.title-card-icon {
  font-size: 28px;
  line-height: 1;
}
.title-card-name {
  font-size: 13px;
  color: #4e5969;
  font-weight: 500;
  text-align: center;
}
.title-card-need {
  font-size: 10px;
  color: #999;
  text-align: center;
  margin-top: 2px;
}
.title-card-locked {
  opacity: 0.5;
  cursor: not-allowed;
  filter: grayscale(0.5);
}
.title-card-locked:hover {
  border-color: transparent !important;
  background: rgba(255,255,255,0.5) !important;
  transform: none !important;
  box-shadow: none !important;
}
.title-custom {
  border-style: dashed;
  border-color: #dcdfe6;
}
.custom-title-input {
  padding: 0 8px 4px;
}
</style>
<style>
/* ════════ 头像点击后的下拉菜单 (全局玻璃化) ════════ */
.avatar-glass-popper {
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(24px) saturate(150%) !important;
  -webkit-backdrop-filter: blur(24px) saturate(150%) !important;
  border: 1px solid rgba(255, 255, 255, 1) !important;
  border-radius: 16px !important;
  padding: 8px !important;
  box-shadow: 0 16px 32px rgba(0, 50, 150, 0.1) !important;
}

.avatar-glass-popper .el-dropdown-menu {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

.avatar-glass-popper .el-dropdown-menu__item {
  border-radius: 10px;
  padding: 10px 16px;
  font-size: 14px;
  color: #4e5969 !important;
  font-weight: 500;
  transition: all 0.3s ease;
}

.avatar-glass-popper .el-dropdown-menu__item:hover {
  background: rgba(22, 93, 255, 0.08) !important;
  color: #165dff !important;
  transform: translateX(4px);
}

.avatar-glass-popper .el-popper__arrow::before {
  background: rgba(255, 255, 255, 0.95) !important;
  border: 1px solid rgba(255, 255, 255, 1) !important;
}

/* ════════ 头像框选择弹窗 (全局玻璃化) ════════ */
.glass-dialog {
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(30px) saturate(150%) !important;
  -webkit-backdrop-filter: blur(30px) saturate(150%) !important;
  border: 1px solid rgba(255, 255, 255, 1) !important;
  border-radius: 24px !important;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.1) !important;
}
.glass-dialog .el-dialog__header {
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  margin-right: 0;
  padding: 20px 24px;
}
.glass-dialog .el-dialog__title {
  font-weight: 700;
  font-size: 17px;
  color: #1d2129;
}
.glass-dialog .el-dialog__body {
  padding: 20px 24px;
}
.glass-dialog .el-dialog__footer {
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  padding: 16px 24px;
}
</style>
