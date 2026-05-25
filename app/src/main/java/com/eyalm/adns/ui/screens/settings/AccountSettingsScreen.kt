package com.eyalm.adns.ui.screens.settings

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.ui.components.ProfilesList
import com.eyalm.adns.ui.screens.providerLogin.CreateProfileDialog
import com.eyalm.adns.ui.theme.pageTitle
import com.eyalm.adns.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit = {},
    provider: DnsProvider
) {
    val viewModel: SettingsViewModel = viewModel()
    val email = viewModel.email
    var selectedProfile by remember { mutableStateOf(viewModel.currentProfile) }
    var openCreateProfileDialog by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        selectedProfile = viewModel.getCurrentProfile()
    }

    when {
        openCreateProfileDialog -> {
            CreateProfileDialog(
                onDismissRequest = { openCreateProfileDialog = false },
                onConfirmation = { name ->
                    openCreateProfileDialog = false
                    viewModel.createProfile(name)
                }
            )

        }
    }


    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                title = { },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "${provider.name} Settings",
                    style = MaterialTheme.typography.pageTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 40.dp),
                )
            }

            if (selectedProfile == null) {
                item {
                    Loading()
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "LOGGED IN AS",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = email ?: "Loading...",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.logout()
                                    Log.d("logout", "loggedout")
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Logout", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                viewModel.profiles?.let { currentProfiles ->
                    item {
                        ProfilesList(
                            profiles = currentProfiles,
                            selectedProfile = selectedProfile,
                            onProfileSelected = {
                                selectedProfile = it
                                viewModel.setProfile(it)
                            },
                            onCreateProfileClick = {
                                openCreateProfileDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Loading() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ContainedLoadingIndicator(modifier = Modifier.size(100.dp))
    }
}