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
public class YTDLPSearchArg extends YTDLPArg{

    public static final String CNT_OP = "-I";
    public static final String CNT = ":10";

    public List<String> build(String channelUrl) {
        List<String> ret = ImmutableList.<String>builder()
                .add(ytDLP)
                .add(CNT_OP)
                .add(CNT)
                .add("--print")
                .add("id")
                .add("--flat-playlist")
                .add(channelUrl)
                .build();
        log.info(String.join(" ", ret));
        return ret;
    }

}
