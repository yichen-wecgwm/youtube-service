package com.wecgwm.youtube.model.req;

import javax.validation.constraints.NotNull;

/**
 * e.g. JpTqSzm4JOk in www.youtube.com/watch?v=JpTqSzm4JOk
 *
 * @author ：wecgwm
 * @date ：2023/07/10 20:42
 */
public record YoutubeVideoRequest (@NotNull String videoId){ }
