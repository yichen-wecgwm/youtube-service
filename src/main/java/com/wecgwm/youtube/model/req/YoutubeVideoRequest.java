package com.wecgwm.youtube.model.req;

import lombok.Data;

import javax.validation.constraints.NotNull;


/**
 * @author ：wecgwm
 * @date ：2023/07/10 20:42
 */
@Data
public class YoutubeVideoRequest {

    /**
     * e.g. JpTqSzm4JOk in www.youtube.com/watch?v=JpTqSzm4JOk
     */
    @NotNull
    private String videoId;

}
