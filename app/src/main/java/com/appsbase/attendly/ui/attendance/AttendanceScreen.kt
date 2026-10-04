package com.appsbase.attendly.ui.attendance

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appsbase.attendly.R
import com.appsbase.attendly.domain.model.AttendanceStatus
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.ui.attendance.components.AttendanceHistoryBottomSheet
import com.appsbase.attendly.ui.attendance.components.ResetConfirmationDialog
import com.appsbase.attendly.ui.theme.ErrorRed
import com.appsbase.attendly.ui.theme.PrimaryBlue
import com.appsbase.attendly.ui.theme.PrimaryBlueLight
import com.appsbase.attendly.ui.theme.SuccessGreen
import com.appsbase.attendly.ui.theme.SuccessGreenLight
import com.appsbase.attendly.ui.theme.WarningAmber
import com.appsbase.attendly.ui.theme.WarningAmberLight
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    // Location Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        viewModel.onIntent(AttendanceIntent.PermissionResultReceived(granted))
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

    // Google Map Camera State
    val defaultDhaka = LatLng(23.8103, 90.4125)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultDhaka, 17f)
    }

    // Handle Effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AttendanceEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is AttendanceEffect.AnimateMapCamera -> {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(effect.location.latitude, effect.location.longitude),
                            17f
                        )
                    )
                }
            }
        }
    }

    // Initial center on current location if available
    LaunchedEffect(state.currentLocation) {
        state.currentLocation?.let { currentLoc ->
            if (state.officeLocation == null) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(currentLoc.latitude, currentLoc.longitude),
                    17f
                )
            }
        }
    }

    // Center on office location when office is loaded
    LaunchedEffect(state.officeLocation) {
        state.officeLocation?.let { office ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(office.latitude, office.longitude),
                    17.5f
                )
            )
        }
    }

    // Track camera movement to update target office location candidate
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    if (state.simulationConfig.bypassTimeValidation) {
                        Surface(
                            color = WarningAmberLight,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.simulation_active_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber
                                )
                            }
                        }
                    }

                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.menu_options)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // Toggle Simulation
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (state.simulationConfig.bypassTimeValidation)
                                        "Disable Simulation"
                                    else
                                        stringResource(R.string.menu_bypass_time)
                                )
                            },
                            onClick = {
                                showMenu = false
                                viewModel.onIntent(AttendanceIntent.ToggleTimeSimulation)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (state.simulationConfig.bypassTimeValidation)
                                        Icons.Default.Check
                                    else
                                        Icons.Default.Science,
                                    contentDescription = null,
                                    tint = if (state.simulationConfig.bypassTimeValidation)
                                        SuccessGreen
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )

                        // View Attendance History
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${stringResource(R.string.menu_view_history)} (${state.attendanceHistory.size})"
                                )
                            },
                            onClick = {
                                showMenu = false
                                viewModel.onIntent(AttendanceIntent.ShowHistorySheet(true))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null
                                )
                            }
                        )

                        // Reset All
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.menu_reset_all),
                                    color = ErrorRed
                                )
                            },
                            onClick = {
                                showMenu = false
                                viewModel.onIntent(AttendanceIntent.ShowResetConfirmDialog(true))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = ErrorRed
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Permission Banner (if not granted)
            AnimatedVisibility(visible = !state.hasLocationPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningAmberLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.permission_required_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.permission_required_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                        ) {
                            Text(text = stringResource(R.string.permission_grant_btn), color = Color.White)
                        }
                    }
                }
            }

            // Map Section with center pin & 50m geofence circle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = state.hasLocationPermission
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = false,
                        zoomControlsEnabled = false,
                        compassEnabled = true
                    )
                ) {
                    // Draw 50m Geofence Circle around saved office
                    state.officeLocation?.let { office ->
                        Circle(
                            center = LatLng(office.latitude, office.longitude),
                            radius = 50.0,
                            strokeColor = PrimaryBlue,
                            strokeWidth = 4f,
                            fillColor = PrimaryBlue.copy(alpha = 0.20f)
                        )

                        Marker(
                            state = MarkerState(
                                position = LatLng(office.latitude, office.longitude)
                            ),
                            title = "Office (Geofence Center)",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }
                }

                // Center Pin for targeting office coordinates during drag
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = stringResource(R.string.office_pin_label),
                    tint = PrimaryBlue,
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                )

                // Current GPS Location Button overlay
                FloatingActionButton(
                    onClick = {
                        viewModel.onIntent(AttendanceIntent.CenterMapOnCurrentLocation)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = PrimaryBlue,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.my_location_tooltip)
                    )
                }

                // 50m Geofence Indicator Badge on Map
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.geofence_radius_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content Area: Status Card and Action Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Card 1: Distance & Proximity Feedback
                DistanceAndProximityCard(state = state)

                Spacer(modifier = Modifier.height(12.dp))

                // Card 2: Time Validation & Working Hours
                TimeValidationCard(state = state)

                Spacer(modifier = Modifier.height(16.dp))

                // Setup / Update Office Location Button
                val isOfficeSet = state.officeLocation != null && state.officeLocation?.isSet == true
                OutlinedButton(
                    onClick = {
                        viewModel.onIntent(AttendanceIntent.SaveOfficeLocationClicked)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isSavingOffice
                ) {
                    if (state.isSavingOffice) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (isOfficeSet) R.string.update_office_location else R.string.set_office_location
                            ),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mark Attendance Button & Status
                MarkAttendanceSection(
                    state = state,
                    onMarkAttendance = {
                        viewModel.onIntent(AttendanceIntent.MarkAttendanceClicked)
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Modal Dialogs
    if (state.showResetConfirmDialog) {
        ResetConfirmationDialog(
            onConfirm = { viewModel.onIntent(AttendanceIntent.ConfirmResetAll) },
            onDismiss = { viewModel.onIntent(AttendanceIntent.ShowResetConfirmDialog(false)) }
        )
    }

    if (state.showHistorySheet) {
        AttendanceHistoryBottomSheet(
            history = state.attendanceHistory,
            onDismiss = { viewModel.onIntent(AttendanceIntent.ShowHistorySheet(false)) }
        )
    }
}

@Composable
private fun DistanceAndProximityCard(state: AttendanceState) {
    val isOfficeSet = state.officeLocation != null && state.officeLocation?.isSet == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Office Proximity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Proximity Status Chip
                val (chipText, chipBg, chipColor) = when {
                    !isOfficeSet -> Triple(
                        stringResource(R.string.status_unset),
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.isWithinGeofence -> Triple(
                        stringResource(R.string.status_in_range),
                        SuccessGreenLight,
                        SuccessGreen
                    )
                    else -> Triple(
                        stringResource(R.string.status_out_of_range),
                        WarningAmberLight,
                        WarningAmber
                    )
                }

                Surface(
                    color = chipBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = chipText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = chipColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-time Distance Text
            if (!isOfficeSet) {
                Text(
                    text = stringResource(
                        R.string.office_location_not_set,
                        stringResource(R.string.set_office_location)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val distance = state.distanceMeters
                val distanceText = if (distance != null) {
                    if (state.isWithinGeofence) {
                        stringResource(R.string.distance_within_format, distance)
                    } else {
                        stringResource(R.string.distance_away_format, distance)
                    }
                } else {
                    stringResource(R.string.distance_calculating)
                }

                Text(
                    text = distanceText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.isWithinGeofence) SuccessGreen else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TimeValidationCard(state: AttendanceState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Working Hours Window",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                val (timeBadgeText, timeBadgeBg, timeBadgeColor) = when {
                    state.simulationConfig.bypassTimeValidation -> Triple(
                        "Bypassed (Test)",
                        WarningAmberLight,
                        WarningAmber
                    )
                    state.isWithinTimeWindow -> Triple(
                        stringResource(R.string.time_valid),
                        SuccessGreenLight,
                        SuccessGreen
                    )
                    else -> Triple(
                        "Outside Hours",
                        ErrorRed.copy(alpha = 0.15f),
                        ErrorRed
                    )
                }

                Surface(
                    color = timeBadgeBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = timeBadgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = timeBadgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.working_hours_info, "09:00 AM", "06:00 PM"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!state.isWithinTimeWindow && !state.simulationConfig.bypassTimeValidation) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.time_invalid_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed
                )
            }
        }
    }
}

@Composable
private fun MarkAttendanceSection(
    state: AttendanceState,
    onMarkAttendance: () -> Unit
) {
    val isOfficeSet = state.officeLocation != null && state.officeLocation?.isSet == true
    val alreadyMarked = state.todayAttendance != null
    val isEligible = state.eligibilityStatus == AttendanceStatus.ELIGIBLE

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onMarkAttendance,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = isEligible && !state.isMarkingAttendance,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (alreadyMarked) SuccessGreen else PrimaryBlue,
                disabledContainerColor = if (alreadyMarked)
                    SuccessGreen.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        ) {
            if (state.isMarkingAttendance) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else if (alreadyMarked) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.attendance_marked_today),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                Text(
                    text = stringResource(R.string.mark_attendance),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Informative Helper Hint underneath button when disabled
        if (!alreadyMarked && !isEligible) {
            Spacer(modifier = Modifier.height(6.dp))
            val hintResId = when (state.eligibilityStatus) {
                AttendanceStatus.OFFICE_NOT_SET -> R.string.attendance_disabled_unset_reason
                AttendanceStatus.OUTSIDE_GEOFENCE -> R.string.attendance_disabled_distance_reason
                AttendanceStatus.OUTSIDE_TIME_WINDOW -> R.string.attendance_disabled_time_reason
                else -> null
            }

            hintResId?.let { resId ->
                Text(
                    text = stringResource(resId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
