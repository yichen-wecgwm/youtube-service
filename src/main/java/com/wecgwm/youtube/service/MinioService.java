package com.wecgwm.youtube.service;

import com.wecgwm.youtube.model.dto.VideoDto;
import io.minio.ObjectWriteResponse;
import io.minio.StatObjectResponse;

public interface MinioService {

    ObjectWriteResponse upload(String bucket, String object, String fileName, String contentType);

    ObjectWriteResponse put(String bucket, String object, String text);

    void remove(String bucket, String object);

    StatObjectResponse statObject(String bucket, String object);

    <T> T readJson(String bucket, String object, Class<T> clazz);

    boolean tryLock(VideoDto videoDto);

    <T> T unlock(String videoId, Throwable throwable);
}