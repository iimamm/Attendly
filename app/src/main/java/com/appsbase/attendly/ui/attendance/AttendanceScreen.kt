package com.appsbase.attendly.ui.attendance

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appsbase.attendly.domain.model.AttendanceRecord
import com.appsbase.attendly.domain.model.AttendanceStatus
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.model.OfficeLocation
import com.appsbase.attendly.ui.attendance.components.AttendanceHistoryBottomSheet
import com.appsbase.attendly.ui.attendance.components.AttendanceTopBar
import com.appsbase.attendly.ui.attendance.components.DistanceRing
import com.appsbase.attendly.ui.attendance.components.GuidanceBanners
import com.appsbase.attendly.ui.attendance.components.MarkAttendanceSection
import com.appsbase.attendly.ui.attendance.components.OfficeContextCard
import com.appsbase.attendly.ui.attendance.components.RangeStatusChip
import com.appsbase.attendly.ui.attendance.components.ResetConfirmationDialog
import com.appsbase.attendly.ui.attendance.components.StatusHint
import com.appsbase.attendly.ui.theme.AttendlyTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDateTime
import java.time.ZoneId

private val DefaultLocation = LatLng(23.8103, 90.4125)
private const val DefaultZoom = 17f

@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var snackbarMessageRes by remember { mutableStateOf<Int?>(null) }
    val snackbarMessage = snackbarMessageRes?.let { stringResource(it) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onIntent(
            AttendanceIntent.PermissionResultReceived(permissions.values.any { it })
        )
    }

    LaunchedEffect(Unit) {
        if (!state.hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DefaultLocation, DefaultZoom)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(AttendanceIntent.RefreshLocationState)
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AttendanceEffect.ShowSnackbar ->
                    snackbarMessageRes = effect.messageRes

                is AttendanceEffect.AnimateMapCamera ->
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(effect.location.latitude, effect.location.longitude),
                            DefaultZoom
                        )
                    )
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessageRes = null
        }
    }

    LaunchedEffect(state.currentLocation) {
        val currentLoc = state.currentLocation ?: return@LaunchedEffect
        if (state.officeLocation == null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(currentLoc.latitude, currentLoc.longitude),
                DefaultZoom
            )
        }
    }

    LaunchedEffect(state.officeLocation) {
        state.officeLocation?.let { office ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(office.latitude, office.longitude),
                    DefaultZoom + 0.5f
                )
            )
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target
            viewModel.onIntent(
                AttendanceIntent.MapCameraMoved(
                    LocationModel(target.latitude, target.longitude)
                )
            )
        }
    }

    AttendanceContent(
        state = state,
        cameraPositionState = cameraPositionState,
        snackbarHostState = snackbarHostState,
        onBack = { (context as? Activity)?.finish() },
        onIntent = viewModel::onIntent,
        onRequestPermission = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    )
}

@Composable
private fun AttendanceContent(
    state: AttendanceState,
    cameraPositionState: CameraPositionState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onIntent: (AttendanceIntent) -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AttendanceTopBar(
                state = state,
                onBack = onBack,
                onToggleSimulation = { onIntent(AttendanceIntent.ToggleTimeSimulation) },
                onShowHistory = { onIntent(AttendanceIntent.ShowHistorySheet(true)) },
                onShowResetDialog = { onIntent(AttendanceIntent.ShowResetConfirmDialog(true)) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GuidanceBanners(
                state = state,
                onRequestPermission = onRequestPermission,
                onEnableGps = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            )

            OfficeContextCard(
                state = state,
                cameraPositionState = cameraPositionState,
                onSaveOfficeLocation = { onIntent(AttendanceIntent.SaveOfficeLocationClicked) },
                onCenterOnMyLocation = { onIntent(AttendanceIntent.CenterMapOnCurrentLocation) }
            )

            Spacer(Modifier.height(28.dp))

            DistanceRing(
                distanceMeters = state.distanceMeters,
                inRange = state.isWithinGeofence
            )

            Spacer(Modifier.height(14.dp))

            RangeStatusChip(state = state)

            Spacer(Modifier.height(8.dp))

            StatusHint(state = state)

            Spacer(Modifier.height(36.dp))

            MarkAttendanceSection(
                state = state,
                onMarkAttendance = { onIntent(AttendanceIntent.MarkAttendanceClicked) }
            )
        }
    }

    if (state.showResetConfirmDialog) {
        ResetConfirmationDialog(
            onConfirm = { onIntent(AttendanceIntent.ConfirmResetAll) },
            onDismiss = { onIntent(AttendanceIntent.ShowResetConfirmDialog(false)) }
        )
    }

    if (state.showHistorySheet) {
        AttendanceHistoryBottomSheet(
            history = state.attendanceHistory,
            onDismiss = { onIntent(AttendanceIntent.ShowHistorySheet(false)) }
        )
    }
}

