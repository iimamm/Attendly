package com.appsbase.attendly.ui.attendance.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.appsbase.attendly.R
import com.appsbase.attendly.ui.theme.AttendlyTheme
import com.appsbase.attendly.ui.theme.WarningAmber
import com.appsbase.attendly.ui.theme.WarningContainer

@Composable
internal fun GuidanceBanners(
    hasLocationPermission: Boolean,
    isGpsEnabled: Boolean,
    onRequestPermission: () -> Unit,
    onEnableGps: () -> Unit
) {
    if (!hasLocationPermission) {
        GuidanceBanner(
            icon = Icons.Default.Warning,
            titleRes = R.string.permission_required_title,
            messageRes = R.string.permission_required_message,
            actionLabelRes = R.string.permission_grant_btn,
            onAction = onRequestPermission
        )
        Spacer(Modifier.height(20.dp))
    }

    if (hasLocationPermission && !isGpsEnabled) {
        GuidanceBanner(
            icon = Icons.Default.LocationOff,
            titleRes = R.string.gps_disabled_title,
            messageRes = R.string.gps_disabled_message,
            actionLabelRes = R.string.gps_enable_btn,
            onAction = onEnableGps
        )
        Spacer(Modifier.height(20.dp))
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

@Preview(showBackground = true, widthDp = 390, heightDp = 260, name = "Permission banner")
@Composable
private fun PermissionBannerPreview() {
    AttendlyTheme {
        GuidanceBanners(
            hasLocationPermission = false,
            isGpsEnabled = true,
            onRequestPermission = {},
            onEnableGps = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 260, name = "GPS disabled banner")
@Composable
private fun GpsDisabledBannerPreview() {
    AttendlyTheme {
        GuidanceBanners(
            hasLocationPermission = true,
            isGpsEnabled = false,
            onRequestPermission = {},
            onEnableGps = {}
        )
    }
}
