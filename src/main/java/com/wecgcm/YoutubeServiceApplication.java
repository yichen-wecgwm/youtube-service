package com.wecgcm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class YoutubeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(YoutubeServiceApplication.class, args);
    }

    @GetMapping("/up")
    public String up(){
        return "up!";
    }

}
