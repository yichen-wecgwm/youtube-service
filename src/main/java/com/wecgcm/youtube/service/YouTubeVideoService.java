package com.wecgcm.youtube.service;

import io.minio.ObjectWriteResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author ：wecgwm
 * @date ：2023/07/10 17:15
 */
@SuppressWarnings("SpellCheckingInspection")
public interface YouTubeVideoService {

    List<CompletableFuture<List<CompletableFuture<Void>>>> scanAsync();

    /**
     * @param videoId e.g. JpTqSzm4JOk in www.youtube.com/watch?v=JpTqSzm4JOk
     */
    CompletionStage<ObjectWriteResponse> download(String videoId);

}