package com.gdou.marine.config;

import com.gdou.marine.utils.HighlightJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/26
 * @Description Quartz 任务调度配置
 */
@Configuration
public class QuartzConfig {
    @Bean
    public JobDetail highlightJobDetail() {
        return JobBuilder.newJob(HighlightJob.class)
                .withIdentity("highlightJob", "marine-jobs")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger highlightJobTrigger() {
        // 每天早上 9:00 触发
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 0 9 * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(highlightJobDetail())
                .withIdentity("highlightTrigger", "marine-triggers")
                .withSchedule(scheduleBuilder)
                .build();
    }
}
