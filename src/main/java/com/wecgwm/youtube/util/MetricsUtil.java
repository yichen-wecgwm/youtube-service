package com.wecgwm.youtube.util;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author ：wecgwm
 * @date ：2023/07/21 20:16
 */
public class MetricsUtil {

    public static void threadMonitor(ThreadPoolExecutor executor, String key){
        Gauge.builder("thread.pool.active." + key, executor::getActiveCount)
                .tag("active", "count")
                .register(Metrics.globalRegistry);
        Gauge.builder("thread.pool.queue.size." + key, () -> executor.getQueue().size())
                .tag("queue", "size")
                .register(Metrics.globalRegistry);
        Gauge.builder("thread.pool.task.done." + key, executor::getCompletedTaskCount)
                .tag("task", "done")
                .register(Metrics.globalRegistry);
    }

}
