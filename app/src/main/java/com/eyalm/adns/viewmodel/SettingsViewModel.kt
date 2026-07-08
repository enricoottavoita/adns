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
import com.eyalm.adns.data.nextdns.model.nextDnsFaviconUrl
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceItem
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceSource
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceSpec
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceRepository
import com.eyalm.adns.data.nextdns.auth.NextDnsManagementSession
import com.eyalm.adns.data.nextdns.auth.NextDnsSessionManager
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.Locale

data class ScalarSettingsUiState(
    val page: String? = null,
    val profileId: String? = null,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loaded: Boolean = false,
    val values: Map<SettingId, JsonElement> = emptyMap(),
    val saving: Set<SettingId> = emptySet(),
    val pendingConfirmation: PendingSettingChange? = null,
    val refreshRevision: Long = 0,
)

data class ProfileSessionState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val profiles: List<NextDnsProfile> = emptyList(),
    /** The locally configured profile, available even before the profile list can be refreshed. */
    val selectedProfileId: String? = null,
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
        SETUP,
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
    private val resourceRepository = NextDnsResourceRepository()
    private val nextDnsSessionManager = NextDnsSessionManager.getInstance(application)

    private val _dnsUrl = MutableStateFlow(repository.getDnsUrl())
    val dnsUrl: StateFlow<String?> = _dnsUrl.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(repository.isNotificationEnabled())
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _profileSessionState = MutableStateFlow(
        ProfileSessionState(
            selectedProfileId = apiRepository.getCurrentNextDnsProfileId(),
        )
    )
    val profileSessionState = _profileSessionState.asStateFlow()
    private var profileLoadGeneration = 0L

    init {
        viewModelScope.launch {
            // Defer the first StateFlow emission until all ViewModel properties are initialized.
            yield()
            nextDnsSessionManager.state.collect { session ->
                if (session == NextDnsManagementSession.Expired) {
                    profileLoadGeneration++
                    invalidateProfileScopedState()
                    email = null
                    setPage(Page.MAIN)
                    val previous = _profileSessionState.value
                    _profileSessionState.value = previous.copy(
                        loading = false,
                        profiles = emptyList(),
                        selected = null,
                        capabilities = ProfileRole.Unknown.capabilities(),
                        error = ApiResult.ServerFailure(
                            status = 401,
                            problems = emptyList(),
                        ),
                    )
                }
            }
        }
    }

    fun refreshProfileSession(force: Boolean = false) {
        if (nextDnsSessionManager.state.value != NextDnsManagementSession.Active) return
        val generation = ++profileLoadGeneration
        val previous = _profileSessionState.value
        val selectedProfileId = apiRepository.getCurrentNextDnsProfileId()
        val selected = previous.selected?.takeIf { it.id == selectedProfileId }
        val profileChanged = previous.selectedProfileId != selectedProfileId
        if (profileChanged) {
            invalidateProfileScopedState()
            if (_page.value == Page.GENERIC_LIST) {
                setPage(Page.MAIN)
            }
        }
        _profileSessionState.value = previous.copy(
            loading = previous.profiles.isEmpty(),
            refreshing = force && previous.profiles.isNotEmpty(),
            selectedProfileId = selectedProfileId,
            selected = selected,
            capabilities = profileRoleFromWire(selected?.role).capabilities(),
            logsRevision = previous.logsRevision + if (profileChanged) 1 else 0,
            error = null,
        )
        viewModelScope.launch {
            val result = apiRepository.getNextDnsProfilesResult()
            if (generation != profileLoadGeneration) return@launch
            when (result) {
                is ApiResult.Success -> publishProfileSession(result.value)
                else -> {
                    _profileSessionState.value = _profileSessionState.value.copy(
                        loading = false,
                        refreshing = false,
                        error = result,
                    )
                }
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
        val locallySelectedId = apiRepository.getCurrentNextDnsProfileId()
        val selected = availableProfiles.firstOrNull { it.id == locallySelectedId }
        val selectedProfileId = selected?.id
        val profileChanged = previous.selectedProfileId != selectedProfileId
        if (profileChanged) {
            invalidateProfileScopedState()
            if (_page.value == Page.GENERIC_LIST) {
                setPage(Page.MAIN)
            }
        }
        _profileSessionState.value = ProfileSessionState(
            loading = false,
            refreshing = false,
            profiles = availableProfiles,
            selectedProfileId = selectedProfileId,
            selected = selected,
            capabilities = profileRoleFromWire(selected?.role).capabilities(),
            logsRevision = previous.logsRevision + if (profileChanged) 1 else 0,
        )
    }

    private fun invalidateProfileScopedState() {
        listLoadGeneration++
        scalarLoadGeneration++
        _scalarSettings.value = ScalarSettingsUiState()
        _activeListIds.value = emptySet()
        _availableItems.value = emptyList()
        _listLoading.value = false
        _listRefreshing.value = false
        _listError.value = null
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

    fun setPage(page: Page): Boolean {
        if (!requestPageAccess(page)) return false
        _page.value = page
        return true
    }

    private fun requestPageAccess(page: Page): Boolean = when (page) {
        Page.ACCOUNT_SETTINGS,
        Page.SETUP,
        Page.SECURITY,
        Page.PRIVACY,
        Page.PARENTAL_CONTROL,
        Page.SETTINGS_PAGE,
        Page.GENERIC_LIST,
        Page.LOGS,
        -> nextDnsSessionManager.requestFeatureAccess()

        Page.MAIN,
        Page.PROVIDERS,
        Page.LANGUAGE,
        -> true
    }



    var email by mutableStateOf<String?>(null)
    var nextDnsDeviceName by mutableStateOf(apiRepository.getNextDnsDeviceName())
        private set

    fun setProfile(profile: NextDnsProfile) {
        val currentName = apiRepository.getNextDnsDeviceName()
        val nameToSet = currentName.ifEmpty { "ADNS" }
        apiRepository.setNextDnsProfile(profile, nameToSet)
        nextDnsDeviceName = apiRepository.getNextDnsDeviceName()
        val knownProfiles = _profileSessionState.value.profiles
        val updatedProfiles = if (knownProfiles.any { it.id == profile.id }) {
            knownProfiles.map { if (it.id == profile.id) profile else it }
        } else {
            knownProfiles + profile
        }
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
        profileLoadGeneration++
        apiRepository.nextDnsLogOut()
        invalidateProfileScopedState()
        _profileSessionState.value = ProfileSessionState(
            loading = false,
            selectedProfileId = apiRepository.getCurrentNextDnsProfileId(),
        )
        setPage(Page.MAIN)
        refreshProvider()
    }

    private val _scalarSettings = MutableStateFlow(ScalarSettingsUiState())
    val scalarSettings: StateFlow<ScalarSettingsUiState> = _scalarSettings.asStateFlow()
    private var scalarLoadGeneration = 0L

    private val _listLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _listLoading.asStateFlow()
    private val _listRefreshing = MutableStateFlow(false)
    val listRefreshing: StateFlow<Boolean> = _listRefreshing.asStateFlow()
    private val _listError = MutableStateFlow<ApiResult<*>?>(null)
    val listError = _listError.asStateFlow()
    private var listLoadGeneration = 0L
    private val listItemsSaving = mutableSetOf<String>()


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

    fun loadScalarSettings(pageSpec: SettingsPageSpec, force: Boolean = false) {
        val profileId = _profileSessionState.value.selected?.id ?: return
        val current = _scalarSettings.value
        if (!force &&
            current.page == pageSpec.page &&
            current.profileId == profileId &&
            (current.loading || current.loaded)
        ) return
        val generation = ++scalarLoadGeneration

        _scalarSettings.value = if (
            force && current.page == pageSpec.page && current.profileId == profileId && current.loaded
        ) {
            current.copy(
                refreshing = true,
                refreshRevision = current.refreshRevision + 1,
            )
        } else {
            ScalarSettingsUiState(
                page = pageSpec.page,
                profileId = profileId,
                loading = true,
            )
        }
        viewModelScope.launch {
            when (val result = apiRepository.getScalarSettings(pageSpec.page)) {
                is ApiResult.Success -> {
                    if (
                        generation != scalarLoadGeneration ||
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
                        refreshRevision = current.refreshRevision + if (force) 1 else 0,
                    )
                }

                else -> {
                    if (
                        generation != scalarLoadGeneration ||
                        _profileSessionState.value.selected?.id != profileId ||
                        _scalarSettings.value.page != pageSpec.page ||
                        _scalarSettings.value.profileId != profileId
                    ) return@launch
                    val latest = _scalarSettings.value
                    _scalarSettings.value = if (latest.loaded) {
                        latest.copy(refreshing = false)
                    } else {
                        ScalarSettingsUiState(
                            page = pageSpec.page,
                            profileId = profileId,
                        )
                    }
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
        if (!requestPageAccess(Page.GENERIC_LIST)) return
        currentListSetting = listSetting

        listParentPage = when (listSetting.parentPage) {
            NextDnsResourceSpec.ParentPage.SECURITY -> Page.SECURITY
            NextDnsResourceSpec.ParentPage.PRIVACY -> Page.PRIVACY
            NextDnsResourceSpec.ParentPage.PARENTAL_CONTROL -> Page.PARENTAL_CONTROL
            else -> Page.MAIN
        }
        _activeListIds.value = emptySet()
        _availableItems.value = emptyList()
        _listError.value = null

        _page.value = Page.GENERIC_LIST
        loadListData(listSetting)
    }

    fun getListParentPage(): Page = listParentPage

    private fun loadListData(listSetting: NextDnsResourceSpec, force: Boolean = false) {
        val profileId = _profileSessionState.value.selected?.id ?: return
        val requestGeneration = ++listLoadGeneration
        viewModelScope.launch {
            try {
                if (force && _availableItems.value.isNotEmpty()) {
                    _listRefreshing.value = true
                } else {
                    _listLoading.value = true
                }
                val result: ApiResult<Pair<Set<String>, List<NextDnsResourceItem>>> =
                    if (listSetting.allowsCustomInput) {
                        when (
                            val custom = resourceRepository.getCustomList(
                                profileId,
                                listSetting.apiPage,
                            )
                        ) {
                            is ApiResult.Success -> ApiResult.Success(
                                custom.value.activeIds to custom.value.items,
                                custom.status,
                            )
                            is ApiResult.ServerFailure -> custom
                            is ApiResult.NetworkFailure -> custom
                            is ApiResult.SerializationFailure -> custom
                        }
                    } else {
                        when (
                            val active = resourceRepository.getActiveIds(
                                profileId,
                                listSetting.apiPage,
                                listSetting.apiFeature,
                            )
                        ) {
                            is ApiResult.Success -> {
                                val catalog: ApiResult<List<NextDnsResourceItem>> =
                                    when (listSetting.source) {
                                        NextDnsResourceSource.SERVER ->
                                            resourceRepository.getServerCatalog(
                                                listSetting.apiPage,
                                                listSetting.apiFeature,
                                            )

                                        NextDnsResourceSource.LOCALE -> ApiResult.Success(
                                            loadLocaleList(listSetting),
                                            200,
                                        )
                                    }
                                when (catalog) {
                                    is ApiResult.Success -> ApiResult.Success(
                                        active.value to catalog.value,
                                        catalog.status,
                                    )
                                    is ApiResult.ServerFailure -> catalog
                                    is ApiResult.NetworkFailure -> catalog
                                    is ApiResult.SerializationFailure -> catalog
                                }
                            }
                            is ApiResult.ServerFailure -> active
                            is ApiResult.NetworkFailure -> active
                            is ApiResult.SerializationFailure -> active
                        }
                    }

                if (!isCurrentListRequest(requestGeneration, profileId, listSetting)) return@launch
                when (result) {
                    is ApiResult.Success -> {
                        _activeListIds.value = result.value.first
                        _availableItems.value = result.value.second
                        _listError.value = null
                    }
                    else -> {
                        _listError.value = result
                        _errorMessage.emit(
                            getApplication<Application>().getString(
                                R.string.failed_to_load_list_data_check_your_network_connection_and_try_again_later
                            )
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                if (isCurrentListRequest(requestGeneration, profileId, listSetting)) {
                    _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_load_list_data_check_your_network_connection_and_try_again_later))
                }
            } finally {
                if (isCurrentListRequest(requestGeneration, profileId, listSetting)) {
                    _listLoading.value = false
                    _listRefreshing.value = false
                }
            }
        }
    }

    fun refreshCurrentList() {
        currentListSetting?.let { loadListData(it, force = true) }
    }

    private fun isCurrentListRequest(
        requestGeneration: Long,
        profileId: String,
        listSetting: NextDnsResourceSpec,
    ): Boolean =
        requestGeneration == listLoadGeneration &&
                _profileSessionState.value.selected?.id == profileId &&
                currentListSetting == listSetting


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
        if (_listLoading.value || _listRefreshing.value || !listItemsSaving.add(itemId)) return
        val isCurrentlyActive = _activeListIds.value.contains(itemId)
        val newState = !isCurrentlyActive

        if (newState) _activeListIds.value += itemId else _activeListIds.value -= itemId


        viewModelScope.launch {
            try {
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
            } finally {
                listItemsSaving -= itemId
            }
        }
    }

    fun addCustomDomain(domain: String) {
        if (!_profileSessionState.value.capabilities.canEditSettings) return
        val profileId = _profileSessionState.value.selected?.id ?: return
        val listSetting = currentListSetting ?: return
        if (!listSetting.allowsCustomInput) return

        val cleanDomain = domain.trim().lowercase(Locale.ROOT)
        val itemWasPresent = _availableItems.value.any { it.id == cleanDomain }
        val wasActive = cleanDomain in _activeListIds.value
        if (itemWasPresent && wasActive) return
        if (
            _listLoading.value ||
            _listRefreshing.value ||
            !listItemsSaving.add(cleanDomain)
        ) return

        val newItem = NextDnsResourceItem(
            id = cleanDomain,
            name = "*.$cleanDomain",
            icon = nextDnsFaviconUrl(cleanDomain)
                ?.let(ListIcon::Url)
                ?: ListIcon.None,
        )
        if (!itemWasPresent) {
            _availableItems.value = listOf(newItem) + _availableItems.value
        }
        _activeListIds.value += cleanDomain

        viewModelScope.launch {
            try {
                val addResult = if (itemWasPresent) {
                    ApiRepository.AddListResult.AlreadyAdded
                } else {
                    apiRepository.addCustomListItem(listSetting.apiPage, cleanDomain)
                }
                val success = when (addResult) {
                    ApiRepository.AddListResult.Success -> true
                    ApiRepository.AddListResult.AlreadyAdded ->
                        apiRepository.patchCustomListItem(
                            listSetting.apiPage,
                            cleanDomain,
                            isActive = true,
                        )
                    is ApiRepository.AddListResult.Error -> false
                }
                if (!success) {
                    if (_profileSessionState.value.selected?.id != profileId || currentListSetting != listSetting) {
                        return@launch
                    }
                    if (!itemWasPresent) {
                        _availableItems.value = _availableItems.value.filter { it.id != cleanDomain }
                    }
                    if (!wasActive) _activeListIds.value -= cleanDomain
                    _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_add, cleanDomain))
                }
            } finally {
                listItemsSaving -= cleanDomain
            }
        }
    }

    fun deleteCustomDomain(domain: String) {
        if (!_profileSessionState.value.capabilities.canEditSettings) return
        val profileId = _profileSessionState.value.selected?.id ?: return
        val listSetting = currentListSetting ?: return
        if (!listSetting.allowsCustomInput) return
        if (_listLoading.value || _listRefreshing.value || !listItemsSaving.add(domain)) return

        val wasActive = _activeListIds.value.contains(domain)

        _availableItems.value = _availableItems.value.filter { it.id != domain }
        _activeListIds.value -= domain

        viewModelScope.launch {
            try {
                val success = apiRepository.removeCustomListItem(listSetting.apiPage, domain)
                if (!success) {
                    if (_profileSessionState.value.selected?.id != profileId || currentListSetting != listSetting) {
                        return@launch
                    }
                    _availableItems.value = listOf(
                        NextDnsResourceItem(
                            id = domain,
                            name = "*.$domain",
                            icon = nextDnsFaviconUrl(domain)
                                ?.let(ListIcon::Url)
                                ?: ListIcon.None,
                        )
                    ) + _availableItems.value
                    if (wasActive) _activeListIds.value += domain
                    _errorMessage.emit(getApplication<Application>().getString(R.string.failed_to_delete, domain))
                }
            } finally {
                listItemsSaving -= domain
            }
        }
    }

}
