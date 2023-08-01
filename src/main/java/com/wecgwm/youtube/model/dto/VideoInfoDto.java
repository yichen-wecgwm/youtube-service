package com.wecgwm.youtube.model.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * @author ：wecgwm
 * @date ：2023/07/21 18:55
 */
public record VideoInfoDto(String videoId, String title, LocalDate uploadDate, List<String> ext){ }
