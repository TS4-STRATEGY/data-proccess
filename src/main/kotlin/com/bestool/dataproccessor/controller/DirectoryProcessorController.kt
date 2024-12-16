package com.bestool.dataproccessor.controller

import com.bestool.dataproccessor.service.DirectoryProcessorService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DirectoryProcessorController(private val directoryProcessorService: DirectoryProcessorService) {

    @GetMapping("/process-directory")
    fun processDirectory(): String {
        // Inicia el proceso en segundo plano
        directoryProcessorService.processDirectoryAsync()

        // Devuelve el estado actual
        val status = directoryProcessorService.readOrInitializeStatus()

        return """{"status": "success", "currentStatus": "$status"}"""
    }

    @GetMapping("/status")
    fun getStatus(): String {
        return directoryProcessorService.getStatus()
    }

}
