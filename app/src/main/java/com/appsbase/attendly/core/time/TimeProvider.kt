package com.appsbase.attendly.core.time

import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

interface TimeProvider {
    fun now(): LocalDateTime
    fun currentTime(): LocalTime
    fun currentTimeMillis(): Long
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): LocalDateTime = LocalDateTime.now()
    override fun currentTime(): LocalTime = LocalTime.now()
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
