package com.jean202.cardmizer.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.jean202.cardmizer"])
class CardMizerApplication

fun main(args: Array<String>) {
    runApplication<CardMizerApplication>(*args)
}
