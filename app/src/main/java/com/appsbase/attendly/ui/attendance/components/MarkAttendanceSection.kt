package com.appsbase.attendly.ui.attendance.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsbase.attendly.R
import com.appsbase.attendly.domain.model.AttendanceRecord
import com.appsbase.attendly.domain.model.AttendanceStatus
import com.appsbase.attendly.ui.theme.AttendlyTheme
import com.appsbase.attendly.ui.theme.DisabledButton
import com.appsbase.attendly.ui.theme.DisabledTextGrey
import com.appsbase.attendly.ui.theme.HintGrey
import com.appsbase.attendly.ui.theme.LabelGrey
import com.appsbase.attendly.ui.theme.PrimaryBlue
import com.appsbase.attendly.ui.theme.SuccessGreen
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CaptionTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

@Composable
internal fun MarkAttendanceSection(
    eligibilityStatus: AttendanceStatus,
    todayAttendance: AttendanceRecord?,
    isMarking: Boolean,
    isSimulationActive: Boolean,
    checkInWindow: String,
    onMarkAttendance: () -> Unit
) {
    val alreadyMarked = todayAttendance != null
    val isEligible = eligibilityStatus == AttendanceStatus.ELIGIBLE
    val dashColor = LabelGrey.copy(alpha = 0.5f)

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
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeaderIcon(
            alreadyMarked = alreadyMarked,
            isEligible = isEligible
        )

        Spacer(Modifier.height(20.dp))

        MarkAttendanceButton(
            isEligible = isEligible,
            alreadyMarked = alreadyMarked,
            isMarking = isMarking,
            onClick = onMarkAttendance
        )

        Spacer(Modifier.height(14.dp))

        SectionCaption(
            todayAttendance = todayAttendance,
            isSimulationActive = isSimulationActive,
            checkInWindow = checkInWindow
        )
    }
}

@Composable
private fun SectionHeaderIcon(alreadyMarked: Boolean, isEligible: Boolean) {
    val icon = when {
        alreadyMarked -> Icons.Filled.CheckCircle
        isEligible -> Icons.Outlined.LockOpen
        else -> Icons.Outlined.Lock
    }
    val tint = when {
        alreadyMarked -> SuccessGreen
        isEligible -> PrimaryBlue
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(44.dp)
    )
}

@Composable
private fun MarkAttendanceButton(
    isEligible: Boolean,
    alreadyMarked: Boolean,
    isMarking: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = isEligible && !isMarking,
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
            isMarking -> CircularProgressIndicator(
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
}

@Composable
private fun SectionCaption(
    todayAttendance: AttendanceRecord?,
    isSimulationActive: Boolean,
    checkInWindow: String
) {
    val captionText = when {
        todayAttendance != null -> stringResource(
            R.string.caption_marked_at,
            formatTime(todayAttendance.timestamp)
        )
        isSimulationActive -> stringResource(R.string.caption_any_time)
        else -> stringResource(R.string.caption_available_window, checkInWindow)
    }
    Text(
        text = captionText,
        color = HintGrey,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(CaptionTimeFormatter)

private val previewRecord = AttendanceRecord(
    id = "preview-record",
    timestamp = LocalDateTime.of(2026, 10, 4, 10, 30)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli(),
    latitude = 23.8104,
    longitude = 90.4125,
    distanceMeters = 8
)

@Preview(showBackground = true, widthDp = 390, heightDp = 320, name = "Check-in — locked (out of range)")
@Composable
private fun MarkAttendanceLockedPreview() {
    AttendlyTheme {
        MarkAttendanceSection(
            eligibilityStatus = AttendanceStatus.OUTSIDE_GEOFENCE,
            todayAttendance = null,
            isMarking = false,
            isSimulationActive = false,
            checkInWindow = "09:00 AM – 06:00 PM",
            onMarkAttendance = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 320, name = "Check-in — eligible")
@Composable
private fun MarkAttendanceEligiblePreview() {
    AttendlyTheme {
        MarkAttendanceSection(
            eligibilityStatus = AttendanceStatus.ELIGIBLE,
            todayAttendance = null,
            isMarking = false,
            isSimulationActive = false,
            checkInWindow = "09:00 AM – 06:00 PM",
            onMarkAttendance = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 320, name = "Check-in — marked today")
@Composable
private fun MarkAttendanceMarkedPreview() {
    AttendlyTheme {
        MarkAttendanceSection(
            eligibilityStatus = AttendanceStatus.ALREADY_MARKED,
            todayAttendance = previewRecord,
            isMarking = false,
            isSimulationActive = false,
            checkInWindow = "09:00 AM – 06:00 PM",
            onMarkAttendance = {}
        )
    }
}
