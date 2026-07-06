package com.eyalm.adns.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RawOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.R
import com.eyalm.adns.data.network.toHexId
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.ui.components.ExpressiveIcon
import com.eyalm.adns.ui.components.ExpressiveListItem
import com.eyalm.adns.ui.components.ListIconView
import com.eyalm.adns.ui.screens.SettingsCategoryScreenTemplate
import com.eyalm.adns.viewmodel.LogsViewModel
import com.eyalm.adns.viewmodel.ProfileSessionState
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onBack: () -> Unit,
    profileState: ProfileSessionState,
) {
    val profileId = profileState.selectedProfileId ?: return
    val viewModel: LogsViewModel = viewModel(key = "logs-$profileId")
    val context = LocalContext.current
    val items = viewModel.logsList
    val devices = viewModel.devicesList

    var showConfig by remember(profileId) { mutableStateOf(true) }
    var expandedId by remember(profileId) { mutableStateOf<Int?>(null) }

    LaunchedEffect(profileId, profileState.logsRevision) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    SettingsCategoryScreenTemplate(
        onBack = onBack,
        title = stringResource(R.string.logs),
    ) {
            item {
                Row(
                    verticalAlignment = CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    TextField(
                        value = viewModel.searchQuery,
                        onValueChange = {
                            viewModel.updateSearchQuery(it)
                        },
                        placeholder = { Text(stringResource(R.string.search)) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                    ExpressiveIcon(
                        icon = Icons.Default.Settings,
                        selected = showConfig,
                        bgcolor = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clickable(
                                onClick = { showConfig = !showConfig },
                                role = Role.Button
                            )
                    )

                }
            }

            if (showConfig) {
                item {
                    Spacer(Modifier.height(8.dp))
                    ExpressiveListItem(
                        onClick = { viewModel.setBlocked(!viewModel.blockedSelected) },
                        title = stringResource(R.string.blocked_only),
                        description = stringResource(R.string.show_only_blocked_items),
                        icon = Icons.Filled.Block,
                        interactiveItem = { isSelected, onClick ->
                            Switch(
                                checked = isSelected,
                                onCheckedChange = { onClick() }
                            )
                        },
                        isSelected = viewModel.blockedSelected,
                        isFirst = true,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item {
                    ExpressiveListItem(
                        onClick = {
                            viewModel.setRaw(!viewModel.rawEnabled)
                        },
                        title = stringResource(R.string.raw_mode),
                        description = stringResource(R.string.show_raw_dns_logs),
                        icon = Icons.Filled.RawOn,
                        interactiveItem = { isSelected, onClick ->
                            Switch(
                                checked = isSelected,
                                onCheckedChange = { onClick() }
                            )
                        },
                        isSelected = viewModel.rawEnabled,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item {
                    var expanded by remember { mutableStateOf(false) }

                    ExpressiveListItem(
                        onClick = {
                            expanded = true
                        },
                        title = stringResource(R.string.change_device),
                        description = stringResource(R.string.filter_the_logs_to_a_certain_device),
                        icon = Icons.Filled.Devices,
                        interactiveItem = { _, _ ->
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(Alignment.TopEnd)
                            ) {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Show options")
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.all_devices)) },
                                        onClick = {
                                            expanded = false
                                            viewModel.setDevice(null)
                                        }
                                    )
                                    devices.forEach { deviceItem ->
                                        deviceItem.name?.let {
                                            DropdownMenuItem(
                                                text = { Text(it) },
                                                onClick = {
                                                    expanded = false
                                                    viewModel.setDevice(deviceItem.id)
                                                }
                                            )
                                        }
                                    }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.unknown_devices)) },
                                        onClick = {
                                            expanded = false
                                            viewModel.setDevice("__UNIDENTIFIED__")
                                        }
                                    )
                                }
                            }
                        },
                        isLast = true
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            if (viewModel.isInitialLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_logs_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(items.size) { index ->
                    val log = items[index]
                    LaunchedEffect(index) {
                        if (index >= items.size - 5) {
                            viewModel.fetchNextPage()
                        }
                    }
                    ExpressiveListItem(
                        title = log.domain,
                        onClick = { expandedId = if (expandedId == index) null else index },
                        indicatorColor = if (log.status == "blocked") MaterialTheme.colorScheme.error else null,
                        altLeadingContent = {
                            ListIconView(
                                icon = ListIcon.Url("https://favicons.nextdns.io/${log.domain.toHexId()}@3x.png"),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        isFirst = index == 0,
                        isLast = index == items.lastIndex,
                        altContent = {
                            if (expandedId == index) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    log.device?.let { dev ->
                                        val devName = dev.name ?: ""
                                        val devModel = dev.model?.let { " ($it)" } ?: ""
                                        if (devName.isNotEmpty() || devModel.isNotEmpty()) {
                                            DetailRow(label = stringResource(R.string.device), value = "$devName$devModel")
                                        }
                                    }

                                    DetailRow(label = "Time", value = formatLogTimestamp(log.timestamp))

                                    val encryptionStr = if (log.encrypted) stringResource(R.string.encrypted) else stringResource(
                                        R.string.unencrypted
                                    )
                                    DetailRow(label = stringResource(R.string.protocol), value = "${log.protocol}$encryptionStr")

                                    log.clientIp?.let { ip ->
                                        DetailRow(label = stringResource(R.string.client_ip), value = ip)
                                    }

                                    if (viewModel.rawEnabled) {
                                        log.type?.let {
                                            DetailRow(label = "Type", value = it)

                                        }
                                    }
                                    if (log.status == "blocked" && log.reasons.isNotEmpty()) {
                                        val reasonsStr = log.reasons.joinToString(", ") { it.name }
                                        DetailRow(label = stringResource(R.string.blocked_by), value = reasonsStr, isErrorColor = true)
                                    }

                                    Spacer(Modifier.height(8.dp))


                                    val options = listOf(stringResource(R.string.allow),
                                        stringResource(
                                            R.string.deny
                                        ), stringResource(R.string.copy))
                                    val unCheckedIcons =
                                        listOf(
                                            Icons.Filled.Check,
                                            Icons.Filled.Block,
                                            Icons.Filled.CopyAll
                                        )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        options.forEachIndexed { index, label ->
                                            val shapes = when (index) {
                                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                                options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                            }
                                            ToggleButton(
                                                checked = true,
                                                onCheckedChange = { },
                                                modifier = Modifier.weight(1f),
                                                shapes = shapes.copy(checkedShape = shapes.shape), // TODO logic
                                                colors = ToggleButtonDefaults.toggleButtonColors(
                                                    checkedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    checkedContentColor = MaterialTheme.colorScheme.onSurface
                                                )
                                            ) {
                                                Icon(
                                                    unCheckedIcons[index],
                                                    contentDescription = "Localized description",
                                                )
                                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                                Text(label)
                                            }
                                        }
                                    }

                                }
                            }
                        },
                        stickIcon = true,
                        iconSize = 24.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (viewModel.isFetchingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isErrorColor: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isErrorColor) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

fun formatLogTimestamp(timestamp: String): String {
    return try {
        val parsed = ZonedDateTime.parse(timestamp)
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        parsed.format(formatter)
    } catch (e: Exception) {
        timestamp
    }
}
