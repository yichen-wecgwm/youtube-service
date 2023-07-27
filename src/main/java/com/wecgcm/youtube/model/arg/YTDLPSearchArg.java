package com.wecgcm.youtube.model.arg;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${yt-dlp.search.cnt}")
    private String cnt;
    private static final String SEARCH_CNT_OP = "-I";
    @SuppressWarnings("ConstantValue")
    private final String SEARCH_CNT = ":" + cnt;
    private static final String ID = "id";

    public List<String> build(String channelUrl) {
        return ImmutableList.<String>builder()
                .add(ytDLP)
                .add(SEARCH_CNT_OP)
                .add(SEARCH_CNT)
                .add(PRINT_OP)
                .add(ID)
                .add(FLAT_PLAYLIST)
                .add(channelUrl)
                .build();
    }

}