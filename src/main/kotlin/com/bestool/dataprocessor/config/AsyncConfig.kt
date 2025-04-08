package com.bestool.dataprocessor.config

import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.lang.reflect.Method
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {

    private val logger = LoggerFactory.getLogger(AsyncConfig::class.java)

    @Bean(name = ["taskExecutor"])
    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        executor.maxPoolSize = 50
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("AsyncExecutor-")
        executor.setRejectedExecutionHandler { task, _ ->
            logger.error("X Tarea rechazada por saturación del ejecutor: $task")
        }
        executor.initialize()
        return executor
    }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return object : AsyncUncaughtExceptionHandler {
            override fun handleUncaughtException(ex: Throwable, method: Method, vararg params: Any?) {
                logger.error("! Excepción no capturada en método async: ${method.name}", ex)
                val lastSQL = com.bestool.dataprocessor.DynamicSQLInspector.getLastSQL()
                if (lastSQL != null) {
                    logger.error("--Último SQL registrado por DynamicSQLInspector: $lastSQL")
                }
            }
        }
    }
}