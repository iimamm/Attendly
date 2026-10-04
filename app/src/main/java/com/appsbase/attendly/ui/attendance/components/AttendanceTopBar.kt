package com.appsbase.attendly.ui.attendance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsbase.attendly.R
import com.appsbase.attendly.ui.theme.AttendlyTheme
import com.appsbase.attendly.ui.theme.DangerRed
import com.appsbase.attendly.ui.theme.SuccessGreen
import com.appsbase.attendly.ui.theme.TitleNavy
import com.appsbase.attendly.ui.theme.WarningAmber
import com.appsbase.attendly.ui.theme.WarningContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttendanceTopBar(
    isSimulationActive: Boolean,
    historyCount: Int,
    onBack: () -> Unit,
    onToggleSimulation: () -> Unit,
    onShowHistory: () -> Unit,
    onShowResetDialog: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.title_attendance),
                color = TitleNavy,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBackIos,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            if (isSimulationActive) {
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
                isSimulationActive = isSimulationActive,
                historyCount = historyCount,
                onToggleSimulation = onToggleSimulation,
                onShowHistory = onShowHistory,
                onShowResetDialog = onShowResetDialog
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
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
    isSimulationActive: Boolean,
    historyCount: Int,
    onToggleSimulation: () -> Unit,
    onShowHistory: () -> Unit,
    onShowResetDialog: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        if (isSimulationActive) R.string.menu_disable_simulation
                        else R.string.menu_bypass_time
                    )
                )
            },
            onClick = {
                onDismiss()
                onToggleSimulation()
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isSimulationActive) Icons.Default.Check
                    else Icons.Default.Science,
                    contentDescription = null,
                    tint = if (isSimulationActive) SuccessGreen
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.menu_view_history) + " ($historyCount)"
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

@Preview(showBackground = true, widthDp = 412, heightDp = 120, name = "Top bar")
@Composable
private fun AttendanceTopBarPreview() {
    AttendlyTheme {
        AttendanceTopBar(
            isSimulationActive = false,
            historyCount = 2,
            onBack = {},
            onToggleSimulation = {},
            onShowHistory = {},
            onShowResetDialog = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 120, name = "Top bar — simulation active")
@Composable
private fun AttendanceTopBarSimulationPreview() {
    AttendlyTheme {
        AttendanceTopBar(
            isSimulationActive = true,
            historyCount = 2,
            onBack = {},
            onToggleSimulation = {},
            onShowHistory = {},
            onShowResetDialog = {}
        )
    }
}
