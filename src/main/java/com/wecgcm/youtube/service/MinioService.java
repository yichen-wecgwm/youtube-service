package com.wecgcm.youtube.service;


public interface MinioService {

    /**
     * @param videoId e.g. JpTqSzm4JOk in www.youtube.com/watch?v=JpTqSzm4JOk
     */
    void uploadVideo(String videoId);
}
