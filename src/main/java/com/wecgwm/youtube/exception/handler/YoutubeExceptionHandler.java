package com.wecgwm.youtube.exception.handler;

import com.wecgwm.youtube.model.resp.Response;
import com.wecgwm.youtube.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author ：wecgwm
 * @date ：2023/07/10 21:05
 */
@RestControllerAdvice
@Slf4j
public class YoutubeExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Response<String> lastHandler(Throwable e){
        LogUtil.recordOnExceptionHandler(Thread.currentThread(), e);
        return Response.from(e);
    }

}