package com.appsbase.attendly.domain.usecase

import com.appsbase.attendly.domain.model.AttendanceRecord
import com.appsbase.attendly.domain.model.AttendanceStatus
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.model.OfficeLocation
import com.appsbase.attendly.domain.repository.AttendanceRepository
import javax.inject.Inject

class MarkAttendanceUseCase @Inject constructor(
    private val repository: AttendanceRepository,
    private val validateEligibility: ValidateAttendanceEligibilityUseCase
) {
    suspend operator fun invoke(
        currentLocation: LocationModel,
        officeLocation: OfficeLocation,
        todayAttendance: AttendanceRecord?,
        bypassTimeSimulation: Boolean
    ): Result<AttendanceRecord> {
        val eligibility = validateEligibility(
            currentLocation = currentLocation,
            officeLocation = officeLocation,
            todayAttendance = todayAttendance,
            bypassTimeSimulation = bypassTimeSimulation
        )

        return when (eligibility.status) {
            AttendanceStatus.ELIGIBLE -> {
                val record = repository.markAttendance(
                    location = currentLocation,
                    distanceMeters = eligibility.distanceMeters ?: 0
                )
                Result.success(record)
            }
            AttendanceStatus.ALREADY_MARKED -> Result.failure(IllegalStateException("Attendance already marked for today"))
            AttendanceStatus.OUTSIDE_GEOFENCE -> Result.failure(IllegalStateException("Cannot mark attendance: Outside 50m geofence radius"))
            AttendanceStatus.OUTSIDE_TIME_WINDOW -> Result.failure(IllegalStateException("Cannot mark attendance: Outside working hours"))
            AttendanceStatus.OFFICE_NOT_SET -> Result.failure(IllegalStateException("Cannot mark attendance: Office location not set"))
        }
    }
}
