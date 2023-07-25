package com.wecgcm.youtube.service.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wecgcm.youtube.config.ObjectMapperSingleton;
import com.wecgcm.youtube.config.OkHttpClientConfig;
import com.wecgcm.youtube.exception.HttpException;
import com.wecgcm.youtube.model.arg.MinioArg;
import com.wecgcm.youtube.model.dto.ChannelDto;
import com.wecgcm.youtube.model.dto.VideoDto;
import com.wecgcm.youtube.model.req.BilibiliUploadRequest;
import com.wecgcm.youtube.service.MinioService;
import com.wecgcm.youtube.service.YTDLPService;
import com.wecgcm.youtube.service.YouTubeVideoService;
import com.wecgcm.youtube.util.LogUtil;
import com.wecgcm.youtube.util.MetricsUtil;
import io.minio.ObjectWriteResponse;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
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
    private static final String VIDEO_UPLOAD_PATH = "/video/upload";

    @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
    @Value("#{'${yt.channel-id}'.split(',')}")
    private List<Integer> channelIdList;
    @Value("${bilibili-service.url}")
    private String bilibiliServiceUrl;

    private final ThreadPoolExecutor SCAN = new ThreadPoolExecutor(2, 5, 2, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(5), new ThreadFactoryBuilder().setNameFormat("yt-scan-%d").build());
    private final ThreadPoolExecutor DOWNLOAD_AND_UPLOAD = new ThreadPoolExecutor(2, 20, 10, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(5), new ThreadFactoryBuilder().setNameFormat("yt-dl-up-%d").build());

    {
        MetricsUtil.monitor(SCAN, "yt.scan");
        MetricsUtil.monitor(DOWNLOAD_AND_UPLOAD, "yt.dl.up");
    }

    @Override
    public List<CompletableFuture<List<CompletableFuture<Void>>>> scanAsync() {
        return channelIdList.stream().map(channelId ->
                CompletableFuture.completedStage(channelId)
                        .thenApplyAsync(cId -> minioService.readJson(MinioArg.Channel.bucket(), MinioArg.Channel.object(String.valueOf(cId)), ChannelDto.class), SCAN)
                        .thenApply(ytdlpService::search)
                        .thenApply(videoList ->
                                videoList.stream().map(video ->
                                        CompletableFuture.completedStage(video)
                                                .thenComposeAsync(minioService::tryLock)
                                                .thenAcceptAsync(this::uploadVideoTitle, SCAN)
                                                .runAfterBothAsync(this.download(video.getVideoId()), () -> uploadToBilibili(video.getVideoId()), DOWNLOAD_AND_UPLOAD)
                                                .exceptionally(e -> minioService.unlock(video.getVideoId(), e))
                                                .toCompletableFuture()
                                ).toList()
                        )
                        .exceptionally(LogUtil.completionExceptionally())
                        .toCompletableFuture()
        ).toList();
    }

    @Override
    public CompletionStage<ObjectWriteResponse> download(String videoId) {
        return CompletableFuture.completedStage(videoId)
                .thenApplyAsync(ytdlpService::download, DOWNLOAD_AND_UPLOAD)
                .thenApply(filePath -> minioService.upload(MinioArg.Video.bucket(), MinioArg.Video.object(videoId), filePath, MinioArg.VIDEO_TYPE))
                .exceptionally(e -> minioService.unlock(videoId, e));
    }

    private void uploadVideoTitle(VideoDto video){
        minioService.put(MinioArg.Title.bucket(),
                MinioArg.Title.object(video.getVideoId()),
                video.getTitlePrefix() + video.getUploadDate().format(DateTimeFormatter.ofPattern("MM-dd")));
    }

    private void uploadToBilibili(String videoId) {
        String jsonString = Try.success(videoId).map(BilibiliUploadRequest::new)
                .mapTry(req -> ObjectMapperSingleton.INSTANCE.writeValueAsString(req))
                .get();
        Request request = new Request.Builder()
                .url(bilibiliServiceUrl + VIDEO_UPLOAD_PATH)
                .post(RequestBody.create(jsonString, OkHttpClientConfig.JSON))
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
                        throw new HttpException("bilibili resp not 200, body:" + responseBody);
                    }
                    log.info("bilibili resp successful, body{}", Objects.requireNonNull(responseBody).string());
                }
            }
        });
    }

}