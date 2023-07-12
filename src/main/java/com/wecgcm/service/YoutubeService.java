package com.wecgcm.service;

import cn.hutool.core.lang.Assert;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoInfo;


public interface YoutubeService {

    void download(String id);
    void downloadAllFormat(String id);
    YoutubeDownloader getDownloader();

    default Response<VideoInfo> getVideoInfo(String id) {
        Response<VideoInfo> response = getDownloader().getVideoInfo(new RequestVideoInfo(id));
        Assert.isTrue(response.ok());
        return response;
    }
}
