package com.wecgwm.youtube.service.impl;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wecgwm.youtube.exception.YTDLPException;
import com.wecgwm.youtube.model.arg.YTDLPDownloadArg;
import com.wecgwm.youtube.model.arg.YTDLPSearchArg;
import com.wecgwm.youtube.model.arg.YTDLPVideoPrintArg;
import com.wecgwm.youtube.model.dto.ChannelDto;
import com.wecgwm.youtube.model.dto.VideoDto;
import com.wecgwm.youtube.service.YTDLPService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.cache.CaffeineStatsCounter;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;


/**
 * @author ：wecgwm
 * @date ：2023/07/10 17:17
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class YTDLPServiceImpl implements YTDLPService {
    private final YTDLPDownloadArg ytdlpDownloadArg;
    private final YTDLPSearchArg ytdlpSearchArg;
    private final YTDLPVideoPrintArg ytdlpVideoPrintArg;
    private static final String TITLE = "title";
    private static final String UPLOAD_DATE = "upload_date";
    private final AsyncLoadingCache<String, VideoDto> VIDEO_INFO_CACHE = Caffeine.newBuilder()
            .recordStats(() -> new CaffeineStatsCounter(Metrics.globalRegistry, "video_info"))
            .maximumSize(10_00)
            .expireAfterAccess(8, TimeUnit.MINUTES)
            .executor(YouTubeVideoServiceImpl.getSCAN())
            .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> {
                List<String> videoInfo = getVideoInfo(key, TITLE, UPLOAD_DATE);
                return new VideoDto(key, videoInfo.get(0), LocalDate.parse(videoInfo.get(1), DateTimeFormatter.ofPattern("yyyyMMdd")));
            }, executor));

    {
        Gauge.builder("cache.size", VIDEO_INFO_CACHE, cache -> cache.asMap().size())
                .tags("cache", "video_info")
                .description("The approximate number of entries in this cache.")
                .register(Metrics.globalRegistry);
    }


    @Override
    public CompletableFuture<List<VideoDto>> search(ChannelDto channel) {
        List<String> args = ytdlpSearchArg.build(channel.getUrl());
        List<String> videoIdList = processTemplate(() -> new ProcessBuilder(args), process -> readPrint(process.inputReader()), "search");
        // These videos are not dependent on each other
        List<CompletableFuture<VideoDto>> videoInfoList = videoIdList.stream().map(VIDEO_INFO_CACHE::get).toList();
        return CompletableFuture.allOf(videoInfoList.toArray(CompletableFuture[]::new))
                .thenApply(__ -> videoInfoList.stream().map(CompletableFuture::join).toList());
    }

    @Override
    public void download(String videoId) {
        List<String> args = ytdlpDownloadArg.build(videoId);
        processTemplate(() -> new ProcessBuilder(args).redirectOutput(ProcessBuilder.Redirect.DISCARD), __ -> __, "download");
    }

    @Override
    public List<String> getVideoInfo(String videoId, String... target) {
        List<String> args = ytdlpVideoPrintArg.build(videoId, target);
        return processTemplate(() -> new ProcessBuilder(args), process -> readPrint(process.inputReader()), "get-info");
    }

    @Override
    public Logger getLog() {
        return log;
    }

    private List<String> readPrint(BufferedReader reader) {
        return Try.success(reader)
                .mapTry(BufferedReader::lines)
                .mapTry(Stream::toList)
                .andFinallyTry(reader::close)
                .getOrElseThrow(YTDLPException::new);
    }

}