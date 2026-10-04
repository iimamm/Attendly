package com.appsbase.attendly.ui.attendance

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appsbase.attendly.R
import com.appsbase.attendly.data.location.LocationTracker
import com.appsbase.attendly.domain.model.AttendanceStatus
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
    timeValidator: TimeValidator
) : ViewModel() {

    private val _state = MutableStateFlow(AttendanceState())
    val state: StateFlow<AttendanceState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<AttendanceEffect>()
    val effect: SharedFlow<AttendanceEffect> = _effect.asSharedFlow()

    private var locationUpdatesJob: Job? = null

    init {
        val (start, end) = timeValidator.getWorkHoursFormatted()
        val hasPermission = locationTracker.hasLocationPermission()
        val gpsEnabled = locationTracker.isGpsEnabled()
        _state.update {
            it.copy(
                checkInWindow = "$start – $end",
                hasLocationPermission = hasPermission,
                isGpsEnabled = gpsEnabled
            )
        }
        observeRepositoryData()
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

    /**
     * Re-reads permission and GPS provider state (dispatched when the app resumes,
     * e.g. after the user returns from system settings) and restarts tracking if
     * either flag flipped to available.
     */
    private fun refreshLocationState() {
        val hasPermission = locationTracker.hasLocationPermission()
        val gpsEnabled = locationTracker.isGpsEnabled()
        val stateChanged = hasPermission != _state.value.hasLocationPermission ||
                gpsEnabled != _state.value.isGpsEnabled

        _state.update {
            it.copy(hasLocationPermission = hasPermission, isGpsEnabled = gpsEnabled)
        }

        if (hasPermission) {
            if (stateChanged) startLocationTracking()
        } else {
            locationUpdatesJob?.cancel()
        }
    }

    private fun startLocationTracking() {
        locationUpdatesJob?.cancel()
        locationUpdatesJob = viewModelScope.launch {
            val initialLocation = locationTracker.getCurrentLocation()
            if (initialLocation != null) {
                _state.update { current ->
                    current.copy(
                        currentLocation = initialLocation,
                        targetOfficeLocation = when {
                            current.officeLocation != null -> current.targetOfficeLocation
                            else -> initialLocation
                        }
                    )
                }
                recalculateEligibility()
            }

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
        val eligibility = validateEligibilityUseCase(
            currentLocation = _state.value.currentLocation,
            officeLocation = _state.value.officeLocation,
            todayAttendance = _state.value.todayAttendance,
            bypassTimeSimulation = _state.value.simulationConfig.bypassTimeValidation
        )

        _state.update {
            it.copy(
                eligibilityStatus = eligibility.status,
                distanceMeters = eligibility.distanceMeters,
                isWithinGeofence = eligibility.distanceMeters
                    ?.let(GeoFenceCalculator::isWithinGeofence) == true
            )
        }
    }

    fun onIntent(intent: AttendanceIntent) {
        when (intent) {
            is AttendanceIntent.RefreshLocationState -> refreshLocationState()

            is AttendanceIntent.PermissionResultReceived -> {
                _state.update { it.copy(hasLocationPermission = intent.isGranted) }
                if (intent.isGranted) {
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

            is AttendanceIntent.SaveOfficeLocationClicked -> saveOfficeLocation()

            is AttendanceIntent.MarkAttendanceClicked -> markAttendance()

            is AttendanceIntent.ToggleTimeSimulation -> toggleTimeSimulation()

            is AttendanceIntent.ShowHistorySheet -> {
                _state.update { it.copy(showHistorySheet = intent.show) }
            }

            is AttendanceIntent.ShowResetConfirmDialog -> {
                _state.update { it.copy(showResetConfirmDialog = intent.show) }
            }

            is AttendanceIntent.ConfirmResetAll -> resetAllData()
        }
    }

    private fun saveOfficeLocation() {
        val target = _state.value.targetOfficeLocation ?: _state.value.currentLocation
        if (target == null) {
            emitEffect(R.string.snackbar_location_unavailable)
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSavingOffice = true) }
            val result = saveOfficeLocationUseCase(target)
            _state.update { it.copy(isSavingOffice = false) }

            emitEffect(
                if (result.isSuccess) R.string.office_location_saved_success
                else R.string.snackbar_office_save_failed
            )
        }
    }

    private fun markAttendance() {
        val currentLoc = _state.value.currentLocation
        val office = _state.value.officeLocation
        if (currentLoc == null || office == null) {
            emitEffect(R.string.snackbar_location_unavailable)
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isMarkingAttendance = true) }
            val record = markAttendanceUseCase(
                currentLocation = currentLoc,
                officeLocation = office,
                todayAttendance = _state.value.todayAttendance,
                bypassTimeSimulation = _state.value.simulationConfig.bypassTimeValidation
            )
            _state.update { it.copy(isMarkingAttendance = false) }

            emitEffect(
                if (record != null) R.string.attendance_marked_success
                else messageFor(_state.value.eligibilityStatus)
            )
        }
    }

    private fun toggleTimeSimulation() {
        viewModelScope.launch {
            val newVal = !_state.value.simulationConfig.bypassTimeValidation
            repository.setTimeBypassSimulation(newVal)
            emitEffect(
                if (newVal) R.string.simulation_enabled
                else R.string.simulation_disabled
            )
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
            emitEffect(R.string.data_reset_success)
        }
    }

    private fun emitEffect(@StringRes messageRes: Int) {
        viewModelScope.launch {
            _effect.emit(AttendanceEffect.ShowSnackbar(messageRes))
        }
    }

    @StringRes
    private fun messageFor(status: AttendanceStatus): Int = when (status) {
        AttendanceStatus.OFFICE_NOT_SET -> R.string.attendance_disabled_unset_reason
        AttendanceStatus.OUTSIDE_GEOFENCE -> R.string.hint_out_of_range
        AttendanceStatus.OUTSIDE_TIME_WINDOW -> R.string.attendance_disabled_time_reason
        AttendanceStatus.ALREADY_MARKED -> R.string.attendance_already_marked
        AttendanceStatus.ELIGIBLE -> R.string.snackbar_mark_failed
    }
}
