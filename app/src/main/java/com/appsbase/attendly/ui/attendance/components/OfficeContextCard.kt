package com.appsbase.attendly.ui.attendance.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsbase.attendly.R
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.model.OfficeLocation
import com.appsbase.attendly.domain.util.GeoFenceCalculator
import com.appsbase.attendly.ui.theme.AttendlyTheme
import com.appsbase.attendly.ui.theme.LabelGrey
import com.appsbase.attendly.ui.theme.PrimaryBlue
import com.appsbase.attendly.ui.theme.TrackGrey
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
internal fun OfficeContextCard(
    officeLocation: OfficeLocation?,
    targetOfficeLocation: LocationModel?,
    hasLocationPermission: Boolean,
    isSavingOffice: Boolean,
    cameraPositionState: CameraPositionState,
    onSaveOfficeLocation: () -> Unit,
    onCenterOnMyLocation: () -> Unit
) {
    val isOfficeSet = officeLocation?.isSet == true

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            OfficeCardHeader()

            Spacer(Modifier.height(16.dp))

            OfficeMap(
                officeLocation = officeLocation,
                targetOfficeLocation = targetOfficeLocation,
                hasLocationPermission = hasLocationPermission,
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

            SaveOfficeButton(
                isOfficeSet = isOfficeSet,
                isSaving = isSavingOffice,
                onClick = onSaveOfficeLocation
            )
        }
    }
}

@Composable
private fun OfficeCardHeader() {
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
}

@Composable
private fun SaveOfficeButton(
    isOfficeSet: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, PrimaryBlue),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
        enabled = !isSaving
    ) {
        if (isSaving) {
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

@Composable
private fun OfficeMap(
    officeLocation: OfficeLocation?,
    targetOfficeLocation: LocationModel?,
    hasLocationPermission: Boolean,
    cameraPositionState: CameraPositionState,
    onCenterOnMyLocation: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        if (LocalInspectionMode.current) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(TrackGrey.copy(alpha = 0.35f))
            )
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    compassEnabled = true
                )
            ) {
                officeLocation?.let { OfficeGeofenceOverlay(it) }
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

        MyLocationFab(
            onClick = onCenterOnMyLocation,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(40.dp)
        )

        CoordinatesPill(
            location = targetOfficeLocation,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 17.dp)
        )
    }
}

@Composable
private fun OfficeGeofenceOverlay(office: OfficeLocation) {
    Circle(
        center = LatLng(office.latitude, office.longitude),
        radius = GeoFenceCalculator.GEOFENCE_RADIUS_METERS,
        strokeColor = PrimaryBlue,
        strokeWidth = 3f,
        fillColor = PrimaryBlue.copy(alpha = 0.18f)
    )
    Marker(
        state = rememberMarkerState(
            key = "${office.latitude},${office.longitude}",
            position = LatLng(office.latitude, office.longitude)
        ),
        title = stringResource(R.string.office_marker_title),
        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
    )
}

@Composable
private fun MyLocationFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = PrimaryBlue
    ) {
        Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = stringResource(R.string.my_location_tooltip),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CoordinatesPill(
    location: LocationModel?,
    modifier: Modifier = Modifier
) {
    val target = location ?: LocationModel(0.0, 0.0)
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier
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
                text = stringResource(
                    R.string.target_coords_format,
                    target.latitude,
                    target.longitude
                ),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 520, name = "Office card — unset")
@Composable
private fun OfficeContextCardUnsetPreview() {
    AttendlyTheme {
        OfficeContextCard(
            officeLocation = null,
            targetOfficeLocation = LocationModel(23.8103, 90.4125),
            hasLocationPermission = true,
            isSavingOffice = false,
            cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(23.8103, 90.4125), 17f)
            },
            onSaveOfficeLocation = {},
            onCenterOnMyLocation = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 520, name = "Office card — set")
@Composable
private fun OfficeContextCardSetPreview() {
    AttendlyTheme {
        OfficeContextCard(
            officeLocation = OfficeLocation(23.8103, 90.4125, isSet = true),
            targetOfficeLocation = LocationModel(23.8103, 90.4125),
            hasLocationPermission = true,
            isSavingOffice = false,
            cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(23.8103, 90.4125), 17f)
            },
            onSaveOfficeLocation = {},
            onCenterOnMyLocation = {}
        )
    }
}
