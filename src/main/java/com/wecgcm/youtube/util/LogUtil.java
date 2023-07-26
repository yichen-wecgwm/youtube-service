package com.wecgcm.youtube.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Function;

/**
 * @author ：wecgwm
 * @date ：2023/07/18 23:03
 */
@Slf4j
public class LogUtil {

    public static void error(BufferedReader reader, Class<?> clazz) {
        reader.lines().forEach(s -> LoggerFactory.getLogger(clazz).error(s));
        Try.success(reader).andThenTry(BufferedReader::close).get();
    }

    public static <T> Function<Throwable, T> completionExceptionally() {
        return e -> {
            LogUtil.recordOnExceptionHandler(Thread.currentThread(), e);
            return null;
        };
    }

    public static void recordOnExceptionHandler(Thread t, Throwable e) {
        log.error("thread:{}, msg:{}", t, e.getMessage(), e);
        Counter.builder("exception.handler")
                .tag("exception", e.getClass().getSimpleName())
                .tag("thread", t.getName())
                .register(Metrics.globalRegistry)
                .increment();
    }

}
