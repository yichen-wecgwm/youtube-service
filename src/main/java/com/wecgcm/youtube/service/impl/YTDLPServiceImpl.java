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
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.time.LocalDate;
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
    private static final String TITLE = "title";
    private static final String UPLOAD_DATE = "upload_date";

    @Value("${yt.filter-upload-date}")
    private int filterUploadDate;

    @Override
    public List<VideoDto> search(ChannelDto channel) {
        List<String> args = ytdlpSearchArg.build(channel.getUrl());
        List<String> videoIdList = processTemplate(() -> new ProcessBuilder(args), process -> readPrint(process.inputReader()), "search");
        return takeVideoId(videoIdList);
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

    private List<VideoDto> takeVideoId(List<String> videoIdList) {
        List<VideoDto> ret = videoIdList.stream()
                .map(id -> {
                    List<String> videoInfo = getVideoInfo(id, TITLE, UPLOAD_DATE);
                    return new VideoDto(id, videoInfo.get(0), LocalDate.parse(videoInfo.get(1), DateTimeFormatter.ofPattern("yyyyMMdd")));
                })
                .filter(videoDto -> {
                    if (videoDto.getUploadDate().plusDays(filterUploadDate).isBefore(LocalDate.now())) {
                        return false;
                    }
                    return minioService.statObject(MinioArg.Archive.bucket(), MinioArg.Archive.object(videoDto.getVideoId())) == null;
                }).toList();
        log.info("take done, thread:{}, VideoList:{}", Thread.currentThread(), ret);
        return ret;
    }

    private List<String> readPrint(BufferedReader reader) {
        return Try.success(reader)
                .mapTry(BufferedReader::lines)
                .mapTry(Stream::toList)
                .andFinallyTry(reader::close)
                .getOrElseThrow(YTDLPException::new);
    }

}