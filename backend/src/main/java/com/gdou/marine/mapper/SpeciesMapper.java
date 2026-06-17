package com.gdou.marine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdou.marine.entity.Species;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 海洋生物物种信息表 Mapper 接口
 * </p>
 *
 * @author codex
 * @since 2026-04-27 00:43:10
 */
@Mapper
public interface SpeciesMapper extends BaseMapper<Species> {

}
