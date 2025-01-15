package com.bestool.dataprocessor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.PropertySource
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication(scanBasePackages = ["com.bestool"])
@PropertySource("classpath:application.properties")
@EnableAsync
class DataProcessorApplication

fun main(args: Array<String>) {
    runApplication<DataProcessorApplication>(*args)
}
