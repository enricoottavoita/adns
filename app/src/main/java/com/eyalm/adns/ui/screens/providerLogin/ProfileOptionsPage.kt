package com.eyalm.adns.ui.screens.providerLogin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eyalm.adns.data.network.NextDnsProfile
import com.eyalm.adns.ui.components.OnboardingTemplate
import com.eyalm.adns.ui.components.StandardBottomBar

@Composable
fun ProfileOptionPage(
    profiles: List<NextDnsProfile>,
    onNextClick: (profile: NextDnsProfile) -> Unit,
    onBackClick: () -> Unit
) {
    
    var selectedProfile by remember { mutableStateOf(profiles[0]) }
    
    OnboardingTemplate(
        onBackClick = onBackClick,
        bottomBarContent = {
            StandardBottomBar(
                message = "Choose your profile.",
                enabled = true,
                onNextClick = { onNextClick(selectedProfile) }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Choose your profile.")

                profiles.forEach { profile ->
                    Row() {
                        RadioButton(
                            selected = profile == selectedProfile,
                            onClick = { selectedProfile = profile }
                        )
                        Text(profile.name)
                    }
                }
            }


        }
    )
}