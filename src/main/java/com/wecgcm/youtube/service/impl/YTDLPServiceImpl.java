package com.wecgcm.youtube.service.impl;

import com.wecgcm.youtube.exception.ProcessException;
import com.wecgcm.youtube.model.arg.YTDLPArg;
import com.wecgcm.youtube.service.YTDLPService;
import com.wecgcm.youtube.util.LogUtil;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * @author ：wecgwm
 * @date ：2023/07/10 17:17
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class YTDLPServiceImpl implements YTDLPService {
    private final YTDLPArg YTDLPArg;

    @Override
    public String download(String videoId) {
        List<String> args = YTDLPArg.build(videoId);
        Timer.Sample timer = Timer.start();

        ProcessBuilder processBuilder = new ProcessBuilder(args)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process process = Try.of(processBuilder::start)
                .getOrElseThrow(ProcessException::new);
        Try.success(process)
                .andThenTry(Process::waitFor)
                .filter(p -> p.exitValue() == 0, p -> {
                    LogUtil.error(process.getErrorStream(), this.getClass());
                    return new ProcessException("process exit error");
                })
                .andFinally(process::destroy)
                .getOrElseThrow(ProcessException::new);

        timer.stop(Timer.builder("yt-dlp-dl")
                .register(Metrics.globalRegistry));
        log.info("download done, videoId: {}", videoId);
        return videoId;
    }

}