package com.wecgwm;

import com.wecgcm.youtube.YoutubeServiceApplication;
import com.wecgcm.youtube.model.arg.MinIOUploadVideoArg;
import com.wecgcm.youtube.service.MinioService;
import com.wecgcm.youtube.service.YTDLPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
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
    private final YTDLPService ytdlpService;
    private final MinioService minioService;
    private final MinIOUploadVideoArg minIOUploadVideoArg;

    @Test
    public void getChannelInfoTest(){
        log.info(minioService.getChannelInfo(1).toString());
    }

    @Test
    public void concurrentUploadTest() throws InterruptedException {
        IntStream.range(0, 10).forEach(i -> {
            new Thread(() -> {
                minioService.uploadTitle("LsrJNUT0eTk", String.valueOf(i));
            }, String.valueOf(i)).start();
        });
        log.info("123");
        Thread.sleep(Duration.of(30, ChronoUnit.HOURS));
    }

    @Test
    public void searchTest() {
        List.of(1).forEach(channelId ->{
                    try {
                        CompletableFuture.completedStage(channelId)
                                        .thenApply(minioService::getChannelInfo)
                                        .thenApply(ytdlpService::search).toCompletableFuture()
                                        .get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

}
