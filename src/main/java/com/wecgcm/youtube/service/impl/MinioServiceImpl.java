package com.wecgcm.youtube.service.impl;

import com.wecgcm.youtube.exception.UploadException;
import com.wecgcm.youtube.model.arg.MinIOUploadArg;
import com.wecgcm.youtube.service.MinioService;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.minio.MinioClient;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Objects;

/**
 * @author ：wecgwm
 * @date ：2023/07/16 19:32
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class MinioServiceImpl implements MinioService {
    private final MinIOUploadArg minIOUploadArg;
    private final MinioClient minioClient;

    @Override
    public void uploadVideo(String videoId) {
        Timer.Sample timer = Timer.start();

        Try.success(videoId)
                .mapTry(minIOUploadArg::build)
                .recoverWith(e -> Try.failure(new UploadException("minio upload: open file exception", e)))
                .mapTry(minioClient::uploadObject)
                .recoverWith(e -> Try.failure(new UploadException("minio upload: upload exception", e)))
                .filter(Objects::nonNull, () -> new UploadException("minio upload fail, resp is null"));

        //noinspection ResultOfMethodCallIgnored
        new File(minIOUploadArg.getFilePath(videoId)).delete();
        timer.stop(Timer.builder("minio-upload").register(Metrics.globalRegistry));
        log.info("upload done, videoId: {}", videoId);
    }

}
