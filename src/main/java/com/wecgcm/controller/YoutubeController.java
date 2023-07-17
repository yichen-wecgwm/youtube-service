package com.wecgcm.controller;

import com.wecgcm.model.req.YoutubeRequest;
import com.wecgcm.model.resp.Response;
import com.wecgcm.service.YoutubeService;
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
