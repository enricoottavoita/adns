package com.eyalm.adns.data


import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Log.e
import com.eyalm.adns.R
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.network.NextDnsAnalytics
import com.eyalm.adns.data.network.NextDnsCreateProfileRequest
import com.eyalm.adns.data.network.NextDnsDeviceItem
import com.eyalm.adns.data.network.NextDnsDomainsResponse
import com.eyalm.adns.data.network.NextDnsErrorResponse
import com.eyalm.adns.data.network.NextDnsLoginRequest
import com.eyalm.adns.data.network.NextDnsLoginResponse
import com.eyalm.adns.data.network.NextDnsLogsResponse
import com.eyalm.adns.data.network.NextDnsProfile
import com.eyalm.adns.data.network.NextDnsStatsGraphResponse
import com.eyalm.adns.data.network.toHexId
import com.eyalm.adns.data.nextdns.api.NextDnsErrorParser
import com.eyalm.adns.data.nextdns.api.requestId
import com.eyalm.adns.data.nextdns.api.toEmptyApiResult
import com.eyalm.adns.data.nextdns.api.toJsonApiResult
import com.eyalm.adns.data.nextdns.api.toServerFailure
import com.eyalm.adns.data.nextdns.access.AccessEntry
import com.eyalm.adns.data.nextdns.access.AccessRole
import com.eyalm.adns.data.nextdns.access.InviteAccessRequest
import com.eyalm.adns.data.nextdns.access.UpdateAccessRoleRequest
import com.eyalm.adns.data.nextdns.rewrites.CreateRewriteRequest
import com.eyalm.adns.data.nextdns.rewrites.Rewrite
import com.eyalm.adns.data.nextdns.recreation.ParentalRecreationState
import com.eyalm.adns.data.nextdns.recreation.RecreationItemCollection
import com.eyalm.adns.data.nextdns.recreation.RecreationScheduleDto
import com.eyalm.adns.data.nextdns.recreation.UpdateRecreationItemRequest
import com.eyalm.adns.data.nextdns.recreation.UpdateRecreationScheduleRequest
import com.eyalm.adns.data.nextdns.logs.LogExportResult
import com.eyalm.adns.data.nextdns.profile.toDuplicateProfilePayload
import com.eyalm.adns.data.nextdns.settings.ApiBinding
import com.eyalm.adns.data.nextdns.settings.nestedPayload
import com.eyalm.adns.domain.nextdns.ApiResult
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.IOException
import retrofit2.Response

data class Blocklist(
    val id: String,
    val name: String?,
    val website: String?,
    val description: String?,
    val entries: Int,
    val updatedOn: String,
    val isEnabled: Boolean
)

sealed class LoginResult {
    object Success : LoginResult()
    object RequiresTwoFactor : LoginResult()
    data class Error(val message: String) : LoginResult()
}

class ApiRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("adns_settings", Context.MODE_PRIVATE)
    val repository = DnsRepository(context)
    private val keyManager = TokenManager.getInstance(context)


    suspend fun NextDnsLogin(email: String, password: String, code: String? = null): LoginResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val loginRequest = NextDnsLoginRequest(email, password, code)
            val response = ApiClient.nextDnsAuthApi.login(loginRequest)
            val responseText = response.body()?.string() ?: response.errorBody()?.string() ?: ""

            if (response.isSuccessful) {
                if (responseText.trim() == "OK") {
                    val cookiesList: List<String> = response.headers().values("Set-Cookie")

                    var fullCookieString = ""
                    for (cookieLine in cookiesList) {
                        val coreCookie = cookieLine.substringBefore(";")
                        fullCookieString += "$coreCookie; "
                    }

                    val apiKey = ApiClient.nextDnsAuthApi
                        .exchangeCookieForApiKey(fullCookieString.trim())
                        .body()
                        ?.key
                        ?: throw IllegalStateException("Failed to retrieve API key after login")
                    keyManager.saveApiKey(apiKey)
                    keyManager.saveEmail(email)
                    Log.d("ApiRepository", "Login successful, API key saved securely")
                    LoginResult.Success
                } else {
                    LoginResult.Error("Login failed")
                }
            } else {
                val nextDnsResponse = try {
                    Gson().fromJson(responseText, NextDnsLoginResponse::class.java)
                } catch (e: Exception) {
                    null
                }

                if (nextDnsResponse?.requiresCode == true) {
                    LoginResult.RequiresTwoFactor
                } else {
                    val errorMessage = if (responseText.isNotEmpty()) responseText else context.getString(R.string.login_failed, response.code())
                    e("ApiRepository", "Login Failed: $errorMessage")
                    LoginResult.Error(errorMessage)
                }
            }

        } catch (e: Exception) {
            e("ApiRepository", "Network Error during login", e)
            LoginResult.Error(e.message ?: context.getString(R.string.unknown_error))
        }
    }

    suspend fun NextDnsLoginWithApiKey(key: String): LoginResult = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.nextDnsAuthApi.verifyApiKey(key)
            if (!response.isSuccessful) {
                return@withContext LoginResult.Error("Invalid API Key")
            }
            keyManager.saveApiKey(key)
            keyManager.saveEmail(context.getString(R.string.api_key_account))
            LoginResult.Success
        } catch (e: Exception) {
            e("ApiRepository", "API Key verification failed", e)
            LoginResult.Error("Invalid API Key: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    fun getCurrentNextDnsProfileId(): String? {
        return sharedPrefs.getString("enhanced_url", null)?.let { url ->
            val cleanUrl = url.removeSuffix(".dns.nextdns.io")
            if (cleanUrl.contains("-")) {
                cleanUrl.substringAfterLast('-')
            } else {
                cleanUrl
            }
        }
    }

    suspend fun getNextDnsStats(): NextDnsAnalytics? {
        val profileId = requireSelectedProfileId()

        return try {
            ApiClient.nextDnsApi.getAnalytics(profileId, "-30d")
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching analytics", e)
            null
        }
    }

    suspend fun getNextDnsEmail(): String = withContext(Dispatchers.IO) {
        keyManager.getEmail() ?: ""
    }



    suspend fun getNextDnsProfiles(): List<NextDnsProfile> {
        return when (val result = getNextDnsProfilesResult()) {
            is ApiResult.Success -> result.value
            else -> emptyList()
        }
    }

    suspend fun getNextDnsProfilesResult(): ApiResult<List<NextDnsProfile>> = try {
        requireSignedIn()
        when (val result = ApiClient.nextDnsApi.getProfilesRaw().toJsonApiResult()) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return ApiResult.SerializationFailure(
                        IllegalStateException("Missing profiles data")
                    )
                ApiResult.Success(
                    gson.fromJson(data, Array<NextDnsProfile>::class.java).toList(),
                    result.status,
                )
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    } catch (error: IOException) {
        ApiResult.NetworkFailure(error)
    } catch (error: Exception) {
        ApiResult.SerializationFailure(error)
    }

    fun setNextDnsProfile(profile: NextDnsProfile, deviceName: String? = null) {
        val sanitizedName = deviceName?.replace(" ", "--")
        val url = if (sanitizedName.isNullOrEmpty()) {
            "${profile.id}.dns.nextdns.io"
        } else {
            "$sanitizedName-${profile.id}.dns.nextdns.io"
        }
        repository.setProvider(DnsProviders.NEXTDNS.id, url)
    }

    fun setNextDnsDeviceName(deviceName: String) {
        val profileId = getCurrentNextDnsProfileId() ?: return
        val sanitizedName = deviceName.trim().replace(" ", "--")
        val url = if (sanitizedName.isEmpty()) {
            "$profileId.dns.nextdns.io"
        } else {
            "$sanitizedName-$profileId.dns.nextdns.io"
        }
        repository.setProvider(DnsProviders.NEXTDNS.id, url)
    }

    fun getNextDnsDeviceName(): String {
        val url = sharedPrefs.getString("enhanced_url", "") ?: ""
        val cleanUrl = url.removeSuffix(".dns.nextdns.io")
        return if (cleanUrl.contains("-")) {
            cleanUrl.substringBeforeLast("-").replace("--", " ")
        } else {
            ""
        }
    }

    suspend fun createNextDnsProfile(name: String) {
        requireSignedIn()
        try {
            ApiClient.nextDnsApi.createProfile(NextDnsCreateProfileRequest.withName(name))
        } catch (e: Exception) {
            e("ApiRepository", "Error creating profile", e)
        }

    }



    suspend fun getNextDnsStatsGraph(period: String = "-30d"): NextDnsStatsGraphResponse {
        val profileId = requireSelectedProfileId()
        return try {
            val tz = java.util.TimeZone.getDefault().id
            ApiClient.nextDnsApi.getStatsGraph(profileId, period, "start", tz)
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching stats graph", e)
            throw e
        }
    }

    suspend fun getNextDnsDomains(status: String, period: String = "-30d", limit: Int = 10): NextDnsDomainsResponse {
        val profileId = requireSelectedProfileId()
        return try {
            ApiClient.nextDnsApi.getDomains(profileId, status, period, limit)
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching domains ($status)", e)
            throw e
        }
    }

    fun nextDnsLogOut() {
        keyManager.destroyApiKey()
        keyManager.destroyEmail()
        repository.setProvider(DnsProviders.ADGUARD.id)
    }

    fun isSignedIn(): Boolean = keyManager.hasToken()

    private fun requireSignedIn() {
        if (!isSignedIn()) {
            throw IllegalStateException("Not logged in")
        }
    }

    private fun requireSelectedProfileId(): String {
        requireSignedIn()
        val profileId = getCurrentNextDnsProfileId()
            ?: throw IllegalStateException("No profile selected")
        return profileId
    }

    suspend fun getScalarSettings(page: String): ApiResult<JsonObject> {
        val profileId = requireSelectedProfileId()
        return try {
            val response = ApiClient.nextDnsApi.getPageSettings(profileId, page)
            if (!response.isSuccessful) return response.toServerFailure()

            val root = response.body()
                ?: return ApiResult.SerializationFailure(
                    IllegalStateException("Missing $page settings response body")
                )
            val problems = NextDnsErrorParser.parse(root)
            if (problems.isNotEmpty()) {
                return ApiResult.ServerFailure(
                    status = response.code(),
                    problems = problems,
                    requestId = response.requestId(),
                )
            }

            val data = root.get("data")
            if (data == null || !data.isJsonObject) {
                ApiResult.SerializationFailure(
                    IllegalStateException("Missing data object in $page settings response")
                )
            } else {
                ApiResult.Success(data.asJsonObject, response.code())
            }

        } catch (error: IOException) {
            ApiResult.NetworkFailure(error)
        } catch (error: Exception) {
            e("ApiRepository", "Error fetching $page settings", error)
            ApiResult.SerializationFailure(error)
        }
    }

    suspend fun patchScalarSetting(
        binding: ApiBinding,
        encodedValue: Any,
    ): ApiResult<Unit> {
        val profileId = requireSelectedProfileId()
        return try {
            val response = ApiClient.nextDnsApi.patchPageSettings(
                profileId = profileId,
                page = binding.page,
                payload = nestedPayload(binding.path, encodedValue),
            )

            response.toEmptyApiResult()

        } catch (error: IOException) {
            ApiResult.NetworkFailure(error)
        } catch (error: Exception) {
            e("ApiRepository", "Error patching ${binding.page}", error)
            ApiResult.SerializationFailure(error)
        }
    }

    suspend fun getActiveListItems(page: String, feat: String): List<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val profileId = requireSelectedProfileId()
            val response = ApiClient.nextDnsApi.getActiveListItems(
                profileId, page, feat
            )
            val dataArray = response.getAsJsonArray("data")
            dataArray.map { it.asJsonObject.get("id").asString }
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching active $page/$feat", e)
            emptyList()
        }
    }

    suspend fun getAvailableCatalog(page: String, feat: String): JsonObject? {
        return try {
            requireSignedIn()
            ApiClient.nextDnsApi.getAvailableCatalog(page, feat)
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching catalog $page/$feat", e)
            null
        }
    }

    suspend fun addListItem(page: String, feat: String, itemId: String): Boolean {
        return try {
            val profileId = requireSelectedProfileId()
            val response = ApiClient.nextDnsApi.addListItem(
                profileId, page, feat, mapOf("id" to itemId)
            )
            response.isSuccessful
        } catch (e: Exception) {
            e("ApiRepository", "Error adding $itemId to $page/$feat", e)
            false
        }
    }

    suspend fun removeListItem(page: String, feat: String, itemId: String): Boolean {
        return try {
            val profileId = requireSelectedProfileId()
            val response = ApiClient.nextDnsApi.removeListItem(
                profileId, page, feat, itemId.toHexId()
            )
            response.isSuccessful
        } catch (e: Exception) {
            e("ApiRepository", "Error removing $itemId from $page/$feat", e)
            false
        }
    }

    suspend fun getCustomListItems(page: String): JsonArray? {
        return try {
            val profileId = requireSelectedProfileId()
            val response = ApiClient.nextDnsApi.getPageSettings(profileId, page)
            response.body()?.getAsJsonArray("data")
        } catch (e: Exception) { null }
    }

    sealed class AddListResult {
        object Success : AddListResult()
        object AlreadyAdded : AddListResult()
        data class Error(val code: String) : AddListResult()
    }

    suspend fun addCustomListItem(page: String, domain: String): AddListResult = withContext(Dispatchers.IO) {
        val profileId = requireSelectedProfileId()
        return@withContext try {
            val response = ApiClient.nextDnsApi.addCustomItem(profileId, page, mapOf("id" to domain))
            if (response.isSuccessful) {
                AddListResult.Success
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorResponse = try {
                    Gson().fromJson(errorBody, NextDnsErrorResponse::class.java)
                } catch (e: Exception) {
                    null
                }

                val isDuplicate = errorResponse?.errors?.any { it.code == "duplicate" } == true
                if (isDuplicate) {
                    AddListResult.AlreadyAdded
                } else {
                    AddListResult.Error(errorResponse?.errors?.firstOrNull()?.code ?: "unknown")
                }
            }
        } catch (e: Exception) {
            Log.e("ApiRepository", "Error adding $domain to $page", e)
            AddListResult.Error(e.localizedMessage ?: "network_error")
        }
    }

    suspend fun patchCustomListItem(page: String, domain: String, isActive: Boolean): Boolean {
        return try {
            val profileId = requireSelectedProfileId()
            val response = ApiClient.nextDnsApi.patchCustomItem(profileId, page, domain.toHexId(), mapOf("active" to isActive))
            response.isSuccessful
        } catch (e: Exception) { false }
    }

    suspend fun removeCustomListItem(page: String, domain: String): Boolean {
        return try {
            val profileId = requireSelectedProfileId()
            val response = ApiClient.nextDnsApi.removeCustomItem(profileId, page, domain.toHexId())
            response.isSuccessful
        } catch (e: Exception) { false }
    }

    suspend fun getAnalytics(feature: String, params: Map<String, String>): JsonArray? {
        return try {
            val profileId = requireSelectedProfileId()
            ApiClient.nextDnsApi.getAnalyticsFeature(profileId, feature, params)
                .getAsJsonArray("data")
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching analytics/$feature", e)
            null
        }
    }

    suspend fun getNextDnsLogs(
        cursor: String? = null,
        search: String? = null,
        blockedOnly: Boolean = false,
        rawLogs: Boolean = false,
        deviceId: String? = null
    ): NextDnsLogsResponse? = withContext(Dispatchers.IO) {
        return@withContext try {
            val profileId = requireSelectedProfileId()
            ApiClient.nextDnsApi.getLogs(
                profileId = profileId,
                cursor = cursor,
                search = search?.takeIf { it.isNotBlank() },
                status = if (blockedOnly) "blocked" else null,
                raw = if (rawLogs) 1 else null,
                device = deviceId
            )
        } catch (e: Exception) {
            Log.e("ApiRepository", "Failed to load NextDNS logs", e)
            null
        }
    }

    suspend fun getNextDnsDevices(): List<NextDnsDeviceItem> = withContext(Dispatchers.IO) {
        return@withContext try {
            val profileId = requireSelectedProfileId()
            ApiClient.nextDnsApi.getDevices(profileId).data
        } catch (e: Exception) {
            Log.e("ApiRepository", "Failed to load NextDNS devices", e)
            emptyList()
        }
    }

    private val gson = Gson()

    private suspend fun <T> profileCall( // TODO migrate other methods
        block: suspend (profileId: String) -> ApiResult<T>,
    ): ApiResult<T> = try {
            val profileId = requireSelectedProfileId()
            block(profileId)
        } catch (error: IOException) {
            ApiResult.NetworkFailure(error)
        } catch (error: Exception) {
            ApiResult.SerializationFailure(error)
        }

    private suspend fun <T> profileCall(
        profileId: String,
        block: suspend () -> ApiResult<T>,
    ): ApiResult<T> = try {
        requireSignedIn()
        block()
    } catch (error: IOException) {
        ApiResult.NetworkFailure(error)
    } catch (error: Exception) {
        ApiResult.SerializationFailure(error)
    }

    suspend fun clearLogs(profileId: String): ApiResult<Unit> = profileCall(profileId) {
        ApiClient.nextDnsApi.clearLogs(profileId).toEmptyApiResult()
    }

    suspend fun exportLogs(profileId: String, destination: Uri): LogExportResult = withContext(Dispatchers.IO) {
        val response = try {
            requireSignedIn()
            ApiClient.nextDnsApi.downloadLogs(profileId)
        } catch (error: IOException) {
            return@withContext LogExportResult.ApiFailure(ApiResult.NetworkFailure(error))
        } catch (error: Exception) {
            return@withContext LogExportResult.ApiFailure(ApiResult.SerializationFailure(error))
        }

        if (!response.isSuccessful) {
            return@withContext LogExportResult.ApiFailure(response.toServerFailure())
        }

        val body = response.body()
            ?: return@withContext LogExportResult.ApiFailure(
                ApiResult.SerializationFailure(IllegalStateException("Missing logs download body"))
            )

        try {
            body.byteStream().use { input ->
                context.contentResolver.openOutputStream(destination)?.use { output ->
                    input.copyTo(output)
                } ?: return@withContext LogExportResult.DestinationFailure(
                    IllegalStateException("Unable to open export destination")
                )
            }
            LogExportResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LogExportResult.DestinationFailure(error)
        }
    }

    suspend fun getProfileDetail(profileId: String): ApiResult<JsonObject> = profileCall(profileId) {
        when (val result = ApiClient.nextDnsApi.getProfileDetail(profileId).toJsonApiResult()) {
            is ApiResult.Success -> {
                try {
                    val data = result.value.getAsJsonObject("data")
                        ?: return@profileCall ApiResult.SerializationFailure(
                            IllegalStateException("Missing profile detail data")
                        )
                    ApiResult.Success(data, result.status)
                } catch (error: Exception) {
                    ApiResult.SerializationFailure(error)
                }
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun duplicateProfile(profileId: String, newName: String): ApiResult<NextDnsProfile> {
        val detail = getProfileDetail(profileId)
        if (detail !is ApiResult.Success) {
            return when (detail) {
                is ApiResult.ServerFailure -> detail
                is ApiResult.NetworkFailure -> detail
                is ApiResult.SerializationFailure -> detail
                is ApiResult.Success -> error("Handled above")
            }
        }

        return profileCall(profileId) {
            when (
                val result = ApiClient.nextDnsApi
                    .duplicateProfile(detail.value.toDuplicateProfilePayload(newName))
                    .toJsonApiResult()
            ) {
                is ApiResult.Success -> {
                    try {
                        val data = result.value.getAsJsonObject("data")
                            ?: return@profileCall ApiResult.SerializationFailure(
                                IllegalStateException("Missing duplicated profile data")
                            )
                        ApiResult.Success(gson.fromJson(data, NextDnsProfile::class.java), result.status)
                    } catch (error: Exception) {
                        ApiResult.SerializationFailure(error)
                    }
                }

                is ApiResult.ServerFailure -> result
                is ApiResult.NetworkFailure -> result
                is ApiResult.SerializationFailure -> result
            }
        }
    }

    suspend fun renameProfile(profileId: String, newName: String): ApiResult<Unit> = profileCall(profileId) {
        ApiClient.nextDnsApi.renameProfile(
            profileId,
            JsonObject().apply { addProperty("name", newName.trim()) },
        ).toEmptyApiResult()
    }

    suspend fun deleteOrLeaveProfile(profileId: String): ApiResult<Unit> = profileCall(profileId) {
        ApiClient.nextDnsApi.deleteOrLeaveProfile(profileId).toEmptyApiResult()
    }

    suspend fun getRewrites(): ApiResult<List<Rewrite>> = profileCall { profileId ->
        when (val result = ApiClient.nextDnsApi.getRewrites(profileId).toJsonApiResult()) {
            is ApiResult.Success -> {
                try {
                    val data = result.value.getAsJsonArray("data")
                        ?: return@profileCall ApiResult.SerializationFailure(
                            IllegalStateException("Missing rewrite data")
                        )

                    ApiResult.Success(
                        gson.fromJson(data, Array<Rewrite>::class.java).toList(),
                        result.status,
                    )
                } catch (error: Exception) {
                    ApiResult.SerializationFailure(error)
                }
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun deleteRewrite(rewriteId: String): ApiResult<Unit> = profileCall { profileId ->
        ApiClient.nextDnsApi
            .deleteRewrite(profileId, rewriteId)
            .toEmptyApiResult()
    }

    suspend fun createRewrite(name: String, content: String): ApiResult<Rewrite> = profileCall { profileId ->
        when (val result = ApiClient.nextDnsApi.createRewrite(profileId, CreateRewriteRequest(name, content)).toJsonApiResult()) {
            is ApiResult.Success -> {
                try {
                    val data = result.value.getAsJsonObject("data")
                    val rewrite = gson.fromJson(data, Rewrite::class.java)
                        ?: return@profileCall ApiResult.SerializationFailure(
                            IllegalStateException("Missing rewrite data")
                        )

                    ApiResult.Success(
                        rewrite,
                        result.status,
                    )
                } catch (error: Exception) {
                    ApiResult.SerializationFailure(error)
                }
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    fun clearSelectedNextDnsProfile() {
        sharedPrefs.edit().remove("enhanced_url").apply()
        if (repository.getSelectedProvider().id == DnsProviders.NEXTDNS.id) {
            repository.setProvider(DnsProviders.ADGUARD.id)
        }
    }

    suspend fun getAccess(): ApiResult<List<AccessEntry>> = profileCall { profileId ->
        when (val result = ApiClient.nextDnsApi.getAccess(profileId).toJsonApiResult()) {
            is ApiResult.Success -> {
                try {
                    val data = result.value.getAsJsonArray("data")
                        ?: return@profileCall ApiResult.SerializationFailure(
                            IllegalStateException("Missing access data")
                        )
                    ApiResult.Success(
                        gson.fromJson(data, Array<AccessEntry>::class.java).toList(),
                        result.status,
                    )
                } catch (error: Exception) {
                    ApiResult.SerializationFailure(error)
                }
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun inviteAccess(email: String, role: AccessRole): ApiResult<AccessEntry> = profileCall { profileId ->
        when (
            val result = ApiClient.nextDnsApi
                .inviteAccess(profileId, InviteAccessRequest(email, role))
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                try {
                    val data = result.value.getAsJsonObject("data")
                        ?: return@profileCall ApiResult.SerializationFailure(
                            IllegalStateException("Missing invited access entry")
                        )
                    val entry = gson.fromJson(data, AccessEntry::class.java)
                        ?: return@profileCall ApiResult.SerializationFailure(
                            IllegalStateException("Missing invited access entry")
                        )
                    ApiResult.Success(entry, result.status)
                } catch (error: Exception) {
                    ApiResult.SerializationFailure(error)
                }
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun updateAccessRole(email: String, role: AccessRole): ApiResult<Unit> = profileCall { profileId ->
        ApiClient.nextDnsApi
            .updateAccessRole(profileId, email, UpdateAccessRoleRequest(role))
            .toEmptyApiResult()
    }

    suspend fun deleteAccess(email: String): ApiResult<Unit> = profileCall { profileId ->
        ApiClient.nextDnsApi
            .deleteAccess(profileId, email)
            .toEmptyApiResult()
    }

    suspend fun getParentalRecreation(): ApiResult<ParentalRecreationState> = profileCall { profileId ->
        when (val result = ApiClient.nextDnsApi.getParentalControl(profileId).toJsonApiResult()) {
            is ApiResult.Success -> {
                try {
                    val data = result.value.getAsJsonObject("data")
                        ?: return@profileCall ApiResult.SerializationFailure(
                            IllegalStateException("Missing parental control data")
                        )
                    val state = gson.fromJson(data, ParentalRecreationState::class.java)
                        ?: return@profileCall ApiResult.SerializationFailure(
                            IllegalStateException("Missing parental recreation data")
                        )
                    ApiResult.Success(state, result.status)
                } catch (error: Exception) {
                    ApiResult.SerializationFailure(error)
                }
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun updateRecreationSchedule(schedule: RecreationScheduleDto): ApiResult<Unit> = profileCall { profileId ->
        ApiClient.nextDnsApi
            .updateRecreationSchedule(profileId, UpdateRecreationScheduleRequest(schedule))
            .toEmptyApiResult()
    }

    suspend fun updateRecreationItem(
        collection: RecreationItemCollection,
        itemId: String,
        recreation: Boolean,
    ): ApiResult<Unit> = profileCall { profileId ->
        ApiClient.nextDnsApi
            .updateRecreationItem(
                profileId = profileId,
                collection = collection.wireName,
                hexId = itemId.toHexId(),
                request = UpdateRecreationItemRequest(recreation),
            )
            .toEmptyApiResult()
    }

}
