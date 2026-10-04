package com.appsbase.attendly.domain.model

import androidx.compose.runtime.Stable

@Stable
data class LocationModel(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f
)

@Stable
data class OfficeLocation(
    val latitude: Double,
    val longitude: Double,
    val isSet: Boolean = true
)

@Stable
data class AttendanceRecord(
    val id: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int
)

@Stable
data class SimulationConfig(
    val bypassTimeValidation: Boolean = false
)

enum class AttendanceStatus {
    OFFICE_NOT_SET,
    OUTSIDE_GEOFENCE,
    OUTSIDE_TIME_WINDOW,
    ELIGIBLE,
    ALREADY_MARKED
}
