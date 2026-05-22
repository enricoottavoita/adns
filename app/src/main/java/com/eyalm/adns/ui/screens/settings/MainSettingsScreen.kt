package com.eyalm.adns.ui.screens.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BroadcastOnPersonal
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.BuildConfig
import com.eyalm.adns.R
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.ui.components.ClickableCardSettings
import com.eyalm.adns.viewmodel.SettingsViewModel
import com.eyalm.adns.viewmodel.SettingsViewModel.Page


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainSettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onAddQuickTile: () -> Unit = {},
    permissionLauncher: ActivityResultLauncher<String>? = null,
    currentPage: Page = Page.MAIN,
    onPageChange: (Page) -> Unit = {}
) {
    val viewModel: SettingsViewModel = viewModel()
    val provider by viewModel.selectedProvider.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    /*
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }

                     */
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 48.dp, bottom = 16.dp),
                fontSize = 32.sp,
            ) }
            if (provider is DnsProvider.Enhanced) {
                item {
                    ClickableCardSettings(
                        title = "${provider.name} Settings",
                        description = "Change account settings for ${provider.name}",
                        onClick = {
                            onPageChange(Page.ACCOUNT_SETTINGS)
                        },
                        icon = Icons.Filled.AccountCircle
                    )
                }
                item {
                    ClickableCardSettings(
                        title = "${provider.name} Blocklists",
                        description = "Change blocklists for ${provider.name}",
                        onClick = {
                            onPageChange(Page.BLOCKLISTS)
                        },
                        icon = Icons.Filled.FilterList

                    )
                }
            }
            item {
                ClickableCardSettings(
                    onClick = { onPageChange(Page.PROVIDERS) },
                    title = "Change Provider",
                    description = "Change the provider to use",
                    icon = Icons.Filled.BroadcastOnPersonal
                )
            }
            item {
                ClickableCardSettings(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    title = "State Notifications",
                    description = "Enable or disable blocker state notifications",
                    icon = Icons.Filled.Notifications
                )
            }
            item {
                ClickableCardSettings(
                    onClick = onAddQuickTile,
                    title = "Add the quick settings tile",
                    description = "Add the quick settings tile to your device",
                    icon = Icons.Filled.SettingsSuggest
                )
            }
            item {
                ClickableCardSettings(
                    title = "Security",
                    description = "Threat protection, DNS rebinding, TLDs",
                    onClick = { onPageChange(Page.SECURITY) },
                    icon = Icons.Filled.Security
                )
            }
            item {
                ClickableCardSettings(
                    title = "Privacy",
                    description = "Blocklists, trackers, affiliate links",
                    onClick = { onPageChange(Page.PRIVACY) },
                    icon = Icons.Filled.PrivacyTip
                )
            }
            item {
                ClickableCardSettings(
                    title = "Parental Control",
                    description = "SafeSearch, blocked apps, categories",
                    onClick = { onPageChange(Page.PARENTAL_CONTROL) },
                    icon = Icons.Filled.FamilyRestroom
                )
            }
            item {
                ClickableCardSettings(
                    title = "Profile Settings",
                    description = "Logs, performance, block page",
                    onClick = { onPageChange(Page.SETTINGS_PAGE) },
                    icon = Icons.Filled.Tune
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val url = "https://github.com/eyalm2000/adns"
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        try {
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Log.e("Settings", "No browser found to open GitHub URL", e)
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Icon(
                            painter = painterResource(id = R.drawable.ic_adns_filled),
                            contentDescription = "App icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(64.dp)
                        )

                        Text(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                .padding(top = 8.dp, bottom = 8.dp),
                            text = "ADNS",
                            fontWeight = Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}\nCreated by Eyal Meirom",
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                .padding(bottom = 8.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }

            }
        }
    }
}
