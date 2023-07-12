package com.wecgcm.config;

import cn.hutool.core.net.Ipv4Util;
import cn.hutool.extra.spring.SpringUtil;
import com.github.kiulian.downloader.Config;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author ：wecgwm
 * @date ：2023/07/10 16:57
 */
@Configuration
@Import(SpringUtil.class)
public class YTDownloaderConfig {
    private static final String DEV = "dev";
    private final ThreadPoolExecutor DOWNLOAD_THREAD_POOL = new ThreadPoolExecutor(2, 10, 60, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(5), new ThreadFactoryBuilder().setNameFormat("yt-dl-%d").build());
    @Value("${yt-service.env}")
    private String env;
    @Value("${yt-dl.retry}")
    private int retry;

    {
        Gauge.builder("thread.pool.active.yt.dl", DOWNLOAD_THREAD_POOL::getActiveCount)
                .tag("active", "count")
                .register(Metrics.globalRegistry);
        Gauge.builder("thread.pool.queue.yt.dl", () -> DOWNLOAD_THREAD_POOL.getQueue().size())
                .tag("queue", "size")
                .register(Metrics.globalRegistry);
    }

    @Bean
    public YoutubeDownloader downloader() {
        Config config = new Config.Builder()
                .executorService(DOWNLOAD_THREAD_POOL)
                .maxRetries(retry)
                .build();
        if (DEV.equals(env)) {
            config.setProxy(Ipv4Util.LOCAL_IP, 7890);
        }
        return new YoutubeDownloader(config);
    }
}
