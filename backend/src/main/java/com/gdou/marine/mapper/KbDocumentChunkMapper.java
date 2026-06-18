package com.gdou.marine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdou.marine.entity.KbDocumentChunk;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/17
 * @Description
 */
@Mapper
public interface KbDocumentChunkMapper extends BaseMapper<KbDocumentChunk> {

        default List<KbDocumentChunk> selectByDocumentId(Long documentId) {
            return selectList(new LambdaQueryWrapper<KbDocumentChunk>()
                    .eq(KbDocumentChunk::getDocumentId, documentId)
                    .orderByAsc(KbDocumentChunk::getChunkIndex));
        }
}
