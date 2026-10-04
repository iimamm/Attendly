package com.appsbase.attendly.domain.util

import com.appsbase.attendly.core.time.TimeProvider
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeValidator @Inject constructor(
    private val timeProvider: TimeProvider
) {
    companion object {
        val DEFAULT_WORK_START: LocalTime = LocalTime.of(9, 0)
        val DEFAULT_WORK_END: LocalTime = LocalTime.of(18, 0)
    }

    fun isWithinWorkHours(
        currentTime: LocalTime = timeProvider.currentTime(),
        startTime: LocalTime = DEFAULT_WORK_START,
        endTime: LocalTime = DEFAULT_WORK_END,
        bypassSimulation: Boolean = false
    ): Boolean {
        if (bypassSimulation) return true
        return (currentTime == startTime || currentTime.isAfter(startTime)) &&
                (currentTime == endTime || currentTime.isBefore(endTime))
    }

    fun getWorkHoursFormatted(
        startTime: LocalTime = DEFAULT_WORK_START,
        endTime: LocalTime = DEFAULT_WORK_END
    ): Pair<String, String> {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
        return Pair(startTime.format(formatter), endTime.format(formatter))
    }
}
