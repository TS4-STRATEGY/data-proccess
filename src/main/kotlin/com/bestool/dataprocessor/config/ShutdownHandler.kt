package com.bestool.dataprocessor.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit


@Component
class ShutdownHandler(
    @Qualifier("taskExecutor") private val taskExecutor: Executor
) : ApplicationListener<ContextClosedEvent> {

    private val logger = LoggerFactory.getLogger(ShutdownHandler::class.java)

    override fun onApplicationEvent(event: ContextClosedEvent) {
        if (taskExecutor is ThreadPoolTaskExecutor) {
            logger.info("Iniciando cierre del pool de hilos...")
            taskExecutor.shutdown() // Detener el pool de hilos
            try {
                // Esperar a que las tareas finalicen
                if (!taskExecutor.threadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.warn("Tareas asíncronas no finalizaron en el tiempo esperado.")
                } else {
                    logger.info("Todas las tareas asíncronas finalizaron correctamente.")
                }
            } catch (ex: InterruptedException) {
                logger.error("Error esperando la finalización de tareas asíncronas", ex)
                Thread.currentThread().interrupt()
            }
        }
    }
}