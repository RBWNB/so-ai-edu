<template>
  <div class="edu-quiz">
    <!-- ============ 开始界面 ============ -->
    <template v-if="compStage === 'NOT_STARTED' && phase === 'start'">
      <div class="start-screen">
        <div class="start-icon">🏆</div>
        <h2>答题闯关</h2>
        <p class="subtitle">通过趣味答题巩固海洋生物知识，每次随机抽取 30 道题</p>
        <div class="start-info">
          <div class="info-item">
            <span class="info-icon">📝</span>
            <span>单选 / 多选 / 判断</span>
          </div>
          <div class="info-item">
            <span class="info-icon">🔊</span>
            <span>支持语音朗读题目</span>
          </div>
          <div class="info-item">
            <span class="info-icon">📊</span>
            <span>实时评分 + 详细解析</span>
          </div>
        </div>

        <!-- 模式切换 Tab -->
        <div class="mode-tabs">
          <button
            class="mode-tab"
            :class="{ active: gameMode === 'normal' }"
            @click="switchMode('normal')"
          >
            <span class="mode-icon">📚</span>
            <span class="mode-label">休闲模式</span>
            <span class="mode-desc">无时间限制，从容作答</span>
          </button>
          <button
            class="mode-tab"
            :class="{ active: gameMode === 'competition' }"
            @click="switchMode('competition')"
          >
            <span class="mode-icon">🏆</span>
            <span class="mode-label">竞技模式</span>
            <span class="mode-desc">10秒极限挑战，全国排名</span>
          </button>
        </div>

        <!-- 排行榜入口 -->
        <div class="leaderboard-entry">
          <button class="lb-btn" @click="openLeaderboard">
            <span class="lb-btn-icon">🏆</span>
            <span class="lb-btn-text">排行榜</span>
          </button>
        </div>

        <!-- 竞技模式提示 -->
        <div v-if="gameMode === 'competition'" class="comp-tip">
          <span class="tip-icon">⚡</span>
          每题限时 10 秒，超时自动判错，共 10 题
        </div>

        <el-button
          type="primary"
          size="large"
          class="start-btn"
          :class="{ 'comp-start-btn': gameMode === 'competition' }"
          :loading="gameMode === 'normal' ? startLoading : compStartLoading"
          @click="gameMode === 'competition' ? startCompetition() : handleStart()"
        >
          {{ gameMode === 'competition' ? '⚡ 开始竞技' : '开始答题' }}
        </el-button>

        <!-- 历史成绩（休闲模式） -->
        <div v-if="history.length > 0 && gameMode === 'normal'" class="history-section">
          <h3>📈 答题记录</h3>
          <div class="history-list">
            <div v-for="(h, i) in history" :key="i" class="history-item">
              <span class="history-time">{{ h.time }}</span>
              <span>答对 <b>{{ h.correct }}</b>/{{ h.total }}</span>
              <el-tag :type="h.score >= 80 ? 'success' : h.score >= 60 ? 'warning' : 'danger'" size="small">
                {{ h.score }} 分
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 竞技历史（竞技模式） -->
        <div v-if="compHistory.length > 0 && gameMode === 'competition'" class="history-section">
          <h3>🏆 竞技记录</h3>
          <div class="history-list">
            <div v-for="(h, i) in compHistory" :key="i" class="history-item">
              <span class="history-time">{{ h.time }}</span>
              <span>正确率 <b>{{ h.accuracy }}%</b></span>
              <el-tag :type="h.accuracy >= 80 ? 'success' : h.accuracy >= 60 ? 'warning' : 'danger'" size="small">
                {{ h.tier }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ============ 普通答题界面 ============ -->
    <template v-if="phase === 'answering' && compStage === 'NOT_STARTED'">
      <div class="answer-screen">
        <!-- 进度条 -->
        <div class="progress-bar">
          <div class="progress-info">
            <span class="progress-text">{{ currentIndex + 1 }} / {{ questions.length }}</span>
            <span class="progress-percent">{{ Math.round((currentIndex + 1) / questions.length * 100) }}%</span>
          </div>
          <el-progress :percentage="Math.round((currentIndex + 1) / questions.length * 100)" :stroke-width="8" />
        </div>

        <!-- 题目卡片 -->
        <div class="question-card" v-if="currentQuestion">
          <div class="q-header">
            <el-tag :type="typeTag(currentQuestion.questionType)" size="small" effect="dark">
              {{ typeLabel(currentQuestion.questionType) }}
            </el-tag>
            <el-tag
              :type="currentQuestion.difficulty === 'easy' ? 'success' : currentQuestion.difficulty === 'hard' ? 'danger' : 'warning'"
              effect="plain"
              size="small"
            >
              {{ diffLabel(currentQuestion.difficulty) }}
            </el-tag>
            <el-button
              class="tts-btn"
              size="small"
              circle
              :loading="currentQuestion._ttsLoading"
              @click="playTts(currentQuestion)"
            >
              🔊
            </el-button>
          </div>

          <div class="q-stem">{{ currentQuestion.stem }}</div>

          <!-- 单选题 -->
          <template v-if="currentQuestion.questionType === 'single'">
            <div class="options-list">
              <div
                v-for="opt in parsedOptions"
                :key="opt.label"
                class="option-item"
                :class="{ selected: userAnswers[currentIndex] === opt.label }"
                @click="selectAnswer(opt.label)"
              >
                <span class="indicator radio-indicator" :class="{ active: userAnswers[currentIndex] === opt.label }">
                  <span v-if="userAnswers[currentIndex] === opt.label" class="indicator-dot" />
                </span>
                <span class="opt-label">{{ opt.label }}.</span>
                <span class="opt-text">{{ opt.text }}</span>
              </div>
            </div>
          </template>

          <!-- 多选题 -->
          <template v-if="currentQuestion.questionType === 'multiple'">
            <div class="options-list">
              <div
                v-for="opt in parsedOptions"
                :key="opt.label"
                class="option-item"
                :class="{ selected: selectedMultiple.includes(opt.label) }"
                @click="toggleMultiple(opt.label)"
              >
                <span class="indicator checkbox-indicator" :class="{ active: selectedMultiple.includes(opt.label) }">
                  <span v-if="selectedMultiple.includes(opt.label)" class="indicator-check">✓</span>
                </span>
                <span class="opt-label">{{ opt.label }}.</span>
                <span class="opt-text">{{ opt.text }}</span>
              </div>
            </div>
          </template>

          <!-- 判断题 -->
          <template v-if="currentQuestion.questionType === 'judge'">
            <div class="options-list judge-options">
              <div
                class="option-item"
                :class="{ selected: userAnswers[currentIndex] === '正确' }"
                @click="selectAnswer('正确')"
              >
                <span class="indicator radio-indicator" :class="{ active: userAnswers[currentIndex] === '正确' }">
                  <span v-if="userAnswers[currentIndex] === '正确'" class="indicator-dot" />
                </span>
                <span class="opt-text">正确</span>
              </div>
              <div
                class="option-item"
                :class="{ selected: userAnswers[currentIndex] === '错误' }"
                @click="selectAnswer('错误')"
              >
                <span class="indicator radio-indicator" :class="{ active: userAnswers[currentIndex] === '错误' }">
                  <span v-if="userAnswers[currentIndex] === '错误'" class="indicator-dot" />
                </span>
                <span class="opt-text">错误</span>
              </div>
            </div>
          </template>
        </div>

        <!-- 底部导航 -->
        <div class="nav-bar">
          <el-button :disabled="currentIndex === 0" @click="prevQuestion">上一题</el-button>

          <div class="nav-center">
            <el-tag v-if="isAnswered" type="success" effect="plain" size="small">✓ 已作答</el-tag>
            <el-tag v-else type="info" effect="plain" size="small">待作答</el-tag>
          </div>

          <el-button
            v-if="currentIndex < questions.length - 1"
            type="primary"
            @click="nextQuestion"
          >下一题</el-button>
          <el-button
            v-else
            type="success"
            :disabled="remainingCount > 0"
            @click="handleSubmit"
          >
            提交（{{ remainingCount > 0 ? '还剩' + remainingCount + '题' : '交卷' }}）
          </el-button>
        </div>

        <!-- 答题卡 -->
        <div class="answer-sheet">
          <span class="sheet-title">答题卡</span>
          <div class="sheet-grid">
            <div
              v-for="(_, idx) in questions"
              :key="idx"
              class="sheet-item"
              :class="{
                current: idx === currentIndex,
                answered: userAnswers[idx] !== null && userAnswers[idx] !== undefined && userAnswers[idx] !== '',
                unanswered: (userAnswers[idx] === null || userAnswers[idx] === undefined || userAnswers[idx] === '') && idx !== currentIndex
              }"
              @click="goToQuestion(idx)"
            >
              {{ idx + 1 }}
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ============ 普通结果界面 ============ -->
    <template v-if="phase === 'result' && compStage === 'NOT_STARTED'">
      <div class="result-screen">
        <div class="result-header">
          <div class="score-circle" :class="scoreClass">
            <span class="score-number">{{ score }}</span>
            <span class="score-label">分</span>
          </div>
          <h2>{{ scoreMessage }}</h2>
          <p class="score-summary">答对 {{ correctCount }} / {{ totalCount }} 题</p>
        </div>

        <el-button type="primary" size="large" class="retry-btn" @click="resetExam">
          再来一次
        </el-button>

        <!-- 详细解析 -->
        <div class="detail-section">
          <h3>📝 题目解析</h3>
          <div v-for="(d, idx) in resultDetails" :key="idx" class="detail-card" :class="{ wrong: !d.correct }">
            <div class="detail-header">
              <span class="detail-num">{{ idx + 1 }}.</span>
              <el-tag :type="typeTag(d.questionType || 'single')" size="small">
                {{ typeLabel(d.questionType || 'single') }}
              </el-tag>
              <el-tag :type="d.correct ? 'success' : 'danger'" size="small" effect="dark">
                {{ d.correct ? '✓ 正确' : '✗ 错误' }}
              </el-tag>
            </div>
            <div class="detail-stem">{{ d.stem }}</div>
            <div v-if="d.optionsJson" class="detail-options">
              <div v-for="opt in parseOptions(d.optionsJson)" :key="opt.label" class="detail-option">
                {{ opt.label }}. {{ opt.text }}
              </div>
            </div>
            <div class="detail-answers">
              <div>你的答案：<el-tag :type="d.correct ? 'success' : 'danger'" size="small">{{ d.userAnswer || '未作答' }}</el-tag></div>
              <div v-if="!d.correct">正确答案：<el-tag type="success" size="small">{{ d.correctAnswer }}</el-tag></div>
            </div>
            <div v-if="d.explanation" class="detail-explanation">
              <span class="exp-label">💡 解析：</span>{{ d.explanation }}
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ═══════════════════════════════════════════════════ -->
    <!-- 竞技模式：答题中                                    -->
    <!-- ═══════════════════════════════════════════════════ -->
    <template v-if="compStage === 'PLAYING'">
      <div class="competition-playing">
        <!-- 顶部状态栏 -->
        <div class="comp-top-bar">
          <div class="comp-progress-info">
            <span class="comp-progress-num">{{ compCurrentIndex + 1 }}</span>
            <span class="comp-progress-sep">/</span>
            <span class="comp-progress-total">{{ compQuestions.length }}</span>
          </div>

          <!-- 倒计时 -->
          <div class="comp-countdown" :class="countdownClass">
            <svg class="countdown-ring" viewBox="0 0 56 56">
              <circle cx="28" cy="28" r="24" fill="none" stroke="rgba(0,0,0,0.06)" stroke-width="4" />
              <circle
                cx="28" cy="28" r="24"
                fill="none"
                :stroke="countdownColor"
                stroke-width="4"
                stroke-linecap="round"
                :stroke-dasharray="countdownDashArray"
                :stroke-dashoffset="countdownDashOffset"
                transform="rotate(-90 28 28)"
                class="countdown-arc"
              />
            </svg>
            <span class="countdown-number">{{ countdown }}</span>
            <span class="countdown-unit">秒</span>
          </div>
        </div>

        <!-- 进度条 -->
        <div class="comp-progress-bar">
          <el-progress
            :percentage="Math.round((compCurrentIndex + 1) / compQuestions.length * 100)"
            :stroke-width="6"
          />
        </div>

        <!-- 题目卡片（复用现有结构 + 竞技反馈） -->
        <div class="question-card comp-question-card" v-if="compCurrentQ">
          <div class="q-header">
            <el-tag :type="typeTag(compCurrentQ.questionType)" size="small" effect="dark">
              {{ typeLabel(compCurrentQ.questionType) }}
            </el-tag>
            <el-tag
              :type="compCurrentQ.difficulty === 'easy' ? 'success' : compCurrentQ.difficulty === 'hard' ? 'danger' : 'warning'"
              effect="plain"
              size="small"
            >
              {{ diffLabel(compCurrentQ.difficulty) }}
            </el-tag>
          </div>

          <div class="q-stem">{{ compCurrentQ.stem }}</div>

          <!-- 竞技选项（带对错反馈） -->
          <div class="options-list">
            <div
              v-for="opt in compParsedOptions"
              :key="opt.label"
              class="option-item comp-option"
              :class="{
                selected: compSelectedAnswer === opt.label && !compFeedback,
                correct: compFeedback && opt.label === compCurrentQ.correctAnswer,
                wrong: compFeedback && compSelectedAnswer === opt.label && opt.label !== compCurrentQ.correctAnswer,
                dimmed: compFeedback && opt.label !== compCurrentQ.correctAnswer && opt.label !== compSelectedAnswer,
              }"
              @click="compSelectAnswer(opt.label)"
            >
              <span
                class="indicator radio-indicator"
                :class="{
                  active: compSelectedAnswer === opt.label && !compFeedback,
                  'indicator-correct': compFeedback && opt.label === compCurrentQ.correctAnswer,
                  'indicator-wrong': compFeedback && compSelectedAnswer === opt.label && opt.label !== compCurrentQ.correctAnswer,
                }"
              >
                <span v-if="compFeedback && opt.label === compCurrentQ.correctAnswer" class="indicator-check">✓</span>
                <span v-else-if="compFeedback && compSelectedAnswer === opt.label && opt.label !== compCurrentQ.correctAnswer" class="indicator-cross">✗</span>
                <span v-else-if="compSelectedAnswer === opt.label && !compFeedback" class="indicator-dot" />
              </span>
              <span class="opt-label">{{ opt.label }}.</span>
              <span class="opt-text">{{ opt.text }}</span>
            </div>
          </div>

          <!-- 超时提示 -->
          <div v-if="compTimeoutFlag" class="comp-timeout-tip">
            <span>⏰ 时间到！正确答案：<b>{{ compCurrentQ.correctAnswer }}</b></span>
          </div>
        </div>
      </div>
    </template>

    <!-- ═══════════════════════════════════════════════════ -->
    <!-- 竞技模式：结算结果                                  -->
    <!-- ═══════════════════════════════════════════════════ -->
    <template v-if="compStage === 'RESULT'">
      <div class="competition-result">
        <!-- 段位徽章 -->
        <div class="comp-result-header">
          <div class="comp-tier-badge" :class="'tier-' + compRankTier">
            <span class="tier-icon">{{ tierIcon }}</span>
            <span class="tier-name">{{ compRankTier }}</span>
          </div>
          <h2 class="comp-result-title">竞技结算</h2>
          <p class="comp-result-subtitle">Rank #{{ compRank }} · 全国排名</p>
        </div>

        <!-- 5 维数据卡片 -->
        <div class="comp-stats-grid">
          <!-- 正确率（环形图） -->
          <div class="comp-stat-card stat-accuracy">
            <svg class="accuracy-ring" viewBox="0 0 120 120">
              <defs>
                <linearGradient id="ringGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" stop-color="#165dff" />
                  <stop offset="100%" stop-color="#00d2ff" />
                </linearGradient>
              </defs>
              <circle cx="60" cy="60" r="50" fill="none" stroke="#eef0f5" stroke-width="10" />
              <circle
                cx="60" cy="60" r="50"
                fill="none"
                stroke="url(#ringGrad)"
                stroke-width="10"
                stroke-linecap="round"
                :stroke-dasharray="314.159"
                :stroke-dashoffset="314.159 * (1 - compAccuracy / 100)"
                transform="rotate(-90 60 60)"
                class="accuracy-arc"
              />
              <text x="60" y="56" text-anchor="middle" class="accuracy-value">{{ compAccuracy }}%</text>
              <text x="60" y="76" text-anchor="middle" class="accuracy-label">正确率</text>
            </svg>
          </div>

          <div class="comp-stat-card">
            <span class="stat-value">{{ compRank }}</span>
            <span class="stat-label">当前排名</span>
          </div>
          <div class="comp-stat-card">
            <span class="stat-value">{{ compParticipationCount }}</span>
            <span class="stat-label">参赛次数</span>
          </div>
          <div class="comp-stat-card">
            <span class="stat-value">{{ compTotalAnsweredCount }}</span>
            <span class="stat-label">总答题量</span>
          </div>
          <div class="comp-stat-card">
            <span class="stat-value">{{ compAvgTime }}s</span>
            <span class="stat-label">平均耗时/题</span>
          </div>
        </div>

        <!-- 本轮详情 -->
        <div class="comp-round-detail">
          <div class="round-summary">
            <span>正确 <b>{{ compCorrectCount }}</b> / {{ compTotalCount }} 题</span>
            <span class="round-divider">|</span>
            <span>总耗时 <b>{{ compTotalTime }}s</b></span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="comp-actions">
          <el-button type="primary" size="large" class="retry-btn" :loading="compStartLoading" @click="startCompetition">
            ⚡ 再来一局
          </el-button>
          <el-button size="large" class="back-btn" @click="backToStart">
            ← 返回首页
          </el-button>
        </div>

        <!-- 错题回顾 -->
        <div v-if="compWrongDetails.length > 0" class="detail-section">
          <h3>📝 错题回顾</h3>
          <div v-for="(d, idx) in compWrongDetails" :key="idx" class="detail-card wrong">
            <div class="detail-header">
              <span class="detail-num">{{ idx + 1 }}.</span>
              <el-tag :type="typeTag(d.questionType || 'single')" size="small">
                {{ typeLabel(d.questionType || 'single') }}
              </el-tag>
              <el-tag type="danger" size="small" effect="dark">✗ 错误</el-tag>
            </div>
            <div class="detail-stem">{{ d.stem }}</div>
            <div v-if="d.optionsJson" class="detail-options">
              <div
                v-for="opt in parseOptions(d.optionsJson)"
                :key="opt.label"
                class="detail-option"
                :class="{
                  'detail-opt-correct': opt.label === d.correctAnswer,
                  'detail-opt-wrong': opt.label === d.userAnswer && opt.label !== d.correctAnswer,
                }"
              >
                {{ opt.label }}. {{ opt.text }}
              </div>
            </div>
            <div class="detail-answers">
              <div>你的答案：<el-tag type="danger" size="small">{{ d.userAnswer || '超时未答' }}</el-tag></div>
              <div>正确答案：<el-tag type="success" size="small">{{ d.correctAnswer }}</el-tag></div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ═══════════════════════════════════════════════════ -->
    <!-- 排行榜弹窗                                         -->
    <!-- ═══════════════════════════════════════════════════ -->
    <el-dialog
      v-model="lbVisible"
      title=""
      :width="lbDialogWidth"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
      :destroy-on-close="false"
      class="leaderboard-dialog"
      @closed="onLbClosed"
    >
      <div class="lb-container">
        <!-- 头部 -->
        <div class="lb-header">
          <div class="lb-title-row">
            <span class="lb-title-icon">🏆</span>
            <span class="lb-title-text">竞技排行榜</span>
          </div>
          <span class="lb-subtitle">综合实力排名 TOP 50</span>
        </div>

        <!-- Loading -->
        <div v-if="lbLoading" class="lb-loading">
          <div v-for="i in 5" :key="i" class="lb-skel-row">
            <span class="lb-skel-rank" />
            <span class="lb-skel-avatar" />
            <span class="lb-skel-name" />
            <span class="lb-skel-stat" />
          </div>
        </div>

        <!-- 错误 -->
        <el-empty
          v-else-if="lbError"
          description="加载失败"
          :image-size="80"
        >
          <el-button size="small" @click="fetchLeaderboard">重试</el-button>
        </el-empty>

        <!-- 列表 -->
        <div v-else class="lb-list" ref="lbListRef">
          <div
            v-for="item in lbData"
            :key="item.userId"
            class="lb-row"
            :class="{
              'lb-row-me': item.isMe,
              'lb-row-top1': item.rank === 1,
              'lb-row-top2': item.rank === 2,
              'lb-row-top3': item.rank === 3,
            }"
          >
            <!-- 排名 -->
            <span class="lb-rank" :class="'lb-rank-' + item.rank">
              <template v-if="item.rank === 1">🥇</template>
              <template v-else-if="item.rank === 2">🥈</template>
              <template v-else-if="item.rank === 3">🥉</template>
              <template v-else>{{ item.rank }}</template>
            </span>

            <!-- 头像 -->
            <div class="lb-avatar-frame" :class="'frame-' + (item.avatarFrame || 'default')">
              <el-avatar :size="36" :src="formatAvatarUrl(item.avatarUrl)">
                <el-icon :size="18"><User /></el-icon>
              </el-avatar>
            </div>

            <!-- 用户名 -->
            <span class="lb-username">{{ item.username }}</span>
            <span v-if="item.isMe" class="lb-me-tag">我</span>

            <!-- 右侧统计 -->
            <span class="lb-stat lb-stat-matches">{{ item.totalMatches }}场</span>
            <span class="lb-stat lb-stat-accuracy">{{ item.averageAccuracy }}%</span>
            <span class="lb-tier-badge-sm" :class="'tier-sm-' + item.bestTier">{{ item.bestTier }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
  <LevelUpPopup ref="levelUpRef" />
</template>

<script setup>
import { ref, reactive, computed, nextTick, onBeforeUnmount, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { startExam, submitExam, getExamHistory, examTts } from '@/api/exam'
import { getLearningProfile } from '@/api/learning'
import { submitCompetitionResult, getLeaderboard } from '@/api/competition'
import LevelUpPopup from "@/components/LevelUpPopup.vue";

// ==================== 模式切换 ====================
const gameMode = ref('normal') // 'normal' | 'competition'

const switchMode = (mode) => {
  gameMode.value = mode
}

// ==================== 普通模式状态 ====================
const phase = ref('start')
const startLoading = ref(false)
const submitLoading = ref(false)
const levelUpRef = ref(null)

const questions = ref([])
const currentIndex = ref(0)
const userAnswers = ref([])
const selectedMultiple = ref([])

const score = ref(0)
const correctCount = ref(0)
const totalCount = ref(0)
const resultDetails = ref([])
const history = ref([])

// ==================== 竞技模式状态 ====================
const compStage = ref('NOT_STARTED') // 'NOT_STARTED' | 'PLAYING' | 'RESULT'
const compStartLoading = ref(false)
const compQuestions = ref([])
const compCurrentIndex = ref(0)
const compUserAnswers = ref([]) // { answer, correct, timeMs } per question
const compQuestionStartTime = ref(0)
const countdown = ref(10)
const compFeedback = ref(false)     // 是否正在展示对错反馈
const compSelectedAnswer = ref('')  // 当前题用户选择的答案
const compTimeoutFlag = ref(false)  // 是否因超时展示提示

// 结算数据
const compCorrectCount = ref(0)
const compTotalCount = ref(0)
const compAccuracy = ref(0)
const compRank = ref(0)
const compRankTier = ref('')
const compParticipationCount = ref(0)
const compTotalAnsweredCount = ref(0)
const compAvgTime = ref(0)
const compTotalTime = ref(0)
const compWrongDetails = ref([])
const compHistory = ref([]) // 竞技历史记录（localStorage）

// 排行榜状态
const lbVisible = ref(false)
const lbLoading = ref(false)
const lbError = ref(false)
const lbData = ref([])
const lbDialogWidth = ref('680px')

let countdownTimer = null
let autoAdvanceTimer = null

// ==================== 计算属性（普通模式） ====================
const currentQuestion = computed(() => {
  return questions.value[currentIndex.value] || null
})

const parsedOptions = computed(() => {
  const q = currentQuestion.value
  if (!q || !q.optionsJson) return []
  return parseOptions(q.optionsJson)
})

const isAnswered = computed(() => {
  const ans = userAnswers.value[currentIndex.value]
  return ans !== null && ans !== undefined && ans !== ''
})

const remainingCount = computed(() => {
  let count = 0
  for (let i = 0; i < questions.value.length; i++) {
    const ans = userAnswers.value[i]
    if (ans === null || ans === undefined || ans === '') count++
  }
  return count
})

const scoreClass = computed(() => {
  if (score.value >= 80) return 'score-excellent'
  if (score.value >= 60) return 'score-good'
  return 'score-poor'
})

const scoreMessage = computed(() => {
  if (score.value >= 90) return '🌟 太棒了！你是海洋知识达人！'
  if (score.value >= 80) return '👏 非常优秀！继续加油！'
  if (score.value >= 60) return '💪 还不错，再接再厉！'
  return '📚 需要多学习海洋知识哦！'
})

// ==================== 计算属性（竞技模式） ====================
const compCurrentQ = computed(() => {
  return compQuestions.value[compCurrentIndex.value] || null
})

const compParsedOptions = computed(() => {
  const q = compCurrentQ.value
  if (!q || !q.optionsJson) return []
  return parseOptions(q.optionsJson)
})

const countdownDashArray = computed(() => {
  // 2 * PI * 24 ≈ 150.796
  return 150.796
})

const countdownDashOffset = computed(() => {
  // 从满到空：0s → 150.796, 10s → 0
  return 150.796 * (1 - countdown.value / 10)
})

const countdownColor = computed(() => {
  if (countdown.value <= 3) return '#f53f3f'
  if (countdown.value <= 5) return '#ff7d00'
  return '#165dff'
})

const countdownClass = computed(() => {
  if (countdown.value <= 3) return 'countdown-danger'
  if (countdown.value <= 5) return 'countdown-warn'
  return 'countdown-normal'
})

const tierIcon = computed(() => {
  const map = { '王者': '👑', '钻石': '💎', '黄金': '🥇', '白银': '🥈', '青铜': '🥉' }
  return map[compRankTier.value] || '🏅'
})

// ==================== 普通模式：开始答题 ====================
const handleStart = async () => {
  startLoading.value = true
  try {
    const res = await startExam()
    if (res.data.success) {
      const list = res.data.data || []
      if (list.length === 0) {
        ElMessage.warning('暂无题目')
        return
      }
      questions.value = list.map(q => ({ ...q, _ttsLoading: false }))
      userAnswers.value = new Array(list.length).fill(null)
      currentIndex.value = 0
      phase.value = 'answering'
      fetchHistory()
    } else {
      ElMessage.warning(res.data.message || '获取题目失败')
    }
  } catch (err) {
    ElMessage.error('获取题目失败')
  } finally {
    startLoading.value = false
  }
}

// ==================== 普通模式：答案选择 ====================
const selectAnswer = (answer) => {
  userAnswers.value[currentIndex.value] = answer
}

const toggleMultiple = (label) => {
  const idx = selectedMultiple.value.indexOf(label)
  if (idx === -1) {
    selectedMultiple.value.push(label)
  } else {
    selectedMultiple.value.splice(idx, 1)
  }
  userAnswers.value[currentIndex.value] = selectedMultiple.value.join(',')
}

const initMultipleSelection = (index) => {
  const q = questions.value[index]
  if (q && q.questionType === 'multiple') {
    const existing = userAnswers.value[index]
    if (existing) {
      selectedMultiple.value = existing.split(',').map(s => s.trim()).filter(Boolean)
    } else {
      selectedMultiple.value = []
    }
  } else {
    selectedMultiple.value = []
  }
}

// ==================== 普通模式：导航 ====================
const nextQuestion = () => {
  stopAudio()
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
    initMultipleSelection(currentIndex.value)
  }
}

const prevQuestion = () => {
  stopAudio()
  if (currentIndex.value > 0) {
    currentIndex.value--
    initMultipleSelection(currentIndex.value)
  }
}

const goToQuestion = (idx) => {
  stopAudio()
  currentIndex.value = idx
  initMultipleSelection(idx)
}

// ==================== TTS ====================
let currentAudio = null
let currentAudioQuestionId = null

const stopAudio = () => {
  if (currentAudio) {
    currentAudio.pause()
    currentAudio.currentTime = 0
    currentAudio = null
    currentAudioQuestionId = null
  }
}

const playTts = async (q) => {
  if (currentAudio && currentAudioQuestionId === q.id) {
    stopAudio()
    return
  }
  stopAudio()
  q._ttsLoading = true
  try {
    const res = await examTts({
      stem: q.stem || '',
      optionsJson: q.optionsJson || '',
    })
    if (res.data.success) {
      const audio = new Audio(res.data.url)
      currentAudio = audio
      currentAudioQuestionId = q.id
      audio.onended = () => {
        currentAudio = null
        currentAudioQuestionId = null
      }
      audio.play().catch(() => {})
    } else {
      ElMessage.error(res.data.message || '语音合成失败')
    }
  } catch (err) {
    ElMessage.error('语音播放失败')
  } finally {
    q._ttsLoading = false
  }
}

// ==================== 普通模式：提交 ====================
const handleSubmit = async () => {
  const answers = []
  for (let i = 0; i < questions.value.length; i++) {
    const ans = userAnswers.value[i]
    if (ans !== null && ans !== undefined && ans !== '') {
      answers.push({
        questionId: questions.value[i].id,
        userAnswer: ans,
      })
    }
  }

  if (answers.length === 0) {
    ElMessage.warning('请至少回答一道题')
    return
  }

  const oldLevel = await getLearningProfile()
    .then(r => r.data.data?.level ?? null)
    .catch(() => null)

  submitLoading.value = true
  try {
    const res = await submitExam({ answers })
    if (res.data.success) {
      score.value = res.data.score
      correctCount.value = res.data.correctCount
      totalCount.value = res.data.totalCount
      resultDetails.value = (res.data.details || []).map((d, idx) => ({
        ...d,
        stem: questions.value[idx]?.stem || '',
        optionsJson: questions.value[idx]?.optionsJson || '',
        questionType: questions.value[idx]?.questionType || 'single',
      }))

      phase.value = 'result'

      if (oldLevel !== null) {
        try {
          const profileRes = await getLearningProfile()
          const newLevel = profileRes.data.data?.level ?? oldLevel
          if (newLevel > oldLevel) {
            setTimeout(() => {
              levelUpRef.value?.show(newLevel, ['+ 答题经验', '解锁新等级特权'])
            }, 600)
          }
        } catch { /* 静默 */ }
      }
    } else {
      ElMessage.error(res.data.message || '提交失败')
    }
  } catch (err) {
    ElMessage.error('提交失败')
  } finally {
    submitLoading.value = false
  }
}

// ==================== 普通模式：重置 ====================
const resetExam = () => {
  phase.value = 'start'
  questions.value = []
  currentIndex.value = 0
  userAnswers.value = []
  selectedMultiple.value = []
  score.value = 0
  correctCount.value = 0
  totalCount.value = 0
  resultDetails.value = []
  fetchHistory()
}

// ==================== 普通模式：历史 ====================
const fetchHistory = async () => {
  try {
    const res = await getExamHistory()
    if (res.data.success) {
      history.value = res.data.data || []
    }
  } catch {
    // 静默失败
  }
}

// ═══════════════════════════════════════════════════════
// 竞技模式：Mock 数据
// ═══════════════════════════════════════════════════════

const QUESTION_POOL = [
  { id: 1, stem: '世界上最大的海洋哺乳动物是？', questionType: 'single', difficulty: 'easy', optionsJson: JSON.stringify([{ label: 'A', text: '蓝鲸' }, { label: 'B', text: '虎鲸' }, { label: 'C', text: '座头鲸' }, { label: 'D', text: '抹香鲸' }]), correctAnswer: 'A', explanation: '蓝鲸是地球上有史以来最大的动物，体长可达30米，体重超过180吨。' },
  { id: 2, stem: '珊瑚礁被称为海洋中的什么？', questionType: 'single', difficulty: 'easy', optionsJson: JSON.stringify([{ label: 'A', text: '热带草原' }, { label: 'B', text: '热带雨林' }, { label: 'C', text: '沙漠绿洲' }, { label: 'D', text: '高山草甸' }]), correctAnswer: 'B', explanation: '珊瑚礁物种极其丰富，占海洋生物总量的25%以上，被称为"海洋中的热带雨林"。' },
  { id: 3, stem: '海马是由谁来孵化后代的？', questionType: 'single', difficulty: 'normal', optionsJson: JSON.stringify([{ label: 'A', text: '雌性海马' }, { label: 'B', text: '雄性海马' }, { label: 'C', text: '双方轮流' }, { label: 'D', text: '海马不孵化' }]), correctAnswer: 'B', explanation: '雄性海马腹部有育儿袋，雌性将卵产入其中，由雄海马孵化并"分娩"小海马。' },
  { id: 4, stem: '以下哪种生物不属于海洋哺乳动物？', questionType: 'single', difficulty: 'easy', optionsJson: JSON.stringify([{ label: 'A', text: '海豚' }, { label: 'B', text: '鲸鱼' }, { label: 'C', text: '海龟' }, { label: 'D', text: '海豹' }]), correctAnswer: 'C', explanation: '海龟属于爬行动物，卵生并用肺呼吸，但与哺乳动物不同，它们是变温动物。' },
  { id: 5, stem: '世界上最深的海沟是？', questionType: 'single', difficulty: 'easy', optionsJson: JSON.stringify([{ label: 'A', text: '汤加海沟' }, { label: 'B', text: '菲律宾海沟' }, { label: 'C', text: '马里亚纳海沟' }, { label: 'D', text: '日本海沟' }]), correctAnswer: 'C', explanation: '马里亚纳海沟最深处"挑战者深渊"深约11034米，比珠穆朗玛峰的高度还多2000多米。' },
  { id: 6, stem: '章鱼有几条腕足？', questionType: 'single', difficulty: 'easy', optionsJson: JSON.stringify([{ label: 'A', text: '6条' }, { label: 'B', text: '8条' }, { label: 'C', text: '10条' }, { label: 'D', text: '12条' }]), correctAnswer: 'B', explanation: '章鱼有8条腕足，每条腕足上有数百个吸盘，具有独立的神经节，可以自主活动。' },
  { id: 7, stem: '红树林主要生长在？', questionType: 'single', difficulty: 'normal', optionsJson: JSON.stringify([{ label: 'A', text: '深海区域' }, { label: 'B', text: '潮间带' }, { label: 'C', text: '淡水湖泊' }, { label: 'D', text: '高山地区' }]), correctAnswer: 'B', explanation: '红树林生长在热带和亚热带海岸潮间带，具有防风消浪、净化海水等重要生态功能。' },
  { id: 8, stem: '以下哪种鲨鱼是滤食性动物？', questionType: 'single', difficulty: 'normal', optionsJson: JSON.stringify([{ label: 'A', text: '大白鲨' }, { label: 'B', text: '锤头鲨' }, { label: 'C', text: '鲸鲨' }, { label: 'D', text: '虎鲨' }]), correctAnswer: 'C', explanation: '鲸鲨是世界上最大的鱼类，以浮游生物、小鱼和鱼卵为食，属于滤食性动物。' },
  { id: 9, stem: '海洋覆盖地球表面积约？', questionType: 'single', difficulty: 'easy', optionsJson: JSON.stringify([{ label: 'A', text: '50%' }, { label: 'B', text: '60%' }, { label: 'C', text: '71%' }, { label: 'D', text: '85%' }]), correctAnswer: 'C', explanation: '海洋覆盖了地球表面约71%的面积，储存了地球97%的水资源。' },
  { id: 10, stem: '海龟在哪里产卵？', questionType: 'single', difficulty: 'easy', optionsJson: JSON.stringify([{ label: 'A', text: '深海中' }, { label: 'B', text: '沙滩上' }, { label: 'C', text: '珊瑚礁中' }, { label: 'D', text: '红树林里' }]), correctAnswer: 'B', explanation: '海龟夜间爬上沙滩挖掘巢穴产卵，孵化后的小海龟会凭本能爬向大海。' },
  { id: 11, stem: '海洋酸化主要是由于大气中什么增加引起的？', questionType: 'single', difficulty: 'normal', optionsJson: JSON.stringify([{ label: 'A', text: '氧气' }, { label: 'B', text: '氮气' }, { label: 'C', text: '二氧化碳' }, { label: 'D', text: '臭氧' }]), correctAnswer: 'C', explanation: '大气中CO₂浓度升高，海洋吸收后生成碳酸，导致海水pH值下降，影响珊瑚等钙化生物。' },
  { id: 12, stem: '以下哪种不是真正的鱼类？', questionType: 'single', difficulty: 'easy', optionsJson: JSON.stringify([{ label: 'A', text: '海马' }, { label: 'B', text: '鲸鱼' }, { label: 'C', text: '小丑鱼' }, { label: 'D', text: '金枪鱼' }]), correctAnswer: 'B', explanation: '鲸鱼属于哺乳动物（鲸目），用肺呼吸、胎生哺乳，而海马虽然外形奇特但确实是鱼类。' },
  { id: 13, stem: '座头鲸的歌声主要用于？', questionType: 'single', difficulty: 'normal', optionsJson: JSON.stringify([{ label: 'A', text: '回声定位导航' }, { label: 'B', text: '求偶和社交沟通' }, { label: 'C', text: '驱赶捕食者' }, { label: 'D', text: '标记领地' }]), correctAnswer: 'B', explanation: '雄性座头鲸的复杂歌声主要用于繁殖季节的求偶行为，也用于群体间的社交交流。' },
  { id: 14, stem: '海胆属于哪个动物门？', questionType: 'single', difficulty: 'hard', optionsJson: JSON.stringify([{ label: 'A', text: '软体动物门' }, { label: 'B', text: '节肢动物门' }, { label: 'C', text: '棘皮动物门' }, { label: 'D', text: '脊索动物门' }]), correctAnswer: 'C', explanation: '海胆属于棘皮动物门，与海星、海参同属一门，具有独特的水管系统和五辐对称结构。' },
  { id: 15, stem: '以下哪个是海洋中的顶级捕食者？', questionType: 'single', difficulty: 'easy', optionsJson: JSON.stringify([{ label: 'A', text: '海豚' }, { label: 'B', text: '金枪鱼' }, { label: 'C', text: '虎鲸' }, { label: 'D', text: '海豹' }]), correctAnswer: 'C', explanation: '虎鲸（逆戟鲸）是海洋食物链的顶端捕食者，甚至捕食大白鲨和其他鲸类。' },
  { id: 16, stem: '海洋中的"死亡区域"主要是由于什么造成的？', questionType: 'single', difficulty: 'hard', optionsJson: JSON.stringify([{ label: 'A', text: '过度捕捞' }, { label: 'B', text: '水体缺氧' }, { label: 'C', text: '海水升温' }, { label: 'D', text: '噪音污染' }]), correctAnswer: 'B', explanation: '富营养化导致藻类大量繁殖，分解时消耗大量溶解氧，形成缺氧"死亡区"，生物无法生存。' },
  { id: 17, stem: '世界上最大的珊瑚礁群是？', questionType: 'single', difficulty: 'easy', optionsJson: JSON.stringify([{ label: 'A', text: '澳大利亚大堡礁' }, { label: 'B', text: '马尔代夫环礁' }, { label: 'C', text: '红海珊瑚礁' }, { label: 'D', text: '加勒比海珊瑚礁' }]), correctAnswer: 'A', explanation: '澳大利亚大堡礁全长约2300公里，是地球上最大的生物构造体，甚至可以从太空中看到。' },
  { id: 18, stem: '海洋中数量最多的浮游植物类群是？', questionType: 'single', difficulty: 'hard', optionsJson: JSON.stringify([{ label: 'A', text: '蓝藻' }, { label: 'B', text: '硅藻' }, { label: 'C', text: '甲藻' }, { label: 'D', text: '绿藻' }]), correctAnswer: 'B', explanation: '硅藻贡献了全球约20%的初级生产力，其二氧化硅细胞壁形成精美的微观结构。' },
  { id: 19, stem: '抹香鲸通常能潜入多深的海域？', questionType: 'single', difficulty: 'normal', optionsJson: JSON.stringify([{ label: 'A', text: '约500米' }, { label: 'B', text: '约1000米' }, { label: 'C', text: '超过2000米' }, { label: 'D', text: '超过4000米' }]), correctAnswer: 'C', explanation: '抹香鲸是潜水最深的有肺动物，可潜入2000米以上深海捕食大王乌贼，一次潜水可持续90分钟。' },
  { id: 20, stem: '海星具有惊人的再生能力，以下说法正确的是？', questionType: 'single', difficulty: 'normal', optionsJson: JSON.stringify([{ label: 'A', text: '只能再生表皮组织' }, { label: 'B', text: '断掉的手臂可再生' }, { label: 'C', text: '可完全再生整个身体' }, { label: 'D', text: '完全没有再生能力' }]), correctAnswer: 'B', explanation: '海星可再生断掉的腕足，有些种类甚至可以从一条断臂上再生出完整的身体。' },
]

/**
 * 模拟从数据库随机获取 10 道竞技题目
 */
const fetchCompetitionQuestions = () => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const shuffled = [...QUESTION_POOL].sort(() => Math.random() - 0.5)
      resolve(shuffled.slice(0, 10))
    }, 400)
  })
}

