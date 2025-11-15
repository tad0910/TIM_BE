package com.tim.appTim;

import com.tim.appTim.config.UserAgentInterceptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class AppTimApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppTimApplication.class, args);
    }
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setInterceptors(Collections.singletonList(new UserAgentInterceptor()));

        return restTemplate;
    }
}