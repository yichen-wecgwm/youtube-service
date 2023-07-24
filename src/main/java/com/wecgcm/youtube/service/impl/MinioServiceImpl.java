package com.wecgcm.youtube.service.impl;

import com.wecgcm.youtube.config.ObjectMapperSingleton;
import com.wecgcm.youtube.exception.MinioException;
import com.wecgcm.youtube.service.MinioService;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
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

    @Override
    public ObjectWriteResponse upload(String bucket, String object, String fileName, String contentType) {
        Timer.Sample timer = Timer.start();

        ObjectWriteResponse resp = Try.of(() -> UploadObjectArgs
                        .builder()
                        .bucket(bucket)
                        .object(object)
                        .filename(fileName)
                        .contentType(contentType)
                        .build()
                )
                .recoverWith(e -> Try.failure(new MinioException("minio upload: open file exception", e)))
                .mapTry(minioClient::uploadObject)
                .recoverWith(e -> Try.failure(new MinioException("minio upload: upload exception", e)))
                .filter(Objects::nonNull, () -> new MinioException("minio upload fail, resp is null"))
                .get();

        //noinspection ResultOfMethodCallIgnored
        new File(fileName).delete();
        timer.stop(Timer.builder("minio-upload").register(Metrics.globalRegistry));
        log.info("upload done, thread:{}, bucket: {}, object:{}, fileName:{}, contentType:{}, eTag:{}, resp:{}", Thread.currentThread(), bucket, object, fileName, contentType, resp.etag(), resp.object());
        return resp;
    }

    @Override
    public ObjectWriteResponse put(String bucket, String object, String text) {
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = Try.success(pipedOutputStream)
                .mapTry(PipedInputStream::new)
                .getOrElseThrow(MinioException::new);
        Try.of(() -> new BufferedWriter(new OutputStreamWriter(pipedOutputStream)))
                .andThenTry(bufferedWriter -> bufferedWriter.write(text))
                .andThenTry(BufferedWriter::flush)
                .andThenTry(BufferedWriter::close)
                .getOrElseThrow(MinioException::new);
        return Try.of(() -> PutObjectArgs
                        .builder()
                        .bucket(bucket)
                        .object(object)
                        .stream(pipedInputStream, -1, 10485760)
                        .build())
                .mapTry(minioClient::putObject)
                .andThenTry(resp -> log.info("put done, thread:{}, bucket: {}, object:{}, text:{}, eTag:{}, resp:{}", Thread.currentThread(), bucket, object, text, resp.etag(), resp.object()))
                .getOrElseThrow(MinioException::new);
    }

    @Override
    public StatObjectResponse statObject(String bucket, String object) {
        return Try.of(() -> minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(object).build()))
                .recoverWith(ErrorResponseException.class, e -> {
                    if ("NoSuchKey" .equals(e.errorResponse().code())) {
                        // Not found
                        return Try.success(null);
                    }
                    return Try.failure(new MinioException(e));
                })
                .getOrElseThrow(MinioException::new);
    }

    @Override
    public <T> T readJson(String bucket, String object, Class<T> clazz) {
        InputStream resp = Try.of(() -> GetObjectArgs
                        .builder()
                        .bucket(bucket)
                        .object(object)
                        .build()
                )
                .mapTry(minioClient::getObject)
                .getOrElseThrow(MinioException::new);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resp));
        return Try.success(bufferedReader)
                .mapTry(BufferedReader::lines)
                .mapTry(lines -> lines.collect(Collectors.joining()))
                .mapTry(json -> ObjectMapperSingleton.INSTANCE.readValue(json, clazz))
                .andThenTry(ret -> log.info("read json done, thread:{}, bucket: {}, object:{}, content:{}", Thread.currentThread(), bucket, object, ret))
                .andFinallyTry(bufferedReader::close)
                .getOrElseThrow(MinioException::new);
    }

    @Override
    public boolean tryLock(String videoId, Thread thread) {
        // TODO
        return false;
    }

}