/**
 * 模拟排名计算
 * 根据正确率、参赛次数、总答题量、答题速度综合评分
 */
const getUserRanking = ({ accuracy, participationCount, totalAnswered, avgTimeMs }) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const avgSeconds = avgTimeMs / 1000
      // 速度分：平均 2s 满分，10s 零分
      const speedScore = Math.max(0, Math.min(100, (10 - avgSeconds) / 8 * 100))
      // 量级分：答 100 题满分
      const volumeScore = Math.min(100, totalAnswered)
      // 经验分：参赛 10 次满分
      const expScore = Math.min(100, participationCount * 10)
      // 综合评分：正确率权重最高
      const totalScore = accuracy * 0.45 + speedScore * 0.25 + volumeScore * 0.15 + expScore * 0.15

      // eslint-disable-next-line prefer-const
      let tier
      if (totalScore >= 88) tier = '王者'
      else if (totalScore >= 72) tier = '钻石'
      else if (totalScore >= 55) tier = '黄金'
      else if (totalScore >= 38) tier = '白银'
      else tier = '青铜'

      const rank = Math.max(1, Math.min(100, Math.round(101 - totalScore + (Math.random() - 0.5) * 10)))
      resolve({ rank, tier })
    }, 250)
  })
}

// ═══════════════════════════════════════════════════════
// 竞技模式：核心逻辑
// ═══════════════════════════════════════════════════════

