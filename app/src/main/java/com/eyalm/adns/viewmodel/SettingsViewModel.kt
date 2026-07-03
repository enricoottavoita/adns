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
import com.eyalm.adns.data.DnsRepository
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.data.network.NextDnsProfile
import com.eyalm.adns.data.network.toHexId
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceItem
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceSource
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceSpec
import com.eyalm.adns.data.nextdns.settings.BooleanSettingSpec
import com.eyalm.adns.data.nextdns.settings.IntSelectSettingSpec
import com.eyalm.adns.data.nextdns.settings.ProfileSettingSpec
import com.eyalm.adns.data.nextdns.settings.SettingId
import com.eyalm.adns.data.nextdns.settings.SettingsPageSpec
import com.eyalm.adns.data.nextdns.settings.StringSelectSettingSpec
import com.eyalm.adns.data.nextdns.settings.valueAt
import com.eyalm.adns.domain.nextdns.ApiResult
import com.eyalm.adns.domain.nextdns.ProfileCapabilities
import com.eyalm.adns.domain.nextdns.ProfileRole
import com.eyalm.adns.domain.nextdns.capabilities
import com.eyalm.adns.domain.nextdns.profileRoleFromWire
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScalarSettingsUiState(
    val page: String? = null,
    val profileId: String? = null,
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val values: Map<SettingId, JsonElement> = emptyMap(),
    val saving: Set<SettingId> = emptySet(),
    val pendingConfirmation: PendingSettingChange? = null,
)

data class ProfileSessionState(
    val loading: Boolean = true,
    val profiles: List<NextDnsProfile> = emptyList(),
    val selected: NextDnsProfile? = null,
    val capabilities: ProfileCapabilities = ProfileRole.Unknown.capabilities(),
    val logsRevision: Long = 0,
    val error: ApiResult<*>? = null,
)

