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
public class YTDLPVideoPrintArg extends YTDLPArg{

    public List<String> build(String videoId, String printItem) {
        List<String> ret = ImmutableList.<String>builder()
                .add(ytDLP)
                .add(FLAT_PLAYLIST)
                .add(PRINT_OP)
                .add(printItem)
                .add(VIDEO_URL_PREFIX + videoId)
                .build();
        return ret;
    }

}