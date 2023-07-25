package com.wecgwm;

import com.wecgcm.youtube.YoutubeServiceApplication;
import com.wecgcm.youtube.model.arg.MinioArg;
import com.wecgcm.youtube.model.dto.ChannelDto;
import com.wecgcm.youtube.service.MinioService;
import com.wecgcm.youtube.service.YTDLPService;
import com.wecgcm.youtube.service.impl.YouTubeVideoServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author ：wecgwm
 * @date ：2023/07/18 0:10
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_= @Autowired)
@SpringBootTest(classes = YoutubeServiceApplication.class)
public class YoutubeServiceTest {
    private final YouTubeVideoServiceImpl youTubeVideoServiceImpl;
    private final YTDLPService ytdlpService;
    private final MinioService minioService;

    private static final String videoId = "LsrJNUT0eTk";
    @Test
    public void scanAsyncTest(){
        List<CompletableFuture<List<CompletableFuture<Void>>>> completableFutures = youTubeVideoServiceImpl.scanAsync();
        while (true) {
            if (completableFutures.stream().allMatch(CompletableFuture::isDone)
                    && completableFutures.stream().map(CompletableFuture::resultNow).filter(Objects::nonNull).flatMap(List::stream).allMatch(CompletableFuture::isDone)) {
                break;
            }
        }
    }

    @Test
    public void downloadTest() throws InterruptedException, ExecutionException {
        youTubeVideoServiceImpl.download(videoId).toCompletableFuture().get();
    }

    @Test
    public void putTitleTest() throws InterruptedException {
        minioService.put(MinioArg.Title.bucket(), MinioArg.Title.object(videoId), "from unit test");
    }

    @Test
    public void removeTest() throws InterruptedException {
        minioService.remove(MinioArg.Title.bucket(), MinioArg.Title.object(videoId));
    }

    @Test
    public void getChannelInfoTest(){
        log.info(minioService.readJson(MinioArg.Channel.bucket(), MinioArg.Channel.object("1"), ChannelDto.class).toString());
    }

    @Test
    public void searchTest() {
        List.of(1).forEach(channelId ->{
                    try {
                        CompletableFuture.completedStage(channelId)
                                        .thenApply(cId -> minioService.readJson(MinioArg.Channel.bucket(), MinioArg.Channel.object(String.valueOf(cId)), ChannelDto.class))
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
        log.info(String.valueOf(minioService.statObject(MinioArg.Title.bucket(), "AdLrsE9d9aI/title")));
        log.info(String.valueOf(minioService.statObject(MinioArg.Title.bucket(), "AdLrsE9d9aI/title2")));
    }

    @Test
    public void getUploadDateTest() {
        LocalDateTime uploadDate = LocalDate.parse(ytdlpService.getVideoInfo(videoId, "upload_date"), DateTimeFormatter.ofPattern("yyyyMMdd")).atStartOfDay();
        System.out.println(uploadDate);
    }

    @Test
    public void bilibiliUploadTest() throws InterruptedException {
        Method uploadToBilibili = ReflectionUtils.findMethod(YouTubeVideoServiceImpl.class, "uploadToBilibili", String.class);
        Objects.requireNonNull(uploadToBilibili).setAccessible(true);
        ReflectionUtils.invokeMethod(uploadToBilibili, youTubeVideoServiceImpl, videoId);
        Thread.sleep(Duration.of(30, ChronoUnit.SECONDS));
    }

}
