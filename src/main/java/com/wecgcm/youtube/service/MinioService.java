package com.wecgcm.youtube.service;


import com.wecgcm.youtube.model.dto.ChannelDto;
import io.minio.ObjectWriteResponse;

public interface MinioService {

    /**
     * @param videoId e.g. JpTqSzm4JOk in www.youtube.com/watch?v=JpTqSzm4JOk
     */
    ObjectWriteResponse uploadVideo(String videoId);

    ChannelDto getChannelInfo(int channelId);

    ObjectWriteResponse uploadTitle(String videoId, String titlePrefix);
}
