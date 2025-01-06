package com.bestool.dataprocessor.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun api(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("btpas-centros-costos-ms API")
                    .description("API for managing cost centers")
                    .version("1.0")
            )
    }
}
