package com.wecgcm.youtube.model.arg;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ：wecgwm
 * @date ：2023/07/18 23:28
 */
@SuppressWarnings("SpellCheckingInspection")
@Slf4j
@Component
public class YTDLPDownloadArg extends YTDLPArg{

    public List<String> build(String videoId) {
        List<String> ret = ImmutableList.<String>builder()
                .add(ytDLP)
                .add(OUT_PUT_OP)
                .add(filePath(videoId))
                .add(VIDEO_URL_PREFIX + videoId)
                .build();
        log.info(String.join(" ", ret));
        return ret;
    }

    public static String filePath(String videoId) {
        return YTDLPArg.OUT_PUT_DIR + videoId + YTDLPArg.VIDEO_EXT;
    }

}
