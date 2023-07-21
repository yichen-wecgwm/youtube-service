package com.wecgcm.youtube.service;


import com.wecgcm.youtube.model.dto.ChannelDto;

public interface MinioService {

    /**
     * @param videoId e.g. JpTqSzm4JOk in www.youtube.com/watch?v=JpTqSzm4JOk
     */
    String uploadVideo(String videoId);

    ChannelDto getChannelInfo(int id);

    void uploadTitle(String videoId, String titlePrefix);
}
