package com.wecgwm.youtube.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wecgwm.youtube.config.ObjectMapperSingleton;
import com.wecgwm.youtube.exception.LockException;
import com.wecgwm.youtube.exception.MinioException;
import com.wecgwm.youtube.model.arg.minio.MinioArchiveArg;
import com.wecgwm.youtube.model.arg.minio.MinioLockArg;
import com.wecgwm.youtube.model.dto.VideoInfoDto;
import com.wecgwm.youtube.service.MinioService;
import com.wecgwm.youtube.util.LogUtil;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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
    @Value("${yt.lock-time-out-minute}")
    private int lockTimeOutMinute;
    @Value("${yt.filter-upload-date}")
    private int filterUploadDate;

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
    public void remove(String bucket, String object) {
        Try.run(() -> minioClient
                        .removeObject(RemoveObjectArgs
                                .builder()
                                .bucket(bucket)
                                .object(object)
                                .build()))
                .getOrElseThrow(MinioException::new);
    }

    @Override
    public StatObjectResponse statObject(String bucket, String object) {
        return Try.of(() -> minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(object).build()))
                .recoverWith(ErrorResponseException.class, e -> {
                    if ("NoSuchKey".equals(e.errorResponse().code())) {
                        // Not found
                        return Try.success(null);
                    }
                    return Try.failure(new MinioException(e));
                })
                .getOrElseThrow(MinioException::new);
    }

    @Override
    public <T> T readJson(String bucket, String object, Class<T> clazz) {
        return readJson(bucket, object, (Type) clazz);
    }

    @Override
    public <T> T readJson(String bucket, String object, TypeReference<T> valueTypeRef) {
        return readJson(bucket, object, valueTypeRef.getType());
    }

    @Override
    public CompletionStage<Void> tryLock(VideoInfoDto videoInfoDto) {
        // todo test
        if (videoInfoDto.videoId().equals("EW0IPfC_il8")) {
            return CompletableFuture.completedStage(null);
        }
        String videoId = videoInfoDto.videoId();
        if (videoInfoDto.uploadDate().plusDays(filterUploadDate).isBefore(LocalDate.now())) {
            return CompletableFuture.failedStage(new LockException("expired date"));
        }
        if (statObject(MinioArchiveArg.bucket(), MinioArchiveArg.object(videoInfoDto.videoId())) != null) {
            return CompletableFuture.failedStage(new LockException("already download"));
        }
        String lockObject = MinioLockArg.object(videoId);
        StatObjectResponse resp = statObject(MinioLockArg.bucket(), lockObject);
        if (resp != null && resp.lastModified().plusMinutes(lockTimeOutMinute).isAfter(ZonedDateTime.now())) {
            return CompletableFuture.failedStage(new LockException("lock not expired"));
        }
        // todo concurrent not support yet
        ObjectWriteResponse putResp = put(MinioLockArg.bucket(), lockObject, Thread.currentThread().getName());
        if (putResp == null) {
            return CompletableFuture.failedStage(new MinioException("put lock fail"));
        }
        log.info("lock success, thread:{}, videoId:{}", Thread.currentThread(), videoId);
        return CompletableFuture.completedStage(null);
    }

    @Override
    public <T> T unlock(String videoId, Throwable throwable) {
        // TODO concurrent not support yet
        remove(MinioLockArg.bucket(), MinioLockArg.object(videoId));
        return LogUtil.<T>completionExceptionally().apply(throwable);
    }

    private <T> T readJson(String bucket, String object, Type type){
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
                .mapTry(json -> ObjectMapperSingleton.INSTANCE.<T>readValue(json, ObjectMapperSingleton.INSTANCE.getTypeFactory().constructType(type)))
                .andThenTry(ret -> log.info("read json done, thread:{}, bucket: {}, object:{}, content:{}", Thread.currentThread(), bucket, object, ret))
                .andFinallyTry(bufferedReader::close)
                .getOrElseThrow(MinioException::new);
    }

}
