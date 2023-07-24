package com.wecgcm.youtube.service.impl;

import com.wecgcm.youtube.exception.YTDLPException;
import com.wecgcm.youtube.model.arg.MinioArg;
import com.wecgcm.youtube.model.arg.YTDLPDownloadArg;
import com.wecgcm.youtube.model.arg.YTDLPSearchArg;
import com.wecgcm.youtube.model.arg.YTDLPVideoPrintArg;
import com.wecgcm.youtube.model.dto.ChannelDto;
import com.wecgcm.youtube.model.dto.VideoDto;
import com.wecgcm.youtube.service.MinioService;
import com.wecgcm.youtube.service.YTDLPService;
import com.wecgcm.youtube.util.LogUtil;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.minio.StatObjectResponse;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
    private final YTDLPDownloadArg ytdlpDownloadArg;
    private final YTDLPSearchArg ytdlpSearchArg;
    private final YTDLPVideoPrintArg ytdlpVideoPrintArg;
    private final MinioService minioService;
    private static final int PROCESS_NORMAL_TERMINATION = 0;
    private static final String UPLOAD_DATE = "upload_date";
    private static final String ARCHIVE = "archive";
    private static final String LOCK = "lock";
    @Value("${yt.filter-upload-date}")
    private int filterUploadDate;
    @Value("${yt.lock-time-out-minute}")
    private int lockTimeOutMinute;

    @Override
    public List<VideoDto> search(ChannelDto channel) {
        List<String> args = ytdlpSearchArg.build(channel.getUrl());
        Timer.Sample timer = Timer.start();

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = Try.of(processBuilder::start)
                .getOrElseThrow(YTDLPException::new);
        List<String> videoIdList = Try.of(process::getInputStream)
                .mapTry(this::readPrint)
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
        return takeVideoId(videoIdList, channel.getTitlePrefix());
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
        log.info("download done, thread:{}, videoId:{}", Thread.currentThread(), videoId);
        return YTDLPDownloadArg.filePath(videoId);
    }

    @Override
    public LocalDateTime getUploadDate(String videoId) {
        List<String> args = ytdlpVideoPrintArg.build(videoId, UPLOAD_DATE);

        Timer.Sample timer = Timer.start();

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = Try.of(processBuilder::start)
                .getOrElseThrow(YTDLPException::new);
        String uploadDate = Try.of(process::getInputStream)
                .mapTry(this::readPrint)
                .andThenTry(process::waitFor)
                .filter(__ -> process.exitValue() == PROCESS_NORMAL_TERMINATION, p -> {
                    LogUtil.error(process.getErrorStream(), this.getClass());
                    return new YTDLPException("yt-dlp print process exit error");
                })
                .andFinally(process::destroy)
                .getOrElseThrow(YTDLPException::new)
                .get(0);

        timer.stop(Timer.builder("yt-dlp-print")
                .tag("print", "upload-date")
                .register(Metrics.globalRegistry));
        log.info("print upload date done, thread:{}, videoId:{}, uploadDate:{}", Thread.currentThread(), videoId, uploadDate);
        return LocalDate.parse(uploadDate, DateTimeFormatter.ofPattern("yyyyMMdd")).atStartOfDay();
    }

    private List<VideoDto> takeVideoId(List<String> videoIdList, String titlePrefix) {
        List<VideoDto> ret = videoIdList.stream().map(id -> new VideoDto(id, getUploadDate(id), titlePrefix)).filter(videoDto -> {
            if (videoDto.getUploadDate().plusDays(filterUploadDate).isBefore(LocalDateTime.now())) {
                return false;
            }
            String videoId = videoDto.getVideoId();
            StatObjectResponse archive = minioService.statObject(MinioArg.VIDEO_BUCKET_NAME, videoId + MinioArg.SLASH + ARCHIVE);
            if (archive != null) {
                return false;
            }
            StatObjectResponse lock = minioService.statObject(MinioArg.VIDEO_BUCKET_NAME, videoId + MinioArg.SLASH + LOCK);
            if (lock != null && lock.lastModified().plusMinutes(lockTimeOutMinute).isAfter(ZonedDateTime.now())) {
                return false;
            }
            return minioService.tryLock(videoId, Thread.currentThread());
        }).toList();
        log.info("take done, thread:{}, VideoList:{}", Thread.currentThread(), ret);
        return ret;
    }

    private  List<String> readPrint(InputStream inputStream) {
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