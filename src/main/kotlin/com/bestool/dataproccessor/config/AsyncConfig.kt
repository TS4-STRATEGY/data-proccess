package com.bestool.dataproccessor.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {
    @Bean
    fun taskExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 10  // Número inicial de hilos
            maxPoolSize = 20   // Número máximo de hilos
            queueCapacity = 100 // Capacidad de cola para tareas pendientes
            setRejectedExecutionHandler(CustomRejectedExecutionHandler())
            setThreadNamePrefix("FileProcessor-")
            initialize()
        }
    }
}