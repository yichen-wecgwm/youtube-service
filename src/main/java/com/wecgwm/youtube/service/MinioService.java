package com.wecgwm.youtube.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wecgwm.youtube.model.dto.VideoInfoDto;
import io.minio.ObjectWriteResponse;
import io.minio.StatObjectResponse;

import java.util.concurrent.CompletionStage;

public interface MinioService {

    ObjectWriteResponse upload(String bucket, String object, String fileName, String contentType);

    ObjectWriteResponse put(String bucket, String object, String text);

    void remove(String bucket, String object);

    StatObjectResponse statObject(String bucket, String object);

    <T> T readJson(String bucket, String object, Class<T> clazz);

    <T> T readJson(String bucket, String object, TypeReference<T> valueTypeRef);

    CompletionStage<Void> tryLock(VideoInfoDto videoInfoDto);

    <T> T unlock(String videoId, Throwable throwable);
}
