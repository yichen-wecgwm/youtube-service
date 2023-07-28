package com.wecgwm;

import com.wecgwm.youtube.YoutubeServiceApplication;
import com.wecgwm.youtube.model.arg.MinioArg;
import com.wecgwm.youtube.model.dto.ChannelDto;
import com.wecgwm.youtube.service.MinioService;
import com.wecgwm.youtube.service.YTDLPService;
import com.wecgwm.youtube.service.impl.YouTubeVideoServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ReflectionUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
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
    public void scanAsyncTest(){
        IntStream.range(0, 3).forEach(__ -> {
            List<CompletableFuture<List<CompletableFuture<String>>>> completableFutures = youTubeVideoServiceImpl.scanAsync();
            while (true) {
                if (completableFutures.stream().allMatch(CompletableFuture::isDone)
                        && completableFutures.stream().map(CompletableFuture::resultNow).filter(Objects::nonNull).flatMap(List::stream).allMatch(CompletableFuture::isDone)) {
                    break;
                }
            }
            try {
                Thread.sleep(Duration.of(3, ChronoUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("should be cached............");
        });
    }

    @Test
    public void tempTest() throws InterruptedException, ExecutionException {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        List<Integer> twoEvenSquares = numbers.stream().filter(n -> {
            System.out.println("filtering " + n);
            return n % 2 == 0;
        }).map(n -> {
            System.out.println("mapping " + n);
            return n * n;
        }).collect(Collectors.toList());


        for(Integer i : twoEvenSquares)
        {
            System.out.println(i);
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
