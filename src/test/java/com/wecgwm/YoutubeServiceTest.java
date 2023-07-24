package com.wecgwm;

import com.wecgcm.youtube.YoutubeServiceApplication;
import com.wecgcm.youtube.model.arg.MinioArg;
import com.wecgcm.youtube.model.dto.ChannelDto;
import com.wecgcm.youtube.service.MinioService;
import com.wecgcm.youtube.service.YTDLPService;
import com.wecgcm.youtube.service.YouTubeVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

/**
 * @author ：wecgwm
 * @date ：2023/07/18 0:10
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_= @Autowired)
@SpringBootTest(classes = YoutubeServiceApplication.class)
public class YoutubeServiceTest {
    private final YouTubeVideoService youTubeVideoService;
    private final YTDLPService ytdlpService;
    private final MinioService minioService;

    private static final String videoId = "LsrJNUT0eTk";
    @Test
    public void scanAsyncTest(){
        List<CompletableFuture<Void>> completableFutures = youTubeVideoService.scanAsync();
        while (true) {
            if (completableFutures.stream().allMatch(CompletableFuture::isDone)) {
                break;
            }
        }
    }

    @Test
    public void downloadTest() throws InterruptedException, ExecutionException {
        youTubeVideoService.download(videoId).toCompletableFuture().get();
    }

    @Test
    public void putTitleTest() throws InterruptedException {
        minioService.put(MinioArg.VIDEO_BUCKET_NAME, videoId + MinioArg.SLASH + "title", "[123]" + "0502" + MinioArg.SLASH + videoId);
    }

    @Test
    public void getChannelInfoTest(){
        log.info(minioService.readJson(MinioArg.CHANNEL_BUCKET_NAME, "1", ChannelDto.class).toString());
    }

    @Test
    public void searchTest() {
        List.of(1).forEach(channelId ->{
                    try {
                        CompletableFuture.completedStage(channelId)
                                        .thenApply(cId -> minioService.readJson(MinioArg.CHANNEL_BUCKET_NAME, String.valueOf(cId), ChannelDto.class))
                                        .thenApply(ytdlpService::search).toCompletableFuture()
                                        .get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @Test
    public void statObjectTest() {
        log.info(String.valueOf(minioService.statObject(MinioArg.VIDEO_BUCKET_NAME, "AdLrsE9d9aI/title")));
        log.info(String.valueOf(minioService.statObject(MinioArg.VIDEO_BUCKET_NAME, "AdLrsE9d9aI/title2")));
    }

    @Test
    public void getUploadDateTest() {
        LocalDateTime uploadDate = ytdlpService.getUploadDate(videoId);
        System.out.println(uploadDate);
    }

    @Test
    public void concurrentUploadTest() throws InterruptedException {
        IntStream.range(0, 10).forEach(i -> {
            new Thread(() -> {
                //minioService.put("LsrJNUT0eTk", String.valueOf(i));
            }, String.valueOf(i)).start();
        });
        log.info("123");
        Thread.sleep(Duration.of(30, ChronoUnit.HOURS));
    }

}
