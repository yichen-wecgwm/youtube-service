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
public final class YTDLPVideoPrintArg extends YTDLPArg{
    private static final String DELIMITER = ",";

    public List<String> build(String videoId, String... printItemList) {
        return ImmutableList.<String>builder()
                .add(ytDLP)
                .add(FLAT_PLAYLIST)
                .add(PRINT_OP)
                .add(String.join(DELIMITER, printItemList))
                .add(VIDEO_URL_PREFIX + videoId)
                .build();
    }

}