package com.wecgcm.youtube.service.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wecgcm.youtube.service.MinioService;
import com.wecgcm.youtube.service.YTDLPService;
import com.wecgcm.youtube.service.YouTubeVideoService;
import com.wecgcm.youtube.util.LogUtil;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @author ：wecgwm
 * @date ：2023/07/10 17:17
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class YouTubeVideoServiceImpl implements YouTubeVideoService {
    private final YTDLPService ytdlpService;
    private final MinioService minioService;
    private final ThreadPoolExecutor DOWNLOAD_AND_UPLOAD_THREAD_POOL = new ThreadPoolExecutor(2, 10, 60, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(5), new ThreadFactoryBuilder().setNameFormat("yt-video-%d").build());

    {
        Gauge.builder("thread.pool.active.yt.video", DOWNLOAD_AND_UPLOAD_THREAD_POOL::getActiveCount)
                .tag("active", "count")
                .register(Metrics.globalRegistry);
        Gauge.builder("thread.pool.queue.yt.video", () -> DOWNLOAD_AND_UPLOAD_THREAD_POOL.getQueue().size())
                .tag("queue", "size")
                .register(Metrics.globalRegistry);
    }

    @Override
    public void download(String videoId) {
        CompletableFuture.completedStage(videoId)
                .thenApplyAsync(ytdlpService::download, DOWNLOAD_AND_UPLOAD_THREAD_POOL)
                .thenAccept(minioService::uploadVideo)
                .exceptionally(e -> {
                    LogUtil.recordOnExceptionHandler(Thread.currentThread(), e);
                    return new CompletableFuture<Void>().resultNow();
                });
    }

    // TODO yt-dlp -I :10 --print id --flat-playlist https://www.youtube.com/@15ya.fullmoon/videos   / yt-dlp --flat-playlist --print upload_date https://www.youtube.com/watch?v=30_Z-81k5Aw
    @Override
    public List<String> search(String channel) {
        return Collections.emptyList();
    }

}