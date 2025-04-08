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
    // Evita handlers duplicados
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    SLF4JBridgeHandler.removeHandlersForRootLogger()

    // Limpia el root logger de JUL (por si WebLogic puso algo raro)
    LogManager.getLogManager().reset()

    // Instala el puente JUL → SLF4J
    SLF4JBridgeHandler.install()
    runApplication<DataProcessorApplication>(*args)
}

