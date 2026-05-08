package com.eyalm.adns.ui.screens

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.ui.components.InfoCard
import com.eyalm.adns.ui.components.ProviderCard
import com.eyalm.adns.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun ProvidersScreen(
    onBack: () -> Unit = { },
    onEnhancedModeClick: (providerId: String) -> Unit = { },
) {

    val viewModel: SettingsViewModel = viewModel()
    val currentProvider by viewModel.selectedProvider.collectAsState()

    val provider = currentProvider

    val customUrlText = remember {
        mutableStateOf(
            when (provider) {
                is DnsProvider.Custom -> provider.userUrl
                else -> ""
            }
        )
    }

    val isCustomValid = customUrlText.value.isNotEmpty() && Patterns.DOMAIN_NAME.matcher(customUrlText.value).matches()
    // val isConfirmEnabled = isAdGuard.value || isCustomValid

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text(
                text = "Providers",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 48.dp),
                fontSize = 32.sp,
            ) }

            item {
                Text(text = "Select your preferred DNS provider")
            }

            DnsProviders.getAllProviders.forEach { provider ->
                item {
                    ProviderCard(
                        title = provider.name,
                        description = provider.description,
                        selected = provider.id == currentProvider.id,
                        onClick = {
                            if (!provider.isEnhanced) {
                                viewModel.setProvider(provider.id)
                            } else {
                                if (viewModel.isLoggedIn(provider)) {
                                    viewModel.setProvider(provider.id)
                                }
                                else if (provider.id != currentProvider.id) {
                                    onEnhancedModeClick(provider.id)
                                }
                            }
                        },
                        isEnhanced = provider.isEnhanced,
                        modifier = Modifier
                    )
                }
            }

            item {
                InfoCard(
                    content = { contentColor ->
                        Column() {
                            Row() {
                                Text(
                                    text = "Custom Hostname (advanced)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = contentColor
                                )
                            }

                            Text(
                                text = "Use a custom hostname for the DNS server",
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor
                            )
                            TextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                value = customUrlText.value,
                                onValueChange = {
                                    customUrlText.value = it
                                    // isAdGuard.value = false
                                },
                                isError = !isCustomValid, // !isAdGuard.value && !isCustomValid,
                                supportingText = {
                                    if (!isCustomValid && customUrlText.value.isNotEmpty()) {
                                        Text("Invalid hostname")
                                    }
                                }
                            )

                        }
                    },
                    selected = currentProvider.id == "custom",
                    onClick = {  },
                    modifier = Modifier
                )
            }
            item { Button(
                onClick = { viewModel.setProvider("custom", url = customUrlText.value)
                            onBack()
                          },
            ){ Text("Confirm") } }
        }
    }
}