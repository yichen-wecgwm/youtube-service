package com.wecgcm.service.impl;

import com.wecgcm.exception.ProcessException;
import com.wecgcm.exception.UploadException;
import com.wecgcm.service.MinioService;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author ：wecgwm
 * @date ：2023/07/16 19:32
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class MinioServiceImpl implements MinioService {
    private static final String BUCKET_NAME = "videos";
    private static final String SLASH = "/";
    private static final String VIDEO_EXT = ".mp4";
    public static final String DASHED = "-";
    private final MinioClient minioClient;

    @Override
    public String upload(String videoId, Process process) {
        try (InputStream inputStream = process.getInputStream()){
            Timer.Sample timer = Timer.start();
            ObjectWriteResponse objectWriteResponse = minioClient.putObject(
                    PutObjectArgs
                            .builder()
                            .bucket(BUCKET_NAME)
                            .object(videoId + SLASH + videoId + VIDEO_EXT)
                            .stream(inputStream, -1, 10485760)
                            .contentType("video/mp4")
                            .build());
            if (process.exitValue() == 0 && !videoId.startsWith(DASHED)) {
                throw new ProcessException("process exit error");
            }
            if (objectWriteResponse == null) {
                throw new UploadException("minio upload fail, resp is null");
            }
            timer.stop(Timer.builder("yt-upload")
                    .register(Metrics.globalRegistry));
        } catch (ErrorResponseException | InsufficientDataException | XmlParserException | ServerException |
                 NoSuchAlgorithmException | IOException | InvalidResponseException | InvalidKeyException |
                 InternalException e) {
            throw new RuntimeException(e);
        }
        return videoId;
    }

}
