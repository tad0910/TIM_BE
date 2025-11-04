package com.tim.appTim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AppTimApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppTimApplication.class, args);
    }
}