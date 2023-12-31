package com.wecgwm.youtube.model.arg.ytdlp;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ：wecgwm
 * @date ：2023/07/18 23:28
 */
@Slf4j
@Component
public final class YTDLPDownloadArg extends YTDLPArg{

    public List<String> build(String videoId) {
        return ImmutableList.<String>builder()
                .add(ytDLP)
                .add(WRITE_THUMBNAIL)
                .add(THUMBNAILS_FORMAT_OP)
                .add(THUMBNAILS_FORMAT)
                .add(OUT_PUT_OP)
                .add(videoPath(videoId))
                .add(VIDEO_URL_PREFIX + videoId)
                .build();
    }

    public static String videoPath(String videoId) {
        return YTDLPArg.OUT_PUT_DIR + videoId + YTDLPArg.VIDEO_EXT;
    }

    public static String thumbnailPath(String videoId) {
        return YTDLPArg.OUT_PUT_DIR + videoId + YTDLPArg.THUMBNAILS_EXT;
    }

}
