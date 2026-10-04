package com.appsbase.attendly.domain.usecase

import com.appsbase.attendly.domain.model.AttendanceRecord
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.model.OfficeLocation
import com.appsbase.attendly.domain.repository.AttendanceRepository
import javax.inject.Inject

class MarkAttendanceUseCase @Inject constructor(
    private val repository: AttendanceRepository,
    private val validateEligibility: ValidateAttendanceEligibilityUseCase
) {
    /**
     * Marks attendance after re-validating eligibility. Returns null when any
     * rule (office set, inside geofence, within hours, not already marked) fails.
     */
    suspend operator fun invoke(
        currentLocation: LocationModel,
        officeLocation: OfficeLocation,
        todayAttendance: AttendanceRecord?,
        bypassTimeSimulation: Boolean
    ): AttendanceRecord? {
        val eligibility = validateEligibility(
            currentLocation = currentLocation,
            officeLocation = officeLocation,
            todayAttendance = todayAttendance,
            bypassTimeSimulation = bypassTimeSimulation
        )
        if (!eligibility.isEligible) return null

        return repository.markAttendance(
            location = currentLocation,
            distanceMeters = eligibility.distanceMeters ?: 0
        )
    }
}
