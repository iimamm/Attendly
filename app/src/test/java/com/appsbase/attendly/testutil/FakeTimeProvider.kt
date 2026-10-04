package com.appsbase.attendly.testutil

import com.appsbase.attendly.core.time.TimeProvider
import java.time.LocalDateTime
import java.time.LocalTime

class FakeTimeProvider(
    private var customDateTime: LocalDateTime = LocalDateTime.of(2026, 10, 4, 10, 0),
    private var customTimeMillis: Long = 1728038400000L
) : TimeProvider {

    fun setTime(time: LocalTime) {
        customDateTime = customDateTime.withHour(time.hour).withMinute(time.minute).withSecond(time.second)
    }

    fun setDateTime(dateTime: LocalDateTime) {
        customDateTime = dateTime
    }

    override fun now(): LocalDateTime = customDateTime

    override fun currentTime(): LocalTime = customDateTime.toLocalTime()

    override fun currentTimeMillis(): Long = customTimeMillis
}
