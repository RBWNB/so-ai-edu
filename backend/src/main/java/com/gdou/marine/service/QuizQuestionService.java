package com.gdou.marine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gdou.marine.dto.AiGenerateDTO;
import com.gdou.marine.entity.QuizQuestion;

import java.util.List;

/**
 * 题库 Service 接口
 */
public interface QuizQuestionService extends IService<QuizQuestion> {

    /**
     * AI 生成题目
     *
     * @param dto  生成参数
     * @param userId 操作人ID
     * @return 生成的题目列表（尚未入库）
     */
    List<QuizQuestion> generateByAi(AiGenerateDTO dto, Long userId);
}
