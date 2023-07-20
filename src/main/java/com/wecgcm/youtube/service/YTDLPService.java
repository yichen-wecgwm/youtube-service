package com.wecgcm.youtube.service;

/**
 * @author ：wecgwm
 * @date ：2023/07/21 0:16
 */
public interface YTDLPService {

    /**
     * @param videoId videoId e.g. JpTqSzm4JOk in www.youtube.com/watch?v=JpTqSzm4JOk
     * @return videoId
     */
    String download(String videoId);

}
