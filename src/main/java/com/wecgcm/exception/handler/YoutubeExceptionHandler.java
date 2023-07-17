package com.wecgcm.exception.handler;

import com.wecgcm.model.resp.Response;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
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
        recordOnException(e);
        return Response.from(e);
    }

    private void recordOnException(Throwable e){
        log.error("msg:{}", e.getMessage(), e);
        Counter.builder("exception.handler")
                .tag("exception", e.getClass().getSimpleName())
                .register(Metrics.globalRegistry)
                .increment();
    }
}
