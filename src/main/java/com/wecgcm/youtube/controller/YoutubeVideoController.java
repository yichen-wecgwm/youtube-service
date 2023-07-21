package com.wecgcm.youtube.controller;

import com.wecgcm.youtube.model.req.YoutubeVideoRequest;
import com.wecgcm.youtube.model.resp.Response;
import com.wecgcm.youtube.service.YouTubeVideoService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：wecgwm
 * @date ：2023/07/10 20:00
 */
@Getter
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("${spring.application.name}/video")
public class YoutubeVideoController {
    private final YouTubeVideoService youTubeVideoService;

    @PostMapping("/download")
    public Response<String> download(@RequestBody YoutubeVideoRequest request){
        youTubeVideoService.download(request.getVideoId());
        return Response.ok();
    }
}