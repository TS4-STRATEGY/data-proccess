package com.bestool.dataprocessor.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableAsync
class AsyncConfig {
    private val logger = LoggerFactory.getLogger(AsyncConfig::class.java)

    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        executor.maxPoolSize = 50
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("AsyncExecutor-")
        executor.setRejectedExecutionHandler { task, executor ->
            logger.error("Tarea rechazada: ${task.toString()} debido a la saturación del ejecutor.")
        }
        executor.initialize()
        return executor
    }


}
