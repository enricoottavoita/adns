package com.eyalm.adns.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.data.ApiRepository
import com.eyalm.adns.data.DnsRepository
import com.eyalm.adns.data.ListCard
import com.eyalm.adns.data.PercentCard
import com.eyalm.adns.data.StatRow
import com.eyalm.adns.data.StatsRegistry
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.data.network.NextDnsStatsGraphResponse
import com.eyalm.adns.data.parseList
import com.eyalm.adns.data.parsePercent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CardState {
    object Loading : CardState()
    data class ListData(val rows: List<StatRow>) : CardState()
    data class PercentData(val percent: Float) : CardState()
    object Error : CardState()
}

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiRepository(application)
    private val repository = DnsRepository(application)


    private val _stats = MutableStateFlow<NextDnsStatsGraphResponse?>(null)
    val stats: StateFlow<NextDnsStatsGraphResponse?> = _stats.asStateFlow()

    private val _currentFilter = MutableStateFlow("")
    val currentFilter: StateFlow<String> = _currentFilter.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val graphCache = mutableMapOf<String, NextDnsStatsGraphResponse>()


    private val _states = MutableStateFlow<Map<String, CardState>>(emptyMap())
    val states = _states.asStateFlow()

    private val cardCache = mutableMapOf<String, Map<String, CardState>>()
    private var loadedPeriod: String? = null

    init {
        viewModelScope.launch {
            repository.getDnsUrlFlow().collect {
                val provider = repository.getSelectedProvider()
                if (provider is DnsProvider.Enhanced) {
                    loadAllPeriods()
                } else {
                    _stats.value = null
                    _currentFilter.value = ""
                    _errorMessage.value = ""
                    graphCache.clear()
                    cardCache.clear()
                    loadedPeriod = null
                    _states.value = emptyMap()
                }
            }
        }
    }

    private suspend fun loadAllPeriods() {
        try {
            graphCache.clear()
            cardCache.clear()
            loadedPeriod = null
            listOf("-24h", "-7d", "-30d").forEach { period ->
                graphCache[period] = api.getNextDnsStatsGraph(period)
            }
            _stats.value = graphCache["-30d"]
            _currentFilter.value = "-30d"
            _errorMessage.value = ""
            Log.d("StatsViewModel", "graph data loaded")
            loadCards("-30d")
        } catch (e: Exception) {
            Log.e("StatsViewModel", "Error loading stats", e)
            _errorMessage.value = "Cannot load stats"
        }
    }

    fun getPeriod(period: String) {
        _stats.value = graphCache[period]
        _currentFilter.value = period
        loadCards(period)
    }

    fun refreshStats() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val provider = repository.getSelectedProvider()
            if (provider is DnsProvider.Enhanced) {
                cardCache.clear()
                loadedPeriod = null
                loadAllPeriods()
            }
            _isRefreshing.value = false
        }
    }

    private fun loadCards(period: String) {
        if (period == loadedPeriod) return
        loadedPeriod = period
        cardCache[period]?.let { _states.value = it; return }

        _states.value = StatsRegistry.cards.associate { it.key to CardState.Loading }

        viewModelScope.launch {
            val results = StatsRegistry.cards.map { card ->
                async {
                    val params = card.params + ("from" to period)
                    val data = api.getAnalytics(card.feature, params)
                    card.key to when {
                        data == null        -> CardState.Error
                        card is ListCard    -> CardState.ListData(parseList(card, data))
                        card is PercentCard -> CardState.PercentData(parsePercent(card, data))
                        else                -> CardState.Error
                    }
                }
            }.awaitAll().toMap()

            cardCache[period] = results
            if (loadedPeriod == period) _states.value = results
        }
    }
}
