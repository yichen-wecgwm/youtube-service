package com.wecgcm.youtube.service.impl;

import com.wecgcm.youtube.exception.YTDLPException;
import com.wecgcm.youtube.model.arg.YTDLPDownloadArg;
import com.wecgcm.youtube.model.arg.YTDLPSearchArg;
import com.wecgcm.youtube.model.dto.ChannelDto;
import com.wecgcm.youtube.model.dto.VideoListDto;
import com.wecgcm.youtube.service.YTDLPService;
import com.wecgcm.youtube.util.LogUtil;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Stream;


/**
 * @author ：wecgwm
 * @date ：2023/07/10 17:17
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class YTDLPServiceImpl implements YTDLPService {
    public static final int PROCESS_NORMAL_TERMINATION = 0;
    private final YTDLPDownloadArg ytdlpDownloadArg;
    private final YTDLPSearchArg ytdlpSearchArg;

    @Override
    public VideoListDto search(ChannelDto channel) {
        List<String> args = ytdlpSearchArg.build(channel.getUrl());
        Timer.Sample timer = Timer.start();

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = Try.of(processBuilder::start)
                .getOrElseThrow(YTDLPException::new);
        List<String> videoIdList = Try.of(process::getInputStream)
                .mapTry(this::read)
                .andThenTry(process::waitFor)
                .filter(__ -> process.exitValue() == PROCESS_NORMAL_TERMINATION, p -> {
                    LogUtil.error(process.getErrorStream(), this.getClass());
                    return new YTDLPException("yt-dlp search process exit error");
                })
                .andFinally(process::destroy)
                .getOrElseThrow(YTDLPException::new);

        timer.stop(Timer.builder("yt-dlp-search")
                .register(Metrics.globalRegistry));
        log.info("search done, thread:{}, channel: {}, videoIdList:{}", Thread.currentThread(), channel, videoIdList);

        videoIdList = videoIdList.stream().filter(this::tryTakeVideoId).toList();
        log.info("take done, thread:{}, channel: {}, videoIdList:{}", Thread.currentThread(), channel, videoIdList);
        return new VideoListDto(channel.getTitlePrefix(), videoIdList);
    }

    @Override
    public String download(String videoId) {
        List<String> args = ytdlpDownloadArg.build(videoId);
        Timer.Sample timer = Timer.start();

        ProcessBuilder processBuilder = new ProcessBuilder(args)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process process = Try.of(processBuilder::start)
                .getOrElseThrow(YTDLPException::new);
        Try.success(process)
                .andThenTry(Process::waitFor)
                .filter(p -> p.exitValue() == 0, p -> {
                    LogUtil.error(process.getErrorStream(), this.getClass());
                    return new YTDLPException("yt-dlp download process exit error");
                })
                .andFinally(process::destroy)
                .getOrElseThrow(YTDLPException::new);

        timer.stop(Timer.builder("yt-dlp-dl")
                .register(Metrics.globalRegistry));
        log.info("download done, thread:{}, videoId: {}", Thread.currentThread(), videoId);
        return videoId;
    }

    // yt-dlp --flat-playlist --print upload_date https://www.youtube.com/watch?v=30_Z-81k5Aw
    @Override
    public boolean tryTakeVideoId(String videoId) {
        // videos/archive/$videoId videos/lock/$videoId
        // if exist -> return false
        // if false -> upload archive? fail/concurrent
        return true;
    }

    private  List<String> read(InputStream inputStream) {
        BufferedReader reader = Try.success(inputStream)
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .getOrElseThrow(YTDLPException::new);
        return Try.success(reader)
                .mapTry(BufferedReader::lines)
                .mapTry(Stream::toList)
                .andFinallyTry(reader::close)
                .getOrElseThrow(YTDLPException::new);
    }

}