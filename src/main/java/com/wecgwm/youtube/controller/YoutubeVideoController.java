package com.wecgwm.youtube.controller;

import com.wecgwm.youtube.model.req.YoutubeVideoRequest;
import com.wecgwm.youtube.model.resp.Response;
import com.wecgwm.youtube.service.YouTubeVideoService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

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
    public Response<String> download(@RequestBody @Valid YoutubeVideoRequest request){
        youTubeVideoService.download(request.videoId());
        return Response.ok();
    }

}