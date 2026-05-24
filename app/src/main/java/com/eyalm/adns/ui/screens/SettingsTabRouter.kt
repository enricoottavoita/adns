package com.eyalm.adns.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.data.ParentalControlSettings
import com.eyalm.adns.data.PrivacySettings
import com.eyalm.adns.data.SecuritySettings
import com.eyalm.adns.data.SettingsPageSettings
import com.eyalm.adns.ui.screens.settings.AccountSettingsScreen
import com.eyalm.adns.ui.screens.settings.BlocklistsScreen
import com.eyalm.adns.ui.screens.settings.GenericCategoryScreen
import com.eyalm.adns.ui.screens.settings.GenericListScreen
import com.eyalm.adns.ui.screens.settings.MainSettingsScreen
import com.eyalm.adns.ui.screens.settings.ProvidersScreen
import com.eyalm.adns.viewmodel.SettingsViewModel

@Composable
fun SettingsTabRouter(
    modifier: Modifier = Modifier,
    onNavigateToProvidersActivity: (String) -> Unit,
    permissionLauncher: ActivityResultLauncher<String>? = null,
    innerPadding: PaddingValues

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
                innerPadding = innerPadding
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
        SettingsViewModel.Page.SECURITY -> {
            BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
            GenericCategoryScreen(
                title = "Security",
                apiPage = "security",
                toggles = SecuritySettings.toggles,
                lists = SecuritySettings.lists,
                onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) }
            )
        }

        SettingsViewModel.Page.PRIVACY -> {
            BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
            GenericCategoryScreen(
                title = "Privacy",
                apiPage = "privacy",
                toggles = PrivacySettings.toggles,
                lists = PrivacySettings.lists,
                onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) }
            )
        }

        SettingsViewModel.Page.PARENTAL_CONTROL -> {
            BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
            GenericCategoryScreen(
                title = "Parental Control",
                apiPage = "parentalcontrol",
                toggles = ParentalControlSettings.toggles,
                lists = ParentalControlSettings.lists,
                onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) }
            )
        }

        SettingsViewModel.Page.SETTINGS_PAGE -> {
            BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
            GenericCategoryScreen(
                title = "Settings",
                apiPage = "settings",
                toggles = SettingsPageSettings.toggles,
                // We omit the 'lists' parameter here because it defaults to emptyList()
                onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) }
            )
        }
        SettingsViewModel.Page.GENERIC_LIST -> {
            // Back goes to the parent category, NOT to MAIN
            val parentPage = viewModel.getListParentPage()
            BackHandler { viewModel.setPage(parentPage) }
            GenericListScreen(
                onBack = { viewModel.setPage(parentPage) }
            )
        }
    }


}