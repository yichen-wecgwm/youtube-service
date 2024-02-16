package com.wecgwm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.wecgwm.youtube.YoutubeServiceApplication;
import com.wecgwm.youtube.config.ObjectMapperSingleton;
import com.wecgwm.youtube.model.arg.minio.MinioChannelArg;
import com.wecgwm.youtube.model.arg.minio.MinioVideoInfoArg;
import com.wecgwm.youtube.model.dto.ChannelDto;
import com.wecgwm.youtube.model.dto.VideoInfoDto;
import com.wecgwm.youtube.service.MinioService;
import com.wecgwm.youtube.service.YTDLPService;
import com.wecgwm.youtube.service.impl.YouTubeVideoServiceImpl;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.IntStream;

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
    public void scanAsyncTest() throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        IntStream.range(0, 1).forEach(__ -> {
            stopWatch.start(String.valueOf(__));
            CompletableFuture<List<CompletableFuture<List<CompletableFuture<String>>>>> ret = youTubeVideoServiceImpl.scanAsync();
            while (true) {
                if (ret.isDone() && ret.resultNow().stream().allMatch(CompletableFuture::isDone)
                    && ret.resultNow().stream().map(CompletableFuture::resultNow).filter(Objects::nonNull).flatMap(List::stream).allMatch(CompletableFuture::isDone)) {
                    stopWatch.stop();
                    break;
                }
            }
//            try {
//                Thread.sleep(Duration.of(3, ChronoUnit.SECONDS));
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
            log.info("should be cached............");
        });
        Thread.sleep(Duration.ofMinutes(30));
        log.info(stopWatch.prettyPrint());
    }

    @Test
    public void tempTest() throws InterruptedException, ExecutionException, JsonProcessingException {
        log.info(ObjectMapperSingleton.INSTANCE.writeValueAsString(new VideoInfoDto("123", "356", LocalDate.now(), List.of("1", "5"))));
    }

    @Test
    public void downloadTest() throws InterruptedException, ExecutionException {
        youTubeVideoServiceImpl.download(videoId).toCompletableFuture().get();
    }

    @Test
    public void putTitleTest() throws InterruptedException {
        minioService.put(MinioVideoInfoArg.bucket(), MinioVideoInfoArg.object(videoId), "from unit test");
    }

    @Test
    public void removeTest() throws InterruptedException {
        minioService.remove(MinioVideoInfoArg.bucket(), MinioVideoInfoArg.object(videoId));
    }

    @Test
    public void getChannelInfoTest() {
        log.info(minioService.readJson(MinioChannelArg.bucket(), "1.json", ChannelDto.class).toString());
        log.info(minioService.readJson(MinioChannelArg.bucket(), MinioChannelArg.object(), new TypeReference<List<ChannelDto>>() {
        }).toString());
    }

    @Test
    public void searchTest() {
        List.of(1).forEach(channelId ->
                Try.run(() ->
                        CompletableFuture.completedStage(channelId)
                                .thenApply(cId -> minioService.readJson(MinioChannelArg.bucket(), MinioChannelArg.object(), ChannelDto.class))
                                .thenApply(ytdlpService::search).toCompletableFuture()
                                .get()
                )
        );
    }

    @Test
    public void statObjectTest() {
        log.info(String.valueOf(minioService.statObject(MinioVideoInfoArg.bucket(), "AdLrsE9d9aI/title")));
        log.info(String.valueOf(minioService.statObject(MinioVideoInfoArg.bucket(), "AdLrsE9d9aI/title2")));
    }

    @Test
    public void getVideoInfoTest() throws IOException {
        byte[] bytes = "PSY - GANGNAM STYLE (강남스타일) M/V".getBytes();
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(bytes));
        BufferedReader bufferedReader = new BufferedReader(reader);
        String str = bufferedReader.readLine();
        System.out.println(str);

        InputStreamReader reader1 = new InputStreamReader(System.in);
        System.out.println(reader1.getEncoding());
        List<String> videoInfo = ytdlpService.getVideoInfo(videoId, "upload_date", "title");
        System.out.println(videoInfo);
    }

    @Test
    public void bilibiliUploadTest() throws InterruptedException {
        Method uploadToBilibili = ReflectionUtils.findMethod(YouTubeVideoServiceImpl.class, "uploadToBilibili", String.class);
        Objects.requireNonNull(uploadToBilibili).setAccessible(true);
        ReflectionUtils.invokeMethod(uploadToBilibili, youTubeVideoServiceImpl, videoId);
        Thread.sleep(Duration.of(30, ChronoUnit.SECONDS));
    }

}
