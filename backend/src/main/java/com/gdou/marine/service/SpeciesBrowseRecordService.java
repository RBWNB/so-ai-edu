package com.gdou.marine.service;

/**
 * @author liangguize2024
 * @version 1.0
 * @date 2026/6/22
 * @Description 物种浏览记录 Service
 */
public interface SpeciesBrowseRecordService {

    /**
     * 记录一次物种浏览（幂等：同一用户同一物种重复浏览只更新浏览次数和最后浏览时间）
     *
     * @param userId    用户ID
     * @param speciesId 物种ID
     */
    void recordBrowse(Long userId, Long speciesId);

    /**
     * 统计用户浏览过的不同物种数量
     *
     * @param userId 用户ID
     * @return 去重后的物种数
     */
    long countDistinctSpecies(Long userId);
}
