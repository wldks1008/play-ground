package com.playground.spring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class PlaygroundSpringApplication

fun main(args: Array<String>) {
    runApplication<PlaygroundSpringApplication>(*args)
}
