package com.wecgcm.youtube.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author ：wecgwm
 * @date ：2023/07/21 18:55
 */
@AllArgsConstructor
@Data
public class VideoListDto {
    private String titlePrefix;
    private List<String> videoIdList;
}
