package com.gdou.marine.service;

import java.util.List;
import java.util.Map;
/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/22
 * @Description 勋章自动颁发 Service
 */
public interface BadgeAwardService {

    /**
     * 自动检测并颁发所有满足条件的勋章（幂等：已获得的不重复发）
     * 建议在以下时机调用：
     * - 用户查看勋章墙时（GET /achievement/badges）
     * - 答题提交后
     * - AI 问答后
     * - 签到后
     *
     * @return 本次新颁发的勋章列表
     */
    List<Map<String, String>> autoCheckAndAward(Long userId);
}
