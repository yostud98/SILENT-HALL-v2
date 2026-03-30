package com.umat.quiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class UmatQuizApplication {
    public static void main(String[] args) {
        SpringApplication.run(UmatQuizApplication.class, args);
    }
}
