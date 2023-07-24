package com.wecgcm.youtube.service;

import io.minio.ObjectWriteResponse;
import io.minio.StatObjectResponse;

public interface MinioService {

    ObjectWriteResponse upload(String bucket, String object, String fileName, String contentType);

    ObjectWriteResponse put(String bucket, String object, String text);

    StatObjectResponse statObject(String bucket, String object);

    <T> T readJson(String bucket, String object, Class<T> clazz);

    boolean tryLock(String videoId, Thread thread);
}
