package com.example.bezma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BezmaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BezmaApplication.class, args);
    }

}
