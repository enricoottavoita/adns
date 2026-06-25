package com.eyalm.adns
import com.eyalm.adns.R
import androidx.compose.ui.res.stringResource


import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BroadcastOnPersonal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.eyalm.adns.data.DnsConstants
import com.eyalm.adns.ui.screens.settings.AccountSettingsScreen
import com.eyalm.adns.ui.screens.settings.BlocklistsScreen
import com.eyalm.adns.ui.screens.settings.MainSettingsScreen
import com.eyalm.adns.ui.screens.settings.ProvidersScreen
import com.eyalm.adns.ui.theme.AdnsTheme
import com.eyalm.adns.viewmodel.SettingsViewModel
import com.eyalm.adns.viewmodel.SettingsViewModel.Page
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels()
    private var lastAppliedLang: String? = null

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.eyalm.adns.data.LocaleHelper.onAttach(newBase))
    }

    override fun onResume() {
        super.onResume()
        val savedLang = com.eyalm.adns.data.LocaleHelper.getLanguage(this)
        if (lastAppliedLang != null && lastAppliedLang != savedLang) {
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        com.eyalm.adns.data.LocaleHelper.applyLocale(this)
        lastAppliedLang = com.eyalm.adns.data.LocaleHelper.getLanguage(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dnsUrl by viewModel.dnsUrl.collectAsState()
            val page by viewModel.page.collectAsState()
            val selectedProvider by viewModel.selectedProvider.collectAsState()

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    viewModel.refreshNotification()
                    Log.d("Permission", "Permission Granted")
                    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, this@SettingsActivity.packageName)
                        putExtra(Settings.EXTRA_CHANNEL_ID, "dns_status_channel")
                    }
                    this@SettingsActivity.startActivity(intent)
                } else {
                    Log.d("Permission", "Permission Denied")
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, this@SettingsActivity.packageName)
                    }
                    this@SettingsActivity.startActivity(intent)
                }
            }

            // val showProviders = remember { mutableStateOf(intent.getBooleanExtra("open_providers", false)) }


            AdnsTheme {
                when (page) {
                    Page.PROVIDERS -> {
                        BackHandler { viewModel.setPage(Page.MAIN) }

                        ProvidersScreen(
                            onBack = {
                                viewModel.setPage(Page.MAIN)
                            },
                            onEnhancedModeClick = { providerId ->
                                val intent = Intent(this@SettingsActivity, ProviderLoginActivity::class.java).apply {
                                    putExtra("provider", providerId)
                                }
                                this@SettingsActivity.startActivity(intent)
                            }
                        )
                    }
                    Page.MAIN -> {
                        MainSettingsScreen(
                            modifier = Modifier.fillMaxSize(),
                            onBack = { finish() },
                            onAddQuickTile = { viewModel.addQuickTile() },
                            permissionLauncher = permissionLauncher,
                            currentPage = page,
                            onPageChange = viewModel::setPage,
                            innerPadding = PaddingValues()
                        )
                    }
                    Page.ACCOUNT_SETTINGS -> {
                        AccountSettingsScreen(
                            onBack = { viewModel.setPage(Page.MAIN) },
                            provider = selectedProvider
                        )
                    }
                    Page.BLOCKLISTS -> {
                        BlocklistsScreen(
                            onBack = { viewModel.setPage(Page.MAIN) },
                            provider = selectedProvider
                        )
                    }
                    else -> {}
                }
            }
        }
    }

}

@Composable
fun DnsDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit,
    currentUrl: String
) {
    val isAdGuard = remember { mutableStateOf(currentUrl == DnsConstants.ADGUARD_DNS) }
    val customUrlText = remember { mutableStateOf(if (currentUrl == DnsConstants.ADGUARD_DNS) "" else currentUrl) }

    val isCustomValid = customUrlText.value.isNotEmpty() && Patterns.DOMAIN_NAME.matcher(customUrlText.value).matches()
    val isConfirmEnabled = isAdGuard.value || isCustomValid

    AlertDialog(
        icon = {
            Icon(Icons.Filled.BroadcastOnPersonal, contentDescription = "DNS Server")
        },
        title = {
            Text(text = stringResource(R.string.set_dns_server))
        },
        text = {
            Column {
                Text(text = stringResource(R.string.choose_a_dns_server_to_use))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = isAdGuard.value,
                            onClick = { isAdGuard.value = true },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isAdGuard.value,
                        onClick = null
                    )
                    Text(
                        text = DnsConstants.ADGUARD_DNS,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp
                    )
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = !isAdGuard.value,
                            onClick = { isAdGuard.value = false },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        modifier = Modifier
                            .align(Alignment.Top),
                        selected = !isAdGuard.value,
                        onClick = null
                    )
                    Column(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.custom_hostname),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp
                        )
                        TextField(
                            modifier = Modifier.fillMaxWidth().
                                padding(top = 8.dp),
                            value = customUrlText.value,
                            onValueChange = { 
                                customUrlText.value = it
                                isAdGuard.value = false
                            },
                            isError = !isAdGuard.value && !isCustomValid,
                            supportingText = {
                                if (!isAdGuard.value && !isCustomValid && customUrlText.value.isNotEmpty()) {
                                    Text(stringResource(R.string.invalid_hostname))
                                }
                            }
                        )
                    }
                }
            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalUrl = if (isAdGuard.value) DnsConstants.ADGUARD_DNS else customUrlText.value
                    onConfirmation(finalUrl)
                },
                enabled = isConfirmEnabled
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.dismiss))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DnsDialogPreview() {
    AdnsTheme {
        DnsDialog(
            onDismissRequest = {},
            onConfirmation = {},
            currentUrl = DnsConstants.ADGUARD_DNS
        )
    }
}