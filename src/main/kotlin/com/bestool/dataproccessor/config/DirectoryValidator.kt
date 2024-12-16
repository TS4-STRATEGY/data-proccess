package com.bestool.dataproccessor.config

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File

@Component
class DirectoryValidator(
    @Value("\${directory.input.path}") private val directoryPath: String,
    @Value("\${directory.processed.path}") private val processedPath: String
) {
    @PostConstruct
    fun validateDirectory() {
        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            throw IllegalStateException("The directory $directoryPath does not exist or is not a directory.")
        }
        if (!directory.canRead()) {
            throw IllegalStateException("The directory $directoryPath cannot be read.")
        }
    }
}