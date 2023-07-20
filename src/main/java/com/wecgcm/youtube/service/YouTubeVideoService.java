package com.wecgcm.youtube.service;

import java.util.List;

/**
 * @author ：wecgwm
 * @date ：2023/07/10 17:15
 */
@SuppressWarnings("SpellCheckingInspection")
public interface YouTubeVideoService extends YoutubeService{

    /**
     * Search some videoId that have not been downloaded
     *
     * @param channel channel e.g. @15ya.fullmoon in www.youtube.com/@15ya.fullmoon/videos
     * @return videoId list
     */
    List<String> search(String channel);

}