private fun previewState(
    officeLocation: OfficeLocation?,
    currentLocation: LocationModel,
    distanceMeters: Int?,
    eligibilityStatus: AttendanceStatus,
    isWithinGeofence: Boolean,
    todayAttendance: AttendanceRecord? = null
): AttendanceState = AttendanceState(
    currentLocation = currentLocation,
    officeLocation = officeLocation,
    targetOfficeLocation = currentLocation,
    distanceMeters = distanceMeters,
    eligibilityStatus = eligibilityStatus,
    isWithinGeofence = isWithinGeofence,
    todayAttendance = todayAttendance,
    checkInWindow = "09:00 AM – 06:00 PM",
    hasLocationPermission = true,
    isGpsEnabled = true
)

@Composable
private fun AttendanceCasePreview(state: AttendanceState) {
    AttendlyTheme {
        AttendanceContent(
            state = state,
            cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(DefaultLocation, DefaultZoom)
            },
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onIntent = {},
            onRequestPermission = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "1 — Office unset")
@Composable
private fun AttendanceScreenOfficeUnsetPreview() {
    AttendanceCasePreview(
        previewState(
            officeLocation = null,
            currentLocation = LocationModel(23.8103, 90.4125),
            distanceMeters = null,
            eligibilityStatus = AttendanceStatus.OFFICE_NOT_SET,
            isWithinGeofence = false
        )
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "2 — Set, out of range")
@Composable
private fun AttendanceScreenOutOfRangePreview() {
    AttendanceCasePreview(
        previewState(
            officeLocation = OfficeLocation(23.8103, 90.4125, isSet = true),
            currentLocation = LocationModel(23.8114, 90.4125),
            distanceMeters = 120,
            eligibilityStatus = AttendanceStatus.OUTSIDE_GEOFENCE,
            isWithinGeofence = false
        )
    )
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    name = "3 — Set, in range (eligible)"
)
@Composable
private fun AttendanceScreenInRangePreview() {
    AttendanceCasePreview(
        previewState(
            officeLocation = OfficeLocation(23.8103, 90.4125, isSet = true),
            currentLocation = LocationModel(23.8105, 90.4125),
            distanceMeters = 22,
            eligibilityStatus = AttendanceStatus.ELIGIBLE,
            isWithinGeofence = true
        )
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "4 — Set, marked today")
@Composable
private fun AttendanceScreenMarkedTodayPreview() {
    AttendanceCasePreview(
        previewState(
            officeLocation = OfficeLocation(23.8103, 90.4125, isSet = true),
            currentLocation = LocationModel(23.8104, 90.4125),
            distanceMeters = 8,
            eligibilityStatus = AttendanceStatus.ALREADY_MARKED,
            isWithinGeofence = true,
            todayAttendance = AttendanceRecord(
                id = "preview-record",
                timestamp = LocalDateTime.of(2026, 10, 4, 10, 30)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
                latitude = 23.8104,
                longitude = 90.4125,
                distanceMeters = 8
            )
        )
    )
}
