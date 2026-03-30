package com.example.bezma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BezmaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BezmaApplication.class, args);
    }

}
