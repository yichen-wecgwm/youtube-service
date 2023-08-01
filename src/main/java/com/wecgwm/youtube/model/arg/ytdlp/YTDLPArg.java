package com.wecgwm.youtube.model.arg.ytdlp;

import org.springframework.beans.factory.annotation.Value;

/**
 * @author ：wecgwm
 * @date ：2023/07/18 23:28
 */
@SuppressWarnings("SpellCheckingInspection")
public sealed class YTDLPArg permits YTDLPDownloadArg, YTDLPSearchArg, YTDLPVideoPrintArg {
    static final String VIDEO_URL_PREFIX = "https://www.youtube.com/watch?v=";
    static final String WRITE_THUMBNAIL = "--write-thumbnail";
    static final String THUMBNAILS_FORMAT_OP = "--convert-thumbnails";
    static final String THUMBNAILS_FORMAT = "png";
    static final String OUT_PUT_OP = "-o";
    static final String PRINT_OP = "--print";
    static final String FLAT_PLAYLIST = "--flat-playlist";
    static final String THUMBNAILS_EXT = ".png";
    static final String VIDEO_EXT = ".webm";
    static final String OUT_PUT_DIR = "videos/";
    @Value("${yt-dlp.path}")
    String ytDLP;
}
