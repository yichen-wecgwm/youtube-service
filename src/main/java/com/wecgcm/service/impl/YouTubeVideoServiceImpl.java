package com.wecgcm.service.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wecgcm.exception.ProcessException;
import com.wecgcm.service.MinioService;
import com.wecgcm.service.YouTubeVideoService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;


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
    private static final String DEV = "dev";
    public static final String PROXY_DEV = "127.0.0.1:7890";
    public static final String PROXY_NONE = "\"\"";
    public static final String PROXY_OP = "--proxy";
    public static final String FORMAT_OP = "-f";
    public static final String FORMAT = "bestvideo*+bestaudio/best";
    public static final String QUIET_OP = "--quiet";
    public static final String OUT_PUT_OP = "-o";
    public static final String OUT_PUT = "-";
    private final MinioService minioService;
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
        CompletableFuture.supplyAsync(() -> {
                    String proxy = env.equals(DEV) ? PROXY_DEV : PROXY_NONE;
                    List<String> args = ImmutableList.<String>builder()
                            .add(ytDLP)
                            .add(PROXY_OP)
                            .add(proxy)
                            .add(FORMAT_OP)
                            .add(FORMAT)
                            .add(QUIET_OP)
                            .add(OUT_PUT_OP)
                            .add(OUT_PUT)
                            .add(YOUTUBE_VIDEO_URL_PREFIX + videoId)
                            .build();
                    log.info(String.join(" ", args));
                    try {
                        return minioService.upload(videoId, new ProcessBuilder(args).start());
                    } catch (IOException e) {
                        throw new ProcessException("start process fail by io exception", e);
                    }
                }, DOWNLOAD_AND_UPLOAD_THREAD_POOL)
                .whenComplete((result, e) -> {
                    // todo http call
                    log.info("videoId: {}", result);
                    if (e != null) {
                        log.error("{}, cause :{}", e, e.getCause());
                        throw new RuntimeException(e);
                    }
                });
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

}
