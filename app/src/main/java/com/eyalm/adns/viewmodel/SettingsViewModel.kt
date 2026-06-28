package com.eyalm.adns.viewmodel

import android.app.Application
import android.app.StatusBarManager
import android.content.ComponentName
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.R
import com.eyalm.adns.data.ApiRepository
import com.eyalm.adns.data.Blocklist
import com.eyalm.adns.data.DnsRepository
import com.eyalm.adns.data.ListIcon
import com.eyalm.adns.data.ListItem
import com.eyalm.adns.data.ListSetting
import com.eyalm.adns.data.ListSource
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.ToggleSetting
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.data.network.NextDnsProfile
import com.eyalm.adns.data.network.toHexId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    enum class Page {
        MAIN,
        PROVIDERS,
        ACCOUNT_SETTINGS,
        SECURITY,
        PRIVACY,
        PARENTAL_CONTROL,
        SETTINGS_PAGE,
        GENERIC_LIST,
        BLOCKLISTS,
        LOGS,
        LANGUAGE,
    }

    private val repository = DnsRepository(application)
    private val apiRepository = ApiRepository(application)

    private val _dnsUrl = MutableStateFlow(repository.getDnsUrl())
    val dnsUrl: StateFlow<String?> = _dnsUrl.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(repository.isNotificationEnabled())
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.setNotificationEnabled(enabled)
        _notificationsEnabled.value = enabled
    }

    private val _page = MutableStateFlow(Page.MAIN)
    val page = _page.asStateFlow()

    fun setDnsUrl(url: String) {
        repository.setCustomUrl(url)
        _dnsUrl.value = url
    }

    fun addQuickTile() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager =
                getApplication<Application>().getSystemService(StatusBarManager::class.java)
            statusBarManager?.requestAddTileService(
                ComponentName(
                    getApplication(),
                    com.eyalm.adns.services.AdnsTileService::class.java
                ),
                getApplication<Application>().getString(R.string.adns_adblock),
                android.graphics.drawable.Icon.createWithResource(
                    getApplication(),
                    R.drawable.ic_launcher_foreground
                ),
                getApplication<Application>().mainExecutor
            ) { result ->
                val message = when (result) {
                    1 -> getApplication<Application>().getString(R.string.tile_already_added)
                    2 -> getApplication<Application>().getString(R.string.tile_added)
                    else -> ""
                }
                if (message.isNotEmpty())
                    Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                getApplication(),
                getApplication<Application>().getString(R.string.feature_not_supported_on_this_version),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun refreshNotification() {
        repository.updateNotification()
        _notificationsEnabled.value = repository.isNotificationEnabled()
    }


    private val _selectedProvider = MutableStateFlow(repository.getSelectedProvider())
    val selectedProvider = _selectedProvider.asStateFlow()

    fun setProvider(providerId: String, url: String? = null) {
        repository.setProvider(providerId, url)
        _selectedProvider.value = repository.getSelectedProvider()
    }

    fun refreshProvider() {
        _selectedProvider.value = repository.getSelectedProvider()
    }

    fun isLoggedIn(provider: DnsProvider): Boolean {
        return when (provider) {
            DnsProviders.NEXTDNS -> try {
                apiRepository.requireAuth()
                true
            } catch (e: Exception) {
                false
            }
            else -> false
        }
    }

    suspend fun getEmail(): String {
        return apiRepository.getNextDnsEmail()
    }

    fun setPage(page: Page) {
        _page.value = page
    }





    var profiles by mutableStateOf<List<NextDnsProfile>?>(null)
    var email by mutableStateOf<String?>(null)
    var currentProfile by mutableStateOf<NextDnsProfile?>(null)
    var nextDnsDeviceName by mutableStateOf(apiRepository.getNextDnsDeviceName())
        private set


    suspend fun getProfiles(): List<NextDnsProfile> {
        return apiRepository.getNextDnsProfiles()
    }

    suspend fun getCurrentProfile(): NextDnsProfile? {
        val profileId = apiRepository.getCurrentNextDnsProfileId()
        val currentProfiles = profiles ?: apiRepository.getNextDnsProfiles()
        return currentProfiles.firstOrNull { it.id == profileId }
    }

    fun setProfile(profile: NextDnsProfile) {
        val currentName = apiRepository.getNextDnsDeviceName()
        val nameToSet = currentName.ifEmpty { "ADNS" }
        apiRepository.setNextDnsProfile(profile, nameToSet)
        nextDnsDeviceName = apiRepository.getNextDnsDeviceName()
    }

    fun updateDeviceName(name: String) {
        apiRepository.setNextDnsDeviceName(name)
        nextDnsDeviceName = apiRepository.getNextDnsDeviceName()
        Toast.makeText(getApplication(), getApplication<Application>().getString(R.string.done), Toast.LENGTH_SHORT).show()
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            apiRepository.createNextDnsProfile(name)
            profiles = getProfiles()
        }
    }

    fun logout() {
        apiRepository.nextDnsLogOut()
        setPage(Page.MAIN)
        refreshProvider()
    }

    // new generic methods


    // e.g. "nrd" or "logs.drop.ip"
    private val _pageToggles = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val pageToggles: StateFlow<Map<String, Boolean>> = _pageToggles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    var currentListSetting: ListSetting? = null
        private set

    // Which page to return to when pressing back from the list screen
    private var listParentPage: Page = Page.MAIN

    private val _activeListIds = MutableStateFlow<Set<String>>(emptySet())
    val activeListIds: StateFlow<Set<String>> = _activeListIds.asStateFlow()

    private val _availableItems = MutableStateFlow<List<ListItem>>(emptyList())
    val availableItems: StateFlow<List<ListItem>> = _availableItems.asStateFlow()

    private val _loadedPageId = MutableStateFlow<String>("")
    val loadedPageId: StateFlow<String> = _loadedPageId.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    fun loadPageSettings(page: String, toggles: List<ToggleSetting>) {

        if (_loadedPageId.value == page && _pageToggles.value.isNotEmpty()) {
            return
        }

        _loadedPageId.value = ""
        _pageToggles.value = emptyMap()

        viewModelScope.launch {
            _isLoading.value = true
            val data = apiRepository.getPageSettings(page)
            if (data != null) {
                val states = mutableMapOf<String, Boolean>()
                for (toggle in toggles) {
                    val value = toggle.readFrom(data)
                    if (value != null) {
                        states[toggle.stateKey] = value
                    }
                }
                _pageToggles.value = states
                _loadedPageId.value = page
            } else {
                _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_load_page_data_check_your_network_connection_and_try_again_later))
            }
            _isLoading.value = false
        }
    }

    fun updateToggle(page: String, toggle: ToggleSetting, newValue: Boolean) {

        _pageToggles.value = _pageToggles.value.toMutableMap().apply {
            this[toggle.stateKey] = newValue
        }
        viewModelScope.launch {
            val payload = toggle.buildPatchPayload(newValue)
            val success = apiRepository.patchPageSettings(page, payload)
            if (!success) {
                _pageToggles.value = _pageToggles.value.toMutableMap().apply {
                    this[toggle.stateKey] = !newValue
                }
                _errorMessage.emit(getApplication<Application>().getString(R.string.network_error_please_try_again))
            }
        }
    }


    fun openListScreen(listSetting: ListSetting) {

        currentListSetting = listSetting

        listParentPage = when (listSetting.parentPage) {
            ListSetting.Page.SECURITY -> Page.SECURITY
            ListSetting.Page.PRIVACY -> Page.PRIVACY
            ListSetting.Page.PARENTAL_CONTROL -> Page.PARENTAL_CONTROL
            else -> Page.MAIN
        }
        _activeListIds.value = emptySet()
        _availableItems.value = emptyList()

        setPage(Page.GENERIC_LIST)
        loadListData(listSetting)
    }

    fun getListParentPage(): Page = listParentPage

    private fun loadListData(listSetting: ListSetting) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                if (listSetting.allowsCustomInput) {
                    val dataArray = apiRepository.getCustomListItems(listSetting.apiPage)

                    val activeIds = mutableSetOf<String>()
                    val items = mutableListOf<ListItem>()

                    dataArray?.forEach { element ->
                        val obj = element.asJsonObject
                        val id = obj.get("id").asString
                        val isActive = if (obj.has("active")) obj.get("active").asBoolean else true

                        items.add(ListItem(id = id, name = "*.$id"))
                        if (isActive) activeIds.add(id)
                    }

                    _activeListIds.value = activeIds
                    _availableItems.value = items
                } else {

                    val activeIds = apiRepository.getActiveListItems(
                        listSetting.apiPage, listSetting.apiFeat
                    )
                    _activeListIds.value = activeIds.toSet()

                    val items: List<ListItem> = when (listSetting.source) {
                        ListSource.SERVER -> loadServerList(listSetting)
                        ListSource.LOCALE -> loadLocaleList(listSetting)
                    }
                    _availableItems.value = items
                    if (items.isEmpty()) {
                        _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_load_list_data))
                    }
                }
                _isLoading.value = false

            } catch (e: Exception) {
                _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_load_list_data_check_your_network_connection_and_try_again_later))
                _isLoading.value = false
            }
        }
    }


    private fun getDomain(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            url.replace("https://", "").replace("http://", "")
        } catch (e: Exception) {
            null
        }
    }


    private suspend fun loadServerList(listSetting: ListSetting): List<ListItem> {
        val catalog = apiRepository.getAvailableCatalog(
            listSetting.apiPage, listSetting.apiFeat
        ) ?: return emptyList()

        val dataArray = catalog.getAsJsonArray("data") ?: return emptyList()

        return dataArray.map { element ->
            val obj = element.asJsonObject
            Log.d("loadServerList", "obj: $obj")
            val id = obj.get("id").asString

            when (listSetting.apiFeat) {
                "tlds" -> ListItem(
                    id = id,
                    name = ".$id",
                )
                "blocklists" -> {
                    ListItem(
                        id = id,
                        name = obj.get("name")?.takeIf { !it.isJsonNull }?.asString ?: getApplication<Application>().getString(R.string.nextdns_ads_trackers_blocklist) ,
                        description = obj.get("description")?.takeIf { !it.isJsonNull }?.asString ?: getApplication<Application>().getString(R.string.a_comprehensive_blocklist_to_block_ads_trackers_in_all_countries_this_is_the_recommended_starter),
                    )
                }
                "services" -> {
                    val website = obj.get("website")?.takeIf { !it.isJsonNull }?.asString ?: ""

                    val domain = website
                        .removePrefix("https://")
                        .removePrefix("http://")
                        .substringBefore("/")

                    val prettyName = Locales.getString("parentalControl", "services", "services", id)
                        .takeIf { it.isNotEmpty() } ?: id

                    ListItem(
                        id = id,
                        name = prettyName,
                        icon = ListIcon.Url("https://favicons.nextdns.io/${domain.toHexId()}@3x.png")
                    )
                }
                else -> ListItem(
                    id = id,
                    name = obj.get("name")?.takeIf { !it.isJsonNull }?.asString ?: id,
                    description = obj.get("description")?.takeIf { !it.isJsonNull }?.asString,
                    icon = ListIcon.Vector(androidx.compose.material.icons.Icons.Default.Shield)
                )
            }
        }
    }

    private fun loadLocaleList(listSetting: ListSetting): List<ListItem> {
        val map = Locales.getMap(*listSetting.localePath.toTypedArray())
            ?: return emptyList()

        return map.map { (key, value) ->
            when (listSetting.apiFeat) {
                "natives" -> {
                    val nameVal = if (value is Map<*, *>) (value["name"] as? String) else null
                    val devicesVal = if (value is Map<*, *>) (value["devices"] as? String) else null
                    val vector = when (key.lowercase()) {
                        "windows", "apple" -> androidx.compose.material.icons.Icons.Default.Computer
                        "xiaomi", "samsung", "huawei" -> androidx.compose.material.icons.Icons.Default.Smartphone
                        "sonos" -> androidx.compose.material.icons.Icons.Default.Speaker
                        else -> androidx.compose.material.icons.Icons.Default.Devices
                    }
                    ListItem(
                        id = key,
                        name = nameVal ?: key,
                        description = devicesVal,
                        icon = ListIcon.Vector(vector)
                    )
                }
                "categories" -> {
                    val nameVal = if (value is Map<*, *>) (value["name"] as? String) else null
                    val descVal = if (value is Map<*, *>) (value["description"] as? String) else null
                    val vector = when (key.lowercase()) {
                        "porn" -> androidx.compose.material.icons.Icons.Default.Block
                        "dating" -> androidx.compose.material.icons.Icons.Default.Favorite
                        "social" -> androidx.compose.material.icons.Icons.Default.People
                        "video" -> androidx.compose.material.icons.Icons.Default.PlayCircle
                        "games" -> androidx.compose.material.icons.Icons.Default.SportsEsports
                        "gambling" -> androidx.compose.material.icons.Icons.Default.Casino
                        "shopping" -> androidx.compose.material.icons.Icons.Default.ShoppingBag
                        "chat" -> androidx.compose.material.icons.Icons.Default.Chat
                        "music" -> androidx.compose.material.icons.Icons.Default.MusicNote
                        else -> androidx.compose.material.icons.Icons.Default.Folder
                    }
                    ListItem(
                        id = key,
                        name = nameVal ?: key,
                        description = descVal,
                        icon = ListIcon.Vector(vector)
                    )
                }
                else -> ListItem(
                    id = key,
                    name = (value as? String) ?: key,
                    icon = ListIcon.None
                )
            }
        }
    }


    fun toggleListItem(itemId: String) {
        val listSetting = currentListSetting ?: return
        val isCurrentlyActive = _activeListIds.value.contains(itemId)
        val newState = !isCurrentlyActive

        if (newState) _activeListIds.value += itemId else _activeListIds.value -= itemId


        viewModelScope.launch {
            val success = if (listSetting.allowsCustomInput) {
                apiRepository.patchCustomListItem(listSetting.apiPage, itemId, newState)
            } else {
                if (isCurrentlyActive) {
                    apiRepository.removeListItem(listSetting.apiPage, listSetting.apiFeat, itemId)
                } else {
                    apiRepository.addListItem(listSetting.apiPage, listSetting.apiFeat, itemId)
                }
            }
            if (!success) {
                if (isCurrentlyActive) _activeListIds.value += itemId else _activeListIds.value -= itemId
                _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_update, itemId))
            }
        }
    }

    fun addCustomDomain(domain: String) {
        val listSetting = currentListSetting ?: return
        if (!listSetting.allowsCustomInput) return

        val cleanDomain = domain.trim().lowercase()

        val newItem = ListItem(id = cleanDomain, name = "*.$cleanDomain")
        _availableItems.value = listOf(newItem) + _availableItems.value
        _activeListIds.value += cleanDomain

        viewModelScope.launch {
            val success = apiRepository.addCustomListItem(listSetting.apiPage, cleanDomain) is ApiRepository.AddListResult.Success
            if (!success) {
                _availableItems.value = _availableItems.value.filter { it.id != cleanDomain }
                _activeListIds.value -= cleanDomain
                _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_add, cleanDomain))
            }
        }
    }

    fun deleteCustomDomain(domain: String) {
        val listSetting = currentListSetting ?: return
        if (!listSetting.allowsCustomInput) return

        val wasActive = _activeListIds.value.contains(domain)

        _availableItems.value = _availableItems.value.filter { it.id != domain }
        _activeListIds.value -= domain

        viewModelScope.launch {
            val success = apiRepository.removeCustomListItem(listSetting.apiPage, domain)
            if (!success) {
                _availableItems.value = listOf(ListItem(id = domain, name = domain)) + _availableItems.value
                if (wasActive) _activeListIds.value += domain
                _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_delete, domain))
            }
        }
    }

    // old methods

    var blocklists: List<Blocklist>? by mutableStateOf(null)

    fun getBlocklists() {
        viewModelScope.launch {
            val blocklistsResponse = apiRepository.getNextDnsBlocklists()
            blocklists = blocklistsResponse
        }
    }


    fun updateBlocklists(blocklistId: String) {
        viewModelScope.launch {
            apiRepository.updateNextDnsBlocklists(blocklistId)
        }
    }

    fun removeBlocklists(blocklistId: String) {
        viewModelScope.launch {
            apiRepository.removeNextDnsBlocklists(blocklistId)
        }
    }

}