package com.gdou.marine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.marine.entity.SpeciesBrowseRecord;
import com.gdou.marine.mapper.SpeciesBrowseRecordMapper;
import com.gdou.marine.service.SpeciesBrowseRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @author liangguize2024
 * @version 1.0
 * @date 2026/6/22
 * @Description 物种浏览记录 ServiceImpl
 */
@Service
public class SpeciesBrowseRecordServiceImpl implements SpeciesBrowseRecordService {

    private static final Logger log = LoggerFactory.getLogger(SpeciesBrowseRecordServiceImpl.class);

    private final SpeciesBrowseRecordMapper browseRecordMapper;

    public SpeciesBrowseRecordServiceImpl(SpeciesBrowseRecordMapper browseRecordMapper) {
        this.browseRecordMapper = browseRecordMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordBrowse(Long userId, Long speciesId) {
        if (userId == null || speciesId == null) return;

        SpeciesBrowseRecord existing = browseRecordMapper.selectOne(
                new LambdaQueryWrapper<SpeciesBrowseRecord>()
                        .eq(SpeciesBrowseRecord::getUserId, userId)
                        .eq(SpeciesBrowseRecord::getSpeciesId, speciesId));

        if (existing != null) {
            // 已有记录：浏览次数+1，更新最后浏览时间
            existing.setBrowseCount(existing.getBrowseCount() + 1);
            existing.setLastBrowsedAt(LocalDateTime.now());
            browseRecordMapper.updateById(existing);
        } else {
            // 新记录
            SpeciesBrowseRecord record = new SpeciesBrowseRecord();
            record.setUserId(userId);
            record.setSpeciesId(speciesId);
            record.setBrowseCount(1);
            record.setLastBrowsedAt(LocalDateTime.now());
            record.setCreatedAt(LocalDateTime.now());
            try {
                browseRecordMapper.insert(record);
            } catch (DuplicateKeyException e) {
                // 并发场景下唯一索引冲突，忽略
                log.debug("用户 {} 浏览物种 {} 记录已存在，跳过插入", userId, speciesId);
            }
        }
    }

    @Override
    public long countDistinctSpecies(Long userId) {
        if (userId == null) return 0;
        return browseRecordMapper.selectCount(
                new LambdaQueryWrapper<SpeciesBrowseRecord>()
                        .eq(SpeciesBrowseRecord::getUserId, userId));
    }
}
