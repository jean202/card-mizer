package com.jean202.cardmizer.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.jean202.cardmizer")
public class CardMizerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CardMizerApplication.class, args);
    }
}
