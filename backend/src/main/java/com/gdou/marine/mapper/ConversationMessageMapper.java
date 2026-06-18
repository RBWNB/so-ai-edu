package com.gdou.marine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdou.marine.entity.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/17
 * @Description
 */
@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {

    default List<ConversationMessage> selectBySessionId(String sessionId) {
        return selectList(new LambdaQueryWrapper<ConversationMessage>()
                .eq(ConversationMessage::getSessionId, sessionId)
                .orderByAsc(ConversationMessage::getCreatedAt));
    }

    default List<ConversationMessage> selectRecentBySessionId(String sessionId, int limit) {
        List<ConversationMessage> messages = selectList(new LambdaQueryWrapper<ConversationMessage>()
                .eq(ConversationMessage::getSessionId, sessionId)
                .orderByDesc(ConversationMessage::getCreatedAt)
                .last("LIMIT " + Math.max(1, limit)));
        Collections.reverse(messages);
        return messages;
    }
}
