package com.bestool.dataprocessor.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.multipart.commons.CommonsMultipartResolver

@Configuration
class AppConfig {
    @Bean
    fun multipartResolver(): CommonsMultipartResolver {
        val resolver = CommonsMultipartResolver()
        resolver.setMaxUploadSize(52428800) // Tamaño máximo: 50 MB
        return resolver
    }

}