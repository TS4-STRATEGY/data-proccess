package com.bestool.dataprocessor

import org.hibernate.resource.jdbc.spi.StatementInspector
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

class DynamicSQLInspector : StatementInspector {
    companion object {
        private val log = LoggerFactory.getLogger(DynamicSQLInspector::class.java)
        val isEnabled = AtomicBoolean(false)
        private val lastSQL = ThreadLocal<String>()
        fun getLastSQL(): String? = lastSQL.get()
    }

    override fun inspect(sql: String): String? {
        if (isEnabled.get()) {
            val trimmed = sql.trimStart().lowercase()
            if (!trimmed.startsWith("select")) {
                log.info("${now()} - $sql")
            }
        }
        lastSQL.set(sql)
        return sql
    }

    private fun now(): String = java.time.LocalDateTime.now().toString()
}