const startCountdown = () => {
  clearTimers()
  countdown.value = 10
  compFeedback.value = false
  compSelectedAnswer.value = ''
  compTimeoutFlag.value = false
  compQuestionStartTime.value = Date.now()

  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
      handleTimeout()
    }
  }, 1000)
}

const clearTimers = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  if (autoAdvanceTimer) {
    clearTimeout(autoAdvanceTimer)
    autoAdvanceTimer = null
  }
}

const handleTimeout = () => {
  // 超时 → 判错，显示正确答案
  compTimeoutFlag.value = true
  compFeedback.value = true
  compSelectedAnswer.value = ''

  const timeMs = 10000 // 超时即 10 秒
  recordAnswer('', false, timeMs)

  autoAdvanceTimer = setTimeout(() => {
    advanceQuestion()
  }, 1500)
}

const compSelectAnswer = (label) => {
  // 已展示反馈或未开始倒计时时忽略点击
  if (compFeedback.value || countdown.value <= 0) return

  clearInterval(countdownTimer)
  countdownTimer = null

  compSelectedAnswer.value = label
  const elapsed = Date.now() - compQuestionStartTime.value
  const isCorrect = label === compCurrentQ.value.correctAnswer

  recordAnswer(label, isCorrect, elapsed)

  // 显示反馈
  compFeedback.value = true
  compTimeoutFlag.value = false

  // 答错展示 1.2 秒，答对展示 0.6 秒
  const delay = isCorrect ? 600 : 1200
  autoAdvanceTimer = setTimeout(() => {
    advanceQuestion()
  }, delay)
}

