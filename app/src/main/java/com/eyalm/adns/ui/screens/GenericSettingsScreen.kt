package com.eyalm.adns.ui.screens.settings

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.data.ListSetting
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.ToggleSetting
import com.eyalm.adns.ui.components.ClickableCardSettings
import com.eyalm.adns.ui.components.SwitchSettingCard
import com.eyalm.adns.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericCategoryScreen(
    title: String,
    apiPage: String,
    toggles: List<ToggleSetting>,
    lists: List<ListSetting> = emptyList(),
    onBack: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel()
    val toggleStates by viewModel.pageToggles.collectAsState()
    val loadedPageId by viewModel.loadedPageId.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(apiPage) {
        viewModel.loadPageSettings(apiPage, toggles)
    }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val isDataReady = loadedPageId == apiPage && toggleStates.isNotEmpty()

        if (!isDataReady) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(50.dp))
            }
        } else {
            val toggleStatesMap = toggleStates
            val (multiItemGroups, singleItemGroups) = remember(toggles) {
                toggles.groupBy { it.apiPath.first() }
                    .toList()
                    .partition { it.second.size > 1 }
            }

            @Composable
            fun ToggleItem(toggle: ToggleSetting) {
                SwitchSettingCard(
                    title = toggle.title(),
                    description = toggle.description(),
                    checked = toggleStatesMap[toggle.stateKey] == true,
                    onCheckedChange = { newValue ->
                        viewModel.updateToggle(apiPage, toggle, newValue)
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 48.dp, bottom = 16.dp),
                        fontSize = 32.sp
                    )
                }

                if (multiItemGroups.isNotEmpty()) {
                    multiItemGroups.forEach { (groupKey, settings) ->
                        item(key = groupKey) {
                            Text(
                                text = Locales.getString(apiPage, groupKey, "name"),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(settings, key = { it.stateKey }) { toggle -> ToggleItem(toggle) }
                    }

                    if (singleItemGroups.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        items(
                            singleItemGroups.flatMap { it.second },
                            key = { it.stateKey }) { toggle ->
                            ToggleItem(toggle)
                        }
                    }
                } else {
                    items(toggles, key = { it.stateKey }) { toggle ->
                        ToggleItem(toggle)
                    }
                }



                if (lists.isNotEmpty()) {
                    Log.d("GenericCategoryScreen", "Rendering lists for $apiPage")
                    Log.d("GenericCategoryScreen", "Lists: $lists")
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(lists) { listSetting ->
                        ClickableCardSettings(
                            title = listSetting.title(),
                            description = "Manage blocked items",
                            onClick = { viewModel.openListScreen(listSetting) },
                            icon = Icons.Filled.List
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}