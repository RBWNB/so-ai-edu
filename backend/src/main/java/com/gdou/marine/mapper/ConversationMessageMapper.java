package com.gdou.marine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdou.marine.dto.ChatSessionDTO;
import com.gdou.marine.entity.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 查询指定用户的所有历史会话列表，直接用 DTO 接收返回值
     */
    @Select("""
        SELECT 
            c1.session_id AS sessionId, 
            MIN(c1.created_at) AS startTime,
            (SELECT content FROM conversation_message c2 
             WHERE c2.session_id = c1.session_id AND c2.role = 'user' 
             ORDER BY created_at ASC LIMIT 1) AS title
        FROM conversation_message c1
        WHERE c1.user_id = #{userId}
        GROUP BY c1.session_id
        ORDER BY startTime DESC
    """)
    List<ChatSessionDTO> selectSessionList(@Param("userId") Long userId);
}
