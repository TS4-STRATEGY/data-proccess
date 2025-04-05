package com.bestool.dataprocessor

import org.slf4j.bridge.SLF4JBridgeHandler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.PropertySource
import org.springframework.scheduling.annotation.EnableAsync
import java.util.logging.LogManager

@SpringBootApplication(scanBasePackages = ["com.bestool"])
@PropertySource("classpath:application.properties")
@EnableAsync
class DataProcessorApplication

fun main(args: Array<String>) {
    LogManager.getLogManager().reset()
    SLF4JBridgeHandler.install()
    runApplication<DataProcessorApplication>(*args)
}

