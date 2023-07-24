package com.wecgcm.youtube.service.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wecgcm.youtube.model.arg.MinioArg;
import com.wecgcm.youtube.model.arg.YTDLPArg;
import com.wecgcm.youtube.model.dto.ChannelDto;
import com.wecgcm.youtube.service.MinioService;
import com.wecgcm.youtube.service.YTDLPService;
import com.wecgcm.youtube.service.YouTubeVideoService;
import com.wecgcm.youtube.util.LogUtil;
import com.wecgcm.youtube.util.MetricsUtil;
import io.minio.ObjectWriteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;


/**
 * @author ：wecgwm
 * @date ：2023/07/10 17:17
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class YouTubeVideoServiceImpl implements YouTubeVideoService {
    private final YTDLPService ytdlpService;
    private final MinioService minioService;
    private static final String TITLE = "title";

    @Value("#{'${yt.channel-id}'.split(',')}")
    private List<Integer> channelIdList;

    private final ThreadPoolExecutor SCAN = new ThreadPoolExecutor(2, 5, 2, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(5), new ThreadFactoryBuilder().setNameFormat("yt-scan-%d").build());
    private final ThreadPoolExecutor DOWNLOAD_AND_UPLOAD = new ThreadPoolExecutor(2, 20, 10, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(5), new ThreadFactoryBuilder().setNameFormat("yt-dl-up-%d").build());

    {
        MetricsUtil.monitor(SCAN, "yt.scan");
        MetricsUtil.monitor(DOWNLOAD_AND_UPLOAD, "yt.dl.up");
    }

    @Override
    public List<CompletableFuture<Void>> scanAsync() {
        return channelIdList.stream().map(channelId ->
                CompletableFuture.completedStage(channelId)
                        .thenApplyAsync(cId -> minioService.readJson(MinioArg.CHANNEL_BUCKET_NAME, cId + MinioArg.JSON_EXT, ChannelDto.class), SCAN)
                        .thenApply(ytdlpService::search)
                        .thenAccept(videoList -> {
                            videoList.forEach(video -> {
                                CompletableFuture
                                        .runAsync(() -> minioService.put(MinioArg.VIDEO_BUCKET_NAME,
                                                video.getVideoId() + MinioArg.SLASH + TITLE,
                                                video.getTitlePrefix() + video.getUploadDate().format(DateTimeFormatter.ofPattern("MM-dd"))), SCAN)
                                        .runAfterBothAsync(this.download(video.getVideoId()), () -> uploadToBilibili(video.getVideoId()), DOWNLOAD_AND_UPLOAD)
                                        .exceptionally(LogUtil.completionExceptionally(Void.class));
                            });
                        })
                        .exceptionally(LogUtil.completionExceptionally(Void.class))
                        .toCompletableFuture()
        ).toList();
    }

    @Override
    public CompletionStage<ObjectWriteResponse> download(String videoId) {
        return CompletableFuture.completedStage(videoId)
                .thenApplyAsync(ytdlpService::download, DOWNLOAD_AND_UPLOAD)
                .thenApply(filePath -> minioService.upload(MinioArg.VIDEO_BUCKET_NAME, videoId + MinioArg.SLASH + videoId + YTDLPArg.VIDEO_EXT ,filePath, MinioArg.VIDEO_TYPE))
                .exceptionally(LogUtil.completionExceptionally(ObjectWriteResponse.class));
    }


    private void uploadToBilibili(String videoId) {
        // todo
    }

}