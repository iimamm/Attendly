package com.appsbase.attendly.ui.attendance

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appsbase.attendly.R
import com.appsbase.attendly.domain.model.AttendanceStatus
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.util.GeoFenceCalculator
import com.appsbase.attendly.ui.attendance.components.AttendanceHistoryBottomSheet
import com.appsbase.attendly.ui.attendance.components.ResetConfirmationDialog
import com.appsbase.attendly.ui.theme.DangerContainer
import com.appsbase.attendly.ui.theme.DangerRed
import com.appsbase.attendly.ui.theme.DisabledButton
import com.appsbase.attendly.ui.theme.DisabledTextGrey
import com.appsbase.attendly.ui.theme.HintGrey
import com.appsbase.attendly.ui.theme.LabelGrey
import com.appsbase.attendly.ui.theme.PrimaryBlue
import com.appsbase.attendly.ui.theme.SuccessContainer
import com.appsbase.attendly.ui.theme.SuccessGreen
import com.appsbase.attendly.ui.theme.TitleNavy
import com.appsbase.attendly.ui.theme.TrackGrey
import com.appsbase.attendly.ui.theme.WarningAmber
import com.appsbase.attendly.ui.theme.WarningContainer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DefaultLocation = LatLng(23.8103, 90.4125)
private const val DefaultZoom = 17f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

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

    // Re-check permission and GPS state whenever the app returns to the
    // foreground (e.g. after the user comes back from system settings).
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(AttendanceIntent.RefreshLocationState)
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AttendanceEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(context.getString(effect.messageRes))

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = TitleNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (state.simulationConfig.bypassTimeValidation) {
                        SimulationBadge()
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.menu_options)
                        )
                    }
                    OverflowMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        state = state,
                        onToggleSimulation = {
                            viewModel.onIntent(AttendanceIntent.ToggleTimeSimulation)
                        },
                        onShowHistory = {
                            viewModel.onIntent(AttendanceIntent.ShowHistorySheet(true))
                        },
                        onShowResetDialog = {
                            viewModel.onIntent(AttendanceIntent.ShowResetConfirmDialog(true))
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            if (!state.hasLocationPermission) {
                GuidanceBanner(
                    icon = Icons.Default.Warning,
                    titleRes = R.string.permission_required_title,
                    messageRes = R.string.permission_required_message,
                    actionLabelRes = R.string.permission_grant_btn,
                    onAction = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
                Spacer(Modifier.height(20.dp))
            }

            if (state.hasLocationPermission && !state.isGpsEnabled) {
                GuidanceBanner(
                    icon = Icons.Default.LocationOff,
                    titleRes = R.string.gps_disabled_title,
                    messageRes = R.string.gps_disabled_message,
                    actionLabelRes = R.string.gps_enable_btn,
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                )
                Spacer(Modifier.height(20.dp))
            }

            OfficeContextCard(
                state = state,
                cameraPositionState = cameraPositionState,
                onSaveOfficeLocation = {
                    viewModel.onIntent(AttendanceIntent.SaveOfficeLocationClicked)
                },
                onCenterOnMyLocation = {
                    viewModel.onIntent(AttendanceIntent.CenterMapOnCurrentLocation)
                }
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
                onMarkAttendance = {
                    viewModel.onIntent(AttendanceIntent.MarkAttendanceClicked)
                }
            )
        }
    }

    if (state.showResetConfirmDialog) {
        ResetConfirmationDialog(
            onConfirm = { viewModel.onIntent(AttendanceIntent.ConfirmResetAll) },
            onDismiss = {
                viewModel.onIntent(AttendanceIntent.ShowResetConfirmDialog(false))
            }
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
private fun SimulationBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(WarningContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Science,
            contentDescription = null,
            tint = WarningAmber,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.simulation_active_badge),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = WarningAmber
        )
    }
}

@Composable
private fun OverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    state: AttendanceState,
    onToggleSimulation: () -> Unit,
    onShowHistory: () -> Unit,
    onShowResetDialog: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        if (state.simulationConfig.bypassTimeValidation) {
                            R.string.menu_disable_simulation
                        } else {
                            R.string.menu_bypass_time
                        }
                    )
                )
            },
            onClick = {
                onDismiss()
                onToggleSimulation()
            },
            leadingIcon = {
                Icon(
                    imageVector = if (state.simulationConfig.bypassTimeValidation) {
                        Icons.Default.Check
                    } else {
                        Icons.Default.Science
                    },
                    contentDescription = null,
                    tint = if (state.simulationConfig.bypassTimeValidation) {
                        SuccessGreen
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.menu_view_history) +
                        " (${state.attendanceHistory.size})"
                )
            },
            onClick = {
                onDismiss()
                onShowHistory()
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.History, contentDescription = null)
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.menu_reset_all),
                    color = DangerRed
                )
            },
            onClick = {
                onDismiss()
                onShowResetDialog()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = DangerRed
                )
            }
        )
    }
}

