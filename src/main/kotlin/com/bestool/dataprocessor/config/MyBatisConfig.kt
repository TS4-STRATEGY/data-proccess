package com.bestool.dataprocessor.config

import org.mybatis.spring.annotation.MapperScan
import org.springframework.context.annotation.Configuration

@Configuration
@MapperScan("com.bestool.dataprocessor.mapper") // Asegúrate que este es el paquete correcto
class MyBatisConfig
