package com.wecgcm.youtube.service.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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

    @Value("#{'${yt.channel-id}'.split(',')}")
    private List<Integer> channelIdList;

    private final ThreadPoolExecutor SCAN = new ThreadPoolExecutor(2, 5, 2, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(5), new ThreadFactoryBuilder().setNameFormat("yt-scan-%d").build());
    private final ThreadPoolExecutor DOWNLOAD_AND_UPLOAD = new ThreadPoolExecutor(2, 10, 10, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(5), new ThreadFactoryBuilder().setNameFormat("yt-dl-up-%d").build());

    {
        MetricsUtil.monitor(SCAN, "yt.scan");
        MetricsUtil.monitor(DOWNLOAD_AND_UPLOAD, "yt.dl.up");
    }

    @Override
    public void scanAsync(){
        channelIdList.forEach(channelId ->
                        CompletableFuture.completedStage(channelId)
                            .thenApplyAsync(minioService::getChannelInfo, SCAN)
                            .thenApply(ytdlpService::search)
                            .thenAccept(videoList -> {
                                videoList.getVideoIdList().forEach(videoId -> {
                                    CompletableFuture.completedStage(videoId)
                                            .thenAcceptAsync(v -> minioService.uploadTitle(v, videoList.getTitlePrefix()), SCAN)
                                            .runAfterBothAsync(this.download(videoId), () -> uploadToBilibili(videoId),DOWNLOAD_AND_UPLOAD)
                                            .exceptionally(LogUtil.completionExceptionally(Void.class));
                                });
                            })
                            .exceptionally(LogUtil.completionExceptionally(Void.class))
                );
    }

    @Override
    public CompletionStage<ObjectWriteResponse> download(String videoId) {
        return CompletableFuture.completedStage(videoId)
                .thenApplyAsync(ytdlpService::download, DOWNLOAD_AND_UPLOAD)
                .thenApply(minioService::uploadVideo)
                .exceptionally(LogUtil.completionExceptionally(ObjectWriteResponse.class));
    }

    private void uploadToBilibili(String videoId) {

    }
}