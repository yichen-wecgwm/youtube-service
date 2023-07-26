package com.wecgcm.youtube.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

/**
 * @author ：wecgwm
 * @date ：2023/07/21 18:55
 */
@AllArgsConstructor
@Data
public class VideoDto {
    private String videoId;
    private String title;
    private LocalDate uploadDate;
}