@Composable
private fun GuidanceBanner(
    icon: ImageVector,
    titleRes: Int,
    messageRes: Int,
    actionLabelRes: Int,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = WarningContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = WarningAmber
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
            ) {
                Text(
                    text = stringResource(actionLabelRes),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun OfficeContextCard(
    state: AttendanceState,
    cameraPositionState: CameraPositionState,
    onSaveOfficeLocation: () -> Unit,
    onCenterOnMyLocation: () -> Unit
) {
    val isOfficeSet = state.officeLocation?.isSet == true

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.office_context_step),
                    color = LabelGrey,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                )
            }

            Spacer(Modifier.height(16.dp))

            OfficeMap(
                state = state,
                cameraPositionState = cameraPositionState,
                onCenterOnMyLocation = onCenterOnMyLocation
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.office_context_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSaveOfficeLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, PrimaryBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                enabled = !state.isSavingOffice
            ) {
                if (state.isSavingOffice) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(
                            if (isOfficeSet) R.string.update_office_location
                            else R.string.set_office_location
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OfficeMap(
    state: AttendanceState,
    cameraPositionState: CameraPositionState,
    onCenterOnMyLocation: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = state.hasLocationPermission),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = true
            )
        ) {
            state.officeLocation?.let { office ->
                Circle(
                    center = LatLng(office.latitude, office.longitude),
                    radius = GeoFenceCalculator.GEOFENCE_RADIUS_METERS,
                    strokeColor = PrimaryBlue,
                    strokeWidth = 3f,
                    fillColor = PrimaryBlue.copy(alpha = 0.18f)
                )
                Marker(
                    state = MarkerState(
                        position = LatLng(office.latitude, office.longitude)
                    ),
                    title = stringResource(R.string.office_marker_title),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.Center)
        )

        FloatingActionButton(
            onClick = onCenterOnMyLocation,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(40.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryBlue
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = stringResource(R.string.my_location_tooltip),
                modifier = Modifier.size(18.dp)
            )
        }

        val target = state.targetOfficeLocation
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (target != null) {
                        stringResource(
                            R.string.target_coords_format,
                            target.latitude,
                            target.longitude
                        )
                    } else {
                        stringResource(R.string.target_coords_format, 0.0, 0.0)
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DistanceRing(distanceMeters: Int?, inRange: Boolean) {
    val accent = if (inRange) SuccessGreen else DangerRed
    val sweepFraction = when {
        inRange -> 1f
        distanceMeters != null -> (distanceMeters / 200f).coerceIn(0.1f, 1f)
        else -> 0.1f
    }

    Box(
        modifier = Modifier
            .size(176.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val stroke = 5.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = TrackGrey,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * sweepFraction,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = distanceMeters?.let { "${it}m" } ?: "—",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.distance_away_label),
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                color = HintGrey
            )
        }
    }
}

@Composable
private fun RangeStatusChip(state: AttendanceState) {
    val officeSet = state.officeLocation?.isSet == true
    val (labelRes, color, bg) = when {
        !officeSet -> Triple(R.string.chip_office_unset, LabelGrey, TrackGrey)
        state.todayAttendance != null ->
            Triple(R.string.chip_marked_today, SuccessGreen, SuccessContainer)
        state.isWithinGeofence ->
            Triple(R.string.chip_in_range, SuccessGreen, SuccessContainer)
        else -> Triple(R.string.chip_out_of_range, DangerRed, DangerContainer)
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(labelRes),
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun StatusHint(state: AttendanceState) {
    val hintText = when (state.eligibilityStatus) {
        AttendanceStatus.OFFICE_NOT_SET ->
            stringResource(R.string.attendance_disabled_unset_reason)
        AttendanceStatus.OUTSIDE_GEOFENCE ->
            stringResource(
                R.string.hint_out_of_range,
                GeoFenceCalculator.GEOFENCE_RADIUS_METERS.toInt()
            )
        AttendanceStatus.OUTSIDE_TIME_WINDOW ->
            stringResource(R.string.attendance_disabled_time_reason)
        AttendanceStatus.ALREADY_MARKED ->
            stringResource(R.string.attendance_already_marked)
        AttendanceStatus.ELIGIBLE ->
            stringResource(R.string.hint_in_range)
    }

    Text(
        text = hintText,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

@Composable
private fun MarkAttendanceSection(
    state: AttendanceState,
    onMarkAttendance: () -> Unit
) {
    val alreadyMarked = state.todayAttendance != null
    val isEligible = state.eligibilityStatus == AttendanceStatus.ELIGIBLE
    val bypassActive = state.simulationConfig.bypassTimeValidation

    val headerIcon: ImageVector = when {
        alreadyMarked -> Icons.Filled.CheckCircle
        isEligible -> Icons.Outlined.LockOpen
        else -> Icons.Outlined.Lock
    }
    val headerTint = when {
        alreadyMarked -> SuccessGreen
        isEligible -> PrimaryBlue
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dashColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                    )
                )
            }
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = headerIcon,
            contentDescription = null,
            tint = headerTint,
            modifier = Modifier.size(44.dp)
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onMarkAttendance,
            enabled = isEligible && !state.isMarkingAttendance,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                disabledContainerColor = if (alreadyMarked) {
                    SuccessGreen.copy(alpha = 0.75f)
                } else {
                    DisabledButton
                },
                disabledContentColor = if (alreadyMarked) Color.White else DisabledTextGrey
            )
        ) {
            when {
                state.isMarkingAttendance -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                alreadyMarked -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.attendance_marked_today),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> Text(
                    text = stringResource(R.string.mark_attendance),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        val captionText = when {
            alreadyMarked -> stringResource(
                R.string.caption_marked_at,
                formatTime(state.todayAttendance?.timestamp ?: 0L)
            )
            bypassActive -> stringResource(R.string.caption_any_time)
            else -> stringResource(R.string.caption_available_window, state.checkInWindow)
        }
        Text(
            text = captionText,
            color = HintGrey,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("hh:mm a"))
