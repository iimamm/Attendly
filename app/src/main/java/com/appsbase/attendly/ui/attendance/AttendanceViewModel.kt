package com.appsbase.attendly.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appsbase.attendly.data.location.LocationTracker
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.repository.AttendanceRepository
import com.appsbase.attendly.domain.usecase.MarkAttendanceUseCase
import com.appsbase.attendly.domain.usecase.SaveOfficeLocationUseCase
import com.appsbase.attendly.domain.usecase.ValidateAttendanceEligibilityUseCase
import com.appsbase.attendly.domain.util.GeoFenceCalculator
import com.appsbase.attendly.domain.util.TimeValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val repository: AttendanceRepository,
    private val locationTracker: LocationTracker,
    private val validateEligibilityUseCase: ValidateAttendanceEligibilityUseCase,
    private val markAttendanceUseCase: MarkAttendanceUseCase,
    private val saveOfficeLocationUseCase: SaveOfficeLocationUseCase,
    private val timeValidator: TimeValidator
) : ViewModel() {

    private val _state = MutableStateFlow(AttendanceState())
    val state: StateFlow<AttendanceState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<AttendanceEffect>()
    val effect: SharedFlow<AttendanceEffect> = _effect.asSharedFlow()

    private var locationUpdatesJob: Job? = null

    init {
        observeRepositoryData()
        checkInitialPermissions()
    }

    private fun checkInitialPermissions() {
        val hasPermission = locationTracker.hasLocationPermission()
        val isGpsOn = locationTracker.isGpsEnabled()
        _state.update {
            it.copy(
                hasLocationPermission = hasPermission,
                isGpsEnabled = isGpsOn
            )
        }
        if (hasPermission) {
            startLocationTracking()
        }
    }

    private fun observeRepositoryData() {
        viewModelScope.launch {
            repository.officeLocation.collectLatest { office ->
                _state.update { current ->
                    current.copy(
                        officeLocation = office,
                        targetOfficeLocation = current.targetOfficeLocation
                            ?: office?.let { LocationModel(it.latitude, it.longitude) }
                    )
                }
                recalculateEligibility()
            }
        }

        viewModelScope.launch {
            repository.todayAttendance.collectLatest { today ->
                _state.update { it.copy(todayAttendance = today) }
                recalculateEligibility()
            }
        }

        viewModelScope.launch {
            repository.attendanceHistory.collectLatest { history ->
                _state.update { it.copy(attendanceHistory = history) }
            }
        }

        viewModelScope.launch {
            repository.simulationConfig.collectLatest { config ->
                _state.update { it.copy(simulationConfig = config) }
                recalculateEligibility()
            }
        }
    }

    private fun startLocationTracking() {
        locationUpdatesJob?.cancel()
        locationUpdatesJob = viewModelScope.launch {
            // First fetch immediate one-off location to center map quickly
            val initialLocation = locationTracker.getCurrentLocation()
            if (initialLocation != null) {
                _state.update { current ->
                    current.copy(
                        currentLocation = initialLocation,
                        targetOfficeLocation = current.targetOfficeLocation ?: initialLocation
                    )
                }
                recalculateEligibility()
            }

            // Stream continuous location updates
            locationTracker.locationUpdates.collectLatest { location ->
                _state.update { current ->
                    current.copy(
                        currentLocation = location,
                        targetOfficeLocation = current.targetOfficeLocation ?: location
                    )
                }
                recalculateEligibility()
            }
        }
    }

    private fun recalculateEligibility() {
        val current = _state.value
        val eligibility = validateEligibilityUseCase(
            currentLocation = current.currentLocation,
            officeLocation = current.officeLocation,
            todayAttendance = current.todayAttendance,
            bypassTimeSimulation = current.simulationConfig.bypassTimeValidation
        )

        val isWithinTime = timeValidator.isWithinWorkHours(
            bypassSimulation = current.simulationConfig.bypassTimeValidation
        )

        val isInsideGeofence = eligibility.distanceMeters?.let {
            GeoFenceCalculator.isWithinGeofence(it)
        } ?: false

        _state.update {
            it.copy(
                eligibilityStatus = eligibility.status,
                distanceMeters = eligibility.distanceMeters,
                isWithinGeofence = isInsideGeofence,
                isWithinTimeWindow = isWithinTime
            )
        }
    }

    fun onIntent(intent: AttendanceIntent) {
        when (intent) {
            is AttendanceIntent.RefreshLocationState -> {
                checkInitialPermissions()
            }

            is AttendanceIntent.PermissionResultReceived -> {
                _state.update { it.copy(hasLocationPermission = intent.isGranted) }
                if (intent.isGranted) {
                    startLocationTracking()
                }
            }

            is AttendanceIntent.GpsStatusUpdated -> {
                _state.update { it.copy(isGpsEnabled = intent.isEnabled) }
                if (intent.isEnabled && _state.value.hasLocationPermission) {
                    startLocationTracking()
                }
            }

            is AttendanceIntent.MapCameraMoved -> {
                _state.update { it.copy(targetOfficeLocation = intent.centerLocation) }
            }

            is AttendanceIntent.CenterMapOnCurrentLocation -> {
                _state.value.currentLocation?.let { currentLoc ->
                    _state.update { it.copy(targetOfficeLocation = currentLoc) }
                    viewModelScope.launch {
                        _effect.emit(AttendanceEffect.AnimateMapCamera(currentLoc))
                    }
                }
            }

            is AttendanceIntent.SaveOfficeLocationClicked -> {
                saveOfficeLocation()
            }

            is AttendanceIntent.MarkAttendanceClicked -> {
                markAttendance()
            }

            is AttendanceIntent.ToggleTimeSimulation -> {
                toggleTimeSimulation()
            }

            is AttendanceIntent.ShowHistorySheet -> {
                _state.update { it.copy(showHistorySheet = intent.show) }
            }

            is AttendanceIntent.ShowResetConfirmDialog -> {
                _state.update { it.copy(showResetConfirmDialog = intent.show) }
            }

            is AttendanceIntent.ShowUndoConfirmDialog -> {
                _state.update { it.copy(showUndoConfirmDialog = intent.show) }
            }

            is AttendanceIntent.ConfirmResetAll -> {
                resetAllData()
            }

            is AttendanceIntent.ConfirmUndoAttendance -> {
                undoTodayAttendance()
            }

            is AttendanceIntent.ClearUserMessage -> {
                _state.update { it.copy(userFeedbackMessage = null) }
            }
        }
    }

    private fun saveOfficeLocation() {
        val target = _state.value.targetOfficeLocation ?: _state.value.currentLocation
        if (target == null) {
            viewModelScope.launch {
                _effect.emit(AttendanceEffect.ShowSnackbar("Cannot determine location to set"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSavingOffice = true) }
            val result = saveOfficeLocationUseCase(target)
            _state.update { it.copy(isSavingOffice = false) }

            if (result.isSuccess) {
                _effect.emit(AttendanceEffect.ShowSnackbar("Office location updated successfully!"))
            } else {
                _effect.emit(AttendanceEffect.ShowSnackbar("Failed to update office location"))
            }
        }
    }

    private fun markAttendance() {
        val currentLoc = _state.value.currentLocation
        val office = _state.value.officeLocation

        if (currentLoc == null || office == null) {
            viewModelScope.launch {
                _effect.emit(AttendanceEffect.ShowSnackbar("Location details not fully initialized"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isMarkingAttendance = true) }
            val result = markAttendanceUseCase(
                currentLocation = currentLoc,
                officeLocation = office,
                todayAttendance = _state.value.todayAttendance,
                bypassTimeSimulation = _state.value.simulationConfig.bypassTimeValidation
            )
            _state.update { it.copy(isMarkingAttendance = false) }

            if (result.isSuccess) {
                _effect.emit(AttendanceEffect.ShowSnackbar("Attendance marked successfully!"))
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unable to mark attendance"
                _effect.emit(AttendanceEffect.ShowSnackbar(errorMsg))
            }
        }
    }

    private fun toggleTimeSimulation() {
        viewModelScope.launch {
            val currentVal = _state.value.simulationConfig.bypassTimeValidation
            val newVal = !currentVal
            repository.setTimeBypassSimulation(newVal)
            val message = if (newVal) {
                "Time validation bypassed (Simulation Mode)"
            } else {
                "Standard office hours restored"
            }
            _effect.emit(AttendanceEffect.ShowSnackbar(message))
        }
    }

    private fun undoTodayAttendance() {
        viewModelScope.launch {
            repository.undoTodayAttendance()
            _state.update { it.copy(showUndoConfirmDialog = false) }
            _effect.emit(AttendanceEffect.ShowSnackbar("Today's attendance was undone"))
        }
    }

    private fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            _state.update {
                it.copy(
                    showResetConfirmDialog = false,
                    officeLocation = null,
                    targetOfficeLocation = it.currentLocation
                )
            }
            _effect.emit(AttendanceEffect.ShowSnackbar("All settings and attendance history reset!"))
        }
    }
}
