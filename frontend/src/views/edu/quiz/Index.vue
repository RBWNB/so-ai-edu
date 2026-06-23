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
.edu-quiz {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
  min-height: 600px;
}

/* ============ 开始界面 ============ */
.start-screen {
  text-align: center;
  padding: 60px 20px;
}

.start-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.start-screen h2 {
  font-size: 32px;
  margin: 0 0 8px;
  color: #303133;
}

.subtitle {
  color: #909399;
  font-size: 15px;
  margin-bottom: 32px;
}

.start-info {
  display: flex;
  justify-content: center;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 36px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 14px;
  background: #f0f9ff;
  padding: 8px 16px;
  border-radius: 20px;
}

.info-icon {
  font-size: 18px;
}

.start-btn {
  min-width: 200px;
  font-size: 18px;
  padding: 16px 40px;
  border-radius: 30px;
}

/* 历史 */
.history-section {
  margin-top: 48px;
  text-align: left;
}

.history-section h3 {
  font-size: 18px;
  margin-bottom: 12px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 14px;
}

.history-time {
  color: #909399;
  min-width: 120px;
}

/* ============ 答题界面 ============ */
.answer-screen {
  padding: 8px 0;
}

.progress-bar {
  margin-bottom: 20px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 14px;
}

.progress-text {
  font-weight: 600;
  color: #409eff;
}

.progress-percent {
  color: #909399;
}

/* 题目卡片 */
.question-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.q-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.tts-btn {
  margin-left: auto;
  font-size: 16px;
}

.q-stem {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 20px;
  font-weight: 500;
}

/* 选项 */
.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.option-item:hover {
  border-color: #409eff;
  background: #f5f9ff;
}

.option-item.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

/* 自定义单选/复选指示器 */
.indicator {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: 2px solid #c0c4cc;
  background: #fff;
  flex-shrink: 0;
  transition: all 0.2s;
}

.radio-indicator {
  border-radius: 50%;
}

.checkbox-indicator {
  border-radius: 3px;
}

.indicator.active {
  border-color: #409eff;
  background: #409eff;
}

.indicator-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fff;
}

.indicator-check {
  color: #fff;
  font-size: 12px;
  font-weight: bold;
  line-height: 1;
}

.opt-label {
  font-weight: 600;
  color: #606266;
  min-width: 16px;
  flex-shrink: 0;
}

.opt-text {
  color: #303133;
}

.judge-options {
  flex-direction: row;
  gap: 16px;
}

.judge-options .option-item {
  flex: 1;
  text-align: center;
  justify-content: center;
}

/* 导航 */
.nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.nav-center {
  font-size: 13px;
}

/* 答题卡 */
.answer-sheet {
  background: #f5f7fa;
  border-radius: 10px;
  padding: 16px;
}

.sheet-title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  display: block;
  margin-bottom: 10px;
}

.sheet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 36px);
  gap: 6px;
}

.sheet-item {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  background: #fff;
  border: 1px solid #e8e8e8;
  color: #606266;
}

.sheet-item.current {
  border-color: #409eff;
  background: #409eff;
  color: #fff;
  font-weight: 600;
}

.sheet-item.answered {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}

.sheet-item.unanswered {
  background: #fff;
  border-color: #e8e8e8;
  color: #c0c4cc;
}

.sheet-item:hover {
  border-color: #409eff;
}

/* ============ 结果界面 ============ */
.result-screen {
  text-align: center;
  padding: 20px 0;
}

.result-header {
  margin-bottom: 24px;
}

.score-circle {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  border: 6px solid;
}

.score-excellent {
  border-color: #67c23a;
  color: #67c23a;
}

.score-good {
  border-color: #e6a23c;
  color: #e6a23c;
}

.score-poor {
  border-color: #f56c6c;
  color: #f56c6c;
}

.score-number {
  font-size: 48px;
  font-weight: 700;
  line-height: 1;
}

.score-label {
  font-size: 16px;
  margin-top: 2px;
}

.result-header h2 {
  font-size: 22px;
  margin: 0 0 8px;
  color: #303133;
}

.score-summary {
  color: #909399;
  font-size: 15px;
}

.retry-btn {
  margin-bottom: 40px;
  min-width: 160px;
}

/* 详细解析 */
.detail-section {
  text-align: left;
}

.detail-section h3 {
  font-size: 18px;
  margin-bottom: 16px;
}

.detail-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 12px;
  border-left: 4px solid #67c23a;
}

.detail-card.wrong {
  border-left-color: #f56c6c;
  background: #fff5f5;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.detail-num {
  font-weight: 600;
  color: #303133;
}

.detail-stem {
  font-size: 15px;
  color: #303133;
  margin-bottom: 10px;
  line-height: 1.6;
}

.detail-options {
  margin-bottom: 8px;
}

.detail-option {
  padding: 2px 0;
  font-size: 13px;
  color: #606266;
}

.detail-answers {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.detail-explanation {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-top: 8px;
}

.exp-label {
  font-weight: 600;
  color: #909399;
}
</style>