const recordAnswer = (answer, correct, timeMs) => {
  compUserAnswers.value[compCurrentIndex.value] = {
    answer,
    correct,
    timeMs: Math.min(timeMs, 10000),
  }
}

const advanceQuestion = () => {
  if (compCurrentIndex.value < compQuestions.value.length - 1) {
    compCurrentIndex.value++
    startCountdown()
  } else {
    finishCompetition()
  }
}

const startCompetition = async () => {
  compStartLoading.value = true
  try {
    const questions = await fetchCompetitionQuestions()
    compQuestions.value = questions
    compCurrentIndex.value = 0
    compUserAnswers.value = new Array(questions.length).fill(null)
    compFeedback.value = false
    compSelectedAnswer.value = ''
    compTimeoutFlag.value = false
    compStage.value = 'PLAYING'

    await nextTick()
    startCountdown()
  } catch {
    ElMessage.error('获取题目失败')
  } finally {
    compStartLoading.value = false
  }
}

const finishCompetition = async () => {
  clearTimers()

  // 统计本轮数据
  const answeredList = compUserAnswers.value.filter(a => a !== null)
  const correctList = answeredList.filter(a => a.correct)
  compCorrectCount.value = correctList.length
  compTotalCount.value = compQuestions.value.length
  compAccuracy.value = compTotalCount.value > 0 ? Math.round((compCorrectCount.value / compTotalCount.value) * 100) : 0

  const totalTimeMs = answeredList.reduce((sum, a) => sum + a.timeMs, 0)
  compTotalTime.value = (totalTimeMs / 1000).toFixed(1)
  compAvgTime.value = answeredList.length > 0 ? (totalTimeMs / answeredList.length / 1000).toFixed(1) : '10.0'

  // 错题详情
  compWrongDetails.value = []
  compQuestions.value.forEach((q, i) => {
    const ans = compUserAnswers.value[i]
    if (!ans || !ans.correct) {
      compWrongDetails.value.push({
        stem: q.stem,
        optionsJson: q.optionsJson,
        questionType: q.questionType,
        correctAnswer: q.correctAnswer,
        userAnswer: ans ? ans.answer : '',
        explanation: q.explanation || '',
      })
    }
  })

  // 加载竞技历史
  loadCompHistory()

  // 累计统计
  compParticipationCount.value = compHistory.value.length + 1
  compTotalAnsweredCount.value = compHistory.value.reduce((s, h) => s + (h.total || 0), 0) + compTotalCount.value

  const avgTimeMsVal = answeredList.length > 0 ? Math.round(totalTimeMs / answeredList.length) : 10000
  const localTier = calcTierFromAccuracy(compAccuracy.value, avgTimeMsVal / 1000)

  // 提交成绩到后端，获取真实排名
  try {
    const submitRes = await submitCompetitionResult({
      accuracy: compAccuracy.value,
      totalQuestions: compTotalCount.value,
      correctCount: compCorrectCount.value,
      totalTimeMs: totalTimeMs,
      avgTimeMs: avgTimeMsVal,
      tier: localTier,
    })
    if (submitRes.data.success) {
      compRank.value = submitRes.data.data.rank
      compRankTier.value = submitRes.data.data.tier
    } else {
      throw new Error('submit failed')
    }
  } catch {
    // 后端不可用 → 降级使用前端 Mock 排名
    try {
      const ranking = await getUserRanking({
        accuracy: compAccuracy.value,
        participationCount: compParticipationCount.value,
        totalAnswered: compTotalAnsweredCount.value,
        avgTimeMs: avgTimeMsVal,
      })
      compRank.value = ranking.rank
      compRankTier.value = ranking.tier
    } catch {
      compRank.value = 50
      compRankTier.value = '白银'
    }
  }

  // 保存本轮到竞技历史
  saveCompHistory()

  compStage.value = 'RESULT'
}

