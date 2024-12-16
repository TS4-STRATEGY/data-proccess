package com.bestool.dataproccessor.config

import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor

class CustomRejectedExecutionHandler : RejectedExecutionHandler {
    override fun rejectedExecution(r: Runnable, executor: ThreadPoolExecutor) {
        try {
            Thread.sleep(1000) // Espera un segundo antes de reintentar
            executor.execute(r) // Reintenta agregar la tarea
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RejectedExecutionException("Task rejected and interrupted", e)
        }
    }
}