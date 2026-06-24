<template>
  <div class="edu-quiz">
    <!-- ============ 开始界面 ============ -->
    <template v-if="phase === 'start'">
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
        <el-button type="primary" size="large" class="start-btn" :loading="startLoading" @click="handleStart">
          开始答题
        </el-button>

        <!-- 历史成绩 -->
        <div v-if="history.length > 0" class="history-section">
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
      </div>
    </template>

    <!-- ============ 答题界面 ============ -->
    <template v-if="phase === 'answering'">
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

    <!-- ============ 结果界面 ============ -->
    <template v-if="phase === 'result'">
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
  </div>
  <LevelUpPopup ref="levelUpRef" />
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { startExam, submitExam, getExamHistory, examTts } from '@/api/exam'
import { getLearningProfile } from '@/api/learning'
import LevelUpPopup from "@/components/LevelUpPopup.vue";

// ==================== 状态管理 ====================
const phase = ref('start') // 'start' | 'answering' | 'result'
const startLoading = ref(false)
const submitLoading = ref(false)

const levelUpRef = ref(null) //获得升级弹窗的实例

const questions = ref([])
const currentIndex = ref(0)
const userAnswers = ref([]) // 每个题目用户选择的答案

// 多选题中间状态
const selectedMultiple = ref([])

// 结果
const score = ref(0)
const correctCount = ref(0)
const totalCount = ref(0)
const resultDetails = ref([])

// 历史
const history = ref([])

// ==================== 计算属性 ====================
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

// ==================== 开始答题 ====================
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
      // 加载历史
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

// ==================== 答案选择 ====================
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

// 监听题目变化，初始化多选题选中状态
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

// ==================== 导航 ====================
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
  // 如果正在播放同一题，点击停止
  if (currentAudio && currentAudioQuestionId === q.id) {
    stopAudio()
    return
  }

  // 如果正在播放其他题，先停止
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

// ==================== 提交 ====================
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

  // 提交前获取当前等级（用于升级检测）
  const oldLevel = await getLearningProfile()
    .then(r => r.data.data?.level ?? null)
    .catch(() => null);

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

      // 切换到结果界面
      phase.value = 'result'

      // 检测升级：提交前后等级对比
      if (oldLevel !== null) {
        try {
          const profileRes = await getLearningProfile();
          const newLevel = profileRes.data.data?.level ?? oldLevel;
          if (newLevel > oldLevel) {
            setTimeout(() => {
              levelUpRef.value?.show(newLevel, ['+ 答题经验', '解锁新等级特权']);
            }, 600);
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

// ==================== 重置 ====================
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

// ==================== 历史 ====================
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

// ==================== 工具函数 ====================
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

// 初始化加载历史
fetchHistory()
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
  margin-bottom: 48px;
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

/* ============ 答题界面 (明亮白玻璃) ============ */
.answer-screen { padding: 8px 0; }

/* 进度条 */
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

/* 题目卡片 */
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

/* 选项列表 (明亮交互) */
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
  transform: translateX(6px); /* 悬浮右移 */
}
.option-item.selected {
  background: rgba(22, 93, 255, 0.06);
  border-color: #165dff;
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.1);
}

/* 自定义指示器 */
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

/* 底部导航按钮 */
.nav-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.nav-center { font-size: 14px; }
:deep(.el-button) { border-radius: 12px; font-weight: 600; padding: 10px 24px; transition: all 0.3s; }
:deep(.el-button--default) { background: #fff; border: 1px solid #e5e6eb; color: #4e5969; box-shadow: 0 2px 8px rgba(0,0,0,0.02);}
:deep(.el-button--default:hover) { background: rgba(22, 93, 255, 0.04); border-color: rgba(22, 93, 255, 0.3); color: #165dff; }
:deep(.el-button--primary) { background: linear-gradient(135deg, #165dff, #00d2ff); border: none; box-shadow: 0 4px 12px rgba(22, 93, 255, 0.3); }
:deep(.el-button--success) { background: linear-gradient(135deg, #00b42a, #34d15e); border: none; box-shadow: 0 4px 12px rgba(0, 180, 42, 0.2); }

/* 答题卡 (白透阵列) */
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

/* ============ 结果界面 (结算区) ============ */
.result-screen { text-align: center; padding: 30px 0; }
.result-header { margin-bottom: 40px; }

/* 结算分数环 */
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

/* 详细解析 (明亮玻璃卡) */
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

/* 左侧状态色带 */
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

/* ── 动画 Keyframes ── */
@keyframes floatUp {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 响应式适配 */
@media (max-width: 768px) {
  .edu-quiz { padding: 12px; }
  .start-screen { padding: 40px 16px; }
  .start-icon { font-size: 56px; }
  .start-screen h2 { font-size: 28px; }
  .info-item { width: 100%; justify-content: center; }
  .question-card { padding: 20px; }
  .options-list { gap: 10px; }
  .option-item { padding: 14px 16px; }
  .judge-options { flex-direction: column; gap: 10px; }
  .nav-bar { flex-wrap: wrap; gap: 16px; justify-content: center; }
  .nav-center { order: -1; width: 100%; text-align: center; margin-bottom: 8px; }
  .sheet-grid { grid-template-columns: repeat(auto-fill, minmax(32px, 1fr)); }
  .score-circle { width: 120px; height: 120px; }
  .score-number { font-size: 48px; }
}
</style>
