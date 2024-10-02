package com.example.Graduatcion_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class Graduatcion_projectApplication {

    public static void main(String[] args) {
        SpringApplication.run(Graduatcion_projectApplication.class, args);
    }
}
