package com.wecgcm.controller;

import com.wecgcm.model.resp.Response;
import com.wecgcm.service.YouTubeVideoService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author ：wecgwm
 * @date ：2023/07/10 20:00
 */
@Getter
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("${spring.application.name}/video")
public class YoutubeVideoController extends YoutubeController {
    private final YouTubeVideoService youtubeService;

    @GetMapping("/search")
    public Response<List<String>> search(@RequestParam String query) {
        return Response.from(youtubeService.search(query));
    }
}
