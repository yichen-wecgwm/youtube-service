package com.wecgwm.youtube.model.arg.ytdlp;

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
public final class YTDLPSearchArg extends YTDLPArg{
    @Value("${yt-dlp.search.cnt}")
    private String cnt;
    private static final String CNT_OP = "-I";
    private static final String ID = "id";
    private static final String COLON = ":";

    public List<String> build(String channelUrl) {
        return ImmutableList.<String>builder()
                .add(ytDLP)
                .add(CNT_OP)
                .add(COLON + cnt)
                .add(PRINT_OP)
                .add(ID)
                .add(FLAT_PLAYLIST)
                .add(channelUrl)
                .build();
    }

}