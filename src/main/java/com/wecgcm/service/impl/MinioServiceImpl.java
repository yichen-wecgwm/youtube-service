package com.wecgcm.service.impl;

import com.wecgcm.exception.UploadException;
import com.wecgcm.service.MinioService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author ：wecgwm
 * @date ：2023/07/16 19:32
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class MinioServiceImpl implements MinioService {
    private static final String BUCKET_NAME = "videos";
    private static final String SLASH = "/";

    public static final String VIDEO_TYPE = "video/webm";
    private final MinioClient minioClient;

    @Override
    public String upload(String videoId) {
        String filePath = YouTubeVideoServiceImpl.OUT_PUT_DIR + videoId + YouTubeVideoServiceImpl.VIDEO_EXT;
        Timer.Sample timer = Timer.start();
        try {
            ObjectWriteResponse objectWriteResponse = minioClient.uploadObject(
                    UploadObjectArgs
                            .builder()
                            .bucket(BUCKET_NAME)
                            .object(videoId + SLASH + videoId + YouTubeVideoServiceImpl.VIDEO_EXT)
                            .filename(filePath)
                            .contentType(VIDEO_TYPE)
                            .build());
            if (objectWriteResponse == null) {
                throw new UploadException("minio upload fail, resp is null");
            }
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new UploadException("minio upload exception", e);
        }finally {
            boolean delete = new File(filePath).delete();
            if (!delete) {
                Counter.builder("delete-file-fail")
                        .tag("path", filePath)
                        .register(Metrics.globalRegistry)
                        .increment();
                log.warn("delete file fail, file path: {}", filePath);
            }
        }
        timer.stop(Timer.builder("minio-upload")
                .register(Metrics.globalRegistry));
        return videoId;
    }

}
