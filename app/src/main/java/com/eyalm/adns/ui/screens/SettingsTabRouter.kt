package com.eyalm.adns.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.ui.screens.settings.AccountSettingsScreen
import com.eyalm.adns.ui.screens.settings.BlocklistsScreen
import com.eyalm.adns.ui.screens.settings.MainSettingsScreen
import com.eyalm.adns.ui.screens.settings.ProvidersScreen
import com.eyalm.adns.viewmodel.SettingsViewModel

@Composable
fun SettingsTabRouter(
    modifier: Modifier = Modifier,
    onNavigateToProvidersActivity: (String) -> Unit,
    permissionLauncher: ActivityResultLauncher<String>? = null,

) {
    val viewModel: SettingsViewModel = viewModel()
    val page by viewModel.page.collectAsState()
    val selectedProvider by viewModel.selectedProvider.collectAsState()


    when (page) {
        SettingsViewModel.Page.MAIN -> {
            MainSettingsScreen(
                modifier = modifier,
                onAddQuickTile = { viewModel.addQuickTile() },
                permissionLauncher = permissionLauncher,
                currentPage = page,
                onPageChange = viewModel::setPage,
            )
        }
        SettingsViewModel.Page.PROVIDERS -> {
            BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
            ProvidersScreen(
                onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) },
                onEnhancedModeClick = onNavigateToProvidersActivity
            )
        }
        SettingsViewModel.Page.ACCOUNT_SETTINGS -> {
            BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
            AccountSettingsScreen(
                onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) },
                provider = selectedProvider
            )
        }
        SettingsViewModel.Page.BLOCKLISTS -> {
            BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
            BlocklistsScreen(
                onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) },
                provider = selectedProvider
            )
        }
    }


}