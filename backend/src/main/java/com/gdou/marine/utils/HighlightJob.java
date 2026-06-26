package com.gdou.marine.utils;

import com.gdou.marine.service.HighlightBroadcastService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/26
 * @Description Quartz 任务调度配置
 */
@Component
@DisallowConcurrentExecution // 防止任务并发执行
public class HighlightJob implements Job{
    @Autowired
    private HighlightBroadcastService highlightBroadcastService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        highlightBroadcastService.generateAndSendHighlight();
    }
}
