package com.wecgcm.youtube.service;

import com.wecgcm.youtube.model.dto.ChannelDto;
import com.wecgcm.youtube.model.dto.VideoListDto;

/**
 * @author ：wecgwm
 * @date ：2023/07/21 0:16
 */
public interface YTDLPService {

    /**
     * Search some videoId that have not been downloaded
     */
    VideoListDto search(ChannelDto channel);

    /**
     * @param videoId videoId e.g. JpTqSzm4JOk in www.youtube.com/watch?v=JpTqSzm4JOk
     * @return videoId
     */
    String download(String videoId);

    boolean tryTakeVideoId(String videoId);
}
