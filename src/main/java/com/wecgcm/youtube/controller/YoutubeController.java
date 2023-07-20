package com.wecgcm.youtube.controller;

import com.wecgcm.youtube.model.req.YoutubeRequest;
import com.wecgcm.youtube.model.resp.Response;
import com.wecgcm.youtube.service.YoutubeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author ：wecgwm
 * @date ：2023/07/10 20:01
 */
public abstract class YoutubeController {

    @PostMapping("/download")
    public Response<String> download(@RequestBody YoutubeRequest request){
        getYoutubeService().download(request.getId());
        return Response.ok();
    }

    abstract YoutubeService getYoutubeService();
}
