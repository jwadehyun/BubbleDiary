package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// The React front end (built into src/main/resources/static) is served at "/".
@EnableAsync
@SpringBootApplication
public class BubbleDiaryApplication {

    public static void main(String[] args) {
        SpringApplication.run(BubbleDiaryApplication.class, args);
    }

}