data class PendingSettingChange(
    val spec: ProfileSettingSpec<*>,
    val encodedValue: Any,
)

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
        LOGS,
        LANGUAGE,
    }

    private val repository = DnsRepository(application)
    private val apiRepository = ApiRepository(application)

    private val _dnsUrl = MutableStateFlow(repository.getDnsUrl())
    val dnsUrl: StateFlow<String?> = _dnsUrl.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(repository.isNotificationEnabled())
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _profileSessionState = MutableStateFlow(ProfileSessionState())
    val profileSessionState = _profileSessionState.asStateFlow()

    fun refreshProfileSession() {
        _profileSessionState.value = _profileSessionState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val result = apiRepository.getNextDnsProfilesResult()) {
                is ApiResult.Success -> publishProfileSession(result.value)
                else -> _profileSessionState.value = _profileSessionState.value.copy(
                    loading = false,
                    error = result,
                )
            }
        }
    }

    fun onProfileRemoved(
        removedProfileId: String,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = apiRepository.getNextDnsProfilesResult()
            val remaining = when (result) {
                is ApiResult.Success -> result.value.filterNot { it.id == removedProfileId }
                else -> _profileSessionState.value.profiles.filterNot { it.id == removedProfileId }
            }
            val wasSelected = apiRepository.getCurrentNextDnsProfileId() == removedProfileId
            if (wasSelected) {
                remaining.firstOrNull()?.let { fallback ->
                    apiRepository.setNextDnsProfile(
                        fallback,
                        apiRepository.getNextDnsDeviceName(),
                    )
                } ?: apiRepository.clearSelectedNextDnsProfile()
                refreshProvider()
            }
            invalidateProfileScopedState()
            publishProfileSession(remaining)
            if (result !is ApiResult.Success) {
                _profileSessionState.value = _profileSessionState.value.copy(error = result)
            }
            setPage(Page.MAIN)
            onComplete()
        }
    }

    fun onProfileRenamed() = refreshProfileSession()

    private fun publishProfileSession(availableProfiles: List<NextDnsProfile>) {
        val previous = _profileSessionState.value
        val selectedId = apiRepository.getCurrentNextDnsProfileId()
        val selected = availableProfiles.firstOrNull { it.id == selectedId }
        val profileChanged = previous.selected?.id != selected?.id
        if (profileChanged) {
            invalidateProfileScopedState()
            if (_page.value == Page.GENERIC_LIST) {
                setPage(Page.MAIN)
            }
        }
        profiles = availableProfiles
        currentProfile = selected
        _profileSessionState.value = ProfileSessionState(
            loading = false,
            profiles = availableProfiles,
            selected = selected,
            capabilities = profileRoleFromWire(selected?.role).capabilities(),
            logsRevision = previous.logsRevision + if (profileChanged) 1 else 0,
        )
    }

    private fun invalidateProfileScopedState() {
        listLoadGeneration++
        _scalarSettings.value = ScalarSettingsUiState()
        _activeListIds.value = emptySet()
        _availableItems.value = emptyList()
        _listLoading.value = false
        currentListSetting = null
    }

    fun invalidateLogs() {
        _profileSessionState.value = _profileSessionState.value.copy(
            logsRevision = _profileSessionState.value.logsRevision + 1,
        )
    }

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
            DnsProviders.NEXTDNS -> apiRepository.isSignedIn()
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
        val knownProfiles = _profileSessionState.value.profiles.ifEmpty { profiles.orEmpty() }
        val updatedProfiles = if (knownProfiles.any { it.id == profile.id }) {
            knownProfiles.map { if (it.id == profile.id) profile else it }
        } else {
            knownProfiles + profile
        }
        profiles = updatedProfiles
        publishProfileSession(updatedProfiles)
        refreshProvider()
        refreshProfileSession()
    }

    fun updateDeviceName(name: String) {
        apiRepository.setNextDnsDeviceName(name)
        nextDnsDeviceName = apiRepository.getNextDnsDeviceName()
        Toast.makeText(getApplication(), getApplication<Application>().getString(R.string.done), Toast.LENGTH_SHORT).show()
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            apiRepository.createNextDnsProfile(name)
            when (val result = apiRepository.getNextDnsProfilesResult()) {
                is ApiResult.Success -> publishProfileSession(result.value)
                else -> _profileSessionState.value = _profileSessionState.value.copy(error = result)
            }
        }
    }

    fun logout() {
        apiRepository.nextDnsLogOut()
        setPage(Page.MAIN)
        refreshProvider()
    }

    private val _scalarSettings = MutableStateFlow(ScalarSettingsUiState())
    val scalarSettings: StateFlow<ScalarSettingsUiState> = _scalarSettings.asStateFlow()

    private val _listLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _listLoading.asStateFlow()
    private var listLoadGeneration = 0L


    var currentListSetting: NextDnsResourceSpec? = null
        private set

    // Which page to return to when pressing back from the list screen
    private var listParentPage: Page = Page.MAIN

    private val _activeListIds = MutableStateFlow<Set<String>>(emptySet())
    val activeListIds: StateFlow<Set<String>> = _activeListIds.asStateFlow()

    private val _availableItems = MutableStateFlow<List<NextDnsResourceItem>>(emptyList())
    val availableItems: StateFlow<List<NextDnsResourceItem>> = _availableItems.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    fun loadScalarSettings(pageSpec: SettingsPageSpec) {
        val profileId = _profileSessionState.value.selected?.id ?: return
        val current = _scalarSettings.value
        if (
            current.page == pageSpec.page &&
            current.profileId == profileId &&
            (current.loading || current.loaded)
        ) return

        _scalarSettings.value = ScalarSettingsUiState(
            page = pageSpec.page,
            profileId = profileId,
            loading = true,
        )
        viewModelScope.launch {
            when (val result = apiRepository.getScalarSettings(pageSpec.page)) {
                is ApiResult.Success -> {
                    if (
                        _profileSessionState.value.selected?.id != profileId ||
                        _scalarSettings.value.page != pageSpec.page ||
                        _scalarSettings.value.profileId != profileId
                    ) return@launch
                    val values = buildMap {
                        pageSpec.settings.forEach { spec ->
                            val raw = result.value.valueAt(spec.api.path)
                            if (raw != null && spec.isValid(raw)) {
                                put(spec.id, raw)
                            }
                        }
                    }
                    _scalarSettings.value = ScalarSettingsUiState(
                        page = pageSpec.page,
                        profileId = profileId,
                        loaded = true,
                        values = values,
                    )
                }

                else -> {
                    if (
                        _profileSessionState.value.selected?.id != profileId ||
                        _scalarSettings.value.page != pageSpec.page ||
                        _scalarSettings.value.profileId != profileId
                    ) return@launch
                    _scalarSettings.value = ScalarSettingsUiState(
                        page = pageSpec.page,
                        profileId = profileId,
                    )
                    _errorMessage.emit(
                        getApplication<Application>().getString(
                            R.string.failed_to_load_page_data_check_your_network_connection_and_try_again_later
                        )
                    )
                }
            }
        }
    }

    fun changeBooleanSetting(spec: BooleanSettingSpec, value: Boolean) {
        if (!_profileSessionState.value.capabilities.canEditSettings) return
        requestSettingChange(spec, spec.encode(value))
    }

    fun changeIntSetting(spec: IntSelectSettingSpec, value: Int) {
        if (!_profileSessionState.value.capabilities.canEditSettings) return
        requestSettingChange(spec, spec.encode(value))
    }

    fun changeStringSetting(spec: StringSelectSettingSpec, value: String) {
        if (!_profileSessionState.value.capabilities.canEditSettings) return
        requestSettingChange(spec, spec.encode(value))
    }

    fun confirmPendingSettingChange() {
        val pending = _scalarSettings.value.pendingConfirmation ?: return
        _scalarSettings.value = _scalarSettings.value.copy(pendingConfirmation = null)
        persistSettingChange(pending.spec, pending.encodedValue)
    }

    fun cancelPendingSettingChange() {
        _scalarSettings.value = _scalarSettings.value.copy(pendingConfirmation = null)
    }

    private fun requestSettingChange(spec: ProfileSettingSpec<*>, encodedValue: Any) {
        val state = _scalarSettings.value
        if (spec.id in state.saving || state.pendingConfirmation != null) return
        if (state.values[spec.id] == Gson().toJsonTree(encodedValue)) return

        if (spec.confirmation != null) {
            _scalarSettings.value = state.copy(
                pendingConfirmation = PendingSettingChange(spec, encodedValue)
            )
        } else {
            persistSettingChange(spec, encodedValue)
        }
    }

    private fun persistSettingChange(spec: ProfileSettingSpec<*>, encodedValue: Any) {
        val previousValue = _scalarSettings.value.values[spec.id] ?: return
        val profileId = _scalarSettings.value.profileId ?: return
        val encodedJson = Gson().toJsonTree(encodedValue)

        _scalarSettings.value = _scalarSettings.value.copy(
            values = _scalarSettings.value.values + (spec.id to encodedJson),
            saving = _scalarSettings.value.saving + spec.id,
        )

        viewModelScope.launch {
            val result = apiRepository.patchScalarSetting(spec.api, encodedValue)
            val current = _scalarSettings.value
            if (current.page != spec.api.page || current.profileId != profileId) return@launch

            if (result is ApiResult.Success) {
                _scalarSettings.value = current.copy(saving = current.saving - spec.id)
            } else {
                _scalarSettings.value = current.copy(
                    values = current.values + (spec.id to previousValue),
                    saving = current.saving - spec.id,
                )
                _errorMessage.emit(
                    getApplication<Application>().getString(R.string.network_error_please_try_again)
                )
            }
        }
    }

    private fun ProfileSettingSpec<*>.isValid(raw: JsonElement): Boolean = when (this) {
        is BooleanSettingSpec -> decode(raw) != null
        is IntSelectSettingSpec -> decode(raw) != null
        is StringSelectSettingSpec -> decode(raw) != null
    }


    fun openListScreen(listSetting: NextDnsResourceSpec) {

        currentListSetting = listSetting

        listParentPage = when (listSetting.parentPage) {
            NextDnsResourceSpec.ParentPage.SECURITY -> Page.SECURITY
            NextDnsResourceSpec.ParentPage.PRIVACY -> Page.PRIVACY
            NextDnsResourceSpec.ParentPage.PARENTAL_CONTROL -> Page.PARENTAL_CONTROL
            else -> Page.MAIN
        }
        _activeListIds.value = emptySet()
        _availableItems.value = emptyList()

        setPage(Page.GENERIC_LIST)
        loadListData(listSetting)
    }

    fun getListParentPage(): Page = listParentPage

    private fun loadListData(listSetting: NextDnsResourceSpec) {
        val profileId = _profileSessionState.value.selected?.id ?: return
        val requestGeneration = ++listLoadGeneration
        viewModelScope.launch {
            try {
                _listLoading.value = true
                val (activeIds, items) = if (listSetting.allowsCustomInput) {
                    val dataArray = apiRepository.getCustomListItems(listSetting.apiPage)

                    val activeIds = mutableSetOf<String>()
                    val items = mutableListOf<NextDnsResourceItem>()

                    dataArray?.forEach { element ->
                        val obj = element.asJsonObject
                        val id = obj.get("id").asString
                        val isActive = if (obj.has("active")) obj.get("active").asBoolean else true

                        items.add(NextDnsResourceItem(id = id, name = "*.$id"))
                        if (isActive) activeIds.add(id)
                    }
                    activeIds to items
                } else {
                    val activeIds = apiRepository.getActiveListItems(
                        listSetting.apiPage, listSetting.apiFeature
                    )
                    val items: List<NextDnsResourceItem> = when (listSetting.source) {
                        NextDnsResourceSource.SERVER -> loadServerList(listSetting)
                        NextDnsResourceSource.LOCALE -> loadLocaleList(listSetting)
                    }
                    activeIds.toSet() to items
                }

                if (!isCurrentListRequest(requestGeneration, profileId, listSetting)) return@launch
                _activeListIds.value = activeIds
                _availableItems.value = items
                if (items.isEmpty() && !listSetting.allowsCustomInput) {
                    _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_load_list_data))
                }
            } catch (e: Exception) {
                if (isCurrentListRequest(requestGeneration, profileId, listSetting)) {
                    _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_load_list_data_check_your_network_connection_and_try_again_later))
                }
            } finally {
                if (isCurrentListRequest(requestGeneration, profileId, listSetting)) {
                    _listLoading.value = false
                }
            }
        }
    }

    private fun isCurrentListRequest(
        requestGeneration: Long,
        profileId: String,
        listSetting: NextDnsResourceSpec,
    ): Boolean =
        requestGeneration == listLoadGeneration &&
                _profileSessionState.value.selected?.id == profileId &&
                currentListSetting == listSetting


    private fun getDomain(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            url.replace("https://", "").replace("http://", "")
        } catch (e: Exception) {
            null
        }
    }


    private suspend fun loadServerList(listSetting: NextDnsResourceSpec): List<NextDnsResourceItem> {
        val catalog = apiRepository.getAvailableCatalog(
            listSetting.apiPage, listSetting.apiFeature
        ) ?: return emptyList()

        val dataArray = catalog.getAsJsonArray("data") ?: return emptyList()

        return dataArray.map { element ->
            val obj = element.asJsonObject
            Log.d("loadServerList", "obj: $obj")
            val id = obj.get("id").asString

            when (listSetting.apiFeature) {
                "tlds" -> NextDnsResourceItem(
                    id = id,
                    name = ".$id",
                )
                "blocklists" -> {
                    NextDnsResourceItem(
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

                    NextDnsResourceItem(
                        id = id,
                        name = prettyName,
                        icon = ListIcon.Url("https://favicons.nextdns.io/${domain.toHexId()}@3x.png")
                    )
                }
                else -> NextDnsResourceItem(
                    id = id,
                    name = obj.get("name")?.takeIf { !it.isJsonNull }?.asString ?: id,
                    description = obj.get("description")?.takeIf { !it.isJsonNull }?.asString,
                    icon = ListIcon.Vector(androidx.compose.material.icons.Icons.Default.Shield)
                )
            }
        }
    }

    private fun loadLocaleList(listSetting: NextDnsResourceSpec): List<NextDnsResourceItem> {
        val map = Locales.getMap(*listSetting.localePath.toTypedArray())
            ?: return emptyList()

        return map.map { (key, value) ->
            when (listSetting.apiFeature) {
                "natives" -> {
                    val nameVal = if (value is Map<*, *>) (value["name"] as? String) else null
                    val devicesVal = if (value is Map<*, *>) (value["devices"] as? String) else null
                    val vector = when (key.lowercase()) {
                        "windows", "apple" -> androidx.compose.material.icons.Icons.Default.Computer
                        "xiaomi", "samsung", "huawei" -> androidx.compose.material.icons.Icons.Default.Smartphone
                        "sonos" -> androidx.compose.material.icons.Icons.Default.Speaker
                        else -> androidx.compose.material.icons.Icons.Default.Devices
                    }
                    NextDnsResourceItem(
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
                    NextDnsResourceItem(
                        id = key,
                        name = nameVal ?: key,
                        description = descVal,
                        icon = ListIcon.Vector(vector)
                    )
                }
                else -> NextDnsResourceItem(
                    id = key,
                    name = (value as? String) ?: key,
                    icon = ListIcon.None
                )
            }
        }
    }


    fun toggleListItem(itemId: String) {
        if (!_profileSessionState.value.capabilities.canEditSettings) return
        val profileId = _profileSessionState.value.selected?.id ?: return
        val listSetting = currentListSetting ?: return
        val isCurrentlyActive = _activeListIds.value.contains(itemId)
        val newState = !isCurrentlyActive

        if (newState) _activeListIds.value += itemId else _activeListIds.value -= itemId


        viewModelScope.launch {
            val success = if (listSetting.allowsCustomInput) {
                apiRepository.patchCustomListItem(listSetting.apiPage, itemId, newState)
            } else {
                if (isCurrentlyActive) {
                    apiRepository.removeListItem(listSetting.apiPage, listSetting.apiFeature, itemId)
                } else {
                    apiRepository.addListItem(listSetting.apiPage, listSetting.apiFeature, itemId)
                }
            }
            if (!success) {
                if (_profileSessionState.value.selected?.id != profileId || currentListSetting != listSetting) {
                    return@launch
                }
                if (isCurrentlyActive) _activeListIds.value += itemId else _activeListIds.value -= itemId
                _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_update, itemId))
            }
        }
    }

    fun addCustomDomain(domain: String) {
        if (!_profileSessionState.value.capabilities.canEditSettings) return
        val profileId = _profileSessionState.value.selected?.id ?: return
        val listSetting = currentListSetting ?: return
        if (!listSetting.allowsCustomInput) return

        val cleanDomain = domain.trim().lowercase()

        val newItem = NextDnsResourceItem(id = cleanDomain, name = "*.$cleanDomain")
        _availableItems.value = listOf(newItem) + _availableItems.value
        _activeListIds.value += cleanDomain

        viewModelScope.launch {
            val success = apiRepository.addCustomListItem(listSetting.apiPage, cleanDomain) is ApiRepository.AddListResult.Success
            if (!success) {
                if (_profileSessionState.value.selected?.id != profileId || currentListSetting != listSetting) {
                    return@launch
                }
                _availableItems.value = _availableItems.value.filter { it.id != cleanDomain }
                _activeListIds.value -= cleanDomain
                _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_add, cleanDomain))
            }
        }
    }

    fun deleteCustomDomain(domain: String) {
        if (!_profileSessionState.value.capabilities.canEditSettings) return
        val profileId = _profileSessionState.value.selected?.id ?: return
        val listSetting = currentListSetting ?: return
        if (!listSetting.allowsCustomInput) return

        val wasActive = _activeListIds.value.contains(domain)

        _availableItems.value = _availableItems.value.filter { it.id != domain }
        _activeListIds.value -= domain

        viewModelScope.launch {
            val success = apiRepository.removeCustomListItem(listSetting.apiPage, domain)
            if (!success) {
                if (_profileSessionState.value.selected?.id != profileId || currentListSetting != listSetting) {
                    return@launch
                }
                _availableItems.value = listOf(NextDnsResourceItem(id = domain, name = domain)) + _availableItems.value
                if (wasActive) _activeListIds.value += domain
                _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_delete, domain))
            }
        }
    }

}
