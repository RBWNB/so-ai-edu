package com.gdou.marine.utils;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.stereotype.Component;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/26
 * @Description 雪花 ID 生成器，供 JDBC 直连 INSERT 使用
 */
@Component
public class SnowflakeIdGenerator {

    /**
     * 生成一个全局唯一的雪花 ID
     */
    public long nextId() {
        return IdWorker.getId();
    }
}
