package com.bestool.dataproccessor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class DataProccessorApplication

fun main(args: Array<String>) {
    runApplication<DataProccessorApplication>(*args)
}
