package com.wecgcm.youtube.schedule;

import com.wecgcm.youtube.service.YouTubeVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author ：wecgwm
 * @date ：2023/07/21 16:23
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_= @Autowired)
@ConditionalOnProperty(name = "yt.schedule.enable", havingValue = "true")
@Component
public class YoutubeSchedule {
    private final YouTubeVideoService youTubeVideoService;

    @Scheduled(cron = "${yt.scan.cron}")
    public void scanVideo(){
        log.info("schedule scan video start........");
        youTubeVideoService.scanAsync();
        // todo temp
        log.info("schedule scan video end........");
    }

}

