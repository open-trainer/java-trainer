package com.opentrainer.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.opentrainer")
@EnableScheduling
public class JavaTrainerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(JavaTrainerApplication.class, args);
    }
}