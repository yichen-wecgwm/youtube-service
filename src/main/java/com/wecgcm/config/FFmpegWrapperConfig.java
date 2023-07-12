package com.wecgcm.config;

import com.wecgcm.exception.ConfigException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @author ：wecgwm
 * @date ：2023/07/11 16:27
 */
@SuppressWarnings("SpellCheckingInspection")
@Slf4j
@Configuration
public class FFmpegWrapperConfig {

    @Value("${ffmpeg.path}")
    private String path;

    @Bean
    public FFmpegExecutor executor(){
        try {
            FFmpeg fFmpeg = new FFmpeg(path);
            return new FFmpegExecutor(fFmpeg);
        } catch (IOException e) {
            Counter.builder("config")
                    .tag("exception", "ffmpeg_init")
                    .register(Metrics.globalRegistry)
                    .increment();
            log.error("ffmpeg init fail, path: {}", path);
            throw new ConfigException("ffmpeg init exception", e);
        }
    }

}