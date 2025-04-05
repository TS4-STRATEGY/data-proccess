package com.bestool.dataprocessor.utils

object TransactionContext {
    private val lastException = ThreadLocal<Throwable?>()

    fun setException(ex: Throwable) {
        lastException.set(ex)
    }

    fun getException(): Throwable? = lastException.get()

    fun clear() {
        lastException.remove()
    }
}