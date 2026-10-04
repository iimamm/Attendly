package com.appsbase.attendly.ui.attendance.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsbase.attendly.R
import com.appsbase.attendly.domain.model.AttendanceStatus
import com.appsbase.attendly.domain.util.GeoFenceCalculator
import com.appsbase.attendly.ui.theme.AttendlyTheme
import com.appsbase.attendly.ui.theme.DangerContainer
import com.appsbase.attendly.ui.theme.DangerRed
import com.appsbase.attendly.ui.theme.HintGrey
import com.appsbase.attendly.ui.theme.LabelGrey
import com.appsbase.attendly.ui.theme.SuccessContainer
import com.appsbase.attendly.ui.theme.SuccessGreen
import com.appsbase.attendly.ui.theme.TrackGrey

@Composable
internal fun DistanceRing(distanceMeters: Int?, inRange: Boolean) {
    val accent = if (inRange) SuccessGreen else DangerRed
    val sweepFraction = when {
        inRange -> 1f
        distanceMeters != null -> (distanceMeters / 200f).coerceIn(0.1f, 1f)
        else -> 0.1f
    }

    Box(
        modifier = Modifier
            .size(165.dp)
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
internal fun RangeStatusChip(
    isOfficeSet: Boolean,
    isWithinGeofence: Boolean,
    isMarkedToday: Boolean
) {
    val (labelRes, color, bg) = when {
        !isOfficeSet -> Triple(R.string.chip_office_unset, LabelGrey, TrackGrey)
        isMarkedToday -> Triple(R.string.chip_marked_today, SuccessGreen, SuccessContainer)
        isWithinGeofence -> Triple(R.string.chip_in_range, SuccessGreen, SuccessContainer)
        else -> Triple(R.string.chip_out_of_range, DangerRed, DangerContainer)
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 4.dp),
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
internal fun StatusHint(status: AttendanceStatus) {
    val hintText = when (status) {
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

@Preview(showBackground = true, widthDp = 220, heightDp = 220, name = "Distance ring — out of range")
@Composable
private fun DistanceRingOutPreview() {
    AttendlyTheme {
        DistanceRing(distanceMeters = 120, inRange = false)
    }
}

@Preview(showBackground = true, widthDp = 220, heightDp = 220, name = "Distance ring — in range")
@Composable
private fun DistanceRingInPreview() {
    AttendlyTheme {
        DistanceRing(distanceMeters = 22, inRange = true)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 320, name = "Status chips — all states")
@Composable
private fun RangeStatusChipGalleryPreview() {
    AttendlyTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RangeStatusChip(isOfficeSet = false, isWithinGeofence = false, isMarkedToday = false)
            Spacer(Modifier.height(8.dp))
            RangeStatusChip(isOfficeSet = true, isWithinGeofence = true, isMarkedToday = false)
            Spacer(Modifier.height(8.dp))
            RangeStatusChip(isOfficeSet = true, isWithinGeofence = false, isMarkedToday = false)
            Spacer(Modifier.height(8.dp))
            RangeStatusChip(isOfficeSet = true, isWithinGeofence = true, isMarkedToday = true)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 260, name = "Status hints — main states")
@Composable
private fun StatusHintGalleryPreview() {
    AttendlyTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StatusHint(status = AttendanceStatus.OFFICE_NOT_SET)
            Spacer(Modifier.height(8.dp))
            StatusHint(status = AttendanceStatus.OUTSIDE_GEOFENCE)
            Spacer(Modifier.height(8.dp))
            StatusHint(status = AttendanceStatus.OUTSIDE_TIME_WINDOW)
            Spacer(Modifier.height(8.dp))
            StatusHint(status = AttendanceStatus.ELIGIBLE)
        }
    }
}
