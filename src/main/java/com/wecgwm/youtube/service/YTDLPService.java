package com.wecgwm.youtube.service;

import com.wecgwm.youtube.exception.YTDLPException;
import com.wecgwm.youtube.model.dto.ChannelDto;
import com.wecgwm.youtube.model.dto.VideoInfoDto;
import com.wecgwm.youtube.util.LogUtil;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.vavr.CheckedFunction1;
import io.vavr.control.Try;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author ：wecgwm
 * @date ：2023/07/21 0:16
 */
public interface YTDLPService {
    int PROCESS_NORMAL_TERMINATION = 0;

    /**
     * Search some videoId that have not been downloaded
     */
    CompletableFuture<List<VideoInfoDto>> search(ChannelDto channel);

    /**
     * @param videoId videoId e.g. JpTqSzm4JOk in www.youtube.com/watch?v=JpTqSzm4JOk
     */
    void download(String videoId);

    List<String> getVideoInfo(String videoId, String... target);

    Logger getLog();

    default <T> T processTemplate(Supplier<ProcessBuilder> processBuilderSupplier, CheckedFunction1<Process, T> function, String key) {
        Timer.Sample timer = Timer.start();

        ProcessBuilder processBuilder = processBuilderSupplier.get();
        Process process = Try.of(processBuilder::start)
                .getOrElseThrow(YTDLPException::new);

        T ret = Try.success(process)
                .mapTry(function)
                .andThenTry(process::waitFor)
                .filter(__ -> process.exitValue() == PROCESS_NORMAL_TERMINATION, p -> {
                    getLog().error("{} command:{}, thread:{}", key, processBuilder.command(), Thread.currentThread());
                    LogUtil.error(process.errorReader(), this.getClass());
                    return new YTDLPException("yt-dlp " + key + " process exit error");
                })
                .andFinally(process::destroy)
                .getOrElseThrow(YTDLPException::new);

        timer.stop(Timer.builder("yt-dlp-" + key)
                .register(Metrics.globalRegistry));
        getLog().info("{} command:{}, thread:{}, ret:{}", key, processBuilder.command(), Thread.currentThread(), ret);
        return ret;
    }
}