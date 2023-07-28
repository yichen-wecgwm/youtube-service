package com.wecgwm.youtube.controller;

import com.wecgwm.youtube.model.req.YoutubeVideoRequest;
import com.wecgwm.youtube.model.resp.Response;
import com.wecgwm.youtube.service.YouTubeVideoService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * @author ：wecgwm
 * @date ：2023/07/10 20:00
 */
@Getter
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("${spring.application.name}/video")
@Slf4j
public class YoutubeVideoController {
    private final YouTubeVideoService youTubeVideoService;
    private static final Logger LOG = LoggerFactory.getLogger(YoutubeVideoController.class);

    @PostMapping("/download")
    public Response<String> download(@RequestBody @Valid YoutubeVideoRequest request){
        youTubeVideoService.download(request.getVideoId());
        return Response.ok();
    }

}