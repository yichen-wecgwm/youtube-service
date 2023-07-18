package com.wecgcm.service.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wecgcm.exception.ProcessException;
import com.wecgcm.exception.handler.YoutubeExceptionHandler;
import com.wecgcm.model.YTDownloadArg;
import com.wecgcm.service.MinioService;
import com.wecgcm.service.YouTubeVideoService;
import com.wecgcm.util.LogUtil;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Getter
@Service
public class YouTubeVideoServiceImpl implements YouTubeVideoService {
    private final YTDownloadArg ytDownloadArg;
    private final MinioService minioService;
    private final YoutubeExceptionHandler exceptionHandler;
    private final ThreadPoolExecutor DOWNLOAD_AND_UPLOAD_THREAD_POOL = new ThreadPoolExecutor(2, 10, 60, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(5), new ThreadFactoryBuilder().setNameFormat("yt-dl-up-%d").build());

    {
        Gauge.builder("thread.pool.active.yt.dl.up", DOWNLOAD_AND_UPLOAD_THREAD_POOL::getActiveCount)
                .tag("active", "count")
                .register(Metrics.globalRegistry);
        Gauge.builder("thread.pool.queue.yt.dl.up", () -> DOWNLOAD_AND_UPLOAD_THREAD_POOL.getQueue().size())
                .tag("queue", "size")
                .register(Metrics.globalRegistry);
    }

    @Override
    public void download(String videoId) {
        CompletableFuture.completedStage(videoId)
                .thenApplyAsync(this::downloadVideo, DOWNLOAD_AND_UPLOAD_THREAD_POOL)
                .thenAccept(minioService::uploadVideo)
                .exceptionally(e -> {
                    exceptionHandler.lastHandler(e);
                    return new CompletableFuture<Void>().resultNow();
                });
    }

    // TODO yt-dlp -I :10 --print id --flat-playlist https://www.youtube.com/@15ya.fullmoon/videos   / yt-dlp --flat-playlist --print upload_date https://www.youtube.com/watch?v=30_Z-81k5Aw
    @Override
    public List<String> search(String channel) {
        return Collections.emptyList();
    }

    private String downloadVideo(String videoId) {
        List<String> args = ytDownloadArg.build(videoId);
        Timer.Sample timer = Timer.start();

        ProcessBuilder processBuilder = new ProcessBuilder(args)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process process = Try.of(processBuilder::start)
                .getOrElseThrow(ProcessException::new);
        Try.success(process)
                .andThenTry(Process::waitFor)
                .filter(p -> p.exitValue() == 0, p -> {
                    LogUtil.error(process.getErrorStream(), this.getClass());
                    return new ProcessException("process exit error");
                })
                .andFinally(process::destroy)
                .getOrElseThrow(ProcessException::new);

        timer.stop(Timer.builder("yt-dlp-dl")
                .register(Metrics.globalRegistry));
        log.info("download done, videoId: {}", videoId);
        return videoId;
    }

}
