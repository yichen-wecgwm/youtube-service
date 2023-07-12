package com.wecgcm.controller;

import com.wecgcm.service.YoutubeService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：wecgwm
 * @date ：2023/07/10 20:00
 */
@Getter
@RequiredArgsConstructor(onConstructor_= @Autowired)
@RestController
@RequestMapping("/video")
public class YoutubeVideoController extends YoutubeController{
    private final YoutubeService youtubeService;
}
