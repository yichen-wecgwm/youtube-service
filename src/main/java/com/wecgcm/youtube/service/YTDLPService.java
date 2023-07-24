package com.wecgcm.youtube.service;

import com.wecgcm.youtube.model.dto.ChannelDto;
import com.wecgcm.youtube.model.dto.VideoDto;

import java.util.List;

/**
 * @author ：wecgwm
 * @date ：2023/07/21 0:16
 */
public interface YTDLPService {

    /**
     * Search some videoId that have not been downloaded
     */
    List<VideoDto> search(ChannelDto channel);

    /**
     * @param videoId videoId e.g. JpTqSzm4JOk in www.youtube.com/watch?v=JpTqSzm4JOk
     */
    String download(String videoId);

    String getInfo(String videoId, String target);
}
