package com.wecgcm.youtube.service.impl;

import com.wecgcm.youtube.config.ObjectMapperSingleton;
import com.wecgcm.youtube.exception.MinioException;
import com.wecgcm.youtube.model.arg.MinIOGetChannelArg;
import com.wecgcm.youtube.model.arg.MinIOPutVideoArg;
import com.wecgcm.youtube.model.arg.MinIOUploadVideoArg;
import com.wecgcm.youtube.model.dto.ChannelDto;
import com.wecgcm.youtube.service.MinioService;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author ：wecgwm
 * @date ：2023/07/16 19:32
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class MinioServiceImpl implements MinioService {
    private final MinioClient minioClient;
    private final MinIOUploadVideoArg minIOUploadVideoArg;
    private final MinIOPutVideoArg minIOPutVideoArg;
    private final MinIOGetChannelArg minIOGetChannelArg;

    @Override
    public ObjectWriteResponse uploadVideo(String videoId) {
        Timer.Sample timer = Timer.start();

        ObjectWriteResponse resp = Try.success(videoId)
                .mapTry(minIOUploadVideoArg::build)
                .recoverWith(e -> Try.failure(new MinioException("minio upload: open file exception", e)))
                .mapTry(minioClient::uploadObject)
                .recoverWith(e -> Try.failure(new MinioException("minio upload: upload exception", e)))
                .filter(Objects::nonNull, () -> new MinioException("minio upload fail, resp is null"))
                .get();

        //noinspection ResultOfMethodCallIgnored
        new File(minIOUploadVideoArg.filePath(videoId)).delete();
        timer.stop(Timer.builder("minio-upload").register(Metrics.globalRegistry));
        log.info("upload done, thread:{}, videoId: {}, eTag:{}, versionId:{}, object:{}", Thread.currentThread(), videoId, resp.etag(), resp.versionId(), resp.object());
        return resp;
    }

    @Override
    public ChannelDto getChannelInfo(int channelId) {
        InputStream resp = Try.success(channelId)
                .map(minIOGetChannelArg::build)
                .mapTry(minioClient::getObject)
                .getOrElseThrow(MinioException::new);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resp));
        return Try.success(bufferedReader)
                .mapTry(BufferedReader::lines)
                .mapTry(lines -> lines.collect(Collectors.joining()))
                .mapTry(json -> ObjectMapperSingleton.INSTANCE.readValue(json, ChannelDto.class))
                .andFinallyTry(bufferedReader::close)
                .getOrElseThrow(MinioException::new);
    }

    @Override
    public ObjectWriteResponse uploadTitle(String videoId, String titlePrefix) {
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = Try.success(pipedOutputStream)
                .mapTry(PipedInputStream::new)
                .getOrElseThrow(MinioException::new);
        Try.of(() -> new BufferedWriter(new OutputStreamWriter(pipedOutputStream)))
                .andThenTry(bufferedWriter -> bufferedWriter.write(titlePrefix + LocalDate.now() + videoId))
                .andThenTry(BufferedWriter::flush)
                .andThenTry(BufferedWriter::close)
                .getOrElseThrow(MinioException::new);
        return Try.of(() -> minIOPutVideoArg.build(videoId, pipedInputStream))
                .mapTry(minioClient::putObject)
                .andThenTry(resp -> log.info("upload done, thread:{}, videoId: {}, eTag:{}, versionId:{}, object:{}", Thread.currentThread(), videoId, resp.etag(), resp.versionId(), resp.object()))
                .getOrElseThrow(MinioException::new);
    }

}
