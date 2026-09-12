package com.community.chronic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChronicCareApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronicCareApplication.class, args);
    }
}
