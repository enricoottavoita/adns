package com.eyalm.adns.ui.screens

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.eyalm.adns.ui.components.DnsSwitch
import com.eyalm.adns.ui.theme.AdnsTheme
import com.eyalm.adns.ui.theme.pageTitle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    isEnabled: Boolean,
    runningTime: String,
    onToggle: () -> Unit,
    server: String = "dns.adguard-dns.com",
    onEditClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    innerPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
        ) {
            Text(
                text = if (isEnabled) "Goooodbye,\nAds!" else "Blocker\nDisabled",
                style = MaterialTheme.typography.pageTitle,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 48.sp,
                lineHeight = 48.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(text = "DNS Ad Blocker")
                        Text(
                            text = if (isEnabled) "Running" else "Not running",
                            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(
                        modifier = Modifier.align(Alignment.Top),
                        onClick = onSettingsClick,
                    ) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(text = "Server")
                        Text(text = server)
                    }
                    IconButton(
                        modifier = Modifier.align(Alignment.Top),
                        onClick = onEditClick,
                    ) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Change DNS Server")
                    }
                }

                Column(
                    modifier = Modifier.alpha(if (isEnabled) 1f else 0f)
                ) {
                    Text(text = "Uptime")
                    Text(text = runningTime)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            DnsSwitch(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                isEnabled = isEnabled,
                onToggle = onToggle
            )
        }
    }
}

@Composable
fun UpdateDialog(
    version: String,
    onClose: () -> Unit = {},
) {
    val context = LocalContext.current
    AlertDialog(
        icon = {
            Icon(imageVector = Icons.Filled.Update, contentDescription = "Update Icon")
        },
        title = {
            Text(text = "New Update")
        },
        text = {
            Text(text = "Version v$version is available.\nWould you like to download it?")
        },
        onDismissRequest = {
            onClose()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val url = "https://github.com/eyalm2000/adns/releases"
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    try {
                        context.startActivity(intent)
                    } catch (e: android.content.ActivityNotFoundException) {
                        Log.e("MainActivity", "No browser found to open release URL", e)
                    }
                    onClose()
                }
            ) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onClose()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun UpdateDialogPreview() {
    AdnsTheme {
        UpdateDialog(
            version = "1.0.0",
            onClose = {}
        )
    }
}

/**
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AdnsTheme {
        HomeScreen(
            isEnabled = true,
            runningTime = "00:05:23",
            onToggle = {}
        )
    }
}
        **/