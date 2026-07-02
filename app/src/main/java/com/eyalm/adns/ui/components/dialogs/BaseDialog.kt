package com.eyalm.adns.ui.components.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.eyalm.adns.data.Locales

@Composable
fun BaseDialog(
    title: String,
    body: String? = null,
    confirmLabel: String,
    destructive: Boolean,
    submitting: Boolean = false,
    confirmEnabled: Boolean = true,
    errorMessage: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {


    AlertDialog(
        onDismissRequest = {
            if (!submitting) onDismiss()
        },
        modifier = modifier,
        properties = properties,
        containerColor = backgroundColor,
        title = { Text(title) },
        text = if (body != null || content != null) {
            {
                Column {
                    body?.let { Text(it) }
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    content?.let { it() }
                }
            }
        } else null,
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !submitting && confirmEnabled,
                colors = if (destructive) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    }
                    Text(
                        text = confirmLabel,
                        color = if (submitting) Color.Transparent else Color.Unspecified
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text(Locales.getString("global", "cancel"))
            }
        },
    )
}
