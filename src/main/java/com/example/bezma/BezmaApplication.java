package com.example.bezma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableJpaRepositories(basePackages = "com.example.bezma.repository")

public class BezmaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BezmaApplication.class, args);
    }

}
