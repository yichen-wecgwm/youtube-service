package com.wecgcm.youtube.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author ：wecgwm
 * @date ：2023/07/21 18:55
 */
@Accessors(chain = true)
@Data
public class VideoListDto {
    private String titlePrefix;
    private List<String> videoIdList;
}
