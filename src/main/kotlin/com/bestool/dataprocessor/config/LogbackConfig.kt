package com.bestool.dataprocessor.config

import com.bestool.dataprocessor.BuildConfig
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
class LogbackConfig {

    @PostConstruct
    fun setupLogback() {
        System.setProperty("LOG_PATH", BuildConfig.LOG_PATH)
        LoggerFactory.getLogger(LogbackConfig::class.java).info("LOG_PATH set ambient: ${BuildConfig.ACTIVE_PROFILE} to: ${BuildConfig.LOG_PATH}")
    }
}