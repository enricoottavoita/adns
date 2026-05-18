package com.eyalm.adns.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.data.network.NextDnsAnalytics
import com.eyalm.adns.data.network.NextDnsAnalyticsData
import com.eyalm.adns.ui.theme.AdnsTheme
import com.eyalm.adns.viewmodel.MainViewModel

@Composable
fun StatsScreen(
    paddingValues: PaddingValues,
    viewModel: MainViewModel = viewModel()
) {
    val stats = viewModel.dnsStats
    val cachedProvider = viewModel.dnsProvider
    val error = remember { mutableStateOf<String?>(null) }
    val provider = remember { mutableStateOf<DnsProvider?>(cachedProvider) }
    val isSupported = remember { mutableStateOf<Boolean>(true) }

    LaunchedEffect(Unit) { // TODO: Move this to outside of the screen
        if (viewModel.dnsStats == null) {
            try {
                provider.value = viewModel.getStats()
            } catch (e: Exception) {
                error.value = e.message ?: "Failed to load stats"
                if (e.message?.contains("must be logged") == true) {
                    isSupported.value = false
                }
            }
        }
    }

    StatsScreenContent( // TODO: after switching to a provider, this still shows the previous stats.
        stats = stats,
        error = error.value,
        paddingValues = paddingValues,
        provider = provider.value,
        isSupported = isSupported.value
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsScreenContent(
    stats: NextDnsAnalytics?,
    error: String?,
    paddingValues: PaddingValues,
    provider: DnsProvider?,
    isSupported: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        if (error != null && isSupported) {
            Spacer(modifier = Modifier.weight(1f))
            UnsupportedScreen()
            Spacer(modifier = Modifier.weight(1f))
        } else if (!isSupported) { // TODO UGH WHATEVER
            Spacer(modifier = Modifier.weight(1f))
            UnsupportedScreen()
            Spacer(modifier = Modifier.weight(1f))
        } else if (stats == null) {
            Spacer(modifier = Modifier.weight(1f))
            LoadingIndicator(modifier = Modifier.size(100.dp).align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.weight(1f))
        } else if (provider != null) {
            StatsCards(stats, provider)
        } else {
            Log.d("test0", "test")
        }

    }
}

@Composable
fun UnsupportedScreen() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = "Unsupported",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "To use Stats, switch to an enhanced provider.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Enhanced providers are using your credentials, which enables direct access to your provider's settings and stats. ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun StatsCards(
    stats: NextDnsAnalytics,
    provider: DnsProvider
) {
    val totalQueries = stats.data[0].queries.toInt().coerceAtLeast(1)
    val blockedQueries = stats.data[1].queries.toInt()
    val percentValue = (blockedQueries.toFloat() / totalQueries * 100).toInt()

    StatsCard(
        modifier = Modifier,
        bigText = stats.data[1].queries,
        smallText = "Blocked",
        isRed = true
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatsCard(
            modifier = Modifier.weight(1f),
            bigText = stats.data[0].queries,
            smallText = "Total"
        )
        StatsCard(
            modifier = Modifier.weight(1f),
            bigText = "$percentValue%",
            smallText = "Percent"
        )
    }

    StatsCard(
        modifier = Modifier,
        bigText = provider.name,
        smallText = "Provider"
    )

}

@Composable
fun StatsCard(bigText: String, smallText: String, modifier: Modifier, isRed: Boolean = false) {
    Card(
        modifier = modifier
            .fillMaxWidth()

    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = bigText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = if (isRed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp)
            )
            Text(
                text = smallText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 32.dp)
            )
        }


    }
}

@Preview
@Composable
fun StatsScreenPreview() {
    AdnsTheme {
        StatsScreenContent(
            stats = NextDnsAnalytics(
                data = listOf(
                    NextDnsAnalyticsData(status = "allowed", queries = "100"),
                    NextDnsAnalyticsData(status = "blocked", queries = "25")
                ),
                meta = Any()
            ),
            error = null,
            paddingValues = PaddingValues(),
            provider = DnsProvider.Enhanced(
                id = "3",
                name = "NEXTDNS",
                description = "Test"
            )
        )
    }
}