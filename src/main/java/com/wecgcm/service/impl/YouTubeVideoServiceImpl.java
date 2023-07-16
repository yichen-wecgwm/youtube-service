package com.wecgcm.service.impl;

import cn.hutool.core.text.StrPool;
import cn.hutool.json.JSONUtil;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.request.RequestSearchResult;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.search.*;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.wecgcm.service.YouTubeVideoService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

import static com.wecgcm.util.YoutubeFileUtil.OUT_PUT_DIR;

/**
 * @author ：wecgwm
 * @date ：2023/07/10 17:17
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Getter
@Service
public class YouTubeVideoServiceImpl implements YouTubeVideoService {
    private final YoutubeDownloader downloader;
    private final YoutubeCallback<File> youtubeCallback;

    @Override
    public void download(String videoId) {
        // download the best video and audio
        Response<VideoInfo> videoInfo = getVideoInfo(videoId);
        List<Format> bestFormat = List.of(videoInfo.data().bestVideoFormat(), videoInfo.data().bestAudioFormat());
        bestFormat.forEach(f ->
                downloader.downloadVideoFile(new RequestVideoFileDownload(f)
                        .saveTo(new File(OUT_PUT_DIR))
                        .renameTo(videoId + StrPool.DASHED + f.type())
                        .callback(youtubeCallback)
                        .async()
                )
        );
//        // download in-memory to OutputStream
//        OutputStream os = new ByteArrayOutputStream();
//        RequestVideoStreamDownload request = new RequestVideoStreamDownload(bestFormat.get(0), os);
//        Response<Void> response = downloader.downloadVideoStream(request);
    }

    @Deprecated
    @Override
    public void downloadAllFormat(String videoId) {
        File outputDir = new File(OUT_PUT_DIR + "/all_format");
        Response<VideoInfo> videoInfo = getVideoInfo(videoId);
        videoInfo.data().formats().forEach(f -> {
            log.info("format: {}", JSONUtil.toJsonStr(f));
        });
        videoInfo.data().formats().forEach(f ->
                downloader.downloadVideoFile(new RequestVideoFileDownload(f)
                        .saveTo(outputDir)
                        .renameTo("id + " + f.itag().id() + ", videoQ: " + f.itag().videoQuality().name())
                        .overwriteIfExists(true))
        );
    }

    @Override
    public List<String> search(String query) {
        RequestSearchResult request = new RequestSearchResult(query);
        SearchResult result = downloader.search(request).data();
        // items, 20 max per result (+ possible shelves on first result)
        List<SearchResultItem> items = result.items();
        List<SearchResultVideoDetails> videos = result.videos();
        List<SearchResultChannelDetails> channels = result.channels();
        List<SearchResultPlaylistDetails> playlists = result.playlists();
        List<SearchResultShelf> shelves = result.shelves();
        return shelves.get(0).videos().stream().map(SearchResultVideoDetails::videoId).toList();
    }

}