const loadCompHistory = () => {
  try {
    const raw = localStorage.getItem('comp_history')
    compHistory.value = raw ? JSON.parse(raw) : []
  } catch {
    compHistory.value = []
  }
}

const saveCompHistory = () => {
  const entry = {
    time: new Date().toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }),
    accuracy: compAccuracy.value,
    total: compTotalCount.value,
    tier: compRankTier.value,
    rank: compRank.value,
  }
  const list = [entry, ...compHistory.value].slice(0, 20)
  localStorage.setItem('comp_history', JSON.stringify(list))
  compHistory.value = list
}

const backToStart = () => {
  clearTimers()
  compStage.value = 'NOT_STARTED'
  compQuestions.value = []
  compCurrentIndex.value = 0
  compUserAnswers.value = []
}

// ═══════════════════════════════════════════════════════
// 排行榜
// ═══════════════════════════════════════════════════════

const openLeaderboard = () => {
  lbVisible.value = true
  fetchLeaderboard()
}

const fetchLeaderboard = async () => {
  lbLoading.value = true
  lbError.value = false
  try {
    const res = await getLeaderboard(50)
    if (res.data.success) {
      lbData.value = res.data.data || []
    } else {
      // API 失败 → 降级使用 Mock
      lbData.value = mockLeaderboard()
    }
  } catch {
    // 网络错误 → 降级使用 Mock
    lbData.value = mockLeaderboard()
  } finally {
    lbLoading.value = false
  }
}

const onLbClosed = () => {
  // 弹窗关闭时不重置数据，下次打开时重新拉取
}

const formatAvatarUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('/api')) return url
  return `/api${url}`
}

/**
 * Mock 排行榜数据（后端不可用时的降级方案）
 */
const mockLeaderboard = () => {
  const tiers = ['王者', '钻石', '钻石', '黄金', '黄金', '黄金', '白银', '白银', '白银', '白银', '青铜', '青铜']
  const names = [
    '海洋守护者', '深海旅人', '观鸟爱好者', '潜水教练Mike', '环保先锋',
    '水下摄影师Leo', '生态志愿者', '旅行日记本', '蓝鲸之梦', '珊瑚精灵',
    '海风轻拂', '浪花一朵朵', '北极熊不爱冰', '海龟慢慢爬', '海豚湾恋人',
    '海洋之心', '潮汐猎人', '深海探秘者', '海星闪闪', '水母飘飘',
    '鲨鱼先生', '海鸥飞飞', '船长Jack', '潜水达人', '海岛渔民'
  ]
  const frames = ['ocean', 'gold', 'neon', 'crystal', 'aurora', 'rainbow', 'flame', 'default', 'dashed', 'royal']

  return names.map((name, i) => ({
    rank: i + 1,
    userId: 1000 + i,
    username: name,
    avatarUrl: '',
    avatarFrame: frames[i % frames.length],
    totalMatches: Math.floor(Math.random() * 30) + 5,
    averageAccuracy: Math.round(95 - i * 1.5 + (Math.random() - 0.5) * 4),
    bestTier: tiers[Math.min(i, tiers.length - 1)],
    totalAnswered: Math.floor(Math.random() * 500) + 100,
    isMe: i === 7, // 模拟第8名是当前用户
  }))
}

