package com.bestool.dataprocessor.controller

import com.bestool.dataprocessor.DynamicSQLInspector
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/debug/sql")
class SQLLogController {

    @PostMapping("/enable")
    fun enable(): String {
        DynamicSQLInspector.isEnabled.set(true)
        return "SQL logging ENABLED"
    }

    @PostMapping("/disable")
    fun disable(): String {
        DynamicSQLInspector.isEnabled.set(false)
        return "SQL logging DISABLED"
    }

    @GetMapping("/status")
    fun status(): String {
        return if (DynamicSQLInspector.isEnabled.get()) "SQL logging is ON" else "SQL logging is OFF"
    }
}
