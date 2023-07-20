package com.wecgcm.model.arg;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ：wecgwm
 * @date ：2023/07/18 23:28
 */
@Slf4j
@Component
public class YTDownloadArg {
    private static final String YOUTUBE_VIDEO_URL_PREFIX = "https://www.youtube.com/watch?v=";
    private static final String OUT_PUT_OP = "-o";
    public static final String OUT_PUT_DIR = "videos/";
    public static final String VIDEO_EXT = ".webm";
    @Value("${yt-dlp.path}")
    private String ytDLP;

    public List<String> build(String videoId) {
        List<String> ret = ImmutableList.<String>builder()
                .add(ytDLP)
                .add(OUT_PUT_OP)
                .add(OUT_PUT_DIR + videoId + VIDEO_EXT)
                .add(YOUTUBE_VIDEO_URL_PREFIX + videoId)
                .build();
        log.info(String.join(" ", ret));
        return ret;
    }

}
