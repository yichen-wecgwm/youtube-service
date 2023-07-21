package com.wecgcm.youtube;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class YoutubeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(YoutubeServiceApplication.class, args);
    }

}
