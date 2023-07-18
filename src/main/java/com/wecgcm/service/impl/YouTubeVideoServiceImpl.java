package com.wecgcm.service.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wecgcm.exception.ProcessException;
import com.wecgcm.exception.handler.YoutubeExceptionHandler;
import com.wecgcm.service.MinioService;
import com.wecgcm.service.YouTubeVideoService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
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
    private static final String YOUTUBE_VIDEO_URL_PREFIX = "https://www.youtube.com/watch?v=";
    public static final String VIDEO_EXT = ".webm";
    public static final String OUT_PUT_DIR = "videos/";
    private static final String DEV = "dev";
    private static final String FORMAT_OP = "-f";
    private static final String FORMAT = "bestvideo*+bestaudio/best";
    private static final String QUIET_OP = "--quiet";
    private static final String OUT_PUT_OP = "-o";
    private static final String ERROR = ".error";
    private final MinioService minioService;
    private final YoutubeExceptionHandler exceptionHandler;
    @Value("${yt-dlp.path}")
    private String ytDLP;
    @Value("${spring.profiles.active}")
    private String env;

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
        ImmutableList.Builder<String> builder = ImmutableList.<String>builder()
                .add(ytDLP)
                .add(FORMAT_OP)
                .add(FORMAT)
                .add(OUT_PUT_OP)
                .add(OUT_PUT_DIR + videoId + VIDEO_EXT)
                .add(YOUTUBE_VIDEO_URL_PREFIX + videoId);

        List<String> args = builder.build();
        log.info(String.join(" ", args));
        CompletableFuture.supplyAsync(() -> {
                    Timer.Sample timer = Timer.start();
                    ProcessBuilder processBuilder = new ProcessBuilder(args);
                    processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                    Process process = null;
                    try {
                        process = processBuilder.start();
                        process.waitFor();
                        if (process.exitValue() != 0) {
                            FileCopyUtils.copy(FileCopyUtils.copyToByteArray(process.getErrorStream()), new File(videoId + ERROR));
                            throw new ProcessException("process exit error");
                        }
                    } catch (IOException | InterruptedException e) {
                        throw new ProcessException("yt-dlp process exception", e);
                    } finally {
                        assert process != null;
                        process.destroy();
                    }
                    timer.stop(Timer.builder("yt-dlp-dl")
                            .register(Metrics.globalRegistry));
                    return videoId;
                }, DOWNLOAD_AND_UPLOAD_THREAD_POOL)
                .whenComplete((result, e) -> {
                    // todo http call
                    log.info("videoId: {}", result);
                    if (e != null) {
                        throw new RuntimeException(e);
                    }
                    minioService.upload(result);
                }).exceptionally(e -> exceptionHandler.lastHandler(e).getData());
    }

    /**
     * yt-dlp -I :10 --print id --flat-playlist https://www.youtube.com/@15ya.fullmoon/videos
     * yt-dlp --flat-playlist --print upload_date https://www.youtube.com/watch?v=30_Z-81k5Aw
     *
     * @param query
     * @return
     */
    @Override
    public List<String> search(String query) {
        return Collections.emptyList();
    }

    /**
     * wget https://github.com/biliup/biliup-rs/releases/download/v0.1.17/biliupR-v0.1.17-x86_64-linux.tar.xz
     * apt-get update && apt-get install xz-utils
     * tar -xf biliupR-v0.1.17-x86_64-linux.tar.xz
     * ./biliup login
     * ./biliup upload --copyright 2 --tid 71 --tag "SAKURA" --source "https://www.youtube.com/watch?v=85uATsLB19A" --line bda2 --limit 5 --title "real title123" 85uATsLB19A.webm &> 1.txt
     */

}
