package com.appsbase.attendly.ui.attendance

import com.appsbase.attendly.domain.model.AttendanceRecord
import com.appsbase.attendly.domain.model.AttendanceStatus
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.model.OfficeLocation
import com.appsbase.attendly.domain.model.SimulationConfig

data class AttendanceState(
    val currentLocation: LocationModel? = null,
    val officeLocation: OfficeLocation? = null,
    val targetOfficeLocation: LocationModel? = null, // Location pinned/dragged on the map
    val distanceMeters: Int? = null,
    val eligibilityStatus: AttendanceStatus = AttendanceStatus.OFFICE_NOT_SET,
    val isWithinGeofence: Boolean = false,
    val isWithinTimeWindow: Boolean = false,
    val todayAttendance: AttendanceRecord? = null,
    val attendanceHistory: List<AttendanceRecord> = emptyList(),
    val simulationConfig: SimulationConfig = SimulationConfig(),
    val hasLocationPermission: Boolean = false,
    val isGpsEnabled: Boolean = true,
    val isMarkingAttendance: Boolean = false,
    val isSavingOffice: Boolean = false,
    val showHistorySheet: Boolean = false,
    val showResetConfirmDialog: Boolean = false,
    val showUndoConfirmDialog: Boolean = false,
    val userFeedbackMessage: String? = null
)

sealed interface AttendanceIntent {
    data object RefreshLocationState : AttendanceIntent
    data class PermissionResultReceived(val isGranted: Boolean) : AttendanceIntent
    data class GpsStatusUpdated(val isEnabled: Boolean) : AttendanceIntent
    data class MapCameraMoved(val centerLocation: LocationModel) : AttendanceIntent
    data object CenterMapOnCurrentLocation : AttendanceIntent
    data object SaveOfficeLocationClicked : AttendanceIntent
    data object MarkAttendanceClicked : AttendanceIntent
    data object ToggleTimeSimulation : AttendanceIntent
    data class ShowHistorySheet(val show: Boolean) : AttendanceIntent
    data class ShowResetConfirmDialog(val show: Boolean) : AttendanceIntent
    data class ShowUndoConfirmDialog(val show: Boolean) : AttendanceIntent
    data object ConfirmResetAll : AttendanceIntent
    data object ConfirmUndoAttendance : AttendanceIntent
    data object ClearUserMessage : AttendanceIntent
}

sealed interface AttendanceEffect {
    data class ShowSnackbar(val message: String) : AttendanceEffect
    data class AnimateMapCamera(val location: LocationModel) : AttendanceEffect
}
