package com.wecgwm.youtube.service.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wecgwm.youtube.config.ObjectMapperSingleton;
import com.wecgwm.youtube.config.OkHttpClientConfig;
import com.wecgwm.youtube.exception.HttpException;
import com.wecgwm.youtube.exception.LockException;
import com.wecgwm.youtube.model.arg.MinioArg;
import com.wecgwm.youtube.model.arg.YTDLPDownloadArg;
import com.wecgwm.youtube.model.dto.ChannelDto;
import com.wecgwm.youtube.model.dto.VideoDto;
import com.wecgwm.youtube.model.req.BilibiliUploadRequest;
import com.wecgwm.youtube.service.MinioService;
import com.wecgwm.youtube.service.YTDLPService;
import com.wecgwm.youtube.service.YouTubeVideoService;
import com.wecgwm.youtube.util.LogUtil;
import com.wecgwm.youtube.util.MetricsUtil;
import io.minio.ObjectWriteResponse;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;


/**
 * @author ：wecgwm
 * @date ：2023/07/10 17:17
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class YouTubeVideoServiceImpl implements YouTubeVideoService {
    private final YTDLPService ytdlpService;
    private final MinioService minioService;
    private final OkHttpClient okHttpClient;
    private static final String BILIBILI_VIDEO_UPLOAD_PATH = "/video/upload";

    @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
    @Value("#{'${yt.channel-id}'.split(',')}")
    private List<Integer> channelIdList;
    @Value("${bilibili-service.url}")
    private String bilibiliServiceUrl;

    private @Getter static final ThreadPoolExecutor SCAN = new ThreadPoolExecutor(5, 10, 2, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(20), new ThreadFactoryBuilder().setNameFormat("yt-scan-%d").build());
    private static final ThreadPoolExecutor DOWNLOAD_AND_UPLOAD = new ThreadPoolExecutor(5, 20, 10, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(5), new ThreadFactoryBuilder().setNameFormat("yt-dl-up-%d").build());

    static {
        MetricsUtil.threadMonitor(SCAN, "yt.scan");
        MetricsUtil.threadMonitor(DOWNLOAD_AND_UPLOAD, "yt.dl.up");
    }

    @Override
    public List<CompletableFuture<List<CompletableFuture<String>>>> scanAsync() {
        return channelIdList.stream().map(channelId ->
                CompletableFuture.completedStage(channelId)
                        .thenApplyAsync(cId -> minioService.readJson(MinioArg.Channel.bucket(), MinioArg.Channel.object(String.valueOf(cId)), ChannelDto.class), SCAN)
                        .thenCompose(ytdlpService::search)
                        .thenApply(videoList -> videoList.stream().map(this::processVideoAsync).toList())
                        .exceptionally(LogUtil.completionExceptionally())
                        .toCompletableFuture()
        ).toList();
    }

    @Override
    public CompletionStage<ObjectWriteResponse> download(String videoId) {
        return CompletableFuture.completedStage(videoId)
                .thenAcceptAsync(ytdlpService::download, DOWNLOAD_AND_UPLOAD)
                .thenApply(__ -> minioService.upload(MinioArg.Video.bucket(), MinioArg.Video.object(videoId), YTDLPDownloadArg.videoPath(videoId), MinioArg.VIDEO_TYPE))
                .thenApply(__ -> minioService.upload(MinioArg.Thumbnail.bucket(), MinioArg.Thumbnail.object(videoId), YTDLPDownloadArg.thumbnailPath(videoId), MinioArg.IMG_TYPE))
                .exceptionally(e -> minioService.unlock(videoId, e));
    }

    private CompletableFuture<String> processVideoAsync(VideoDto video) {
        return CompletableFuture.completedStage(video)
                .thenComposeAsync(minioService::tryLock, DOWNLOAD_AND_UPLOAD)
                .thenCompose(videoDto -> CompletableFuture.completedStage(videoDto)
                        .thenAccept(this::uploadVideoTitle)
                        .thenCombine(this.download(videoDto.getVideoId()), (__, ___) -> uploadToBilibili(video.getVideoId()))
                        .exceptionally(e -> minioService.unlock(video.getVideoId(), e))
                )
                .exceptionally(e -> {
                    if (e.getCause() != null && e.getCause() instanceof LockException) {
                        return video.getVideoId();
                    }
                    return LogUtil.<String>completionExceptionally().apply(e);
                })
                .toCompletableFuture();
    }

    private void uploadVideoTitle(VideoDto video) {
        minioService.put(MinioArg.Title.bucket(),
                MinioArg.Title.object(video.getVideoId()),
                video.getTitle());
    }

    private String uploadToBilibili(String videoId) {
        String jsonRequestBody = Try.success(videoId).map(BilibiliUploadRequest::new)
                .mapTry(req -> ObjectMapperSingleton.INSTANCE.writeValueAsString(req))
                .get();
        Request request = new Request.Builder()
                .url(bilibiliServiceUrl + BILIBILI_VIDEO_UPLOAD_PATH)
                .post(RequestBody.create(jsonRequestBody, OkHttpClientConfig.JSON))
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                LogUtil.recordOnExceptionHandler(Thread.currentThread(), e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        throw new HttpException("bilibili service Unexpected code: " + response);
                    }
                    log.info("bilibili resp successful, body{}", Objects.requireNonNull(responseBody).string());
                }
            }
        });
        return videoId;
    }

}