package com.appsbase.attendly.domain.usecase

import com.appsbase.attendly.domain.model.AttendanceRecord
import com.appsbase.attendly.domain.model.AttendanceStatus
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.model.OfficeLocation
import com.appsbase.attendly.domain.util.TimeValidator
import com.appsbase.attendly.testutil.FakeTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class ValidateAttendanceEligibilityUseCaseTest {

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var timeValidator: TimeValidator
    private lateinit var useCase: ValidateAttendanceEligibilityUseCase

    private val officeLocation = OfficeLocation(
        latitude = 23.810300,
        longitude = 90.412500,
        isSet = true
    )

    private val insideLocation = LocationModel(
        latitude = 23.810400,
        longitude = 90.412500
    )

    private val outsideLocation = LocationModel(
        latitude = 23.812000,
        longitude = 90.412500
    )

    @Before
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeTimeProvider.setTime(LocalTime.of(10, 0)) // 10:00 AM (during shift)
        timeValidator = TimeValidator(fakeTimeProvider)
        useCase = ValidateAttendanceEligibilityUseCase(timeValidator)
    }

    @Test
    fun invoke_whenOfficeLocationNotSet_returnsOfficeNotSet() {
        val result = useCase(
            currentLocation = insideLocation,
            officeLocation = null,
            todayAttendance = null,
            bypassTimeSimulation = false
        )

        assertEquals(AttendanceStatus.OFFICE_NOT_SET, result.status)
        assertFalse(result.isEligible)
    }

    @Test
    fun invoke_whenAttendanceAlreadyMarkedToday_returnsAlreadyMarked() {
        val todayRecord = AttendanceRecord(
            id = "rec-1",
            timestamp = System.currentTimeMillis(),
            latitude = 23.8103,
            longitude = 90.4125,
            distanceMeters = 10
        )

        val result = useCase(
            currentLocation = insideLocation,
            officeLocation = officeLocation,
            todayAttendance = todayRecord,
            bypassTimeSimulation = false
        )

        assertEquals(AttendanceStatus.ALREADY_MARKED, result.status)
        assertFalse(result.isEligible)
    }

    @Test
    fun invoke_whenCurrentLocationNull_returnsOutsideGeofence() {
        val result = useCase(
            currentLocation = null,
            officeLocation = officeLocation,
            todayAttendance = null,
            bypassTimeSimulation = false
        )

        assertEquals(AttendanceStatus.OUTSIDE_GEOFENCE, result.status)
        assertFalse(result.isEligible)
    }

    @Test
    fun invoke_whenOutside50Meters_returnsOutsideGeofence() {
        val result = useCase(
            currentLocation = outsideLocation,
            officeLocation = officeLocation,
            todayAttendance = null,
            bypassTimeSimulation = false
        )

        assertEquals(AttendanceStatus.OUTSIDE_GEOFENCE, result.status)
        assertFalse(result.isEligible)
    }

    @Test
    fun invoke_whenInside50Meters_butOutsideWorkHours_returnsOutsideTimeWindow() {
        fakeTimeProvider.setTime(LocalTime.of(20, 0)) // 08:00 PM (after work)

        val result = useCase(
            currentLocation = insideLocation,
            officeLocation = officeLocation,
            todayAttendance = null,
            bypassTimeSimulation = false
        )

        assertEquals(AttendanceStatus.OUTSIDE_TIME_WINDOW, result.status)
        assertFalse(result.isEligible)
    }

    @Test
    fun invoke_whenInside50Meters_outsideWorkHours_withSimulation_returnsEligible() {
        fakeTimeProvider.setTime(LocalTime.of(20, 0)) // 08:00 PM

        val result = useCase(
            currentLocation = insideLocation,
            officeLocation = officeLocation,
            todayAttendance = null,
            bypassTimeSimulation = true // Simulation enabled
        )

        assertEquals(AttendanceStatus.ELIGIBLE, result.status)
        assertTrue(result.isEligible)
    }

    @Test
    fun invoke_whenInside50Meters_andInsideWorkHours_returnsEligible() {
        val result = useCase(
            currentLocation = insideLocation,
            officeLocation = officeLocation,
            todayAttendance = null,
            bypassTimeSimulation = false
        )

        assertEquals(AttendanceStatus.ELIGIBLE, result.status)
        assertTrue(result.isEligible)
    }
}
