package com.wecgcm.service;

import java.util.List;

/**
 * @author ：wecgwm
 * @date ：2023/07/10 17:15
 */
public interface YouTubeVideoService extends YoutubeService{

    List<String> search(String query);

}
