package com.eyalm.adns.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.R
import com.eyalm.adns.data.ApiRepository
import com.eyalm.adns.data.network.NextDnsDeviceItem
import com.eyalm.adns.data.network.NextDnsLogEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class LogsViewModel(application: Application) :  AndroidViewModel(application) {

    private val apiRepository = ApiRepository(application)

    val logsList = mutableStateListOf<NextDnsLogEntry>()
    var devicesList by mutableStateOf<List<NextDnsDeviceItem>>(emptyList())
        private set
    var isInitialLoading by mutableStateOf(false)
        private set
    var isFetchingMore by mutableStateOf(false)
        private set

    var searchQuery by mutableStateOf("")
    private var searchJob: Job? = null

    var blockedSelected by mutableStateOf<Boolean>(false)
    var rawEnabled by mutableStateOf(false)
    var selectedDevice by mutableStateOf<String?>(null)
    private var nextCursor: String? = null
    var hasMorePages by mutableStateOf(true)
        private set

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    private fun loadDevices() {
        viewModelScope.launch {
            devicesList = apiRepository.getNextDnsDevices()
        }
    }
    private fun loadInitialLogs() {
        viewModelScope.launch {
            isInitialLoading = true
            val response = apiRepository.getNextDnsLogs(
                cursor = null,
                search = searchQuery,
                blockedOnly = blockedSelected,
                rawLogs = rawEnabled,
                deviceId = selectedDevice
            )
            logsList.clear()
            if (response == null) {
                _uiEvent.emit(getApplication<Application>().getString(R.string.failed_to_load_logs_check_your_connection))
            }
            response?.data?.let { logsList.addAll(it) }
            nextCursor = response?.meta?.pagination?.cursor
            hasMorePages = nextCursor != null
            isInitialLoading = false
        }
    }

    fun fetchNextPage() {
        if (isFetchingMore || isInitialLoading || !hasMorePages) return
        viewModelScope.launch {
            isFetchingMore = true
            val response = apiRepository.getNextDnsLogs(
                cursor = nextCursor,
                search = searchQuery,
                blockedOnly = blockedSelected,
                rawLogs = rawEnabled,
                deviceId = selectedDevice
            )
            response?.data?.let { logsList.addAll(it) }
            nextCursor = response?.meta?.pagination?.cursor
            hasMorePages = nextCursor != null
            isFetchingMore = false
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500.milliseconds)
            loadInitialLogs()
        }
    }

    fun addDomainToList(page: String, domain: String) {
        viewModelScope.launch {
            val result = apiRepository.addCustomListItem(page, domain)
            val pageName = if (page == "allowlist") getApplication<Application>().getString(R.string.allowlist) else getApplication<Application>().getString(R.string.denylist)
            val message = when (result) {
                is ApiRepository.AddListResult.Success -> getApplication<Application>().getString(R.string.added_to, domain, pageName)
                is ApiRepository.AddListResult.AlreadyAdded -> getApplication<Application>().getString(R.string.is_already_in, domain, pageName)
                is ApiRepository.AddListResult.Error -> getApplication<Application>().getString(R.string.failed_to_add_domain, domain, result.code)
            }
            _uiEvent.emit(message)
        }
    }

    fun setRaw(state: Boolean) {
        rawEnabled = state
        loadInitialLogs()
    }

    fun setDevice(deviceId: String?) {
        selectedDevice = deviceId
        loadInitialLogs()
    }

    fun setBlocked(blocked: Boolean) {
        blockedSelected = blocked
        loadInitialLogs()
    }

    fun refresh() {
        loadDevices()
        loadInitialLogs()
    }
}
