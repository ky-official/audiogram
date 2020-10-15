package com.audiogram.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AudiogramApplication

fun main(args: Array<String>) {
    runApplication<AudiogramApplication>(*args)
}
