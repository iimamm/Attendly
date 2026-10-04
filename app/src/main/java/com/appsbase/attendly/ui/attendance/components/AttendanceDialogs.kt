package com.appsbase.attendly.ui.attendance.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.appsbase.attendly.R
import com.appsbase.attendly.ui.theme.AttendlyTheme

@Composable
fun ResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.confirm_reset_title)) },
        text = { Text(text = stringResource(R.string.confirm_reset_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.btn_reset))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 300, name = "Reset confirmation dialog")
@Composable
private fun ResetConfirmationDialogPreview() {
    AttendlyTheme {
        ResetConfirmationDialog(onConfirm = {}, onDismiss = {})
    }
}
