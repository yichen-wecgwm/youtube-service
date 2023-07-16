package com.wecgcm.downloader;

import cn.hutool.core.text.StrPool;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wecgcm.exception.DownloadException;
import com.wecgcm.service.MinioService;
import com.wecgcm.util.YoutubeFileUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static com.wecgcm.util.YoutubeFileUtil.OUT_PUT_DIR;
import static com.wecgcm.util.YoutubeFileUtil.VIDEO_EXT;

/**
 * @author ：wecgwm
 * @date ：2023/07/11 19:14
 */
@SuppressWarnings("SpellCheckingInspection")
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Component
public class YTDownloaderCallback implements YoutubeCallback<File> {
    private static final String FFMPEG_NOT_CODE = "copy";
    private final Map<String, String> BEST_VIDEO_ID_MAP = new HashMap<>();
    private final FFmpegExecutor fFmpegExecutor;
    private final MinioService minioService;
    private final ThreadPoolExecutor FFMPEG_THREAD_POOL = new ThreadPoolExecutor(4, 4, 60, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(5), new ThreadFactoryBuilder().setNameFormat("ffmepg-merge-%d").build());

    {
        Gauge.builder("thread.pool.active.ffmpeg", FFMPEG_THREAD_POOL::getActiveCount)
                .tag("active", "count")
                .register(Metrics.globalRegistry);
        Gauge.builder("thread.pool.queue.ffmpeg", () -> FFMPEG_THREAD_POOL.getQueue().size())
                .tag("queue", "size")
                .register(Metrics.globalRegistry);
    }

    @Override
    public void onFinished(File file) {
        String videoId = YoutubeFileUtil.getVideoID(file);
        // check all done
        if (!BEST_VIDEO_ID_MAP.containsKey(videoId)) {
            BEST_VIDEO_ID_MAP.put(videoId, file.getPath());
            return;
        }
        File anthorFile = new File(BEST_VIDEO_ID_MAP.remove(videoId));
        // merge
        String filePath = OUT_PUT_DIR + StrPool.SLASH + videoId + VIDEO_EXT;
        FFmpegBuilder fFmpegBuilder = new FFmpegBuilder()
                .addInput(file.getPath())
                .addInput(anthorFile.getPath())
                .addOutput(filePath)
                .setAudioCodec(FFMPEG_NOT_CODE)
                .setVideoCodec(FFMPEG_NOT_CODE)
                .done();
        // upload and delete
        CompletableFuture.runAsync(fFmpegExecutor.createJob(fFmpegBuilder), FFMPEG_THREAD_POOL)
                .whenCompleteAsync((__, e) -> {
                            // upload to minio
                            minioService.upload(videoId);
                            // delete
                            Stream.of(file, anthorFile, new File(filePath)).filter(f -> !f.delete()).forEach(f -> {
                                Counter.builder("downloader_callback")
                                        .tag("file", "delete_fail")
                                        .register(Metrics.globalRegistry)
                                        .increment();
                                log.warn("downloader callback: file delete fail, name:{}", f.getPath());
                            });
                        }
                        , FFMPEG_THREAD_POOL);
    }

    @Override
    public void onError(Throwable throwable) {
        Counter.builder("video_download")
                .tag("on", "error")
                .register(Metrics.globalRegistry)
                .increment();
        throw new DownloadException("video_download_on_error", throwable);
    }
}
