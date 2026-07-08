package com.playground.kafka

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PlaygroundKafkaApplication

fun main(args: Array<String>) {
    runApplication<PlaygroundKafkaApplication>(*args)
}
