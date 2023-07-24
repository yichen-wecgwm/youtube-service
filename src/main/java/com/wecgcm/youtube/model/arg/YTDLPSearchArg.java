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
    private static final String CNT_OP = "-I";
    private static final String CNT = ":10";
    private static final String ID = "id";

    public List<String> build(String channelUrl) {
        List<String> ret = ImmutableList.<String>builder()
                .add(ytDLP)
                .add(CNT_OP)
                .add(CNT)
                .add(PRINT_OP)
                .add(ID)
                .add(FLAT_PLAYLIST)
                .add(channelUrl)
                .build();
        return ret;
    }

}