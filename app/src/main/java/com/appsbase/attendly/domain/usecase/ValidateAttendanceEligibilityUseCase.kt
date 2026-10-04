package com.appsbase.attendly.domain.usecase

import com.appsbase.attendly.domain.model.AttendanceRecord
import com.appsbase.attendly.domain.model.AttendanceStatus
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.model.OfficeLocation
import com.appsbase.attendly.domain.util.GeoFenceCalculator
import com.appsbase.attendly.domain.util.TimeValidator
import javax.inject.Inject

data class AttendanceEligibility(
    val status: AttendanceStatus,
    val distanceMeters: Int?,
    val isEligible: Boolean
)

class ValidateAttendanceEligibilityUseCase @Inject constructor(
    private val timeValidator: TimeValidator
) {
    operator fun invoke(
        currentLocation: LocationModel?,
        officeLocation: OfficeLocation?,
        todayAttendance: AttendanceRecord?,
        bypassTimeSimulation: Boolean
    ): AttendanceEligibility {
        if (officeLocation == null || !officeLocation.isSet) {
            return AttendanceEligibility(
                status = AttendanceStatus.OFFICE_NOT_SET,
                distanceMeters = null,
                isEligible = false
            )
        }

        if (todayAttendance != null) {
            val distance = if (currentLocation != null) {
                GeoFenceCalculator.calculateDistanceMeters(
                    startLat = currentLocation.latitude,
                    startLng = currentLocation.longitude,
                    endLat = officeLocation.latitude,
                    endLng = officeLocation.longitude
                )
            } else null
            return AttendanceEligibility(
                status = AttendanceStatus.ALREADY_MARKED,
                distanceMeters = distance,
                isEligible = false
            )
        }

        if (currentLocation == null) {
            return AttendanceEligibility(
                status = AttendanceStatus.OUTSIDE_GEOFENCE,
                distanceMeters = null,
                isEligible = false
            )
        }

        val distance = GeoFenceCalculator.calculateDistanceMeters(
            startLat = currentLocation.latitude,
            startLng = currentLocation.longitude,
            endLat = officeLocation.latitude,
            endLng = officeLocation.longitude
        )

        val isInsideGeofence = GeoFenceCalculator.isWithinGeofence(distance)
        if (!isInsideGeofence) {
            return AttendanceEligibility(
                status = AttendanceStatus.OUTSIDE_GEOFENCE,
                distanceMeters = distance,
                isEligible = false
            )
        }

        val isWithinTime = timeValidator.isWithinWorkHours(bypassSimulation = bypassTimeSimulation)
        if (!isWithinTime) {
            return AttendanceEligibility(
                status = AttendanceStatus.OUTSIDE_TIME_WINDOW,
                distanceMeters = distance,
                isEligible = false
            )
        }

        return AttendanceEligibility(
            status = AttendanceStatus.ELIGIBLE,
            distanceMeters = distance,
            isEligible = true
        )
    }
}
