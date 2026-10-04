package com.appsbase.attendly.domain.util

import com.appsbase.attendly.testutil.FakeTimeProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class TimeValidatorTest {

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var timeValidator: TimeValidator

    @Before
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        timeValidator = TimeValidator(fakeTimeProvider)
    }

    @Test
    fun isWithinWorkHours_whenWithinShift_returnsTrue() {
        fakeTimeProvider.setTime(LocalTime.of(10, 30))
        assertTrue(timeValidator.isWithinWorkHours())

        fakeTimeProvider.setTime(LocalTime.of(14, 0))
        assertTrue(timeValidator.isWithinWorkHours())
    }

    @Test
    fun isWithinWorkHours_exactBoundaries_returnsTrue() {
        // Shift start 09:00 AM
        fakeTimeProvider.setTime(LocalTime.of(9, 0))
        assertTrue(timeValidator.isWithinWorkHours())

        // Shift end 06:00 PM (18:00)
        fakeTimeProvider.setTime(LocalTime.of(18, 0))
        assertTrue(timeValidator.isWithinWorkHours())
    }

    @Test
    fun isWithinWorkHours_beforeShift_returnsFalse() {
        // 08:59 AM
        fakeTimeProvider.setTime(LocalTime.of(8, 59))
        assertFalse(timeValidator.isWithinWorkHours())
    }

    @Test
    fun isWithinWorkHours_afterShift_returnsFalse() {
        // 06:01 PM
        fakeTimeProvider.setTime(LocalTime.of(18, 1))
        assertFalse(timeValidator.isWithinWorkHours())

        // 10:00 PM
        fakeTimeProvider.setTime(LocalTime.of(22, 0))
        assertFalse(timeValidator.isWithinWorkHours())
    }

    @Test
    fun isWithinWorkHours_whenSimulationBypassed_returnsTrueRegardlessOfTime() {
        // Midnight 01:00 AM outside shift
        fakeTimeProvider.setTime(LocalTime.of(1, 0))

        assertFalse(timeValidator.isWithinWorkHours(bypassSimulation = false))
        assertTrue(timeValidator.isWithinWorkHours(bypassSimulation = true))
    }
}
