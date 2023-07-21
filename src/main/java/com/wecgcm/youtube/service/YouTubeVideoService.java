package com.wecgcm.youtube.service;

import com.wecgcm.youtube.model.dto.ChannelDto;
import com.wecgcm.youtube.model.dto.VideoListDto;

import java.util.concurrent.CompletionStage;

/**
 * @author ：wecgwm
 * @date ：2023/07/10 17:15
 */
@SuppressWarnings("SpellCheckingInspection")
public interface YouTubeVideoService {

    void scanAsync();

    /**
     * Search some videoId that have not been downloaded
     */
    VideoListDto search(ChannelDto channel);

    /**
     * @param videoId e.g. JpTqSzm4JOk in www.youtube.com/watch?v=JpTqSzm4JOk
     */
    CompletionStage<String> download(String videoId);

}