package com.wecgcm.youtube.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wecgcm.youtube.util.LogUtil;
import com.wecgcm.youtube.util.MetricsUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author ：wecgwm
 * @date ：2023/07/21 16:34
 */
public class SpringScheduleConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskExecutor() {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2,
                new ThreadFactoryBuilder()
                        .setNameFormat("yt-schedule-%d")
                        .setUncaughtExceptionHandler(LogUtil::recordOnExceptionHandler)
                        .build());
        MetricsUtil.threadMonitor(scheduledThreadPoolExecutor, "yt.schedule");
        return scheduledThreadPoolExecutor;
    }

}
