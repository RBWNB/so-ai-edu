<template>
  <div class="quiz-manage-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryForm" inline label-width="120px" @keyup.enter="handleSearch">
        <el-row :gutter="16">
          <!-- 题干关键词 -->
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="题干关键词">
              <el-input v-model="queryForm.stem" placeholder="搜索题干" clearable style="width: 100%" />
            </el-form-item>
          </el-col>
          <!-- 题目类型 -->
          <el-col :xs="12" :sm="6" :md="4">
            <el-form-item label="题目类型">
              <el-select v-model="queryForm.questionType" placeholder="全部" clearable style="width:100%; min-width: 100px">
                <el-option label="单选题" value="single" />
                <el-option label="多选题" value="multiple" />
                <el-option label="判断题" value="judge" />
              </el-select>
            </el-form-item>
          </el-col>
          <!-- 难度 -->
          <el-col :xs="12" :sm="6" :md="4">
            <el-form-item label="难度">
              <el-select v-model="queryForm.difficulty" placeholder="全部" clearable style="width:100%; min-width: 80px">
                <el-option label="简单" value="easy" />
                <el-option label="普通" value="normal" />
                <el-option label="困难" value="hard" />
              </el-select>
            </el-form-item>
          </el-col>
          <!-- 出题方式 -->
          <el-col :xs="12" :sm="6" :md="5">
            <el-form-item label="出题方式">
              <el-select v-model="queryForm.createdByAi" placeholder="全部" clearable style="width:100%; min-width: 100px">
                <el-option label="人工" :value="0" />
                <el-option label="AI" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <!-- 状态 -->
          <el-col :xs="12" :sm="6" :md="5">
            <el-form-item label="状态">
              <el-select v-model="queryForm.status" placeholder="全部" clearable style="width:100%; min-width: 80px">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <!-- 按钮单独占整行 -->
          <el-col :xs="24" :sm="24" :md="24">
            <el-form-item label=" ">
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="toolbar">
          <span class="toolbar-title">题库列表（共 {{ total }} 条）</span>
          <div class="toolbar-buttons">
            <el-button type="success" @click="openCreateDialog">
              + 人工出题
            </el-button>
            <el-button type="warning" @click="openAiDialog">
              AI 生成
            </el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column type="index" label="#" width="55" />
        <el-table-column label="题目类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.questionType)" size="small">
              {{ typeLabel(row.questionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" min-width="300" show-overflow-tooltip />
        <el-table-column label="难度" width="80">
          <template #default="{ row }">
            <el-tag
              :type="row.difficulty === 'easy' ? 'success' : row.difficulty === 'hard' ? 'danger' : 'warning'"
              effect="plain"
              size="small"
            >
              {{ diffLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="出题方式" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.createdByAi === 1" type="warning" effect="plain" size="small">AI</el-tag>
            <el-tag v-else type="" effect="plain" size="small">人工</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="row._statusLoading"
              @change="(val) => handleToggleStatus(row, val)"
              inline-prompt
              active-text="启"
              inactive-text="禁"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ row.createdAt ? formatTime(row.createdAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="previewQuestion(row)">预览</el-button>
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              link
              type="success"
              :loading="row._ttsLoading"
              @click="handleTts(row)"
            >
              {{ row._ttsPlaying ? '🔊播放中' : '🔊朗读' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 人工出题/编辑对话框 -->
    <el-dialog
      v-model="formDialog.visible"
      :title="formDialog.isEdit ? '编辑题目' : '人工出题'"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formModel"
        :rules="formRules"
        label-width="90px"
        label-position="top"
      >
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="题目类型" prop="questionType">
              <el-select v-model="formModel.questionType" placeholder="请选择" style="width:100%">
                <el-option label="单选题" value="single" />
                <el-option label="多选题" value="multiple" />
                <el-option label="判断题" value="judge" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="formModel.difficulty" placeholder="请选择" style="width:100%">
                <el-option label="简单" value="easy" />
                <el-option label="普通" value="normal" />
                <el-option label="困难" value="hard" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-switch v-model="formModel._status" active-text="启用" inactive-text="禁用" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="题干" prop="stem">
          <el-input
            v-model="formModel.stem"
            type="textarea"
            :rows="3"
            placeholder="请输入题目题干"
          />
        </el-form-item>

        <!-- 单选题/多选题选项 -->
        <template v-if="formModel.questionType === 'single' || formModel.questionType === 'multiple'">
          <el-divider content-position="left">选项设置</el-divider>
          <div
            v-for="(opt, idx) in formModel._options"
            :key="idx"
            class="option-row"
            :class="{ 'option-selected': isOptionSelected(opt.label) }"
            @click="handleOptionClick(opt)"
          >
            <el-row :gutter="8" align="middle">
              <el-col :span="2">
                <el-tag :type="isOptionSelected(opt.label) ? 'primary' : 'info'" size="small">
                  {{ opt.label }}
                </el-tag>
              </el-col>
              <el-col :span="16">
                <el-input v-model="opt.text" :placeholder="`选项${opt.label}`" size="default" @click.stop />
              </el-col>
              <el-col :span="6" class="option-action-col">
                <span
                  v-if="formModel.questionType === 'single'"
                  class="option-indicator"
                  :class="{ checked: formModel._singleAnswer === opt.label }"
                  @click.stop="formModel._singleAnswer = opt.label"
                >
                  <span class="indicator-inner" v-if="formModel._singleAnswer === opt.label" />
                </span>
                <span
                  v-if="formModel.questionType === 'multiple'"
                  class="option-indicator checkbox"
                  :class="{ checked: opt._checked }"
                  @click.stop="opt._checked = !opt._checked"
                >
                  <span v-if="opt._checked" class="indicator-check">✓</span>
                </span>
              </el-col>
            </el-row>
          </div>
          <el-button v-if="formModel._options.length < 6" size="small" type="primary" plain @click="addOption">
            + 添加选项
          </el-button>
        </template>

        <!-- 判断题 -->
        <template v-if="formModel.questionType === 'judge'">
          <el-divider content-position="left">正确答案</el-divider>
          <el-radio-group v-model="formModel._judgeAnswer">
            <el-radio value="正确">正确</el-radio>
            <el-radio value="错误">错误</el-radio>
          </el-radio-group>
        </template>

        <el-divider content-position="left">答案与解析</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="正确答案" prop="answerJson">
              <div v-if="formModel.questionType === 'single' || formModel.questionType === 'multiple'" class="answer-display">
                <el-tag v-if="formattedAnswer && formattedAnswer !== '请选择'" type="success" size="large" effect="plain">
                  {{ formattedAnswer }}
                </el-tag>
                <span v-else class="answer-placeholder">请在上方选项中选择</span>
              </div>
              <el-tag v-else type="success" size="large">
                {{ formModel._judgeAnswer === '正确' ? '正确' : '错误' }}
              </el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="知识点（逗号分隔）">
              <el-input v-model="formModel._knowledgePoints" placeholder="如：海洋生物分类, 鱼类" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="解析">
          <el-input
            v-model="formModel.explanation"
            type="textarea"
            :rows="3"
            placeholder="题目解析（可选）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ formDialog.isEdit ? '保存修改' : '确认出题' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- AI生成对话框 -->
    <el-dialog
      v-model="aiDialog.visible"
      title="AI 智能出题"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <template v-if="aiDialog.step === 'config'">
        <el-form :model="aiForm" label-width="140px">
          <!-- 来源类型切换 -->
          <el-form-item label="来源类型">
            <el-radio-group v-model="aiForm.sourceType" @change="onSourceTypeChange">
              <el-radio value="kb">知识库帖子</el-radio>
              <el-radio value="species">海洋百科物种</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 选择知识库帖子 -->
          <el-form-item v-if="aiForm.sourceType === 'kb'" label="选择知识库帖子" required>
            <div style="display: flex; gap: 8px; width: 100%;">
              <el-select
                v-model="aiForm.documentId"
                filterable
                remote
                reserve-keyword
                placeholder="输入关键词搜索知识库帖子"
                :remote-method="searchKbDocuments"
                :loading="kbSearchLoading"
                style="flex: 1;"
                @change="onKbDocumentSelect"
              >
                <el-option
                  v-for="doc in kbDocumentList"
                  :key="doc.id"
                  :label="doc.title"
                  :value="doc.id"
                >
                  <span>{{ doc.title }}</span>
                  <span class="kb-option-desc" v-if="doc.source">（{{ doc.source }}）</span>
                </el-option>
              </el-select>
              <el-button @click="openKbBrowseDialog">浏览全部</el-button>
            </div>
          </el-form-item>

          <el-form-item v-if="aiForm.sourceType === 'kb' && selectedDocument" label="已选帖子">
            <el-card shadow="hover" class="doc-card">
              <div class="doc-title">{{ selectedDocument.title }}</div>
              <div class="doc-preview">{{ selectedDocument.content?.slice(0, 200) }}...</div>
            </el-card>
          </el-form-item>

          <!-- 选择海洋百科物种 -->
          <el-form-item v-if="aiForm.sourceType === 'species'" label="选择海洋百科物种" required>
            <div style="display: flex; gap: 8px; width: 100%;">
              <el-select
                v-model="aiForm.speciesId"
                filterable
                remote
                reserve-keyword
                placeholder="输入关键词搜索海洋百科物种"
                :remote-method="searchSpecies"
                :loading="speciesSearchLoading"
                style="flex: 1;"
                @change="onSpeciesSelect"
              >
                <el-option
                  v-for="sp in speciesList"
                  :key="sp.id"
                  :label="sp.chineseName"
                  :value="sp.id"
                >
                  <span>{{ sp.chineseName }}</span>
                  <span class="kb-option-desc" v-if="sp.scientificName">（{{ sp.scientificName }}）</span>
                </el-option>
              </el-select>
              <el-button @click="openSpeciesBrowseDialog">浏览全部</el-button>
            </div>
          </el-form-item>

          <el-form-item v-if="aiForm.sourceType === 'species' && selectedSpecies" label="已选物种">
            <el-card shadow="hover" class="doc-card">
              <div class="doc-title">{{ selectedSpecies.chineseName }}</div>
              <div class="doc-preview" v-if="selectedSpecies.habitDesc">{{ selectedSpecies.habitDesc?.slice(0, 200) }}...</div>
              <div class="doc-preview" v-else-if="selectedSpecies.morphologyDesc">{{ selectedSpecies.morphologyDesc?.slice(0, 200) }}...</div>
            </el-card>
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="题目类型">
                <el-select v-model="aiForm.questionType" style="width:100%">
                  <el-option label="混合出题" value="mixed" />
                  <el-option label="单选题" value="single" />
                  <el-option label="多选题" value="multiple" />
                  <el-option label="判断题" value="judge" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="难度">
                <el-select v-model="aiForm.difficulty" style="width:100%">
                  <el-option label="简单" value="easy" />
                  <el-option label="普通" value="normal" />
                  <el-option label="困难" value="hard" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="题目数量">
                <el-input-number v-model="aiForm.count" :min="1" :max="10" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div class="dialog-footer">
          <el-button @click="closeAiDialog">取消</el-button>
          <el-button
            type="warning"
            :loading="aiDialog.generating"
            :disabled="aiForm.sourceType === 'kb' ? !aiForm.documentId : !aiForm.speciesId"
            @click="handleAiGenerate"
          >
            {{ aiDialog.generating ? 'AI生成中...' : '开始生成' }}
          </el-button>
        </div>
      </template>

      <template v-if="aiDialog.step === 'preview'">
        <div class="ai-result-header">
          <span class="result-title">AI 生成的题目（共 {{ aiGeneratedQuestions.length }} 道）</span>
          <div class="result-actions">
            <el-checkbox v-model="aiDialog.selectAll" :indeterminate="aiDialog.isIndeterminate" @change="handleSelectAll">
              全选
            </el-checkbox>
            <el-button type="primary" :disabled="selectedQuestions.length === 0" @click="handleBatchSave">
              上传选中（{{ selectedQuestions.length }} 道）
            </el-button>
          </div>
        </div>

        <el-divider />

        <div v-if="aiGeneratedQuestions.length === 0" class="empty-result">
          <el-empty description="AI未能生成题目，请重试" />
        </div>

        <div v-for="(q, idx) in aiGeneratedQuestions" :key="q._key || idx" class="question-preview-card">
          <el-checkbox v-model="q._selected" class="question-select" />
          <div class="question-body" :class="{ 'not-selected': !q._selected }">
            <div class="question-header">
              <span class="q-number">{{ idx + 1 }}.</span>
              <el-tag :type="typeTag(q.questionType)" size="small" class="q-type">
                {{ typeLabel(q.questionType) }}
              </el-tag>
              <el-tag
                :type="q.difficulty === 'easy' ? 'success' : q.difficulty === 'hard' ? 'danger' : 'warning'"
                effect="plain"
                size="small"
                class="q-diff"
              >
                {{ diffLabel(q.difficulty) }}
              </el-tag>
            </div>
            <div class="q-stem">{{ q.stem }}</div>

            <!-- 显示选项 -->
            <div v-if="q.optionsJson" class="q-options">
              <div v-for="opt in parseOptions(q.optionsJson)" :key="opt.label" class="q-option">
                <span class="opt-label">{{ opt.label }}.</span>
                <span class="opt-text">{{ opt.text }}</span>
              </div>
            </div>

            <div class="q-answer">
              <span class="answer-label">答案：</span>
              <el-tag type="success" size="small">{{ parseAnswer(q) }}</el-tag>
            </div>
            <div v-if="q.explanation" class="q-explanation">
              <span class="explanation-label">解析：</span>
              {{ q.explanation }}
            </div>
          </div>
        </div>

        <div class="dialog-footer">
          <el-button @click="aiDialog.step = 'config'">返回修改</el-button>
          <el-button type="primary" :disabled="selectedQuestions.length === 0" @click="handleBatchSave">
            上传选中（{{ selectedQuestions.length }} 道）
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 浏览知识库帖子对话框 -->
    <el-dialog
      v-model="kbBrowseDialog.visible"
      title="选择知识库帖子"
      width="650px"
      :close-on-click-modal="false"
    >
      <el-form :model="kbQueryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="kbQueryForm.keyword" placeholder="搜索帖子标题" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="kbQueryForm.categoryId" placeholder="全部" clearable style="width:150px">
            <el-option
              v-for="cat in kbCategoryList"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleKbSearch">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="kbTableLoading"
        :data="kbTableData"
        border
        stripe
        highlight-current-row
        @current-change="handleKbRowSelect"
      >
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="120" show-overflow-tooltip />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">
            {{ getCategoryName(row.categoryId) }}
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="kbPagination.current"
          v-model:page-size="kbPagination.size"
          :total="kbTotal"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next"
          size="small"
          @size-change="fetchKbDocuments"
          @current-change="fetchKbDocuments"
        />
      </div>

      <template #footer>
        <el-button @click="kbBrowseDialog.visible = false">取消</el-button>
        <el-button type="primary" :disabled="!kbSelectedDocId" @click="confirmKbSelect">
          选择此帖子
        </el-button>
      </template>
    </el-dialog>

    <!-- 浏览海洋百科物种对话框 -->
    <el-dialog
      v-model="speciesBrowseDialog.visible"
      title="选择海洋百科物种"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form :model="speciesQueryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="speciesQueryForm.keyword" placeholder="搜索物种名称" clearable style="width:200px" @keyup.enter="handleSpeciesSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSpeciesSearch">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="speciesTableLoading"
        :data="speciesTableData"
        border
        stripe
        highlight-current-row
        @current-change="handleSpeciesRowSelect"
      >
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="chineseName" label="中文名" width="140" show-overflow-tooltip />
        <el-table-column prop="scientificName" label="学名" width="180" show-overflow-tooltip />
        <el-table-column prop="familyName" label="科" width="100" show-overflow-tooltip />
        <el-table-column prop="habitat" label="栖息地" min-width="150" show-overflow-tooltip />
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="speciesPagination.current"
          v-model:page-size="speciesPagination.size"
          :total="speciesTotal"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next"
          size="small"
          @size-change="fetchSpeciesList"
          @current-change="fetchSpeciesList"
        />
      </div>

      <template #footer>
        <el-button @click="speciesBrowseDialog.visible = false">取消</el-button>
        <el-button type="primary" :disabled="!speciesSelectedId" @click="confirmSpeciesSelect">
          选择此物种
        </el-button>
      </template>
    </el-dialog>

    <!-- 预览题目对话框 -->
    <el-dialog
      v-model="previewDialog.visible"
      title="题目预览"
      width="600px"
      :close-on-click-modal="false"
    >
      <template v-if="previewDialog.question">
        <div class="preview-content">
          <div class="preview-field">
            <span class="field-label">类型：</span>
            <el-tag :type="typeTag(previewDialog.question.questionType)" size="small">
              {{ typeLabel(previewDialog.question.questionType) }}
            </el-tag>
            <el-tag
              :type="previewDialog.question.difficulty === 'easy' ? 'success' : previewDialog.question.difficulty === 'hard' ? 'danger' : 'warning'"
              effect="plain"
              size="small"
              style="margin-left: 8px"
            >
              {{ diffLabel(previewDialog.question.difficulty) }}
            </el-tag>
            <el-tag v-if="previewDialog.question.createdByAi === 1" type="warning" effect="plain" size="small" style="margin-left: 8px">
              AI生成
            </el-tag>
          </div>

          <div class="preview-field">
            <span class="field-label">题干：</span>
            <div class="field-value">{{ previewDialog.question.stem }}</div>
          </div>

          <div v-if="previewDialog.question.optionsJson" class="preview-field">
            <span class="field-label">选项：</span>
            <div v-for="opt in parseOptions(previewDialog.question.optionsJson)" :key="opt.label" class="preview-option">
              {{ opt.label }}. {{ opt.text }}
            </div>
          </div>

          <div class="preview-field">
            <span class="field-label">答案：</span>
            <el-tag type="success" size="small">{{ parseAnswer(previewDialog.question) }}</el-tag>
          </div>

          <div v-if="previewDialog.question.explanation" class="preview-field">
            <span class="field-label">解析：</span>
            <div class="field-value">{{ previewDialog.question.explanation }}</div>
          </div>
        </div>
      </template>
      <template #footer>
        <el-button @click="previewDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getQuestionPage,
  getQuestionById,
  createQuestion,
  updateQuestion,
  deleteQuestion,
  toggleQuestionStatus,
  aiGenerateQuestions,
  batchSaveQuestions,
  synthesizeSpeech,
  getKbDocumentPage,
  getKbCategoryList,
} from '@/api/quiz'
import { getSpeciesPage } from '@/api/species'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()

// ==================== 搜索条件 ====================
const queryForm = reactive({
  stem: '',
  questionType: '',
  difficulty: '',
  createdByAi: null,
  status: null,
})

const pagination = reactive({ current: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)

// ==================== 获取列表 ====================
const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
    }
    if (queryForm.stem) params.stem = queryForm.stem
    if (queryForm.questionType) params.questionType = queryForm.questionType
    if (queryForm.difficulty) params.difficulty = queryForm.difficulty
    if (queryForm.createdByAi !== null && queryForm.createdByAi !== '') params.createdByAi = queryForm.createdByAi
    if (queryForm.status !== null && queryForm.status !== '') params.status = queryForm.status

    const res = await getQuestionPage(params)
    tableData.value = (res.data.records || []).map(r => ({
      ...r,
      _ttsLoading: false,
      _ttsPlaying: false,
    }))
    total.value = res.data.total || 0
  } catch (err) {
    console.error('获取题库列表失败', err)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchList()
}

const handleReset = () => {
  queryForm.stem = ''
  queryForm.questionType = ''
  queryForm.difficulty = ''
  queryForm.createdByAi = null
  queryForm.status = null
  pagination.current = 1
  fetchList()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  fetchList()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  fetchList()
}

// ==================== 人工出题/编辑 ====================
const formRef = ref(null)
const submitLoading = ref(false)

const formDialog = reactive({
  visible: false,
  isEdit: false,
  editId: null,
})

const createDefaultForm = () => ({
  questionType: 'single',
  stem: '',
  optionsJson: '',
  answerJson: '',
  explanation: '',
  difficulty: 'normal',
  knowledgePoints: '',
  status: 1,
  _options: [
    { label: 'A', text: '', _checked: false },
    { label: 'B', text: '', _checked: false },
    { label: 'C', text: '', _checked: false },
    { label: 'D', text: '', _checked: false },
  ],
  _singleAnswer: '',
  _judgeAnswer: '正确',
  _status: true,
  _knowledgePoints: '',
})

const formModel = reactive(createDefaultForm())

const formRules = {
  questionType: [{ required: true, message: '请选择题目类型', trigger: 'change' }],
  stem: [{ required: true, message: '请输入题干', trigger: 'blur' }],
}

const formattedAnswer = computed(() => {
  if (formModel.questionType === 'single') {
    return formModel._singleAnswer ? formModel._singleAnswer : '请选择'
  }
  if (formModel.questionType === 'multiple') {
    const selected = formModel._options.filter(o => o._checked).map(o => o.label)
    return selected.length > 0 ? selected.join(',') : '请选择'
  }
  return formModel._judgeAnswer || '正确'
})

const isOptionSelected = (label) => {
  if (formModel.questionType === 'single') {
    return formModel._singleAnswer === label
  }
  return formModel._options.find(o => o.label === label)?._checked || false
}

const handleOptionClick = (opt) => {
  if (formModel.questionType === 'single') {
    formModel._singleAnswer = opt.label
  } else if (formModel.questionType === 'multiple') {
    opt._checked = !opt._checked
  }
}

const addOption = () => {
  if (formModel._options.length >= 6) return
  const nextLabel = String.fromCharCode(65 + formModel._options.length)
  formModel._options.push({ label: nextLabel, text: '', _checked: false })
}

const openCreateDialog = () => {
  formDialog.isEdit = false
  formDialog.editId = null
  Object.assign(formModel, createDefaultForm())
  formDialog.visible = true
}

const openEditDialog = async (row) => {
  formDialog.isEdit = true
  formDialog.editId = row.id
  try {
    const res = await getQuestionById(row.id)
    const q = res.data
    formModel.questionType = q.questionType || 'single'
    formModel.stem = q.stem || ''
    formModel.explanation = q.explanation || ''
    formModel.difficulty = q.difficulty || 'normal'
    formModel._status = q.status === 1

    // 解析知识点
    formModel._knowledgePoints = ''
    if (q.knowledgePoints) {
      try {
        const kps = JSON.parse(q.knowledgePoints)
        formModel._knowledgePoints = Array.isArray(kps) ? kps.join(', ') : q.knowledgePoints
      } catch {
        formModel._knowledgePoints = q.knowledgePoints
      }
    }

    // 解析选项
    if (q.questionType === 'single' || q.questionType === 'multiple') {
      const options = parseOptions(q.optionsJson)
      formModel._options = options.length > 0 ? options.map(o => ({ ...o, _checked: false })) : createDefaultForm()._options

      // 解析答案
      let answers = ''
      try {
        const ansRaw = JSON.parse(q.answerJson)
        answers = typeof ansRaw === 'string' ? ansRaw : String(ansRaw)
      } catch {
        answers = q.answerJson?.replace(/"/g, '') || ''
      }

      if (q.questionType === 'single') {
        formModel._singleAnswer = answers
      } else {
        const ansArr = answers.split(',').map(a => a.trim())
        formModel._options.forEach(o => {
          o._checked = ansArr.includes(o.label)
        })
      }
    } else {
      formModel._judgeAnswer = '正确'
      try {
        const ansRaw = JSON.parse(q.answerJson)
        const ansStr = typeof ansRaw === 'string' ? ansRaw : String(ansRaw)
        formModel._judgeAnswer = ansStr.includes('正确') ? '正确' : '错误'
      } catch {
        formModel._judgeAnswer = '正确'
      }
    }
  } catch (err) {
    ElMessage.error('获取题目详情失败')
  }
  formDialog.visible = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    // 构建答案
    let answerStr = ''
    if (formModel.questionType === 'judge') {
      answerStr = formModel._judgeAnswer
    } else if (formModel.questionType === 'single') {
      if (!formModel._singleAnswer) {
        ElMessage.warning('请选择正确答案')
        submitLoading.value = false
        return
      }
      answerStr = formModel._singleAnswer
    } else {
      const selected = formModel._options.filter(o => o._checked).map(o => o.label)
      if (selected.length < 2) {
        ElMessage.warning('多选题至少选择2个正确答案')
        submitLoading.value = false
        return
      }
      answerStr = selected.join(',')
    }

    // 构建optionsJson
    let optionsJsonStr = ''
    if (formModel.questionType === 'judge') {
      optionsJsonStr = JSON.stringify([
        { label: 'A', text: '正确' },
        { label: 'B', text: '错误' },
      ])
    } else {
      const opts = formModel._options.filter(o => o.text)
        .map(o => ({ label: o.label, text: o.text }))
      optionsJsonStr = JSON.stringify(opts)
    }

    // 构建knowledgePoints
    let kpStr = ''
    if (formModel._knowledgePoints) {
      const kps = formModel._knowledgePoints.split(/[,，、]/).map(s => s.trim()).filter(Boolean)
      kpStr = JSON.stringify(kps)
    }

    const data = {
      questionType: formModel.questionType,
      stem: formModel.stem,
      optionsJson: optionsJsonStr,
      answerJson: JSON.stringify(answerStr),
      explanation: formModel.explanation,
      difficulty: formModel.difficulty,
      knowledgePoints: kpStr,
      status: formModel._status ? 1 : 0,
    }

    if (formDialog.isEdit) {
      await updateQuestion(formDialog.editId, data)
      ElMessage.success('修改成功')
    } else {
      await createQuestion(data)
      ElMessage.success('出题成功')
    }

    formDialog.visible = false
    fetchList()
  } catch (err) {
    console.error('保存失败', err)
  } finally {
    submitLoading.value = false
  }
}

// ==================== 删除 ====================
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除题目「${(row.stem || '').slice(0, 30)}」？`, '确认删除', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await deleteQuestion(row.id)
      ElMessage.success('删除成功')
      fetchList()
    } catch (err) {
      console.error('删除失败', err)
    }
  }).catch(() => {})
}

// ==================== TTS 语音合成 ====================
let currentAudio = null

const handleTts = async (row) => {
  // 如果正在播放同一个题目，则停止
  if (row._ttsPlaying && currentAudio) {
    currentAudio.pause()
    currentAudio.currentTime = 0
    currentAudio = null
    row._ttsPlaying = false
    return
  }

  row._ttsLoading = true
  try {
    const res = await synthesizeSpeech({
      stem: row.stem || '',
      optionsJson: row.optionsJson || '',
    })

    if (res.data.success) {
      const audioUrl = res.data.url
      row._ttsPlaying = true

      // 播放音频
      const audio = new Audio(audioUrl)
      currentAudio = audio
      audio.onended = () => {
        row._ttsPlaying = false
        currentAudio = null
      }
      audio.onerror = () => {
        row._ttsPlaying = false
        currentAudio = null
        ElMessage.error('语音播放失败')
      }
      audio.play().catch(() => {
        row._ttsPlaying = false
        currentAudio = null
        ElMessage.error('语音播放失败')
      })
    } else {
      ElMessage.error(res.data.message || '语音合成失败')
    }
  } catch (err) {
    ElMessage.error('语音合成失败：' + (err.response?.data?.message || err.message || '未知错误'))
    row._ttsLoading = false
  } finally {
    row._ttsLoading = false
  }
}

// ==================== 状态切换 ====================
const handleToggleStatus = async (row, val) => {
  const newStatus = val ? 1 : 0
  row._statusLoading = true
  try {
    await toggleQuestionStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(val ? '已启用' : '已禁用')
  } catch (err) {
    ElMessage.error('操作失败')
  } finally {
    row._statusLoading = false
  }
}

// ==================== 预览 ====================
const previewDialog = reactive({
  visible: false,
  question: null,
})

const previewQuestion = (row) => {
  previewDialog.question = row
  previewDialog.visible = true
}

// ==================== AI 生成 ====================
const aiDialog = reactive({
  visible: false,
  step: 'config',
  generating: false,
  selectAll: false,
  isIndeterminate: false,
})

const aiForm = reactive({
  sourceType: 'kb',
  documentId: null,
  speciesId: null,
  count: 5,
  questionType: 'mixed',
  difficulty: 'normal',
})

const aiGeneratedQuestions = ref([])

const selectedQuestions = computed(() => {
  return aiGeneratedQuestions.value.filter(q => q._selected)
})

const openAiDialog = () => {
  aiDialog.visible = true
  aiDialog.step = 'config'
  aiForm.sourceType = 'kb'
  aiForm.documentId = null
  aiForm.speciesId = null
  aiForm.count = 5
  aiForm.questionType = 'mixed'
  aiForm.difficulty = 'normal'
  aiGeneratedQuestions.value = []
  selectedDocument.value = null
  selectedSpecies.value = null
  fetchKbDocuments()
  fetchKbCategories()
}

const closeAiDialog = () => {
  aiDialog.visible = false
}

// 搜索知识库文档
const kbSearchLoading = ref(false)
const kbDocumentList = ref([])
const selectedDocument = ref(null)

const searchKbDocuments = async (keyword) => {
  if (!keyword) {
    kbDocumentList.value = []
    return
  }
  kbSearchLoading.value = true
  try {
    const res = await getKbDocumentPage({ keyword, pageSize: 20, current: 1 })
    kbDocumentList.value = res.data.records || []
  } catch (err) {
    console.error('搜索知识库失败', err)
  } finally {
    kbSearchLoading.value = false
  }
}

const onKbDocumentSelect = async (docId) => {
  if (!docId) {
    selectedDocument.value = null
    return
  }
  const found = kbDocumentList.value.find(d => d.id === docId)
  if (found) {
    selectedDocument.value = found
  }
}

// 搜索海洋百科物种
const speciesSearchLoading = ref(false)
const speciesList = ref([])
const selectedSpecies = ref(null)

const searchSpecies = async (keyword) => {
  if (!keyword) {
    speciesList.value = []
    return
  }
  speciesSearchLoading.value = true
  try {
    const res = await getSpeciesPage({ keyword, pageSize: 20, current: 1 })
    speciesList.value = res.data.records || []
  } catch (err) {
    console.error('搜索海洋百科物种失败', err)
  } finally {
    speciesSearchLoading.value = false
  }
}

const onSpeciesSelect = async (speciesId) => {
  if (!speciesId) {
    selectedSpecies.value = null
    return
  }
  const found = speciesList.value.find(s => s.id === speciesId)
  if (found) {
    selectedSpecies.value = found
  }
}

const onSourceTypeChange = () => {
  aiForm.documentId = null
  aiForm.speciesId = null
  selectedDocument.value = null
  selectedSpecies.value = null
}

// 浏览全部海洋百科物种
const speciesBrowseDialog = reactive({
  visible: false,
})

const speciesQueryForm = reactive({
  keyword: '',
})

const speciesTableLoading = ref(false)
const speciesTableData = ref([])
const speciesTotal = ref(0)
const speciesPagination = reactive({ current: 1, size: 10 })
const speciesSelectedId = ref(null)

const openSpeciesBrowseDialog = () => {
  speciesBrowseDialog.visible = true
  speciesSelectedId.value = null
  fetchSpeciesList()
}

const fetchSpeciesList = async () => {
  speciesTableLoading.value = true
  try {
    const params = {
      current: speciesPagination.current,
      size: speciesPagination.size,
    }
    if (speciesQueryForm.keyword) params.keyword = speciesQueryForm.keyword

    const res = await getSpeciesPage(params)
    speciesTableData.value = res.data.records || []
    speciesTotal.value = res.data.total || 0
  } catch (err) {
    console.error('获取海洋百科列表失败', err)
  } finally {
    speciesTableLoading.value = false
  }
}

const handleSpeciesSearch = () => {
  speciesPagination.current = 1
  fetchSpeciesList()
}

const handleSpeciesRowSelect = (row) => {
  if (row) {
    speciesSelectedId.value = row.id
  }
}

const confirmSpeciesSelect = () => {
  const sp = speciesTableData.value.find(s => s.id === speciesSelectedId.value)
  if (sp) {
    aiForm.speciesId = sp.id
    selectedSpecies.value = sp
    if (!speciesList.value.find(s => s.id === sp.id)) {
      speciesList.value.push(sp)
    }
  }
  speciesBrowseDialog.visible = false
}

// 浏览全部知识库
const kbBrowseDialog = reactive({
  visible: false,
})

const kbQueryForm = reactive({
  keyword: '',
  categoryId: null,
})

const kbTableLoading = ref(false)
const kbTableData = ref([])
const kbTotal = ref(0)
const kbPagination = reactive({ current: 1, size: 10 })
const kbCategoryList = ref([])
const kbSelectedDocId = ref(null)

const openKbBrowseDialog = () => {
  kbBrowseDialog.visible = true
  kbSelectedDocId.value = null
  fetchKbDocuments()
}

const fetchKbDocuments = async () => {
  kbTableLoading.value = true
  try {
    const params = {
      current: kbPagination.current,
      size: kbPagination.size,
    }
    if (kbQueryForm.keyword) params.keyword = kbQueryForm.keyword
    if (kbQueryForm.categoryId) params.categoryId = kbQueryForm.categoryId

    const res = await getKbDocumentPage(params)
    kbTableData.value = res.data.records || []
    kbTotal.value = res.data.total || 0
  } catch (err) {
    console.error('获取知识库列表失败', err)
  } finally {
    kbTableLoading.value = false
  }
}

const handleKbSearch = () => {
  kbPagination.current = 1
  fetchKbDocuments()
}

const fetchKbCategories = async () => {
  try {
    const res = await getKbCategoryList()
    kbCategoryList.value = res.data || []
  } catch (err) {
    console.error('获取分类失败', err)
  }
}

const getCategoryName = (id) => {
  const cat = kbCategoryList.value.find(c => c.id === id)
  return cat ? cat.name : '未分类'
}

const handleKbRowSelect = (row) => {
  if (row) {
    kbSelectedDocId.value = row.id
  }
}

const confirmKbSelect = () => {
  const doc = kbTableData.value.find(d => d.id === kbSelectedDocId.value)
  if (doc) {
    aiForm.documentId = doc.id
    selectedDocument.value = doc
    // 也加入kbDocumentList以便select显示
    if (!kbDocumentList.value.find(d => d.id === doc.id)) {
      kbDocumentList.value.push(doc)
    }
  }
  kbBrowseDialog.visible = false
}

// AI生成
const handleAiGenerate = async () => {
  if (aiForm.sourceType === 'kb' && !aiForm.documentId) {
    ElMessage.warning('请先选择知识库帖子')
    return
  }
  if (aiForm.sourceType === 'species' && !aiForm.speciesId) {
    ElMessage.warning('请先选择海洋百科物种')
    return
  }
  aiDialog.generating = true
  try {
    const params = {
      sourceType: aiForm.sourceType,
      count: aiForm.count,
      questionType: aiForm.questionType,
      difficulty: aiForm.difficulty,
    }
    if (aiForm.sourceType === 'species') {
      params.speciesId = aiForm.speciesId
    } else {
      params.documentId = aiForm.documentId
    }
    const res = await aiGenerateQuestions(params)

    if (res.data.success) {
      const questions = res.data.data || []
      aiGeneratedQuestions.value = questions.map((q, idx) => ({
        ...q,
        _selected: true,
        _key: `ai_${idx}_${Date.now()}`,
      }))
      aiDialog.step = 'preview'
      aiDialog.selectAll = true
      aiDialog.isIndeterminate = false
    } else {
      ElMessage.error(res.data.message || '生成失败')
    }
  } catch (err) {
    ElMessage.error('生成失败：' + (err.message || '未知错误'))
  } finally {
    aiDialog.generating = false
  }
}

// 全选/取消全选
const handleSelectAll = (val) => {
  aiGeneratedQuestions.value.forEach(q => {
    q._selected = val
  })
  aiDialog.isIndeterminate = false
}

watch(
  () => {
    const list = aiGeneratedQuestions.value
    if (list.length === 0) {
      aiDialog.isIndeterminate = false
      return
    }
    const selectedCount = list.filter(q => q._selected).length
    if (selectedCount === list.length) {
      aiDialog.selectAll = true
      aiDialog.isIndeterminate = false
    } else if (selectedCount === 0) {
      aiDialog.selectAll = false
      aiDialog.isIndeterminate = false
    } else {
      aiDialog.isIndeterminate = true
    }
  },
  { deep: true }
)

// 批量保存
const handleBatchSave = async () => {
  const toSave = selectedQuestions.value.map(q => ({
    questionType: q.questionType,
    stem: q.stem,
    optionsJson: q.optionsJson || '',
    answerJson: q.answerJson,
    explanation: q.explanation || '',
    difficulty: q.difficulty || 'normal',
    knowledgePoints: q.knowledgePoints || '',
    createdByAi: 1,
    status: 1,
  }))

  if (toSave.length === 0) {
    ElMessage.warning('请至少选择一道题')
    return
  }

  try {
    const res = await batchSaveQuestions(toSave)
    if (res.data.success !== false) {
      ElMessage.success(`成功上传 ${toSave.length} 道题目到题库`)
      aiDialog.visible = false
      fetchList()
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } catch (err) {
    ElMessage.error('保存失败：' + (err.message || '未知错误'))
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

const parseAnswer = (q) => {
  if (!q.answerJson) return ''
  try {
    const ans = typeof q.answerJson === 'string' ? JSON.parse(q.answerJson) : q.answerJson
    const ansStr = typeof ans === 'string' ? ans : String(ans)
    if (q.questionType === 'judge') {
      return ansStr.includes('正确') ? '正确' : '错误'
    }
    return ansStr
  } catch {
    return q.answerJson ? q.answerJson.replace(/"/g, '') : ''
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// ==================== 初始化 ====================
onMounted(() => {
  fetchList()
  // 预加载知识库分类和文档列表
  fetchKbCategories()
  fetchKbDocuments()
})
</script>

<style scoped>
.quiz-manage-page {
  padding: 12px;
}

.search-card {
  margin-bottom: 12px;
}

.search-btns {
  display: flex;
  align-items: flex-start;
  padding-top: 2px;
  gap: 8px;
}

.table-card {
  min-height: 400px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-title {
  font-weight: 600;
  font-size: 15px;
}

.toolbar-buttons {
  display: flex;
  gap: 8px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.option-row {
  margin-bottom: 8px;
  padding: 6px 10px;
  background: #fafafa;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s;
}

.option-row:hover {
  border-color: #409eff;
  background: #f5f9ff;
}

.option-row.option-selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.option-action-col {
  text-align: center;
}

.option-indicator {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #c0c4cc;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.option-indicator.checkbox {
  border-radius: 3px;
}

.option-indicator.checked {
  border-color: #409eff;
  background: #409eff;
}

.indicator-inner {
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

.answer-display {
  min-height: 32px;
  display: flex;
  align-items: center;
}

.answer-placeholder {
  color: #c0c4cc;
  font-size: 14px;
}

.dialog-footer {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* AI 生成 */
.ai-result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.result-title {
  font-weight: 600;
  font-size: 15px;
}

.result-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.empty-result {
  padding: 40px 0;
}

.question-preview-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px;
  margin-bottom: 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background: #fafcff;
  transition: all 0.2s;
}

.question-preview-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.question-select {
  margin-top: 4px;
}

.question-body {
  flex: 1;
  min-width: 0;
}

.question-body.not-selected {
  opacity: 0.5;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.q-number {
  font-weight: 600;
  color: #409eff;
}

.q-stem {
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 8px;
  color: #303133;
}

.q-options {
  margin-bottom: 8px;
}

.q-option {
  padding: 2px 0;
  font-size: 13px;
  color: #606266;
}

.opt-label {
  font-weight: 600;
  margin-right: 4px;
}

.q-answer {
  margin-bottom: 4px;
}

.answer-label {
  font-weight: 600;
  color: #67c23a;
  font-size: 13px;
}

.q-explanation {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
  padding: 6px 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.explanation-label {
  font-weight: 600;
  color: #909399;
}

/* 文档卡片 */
.doc-card {
  width: 100%;
}

.doc-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}

.doc-preview {
  font-size: 12px;
  color: #909399;
}

.kb-option-desc {
  color: #909399;
  font-size: 12px;
}

/* 预览对话框 */
.preview-content {
  padding: 4px 0;
}

.preview-field {
  margin-bottom: 12px;
}

.field-label {
  font-weight: 600;
  color: #606266;
  font-size: 14px;
}

.field-value {
  margin-top: 4px;
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  white-space: pre-wrap;
}

.preview-option {
  padding: 2px 0;
  font-size: 14px;
  color: #606266;
  padding-left: 16px;
}
</style>
