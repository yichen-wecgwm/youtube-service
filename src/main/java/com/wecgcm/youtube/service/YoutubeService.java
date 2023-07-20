package com.wecgcm.youtube.service;

public interface YoutubeService {

    /**
     * @param videoId e.g. JpTqSzm4JOk in www.youtube.com/watch?v=JpTqSzm4JOk
     */
    void download(String videoId);
}
