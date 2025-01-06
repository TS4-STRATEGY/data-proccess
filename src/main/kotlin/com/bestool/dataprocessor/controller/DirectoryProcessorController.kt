package com.bestool.dataprocessor.controller

import com.bestool.dataprocessor.service.DirectoryProcessorService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.io.File

@RestController
class DirectoryProcessorController(private val directoryProcessorService: DirectoryProcessorService) {

    @GetMapping("/process-directory/{directoryId}")
    fun processDirectory(@PathVariable directoryId: String): String {
        // Inicia el proceso en segundo plano



        // Inicia el proceso en segundo plano
        directoryProcessorService.processDirectoryAsync(directoryId)
        val status = directoryProcessorService.readOrInitializeStatus()

        return """{"status": "success", "currentStatus": "$status"}"""
    }

    @GetMapping("/status")
    fun getStatus(): String {
        return directoryProcessorService.getStatus()
    }

}
