package com.wecgcm.youtube.model.arg;

import org.springframework.beans.factory.annotation.Value;

/**
 * @author ：wecgwm
 * @date ：2023/07/18 23:28
 */
@SuppressWarnings("SpellCheckingInspection")
public class YTDLPArg {
    static final String VIDEO_URL_PREFIX = "https://www.youtube.com/watch?v=";
    static final String WRITE_THUMBNAIL = "--write-thumbnail";
    static final String OUT_PUT_OP = "-o";
    static final String PRINT_OP = "--print";
    static final String FLAT_PLAYLIST = "--flat-playlist";
    static final String VIDEO_EXT = ".webm";
    static final String THUMBNAIL_EXT = ".webp";
    static final String OUT_PUT_DIR = "videos/";
    @Value("${yt-dlp.path}")
    String ytDLP;
}