// 响应式弹窗宽度
const updateLbDialogWidth = () => {
  lbDialogWidth.value = window.innerWidth < 768 ? '100%' : '680px'
}

onMounted(() => {
  updateLbDialogWidth()
  window.addEventListener('resize', updateLbDialogWidth)
})

// 组件卸载时清理定时器
onBeforeUnmount(() => {
  clearTimers()
  window.removeEventListener('resize', updateLbDialogWidth)
})

// ==================== 工具函数 ====================
const calcTierFromAccuracy = (accuracy, avgSeconds) => {
  const speedBonus = avgSeconds < 4 ? 10 : avgSeconds < 6 ? 5 : 0
  const score = accuracy + speedBonus
  if (score >= 95) return '王者'
  if (score >= 82) return '钻石'
  if (score >= 68) return '黄金'
  if (score >= 50) return '白银'
  return '青铜'
}

const typeLabel = (type) => {
  const map = { single: '单选题', multiple: '多选题', judge: '判断题' }
  return map[type] || type || '未知'
}

const typeTag = (type) => {
  const map = { single: 'primary', multiple: 'success', judge: 'warning' }
  return map[type] || ''
}

const diffLabel = (diff) => {
  const map = { easy: '简单', normal: '普通', hard: '困难' }
  return map[diff] || diff || '未知'
}

const parseOptions = (optionsJson) => {
  if (!optionsJson) return []
  try {
    const parsed = typeof optionsJson === 'string' ? JSON.parse(optionsJson) : optionsJson
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

// 初始化加载
fetchHistory()
loadCompHistory()
</script>

<style scoped>
/* ══════════════════════════════════════════════════════════════════════
   全局容器 (浅滩琉璃基调)
   ══════════════════════════════════════════════════════════════════════ */
.edu-quiz {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px;
  min-height: 600px;
  color: #1d2129;
  animation: fadeIn 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* ============ 开始界面 (清透悬浮感) ============ */
.start-screen {
  text-align: center;
  padding: 60px 20px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(24px) saturate(120%);
  -webkit-backdrop-filter: blur(24px) saturate(120%);
  border: 1px solid rgba(255, 255, 255, 1);
  border-radius: 24px;
  box-shadow: 0 16px 40px rgba(0, 50, 150, 0.05);
}

.start-icon {
  font-size: 72px;
  margin-bottom: 20px;
  filter: drop-shadow(0 10px 16px rgba(22, 93, 255, 0.2));
  animation: floatUp 3s ease-in-out infinite;
}

.start-screen h2 {
  font-size: 36px;
  margin: 0 0 12px;
  color: #1d2129;
  font-weight: 800;
  letter-spacing: 2px;
}

.subtitle {
  color: #86909c;
  font-size: 15px;
  margin-bottom: 40px;
}

.start-info {
  display: flex;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 32px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #165dff;
  font-size: 14px;
  font-weight: 600;
  background: rgba(22, 93, 255, 0.06);
  border: 1px solid rgba(22, 93, 255, 0.15);
  padding: 10px 20px;
  border-radius: 12px;
}

.info-icon { font-size: 18px; }

/* ═══════════════════════════════════════════════════
   模式切换 Tab
   ═══════════════════════════════════════════════════ */
.mode-tabs {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}

.mode-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 18px 32px;
  border: 2px solid #e5e6eb;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  min-width: 160px;
}

.mode-tab:hover {
  border-color: rgba(22, 93, 255, 0.3);
  background: rgba(22, 93, 255, 0.03);
  transform: translateY(-2px);
}

.mode-tab.active {
  border-color: #165dff;
  background: rgba(22, 93, 255, 0.06);
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.12);
}

.mode-icon {
  font-size: 28px;
}

.mode-label {
  font-size: 16px;
  font-weight: 700;
  color: #1d2129;
}

.mode-desc {
  font-size: 12px;
  color: #86909c;
  font-weight: 500;
}

.mode-tab.active .mode-label {
  color: #165dff;
}

.mode-tab.active .mode-desc {
  color: #165dff;
  opacity: 0.7;
}

/* 竞技模式提示 */
.comp-tip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: linear-gradient(135deg, rgba(255, 125, 0, 0.08), rgba(245, 63, 63, 0.06));
  border: 1px solid rgba(255, 125, 0, 0.25);
  border-radius: 10px;
  color: #e06900;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 24px;
  animation: pulse-tip 2s ease-in-out infinite;
}

.tip-icon {
  font-size: 16px;
}

@keyframes pulse-tip {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

/* 开始按钮 (液态蓝渐变) */
.start-btn {
  min-width: 220px;
  font-size: 18px;
  font-weight: bold;
  letter-spacing: 2px;
  padding: 24px 40px;
  border-radius: 30px;
  background: linear-gradient(135deg, #165dff 0%, #00d2ff 100%) !important;
  border: none !important;
  color: #fff !important;
  box-shadow: 0 8px 20px rgba(22, 93, 255, 0.25) !important;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.start-btn:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 28px rgba(22, 93, 255, 0.4) !important;
}

/* 竞技开始按钮 */
.comp-start-btn {
  background: linear-gradient(135deg, #ff7d00 0%, #f53f3f 100%) !important;
  box-shadow: 0 8px 20px rgba(245, 63, 63, 0.25) !important;
  animation: comp-btn-glow 2s ease-in-out infinite;
}
.comp-start-btn:hover {
  box-shadow: 0 12px 28px rgba(245, 63, 63, 0.4) !important;
}

@keyframes comp-btn-glow {
  0%, 100% { box-shadow: 0 8px 20px rgba(245, 63, 63, 0.25); }
  50% { box-shadow: 0 12px 32px rgba(245, 63, 63, 0.45); }
}

/* 历史记录 */
.history-section {
  margin-top: 60px;
  text-align: left;
}
.history-section h3 {
  font-size: 18px;
  color: #1d2129;
  font-weight: 700;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.history-list { display: flex; flex-direction: column; gap: 12px; }
.history-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid #e5e6eb;
  border-radius: 12px;
  font-size: 14px;
  color: #4e5969;
  transition: all 0.3s;
}
.history-item:hover {
  background: #fff;
  border-color: rgba(22, 93, 255, 0.3);
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(22, 93, 255, 0.05);
}
.history-time { color: #86909c; min-width: 140px; font-weight: 500; }

/* ============ 普通答题界面 (明亮白玻璃) ============ */
.answer-screen { padding: 8px 0; }

.progress-bar { margin-bottom: 24px; }
.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 15px;
}
.progress-text { font-weight: 700; color: #165dff; }
.progress-percent { color: #86909c; font-weight: 500; }
:deep(.el-progress-bar__outer) { background-color: rgba(0, 0, 0, 0.04) !important; }
:deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, #165dff, #00d2ff) !important;
  box-shadow: 0 0 8px rgba(22, 93, 255, 0.3);
}

.question-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(24px);
  border: 1px solid #fff;
  border-radius: 20px;
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: 0 16px 40px rgba(0, 50, 150, 0.05);
}

.q-header { display: flex; align-items: center; gap: 10px; margin-bottom: 24px; }
.tts-btn {
  margin-left: auto; font-size: 16px;
  background: rgba(255, 255, 255, 0.8) !important;
  border: 1px solid #e5e6eb !important;
  color: #86909c !important;
  box-shadow: 0 2px 6px rgba(0,0,0,0.02);
}
.tts-btn:hover {
  background: rgba(22, 93, 255, 0.05) !important;
  border-color: #165dff !important;
  color: #165dff !important;
}

.q-stem {
  font-size: 18px;
  line-height: 1.6;
  color: #1d2129;
  margin-bottom: 28px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.options-list { display: flex; flex-direction: column; gap: 14px; }
.option-item {
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid #e5e6eb;
  border-radius: 12px;
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
}
.option-item:hover {
  background: rgba(22, 93, 255, 0.03);
  border-color: rgba(22, 93, 255, 0.3);
  transform: translateX(6px);
}
.option-item.selected {
  background: rgba(22, 93, 255, 0.06);
  border-color: #165dff;
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.1);
}

.indicator {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px;
  border: 2px solid #c9cdd4;
  background: #fff;
  flex-shrink: 0; transition: all 0.3s;
}
.radio-indicator { border-radius: 50%; }
.checkbox-indicator { border-radius: 6px; }
.indicator.active {
  border-color: #165dff;
  background: #165dff;
  box-shadow: 0 2px 8px rgba(22, 93, 255, 0.2);
}
.indicator-dot { width: 8px; height: 8px; border-radius: 50%; background: #fff; }
.indicator-check { color: #fff; font-size: 14px; font-weight: 900; }

.opt-label { font-weight: 700; color: #165dff; min-width: 20px; flex-shrink: 0; font-size: 16px; }
.opt-text { color: #4e5969; font-size: 15px; transition: color 0.3s; font-weight: 500;}
.option-item.selected .opt-text { color: #1d2129; font-weight: 700; }

.judge-options { flex-direction: row; gap: 20px; }
.judge-options .option-item { flex: 1; justify-content: center; }

/* 导航 */
.nav-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.nav-center { font-size: 14px; }
:deep(.el-button) { border-radius: 12px; font-weight: 600; padding: 10px 24px; transition: all 0.3s; }
:deep(.el-button--default) { background: #fff; border: 1px solid #e5e6eb; color: #4e5969; box-shadow: 0 2px 8px rgba(0,0,0,0.02);}
:deep(.el-button--default:hover) { background: rgba(22, 93, 255, 0.04); border-color: rgba(22, 93, 255, 0.3); color: #165dff; }
:deep(.el-button--primary) { background: linear-gradient(135deg, #165dff, #00d2ff); border: none; box-shadow: 0 4px 12px rgba(22, 93, 255, 0.3); }
:deep(.el-button--success) { background: linear-gradient(135deg, #00b42a, #34d15e); border: none; box-shadow: 0 4px 12px rgba(0, 180, 42, 0.2); }

.answer-sheet {
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 24px rgba(0, 50, 150, 0.04);
}
.sheet-title { font-size: 15px; font-weight: 700; color: #1d2129; display: block; margin-bottom: 16px; }
.sheet-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(40px, 1fr)); gap: 10px; }
.sheet-item {
  aspect-ratio: 1;
  display: flex; align-items: center; justify-content: center;
  border-radius: 10px; font-size: 14px; font-weight: 600;
  cursor: pointer; transition: all 0.25s;
}
.sheet-item.unanswered { background: rgba(255, 255, 255, 0.8); border: 1px solid #e5e6eb; color: #86909c; }
.sheet-item.unanswered:hover { border-color: #165dff; color: #165dff; background: rgba(22, 93, 255, 0.04); }
.sheet-item.answered { background: rgba(22, 93, 255, 0.1); border: 1px solid rgba(22, 93, 255, 0.2); color: #165dff; }
.sheet-item.current { background: linear-gradient(135deg, #165dff, #00d2ff); border: none; color: #fff; box-shadow: 0 4px 12px rgba(22, 93, 255, 0.3); transform: scale(1.1); z-index: 2; }

/* ============ 普通结果界面 ============ */
.result-screen { text-align: center; padding: 30px 0; }
.result-header { margin-bottom: 40px; }

.score-circle {
  width: 160px; height: 160px;
  border-radius: 50%;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  margin: 0 auto 30px;
  background: rgba(255, 255, 255, 0.85);
  border: 4px solid;
  box-shadow: 0 8px 24px rgba(0, 50, 150, 0.05);
}
.score-excellent { border-color: #00b42a; color: #00b42a; }
.score-good { border-color: #165dff; color: #165dff; }
.score-poor { border-color: #f53f3f; color: #f53f3f; }

.score-number { font-size: 64px; font-weight: 900; line-height: 1; letter-spacing: -2px; }
.score-label { font-size: 16px; font-weight: 700; opacity: 0.8; margin-top: 4px; }

.result-header h2 { font-size: 28px; margin: 0 0 12px; color: #1d2129; font-weight: 800; letter-spacing: 1px; }
.score-summary { color: #86909c; font-size: 16px; font-weight: 500; }
.retry-btn { margin-bottom: 50px; min-width: 180px; padding: 20px 40px; font-size: 16px; border-radius: 30px; }

.detail-section { text-align: left; }
.detail-section h3 { font-size: 20px; font-weight: 800; color: #1d2129; margin-bottom: 20px; display: flex; align-items: center; gap: 10px; }

.detail-card {
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 16px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 50, 150, 0.04);
  transition: transform 0.3s;
}
.detail-card:hover { transform: translateY(-2px); background: #fff; }

.detail-card::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 4px;
}
.detail-card:not(.wrong)::before { background: #00b42a; }
.detail-card.wrong::before { background: #f53f3f; }
.detail-card.wrong { background: rgba(245, 63, 63, 0.02); border-color: rgba(245, 63, 63, 0.1); }
.detail-card.wrong:hover { background: rgba(245, 63, 63, 0.04); }

.detail-header { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.detail-num { font-size: 18px; font-weight: 800; color: #1d2129; }

.detail-stem { font-size: 16px; color: #1d2129; margin-bottom: 16px; line-height: 1.6; font-weight: 600; }
.detail-options { margin-bottom: 16px; display: flex; flex-direction: column; gap: 8px; }
.detail-option { padding: 8px 14px; font-size: 14px; color: #4e5969; background: #f7f8fa; border: 1px solid #e5e6eb; border-radius: 8px; font-weight: 500;}

.detail-answers { display: flex; flex-wrap: wrap; gap: 20px; margin-bottom: 16px; font-size: 14px; color: #4e5969; font-weight: 600;}
.detail-explanation {
  font-size: 14px; color: #4e5969; line-height: 1.6; font-weight: 500;
  padding: 16px; background: rgba(22, 93, 255, 0.04); border: 1px solid rgba(22, 93, 255, 0.1);
  border-radius: 12px;
  margin-top: 12px;
}
.exp-label { font-weight: 800; color: #165dff; margin-right: 6px; }

/* 错题回顾中选项高亮 */
.detail-opt-correct {
  background: rgba(0, 180, 42, 0.08) !important;
  border-color: rgba(0, 180, 42, 0.3) !important;
  color: #00b42a !important;
}
.detail-opt-wrong {
  background: rgba(245, 63, 63, 0.08) !important;
  border-color: rgba(245, 63, 63, 0.3) !important;
  color: #f53f3f !important;
}

/* ═══════════════════════════════════════════════════
   竞技模式：答题中
   ═══════════════════════════════════════════════════ */
.competition-playing {
  padding: 8px 0;
  animation: fadeIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* 顶部状态栏 */
.comp-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 0 4px;
}

.comp-progress-info {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.comp-progress-num {
  font-size: 32px;
  font-weight: 900;
  color: #165dff;
  line-height: 1;
}

.comp-progress-sep {
  font-size: 20px;
  color: #86909c;
  font-weight: 500;
  margin: 0 2px;
}

.comp-progress-total {
  font-size: 20px;
  color: #86909c;
  font-weight: 600;
}

/* 倒计时圆环 */
.comp-countdown {
  position: relative;
  width: 72px;
  height: 72px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.countdown-ring {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.countdown-arc {
  transition: stroke-dashoffset 0.9s linear, stroke 0.5s ease;
}

.countdown-number {
  font-size: 28px;
  font-weight: 900;
  line-height: 1;
  z-index: 1;
  transition: color 0.5s ease;
}

.countdown-unit {
  font-size: 11px;
  font-weight: 600;
  z-index: 1;
  opacity: 0.7;
}

.countdown-normal .countdown-number { color: #165dff; }
.countdown-normal .countdown-unit { color: #165dff; }
.countdown-warn .countdown-number { color: #ff7d00; animation: pulse-countdown 0.5s ease-in-out infinite; }
.countdown-warn .countdown-unit { color: #ff7d00; }
.countdown-danger .countdown-number { color: #f53f3f; animation: pulse-countdown 0.35s ease-in-out infinite; }
.countdown-danger .countdown-unit { color: #f53f3f; }

@keyframes pulse-countdown {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.12); }
}

/* 竞技进度条 */
.comp-progress-bar {
  margin-bottom: 24px;
}

/* 竞技题目卡片 */
.comp-question-card {
  /* 继承 .question-card 样式 */
}

/* 竞技选项反馈 */
.comp-option {
  position: relative;
}

/* 答对：绿色高亮 */
.comp-option.correct {
  background: rgba(0, 180, 42, 0.06) !important;
  border-color: #00b42a !important;
  box-shadow: 0 4px 16px rgba(0, 180, 42, 0.15) !important;
  pointer-events: none;
}

/* 答错：红色高亮 */
.comp-option.wrong {
  background: rgba(245, 63, 63, 0.06) !important;
  border-color: #f53f3f !important;
  box-shadow: 0 4px 16px rgba(245, 63, 63, 0.12) !important;
  pointer-events: none;
}

/* 未选中的选项变暗 */
.comp-option.dimmed {
  opacity: 0.45;
  pointer-events: none;
}

/* 自定义对错指示器 */
.indicator-correct {
  border-color: #00b42a !important;
  background: #00b42a !important;
  box-shadow: 0 2px 8px rgba(0, 180, 42, 0.3) !important;
}

.indicator-wrong {
  border-color: #f53f3f !important;
  background: #f53f3f !important;
  box-shadow: 0 2px 8px rgba(245, 63, 63, 0.3) !important;
}

.indicator-cross {
  color: #fff;
  font-size: 13px;
  font-weight: 900;
}

/* 超时提示 */
.comp-timeout-tip {
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba(245, 63, 63, 0.06);
  border: 1px solid rgba(245, 63, 63, 0.2);
  border-radius: 10px;
  color: #f53f3f;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
}

.comp-timeout-tip b {
  color: #00b42a;
  font-weight: 700;
}

/* ═══════════════════════════════════════════════════
   竞技模式：结果结算
   ═══════════════════════════════════════════════════ */
.competition-result {
  text-align: center;
  padding: 20px 0;
  animation: fadeIn 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.comp-result-header {
  margin-bottom: 32px;
}

/* 段位徽章 */
.comp-tier-badge {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 24px 48px;
  border-radius: 20px;
  margin-bottom: 20px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(24px);
  border: 2px solid;
  box-shadow: 0 8px 24px rgba(0, 50, 150, 0.06);
}

.tier-王者 {
  border-color: #f6d365;
  background: linear-gradient(135deg, rgba(255, 215, 0, 0.12), rgba(255, 165, 0, 0.08));
  box-shadow: 0 8px 32px rgba(255, 215, 0, 0.2);
}
.tier-钻石 {
  border-color: #00d2ff;
  background: linear-gradient(135deg, rgba(0, 210, 255, 0.1), rgba(22, 93, 255, 0.06));
  box-shadow: 0 8px 32px rgba(0, 210, 255, 0.18);
}
.tier-黄金 {
  border-color: #ffb800;
  background: linear-gradient(135deg, rgba(255, 184, 0, 0.1), rgba(255, 140, 0, 0.05));
  box-shadow: 0 8px 32px rgba(255, 184, 0, 0.15);
}
.tier-白银 {
  border-color: #b0b8c8;
  background: linear-gradient(135deg, rgba(176, 184, 200, 0.1), rgba(140, 150, 170, 0.05));
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
}
.tier-青铜 {
  border-color: #c49a6c;
  background: linear-gradient(135deg, rgba(196, 154, 108, 0.1), rgba(160, 120, 80, 0.05));
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.03);
}

.tier-icon {
  font-size: 40px;
}

.tier-name {
  font-size: 22px;
  font-weight: 800;
  color: #1d2129;
  letter-spacing: 2px;
}

.comp-result-title {
  font-size: 24px;
  font-weight: 800;
  color: #1d2129;
  margin: 0 0 6px;
}

.comp-result-subtitle {
  color: #86909c;
  font-size: 14px;
  font-weight: 500;
}

/* 5 维数据网格 */
.comp-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

/* 正确率占两列（大环形图） */
.stat-accuracy {
  grid-row: span 2;
  display: flex;
  align-items: center;
  justify-content: center;
}

.comp-stat-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 1);
  border-radius: 16px;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 8px 24px rgba(0, 50, 150, 0.04);
  transition: transform 0.3s;
}

.comp-stat-card:hover {
  transform: translateY(-2px);
}

.stat-value {
  font-size: 28px;
  font-weight: 900;
  color: #165dff;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: #86909c;
  font-weight: 600;
}

/* 环形进度图 */
.accuracy-ring {
  width: 120px;
  height: 120px;
}

.accuracy-arc {
  transition: stroke-dashoffset 1s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.accuracy-value {
  font-size: 22px;
  font-weight: 900;
  fill: #165dff;
}

.accuracy-label {
  font-size: 11px;
  fill: #86909c;
  font-weight: 600;
}

/* 本轮汇总 */
.comp-round-detail {
  margin-bottom: 28px;
}

.round-summary {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid #e5e6eb;
  border-radius: 12px;
  font-size: 15px;
  color: #4e5969;
}

.round-summary b {
  color: #165dff;
  font-weight: 700;
}

.round-divider {
  color: #c9cdd4;
}

/* 竞技操作按钮 */
.comp-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
}

.back-btn {
  border: 1px solid #e5e6eb !important;
  background: #fff !important;
  color: #4e5969 !important;
  min-width: 180px;
  padding: 16px 40px;
  font-size: 15px;
  border-radius: 30px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}

.back-btn:hover {
  border-color: rgba(22, 93, 255, 0.3) !important;
  color: #165dff !important;
}

/* ── 动画 Keyframes ── */
@keyframes floatUp {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ═══════════════════════════════════════════════════
   排行榜入口按钮
   ═══════════════════════════════════════════════════ */
.leaderboard-entry {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.lb-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(22, 93, 255, 0.2);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  box-shadow: 0 2px 8px rgba(22, 93, 255, 0.06);
  font-family: inherit;
}

.lb-btn:hover {
  background: rgba(22, 93, 255, 0.06);
  border-color: #165dff;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(22, 93, 255, 0.15);
}

.lb-btn-icon {
  font-size: 20px;
}

.lb-btn-text {
  font-size: 15px;
  font-weight: 700;
  color: #1d2129;
}

/* ═══════════════════════════════════════════════════
   排行榜弹窗 (通过 :deep() 覆盖 el-dialog)
   ═══════════════════════════════════════════════════ */
:deep(.leaderboard-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

:deep(.leaderboard-dialog .el-dialog__header) {
  display: none;
}

:deep(.leaderboard-dialog .el-dialog__body) {
  padding: 0;
  max-height: 70vh;
  overflow: hidden;
}

.lb-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* 头部 */
.lb-header {
  text-align: center;
  padding: 28px 24px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(180deg, rgba(22, 93, 255, 0.03) 0%, #fff 100%);
}

.lb-title-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 6px;
}

.lb-title-icon {
  font-size: 26px;
}

.lb-title-text {
  font-size: 22px;
  font-weight: 800;
  color: #1d2129;
  letter-spacing: 1px;
}

.lb-subtitle {
  font-size: 13px;
  color: #86909c;
  font-weight: 500;
}

/* Loading 骨架 */
.lb-loading {
  padding: 16px 24px;
}

.lb-skel-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.lb-skel-rank {
  width: 28px;
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: lb-shimmer 1.5s ease-in-out infinite;
}

.lb-skel-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: lb-shimmer 1.5s ease-in-out infinite;
}

.lb-skel-name {
  width: 100px;
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: lb-shimmer 1.5s ease-in-out infinite;
  flex: 1;
}

.lb-skel-stat {
  width: 50px;
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: lb-shimmer 1.5s ease-in-out infinite;
}

@keyframes lb-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 排行榜列表 */
.lb-list {
  overflow-y: auto;
  padding: 8px 24px 24px;
  max-height: 55vh;
}

.lb-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  transition: all 0.25s;
  border-bottom: 1px solid #f8f8f8;
}

.lb-row:hover {
  background: rgba(22, 93, 255, 0.03);
}

/* 当前用户高亮 */
.lb-row-me {
  background: rgba(22, 93, 255, 0.06) !important;
  border: 1px solid rgba(22, 93, 255, 0.2);
  border-radius: 10px;
}

/* 前三名高亮 */
.lb-row-top1 {
  background: linear-gradient(90deg, rgba(255, 215, 0, 0.08), transparent) !important;
}
.lb-row-top2 {
  background: linear-gradient(90deg, rgba(192, 192, 192, 0.08), transparent) !important;
}
.lb-row-top3 {
  background: linear-gradient(90deg, rgba(205, 127, 50, 0.06), transparent) !important;
}

/* 排名 */
.lb-rank {
  font-size: 16px;
  font-weight: 800;
  color: #86909c;
  width: 36px;
  text-align: center;
  flex-shrink: 0;
}

.lb-rank-1, .lb-rank-2, .lb-rank-3 {
  font-size: 22px;
}

.lb-rank-1 { color: #f6b100; }
.lb-rank-2 { color: #8c8c8c; }
.lb-rank-3 { color: #cd7f32; }

/* 头像框 */
.lb-avatar-frame {
  display: inline-flex;
  border-radius: 50%;
  padding: 2px;
  flex-shrink: 0;
}

.lb-avatar-frame :deep(.el-avatar) {
  border: 2px solid #fff;
  border-radius: 50%;
  box-sizing: content-box;
}

/* 用户名 */
.lb-username {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.lb-me-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px 7px;
  border-radius: 4px;
  background: #165dff;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

/* 右侧统计 */
.lb-stat {
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.lb-stat-matches {
  color: #86909c;
  min-width: 36px;
  text-align: right;
}

.lb-stat-accuracy {
  color: #165dff;
  min-width: 40px;
  text-align: right;
}

/* 段位小标签 */
.lb-tier-badge-sm {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
  min-width: 28px;
  justify-content: center;
}

.tier-sm-王者 { background: rgba(255, 215, 0, 0.15); color: #b8860b; }
.tier-sm-钻石 { background: rgba(0, 210, 255, 0.12); color: #0088aa; }
.tier-sm-黄金 { background: rgba(255, 184, 0, 0.12); color: #b07700; }
.tier-sm-白银 { background: rgba(176, 184, 200, 0.12); color: #6b7280; }
.tier-sm-青铜 { background: rgba(196, 154, 108, 0.12); color: #8b6914; }

/* 头像框样式（排行榜复用） */
.frame-default { background: #dcdfe6; }
.frame-gold { background: linear-gradient(135deg, #f6d365, #fda085); }
.frame-ocean { background: linear-gradient(135deg, #00d2ff, #165dff); }
.frame-rainbow {
  background: linear-gradient(90deg, #ff6b6b, #feca57, #48dbfb, #ff9ff3);
  background-size: 200% 100%;
  animation: frame-rainbow-spin 3s linear infinite;
}
@keyframes frame-rainbow-spin {
  0% { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}
.frame-flame { background: linear-gradient(135deg, #ff4500, #ff8c00, #ffd700); }
.frame-dashed {
  background: repeating-conic-gradient(#fd7000 0deg 18deg, transparent 18deg 36deg);
}
.frame-neon { background: linear-gradient(135deg, #00fff5, #ff00e4); }
.frame-aurora {
  background: linear-gradient(135deg, #00f260, #0575e6, #a855f7);
  background-size: 200% 200%;
  animation: frame-aurora-shift 4s ease infinite;
}
@keyframes frame-aurora-shift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
.frame-crystal { background: linear-gradient(135deg, #00f5ff, #ff00e5); }
.frame-royal { background: linear-gradient(135deg, #6c3cc7, #9b59b6, #f1c40f); }

/* 响应式适配 */
@media (max-width: 768px) {
  .edu-quiz { padding: 12px; }
  .start-screen { padding: 40px 16px; }
  .start-icon { font-size: 56px; }
  .start-screen h2 { font-size: 28px; }
  .info-item { width: 100%; justify-content: center; }

  /* 模式切换移动端 */
  .mode-tabs {
    flex-direction: column;
    gap: 8px;
  }
  .mode-tab {
    flex-direction: row;
    justify-content: center;
    gap: 10px;
    padding: 14px 20px;
    min-width: 0;
  }
  .mode-icon { font-size: 22px; }
  .mode-desc { display: none; }

  /* 竞技状态栏 */
  .comp-progress-num { font-size: 26px; }
  .comp-countdown { width: 60px; height: 60px; }
  .countdown-number { font-size: 24px; }

  .question-card { padding: 20px; }
  .comp-question-card { padding: 20px; }

  .options-list { gap: 10px; }
  .option-item { padding: 14px 16px; }
  .judge-options { flex-direction: column; gap: 10px; }
  .nav-bar { flex-wrap: wrap; gap: 16px; justify-content: center; }
  .nav-center { order: -1; width: 100%; text-align: center; margin-bottom: 8px; }
  .sheet-grid { grid-template-columns: repeat(auto-fill, minmax(32px, 1fr)); }
  .score-circle { width: 120px; height: 120px; }
  .score-number { font-size: 48px; }

  /* 结算移动端 */
  .comp-stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
  .stat-accuracy {
    grid-column: 1 / -1;
    grid-row: span 1;
    padding: 16px;
  }
  .accuracy-ring { width: 100px; height: 100px; }
  .accuracy-value { font-size: 18px; }
  .stat-value { font-size: 22px; }
  .comp-tier-badge { padding: 16px 32px; }
  .tier-icon { font-size: 32px; }
  .tier-name { font-size: 18px; }

  /* 排行榜移动端 */
  .lb-header { padding: 20px 16px 16px; }
  .lb-title-text { font-size: 18px; }
  .lb-list { padding: 8px 12px 16px; max-height: 50vh; }
  .lb-row { padding: 10px 8px; gap: 6px; }
  .lb-rank { font-size: 14px; width: 28px; }
  .lb-rank-1, .lb-rank-2, .lb-rank-3 { font-size: 18px; }
  .lb-username { font-size: 13px; max-width: 80px; }
  .lb-stat { font-size: 11px; }
  .lb-stat-accuracy { min-width: 32px; }
  .lb-tier-badge-sm { font-size: 10px; padding: 1px 6px; }
  .lb-me-tag { font-size: 10px; padding: 1px 5px; }
}
</style>
