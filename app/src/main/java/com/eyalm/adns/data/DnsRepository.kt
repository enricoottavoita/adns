package com.eyalm.adns.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.TileService
import android.util.Log
import com.eyalm.adns.MainActivity
import com.eyalm.adns.R
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.services.AdnsTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DnsRepository(rawContext: Context) {
    private val context: Context = com.eyalm.adns.data.LocaleHelper.onAttach(rawContext)
    private val resolver = context.contentResolver
    private val sharedPrefs = context.getSharedPreferences("adns_settings", Context.MODE_PRIVATE)
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notificationManager = NotificationsManager(context)

    init {
        // DnsNotificationManager initializes the channel in its constructor
    }

    fun isAdBlockingActive(): Boolean {
        return isSelectedPrivateDnsActive(readPrivateDnsObservation(), getDnsUrl())
    }

    private fun observePrivateDns(): Flow<PrivateDnsObservation> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(readPrivateDnsObservation())
            }
        }

        resolver.registerContentObserver(Settings.Global.getUriFor(DnsConstants.MODE_KEY), false, observer)
        resolver.registerContentObserver(Settings.Global.getUriFor(DnsConstants.SPECIFIER_KEY), false, observer)

        trySend(readPrivateDnsObservation())

        awaitClose {
            resolver.unregisterContentObserver(observer)
        }

    }.distinctUntilChanged()

    fun getDnsStatusFlow(): Flow<Boolean> = combine(
        observePrivateDns(),
        getDnsUrlFlow(),
    ) { observation, selectedHostname ->
        isSelectedPrivateDnsActive(observation, selectedHostname)
    }.distinctUntilChanged().onEach { isActive ->
        if (!isActive) {
            saveStartTime(0L)
        } else if (getStartTime() == 0L) {
            saveStartTime(System.currentTimeMillis())
        }
        repositoryScope.launch {
            updateShortcuts()
            updateNotification()
        }
    }

    private fun readPrivateDnsObservation(): PrivateDnsObservation = try {
        val mode = Settings.Global.getString(resolver, DnsConstants.MODE_KEY)
        val hostname = Settings.Global.getString(resolver, DnsConstants.SPECIFIER_KEY)
        when (mode) {
            DnsConstants.MODE_HOSTNAME -> hostname
                ?.takeIf(String::isNotBlank)
                ?.let(PrivateDnsObservation::Hostname)
                ?: PrivateDnsObservation.Off

            DnsConstants.MODE_AUTOMATIC -> PrivateDnsObservation.Automatic
            else -> PrivateDnsObservation.Off
        }
    } catch (error: SecurityException) {
        Log.e("DnsRepository", "Permission denied checking DNS settings", error)
        PrivateDnsObservation.PermissionMissing
    }


    fun setAdBlockingState(enabled: Boolean): kotlinx.coroutines.Job {
        return repositoryScope.launch {
            try {
                val url = getDnsUrl() ?: throw IllegalStateException("No DNS URL configured")
                if (enabled) {
                    Settings.Global.putString(
                        resolver,
                        DnsConstants.SPECIFIER_KEY,
                        url
                    )
                    Settings.Global.putString(
                        resolver,
                        DnsConstants.MODE_KEY,
                        DnsConstants.MODE_HOSTNAME
                    )
                    saveStartTime(System.currentTimeMillis())
                } else {
                    Settings.Global.putString(resolver, DnsConstants.MODE_KEY, DnsConstants.MODE_OFF)
                    saveStartTime(0L)
                }
                updateNotification()
                updateShortcuts()
                // Notify the system that the tile state might have changed
                TileService.requestListeningState(context, ComponentName(context, AdnsTileService::class.java))
            } catch (e: SecurityException) {
                Log.e("DnsRepository", "Permission denied: app activated?")
            }
        }
    }

    fun setCustomUrl(url: String) {
        require(url.isNotBlank() && url.matches(Regex("""^[a-zA-Z0-9\-\.]+\.[a-zA-Z]{2,}$"""))) {
            context.getString(R.string.invalid_dns_hostname)
        }

        val matchedStandard = DnsProviders.getAllProviders
            .filterIsInstance<DnsProvider.Standard>()
            .find { it.hostname == url }

        val edit = sharedPrefs.edit()

        if (matchedStandard != null) {
            edit.putString("selected_provider_id", matchedStandard.id)
        } else {

            edit.putString("selected_provider_id", "custom")
            edit.putString("custom_url", url)
        }

        edit.apply()


        val isActive = isAdBlockingActive()
        if (isActive) {
            setAdBlockingState(true)
        }
    }


    fun getSelectedProvider(): DnsProvider {

        val savedId = sharedPrefs.getString("selected_provider_id", DnsProviders.ADGUARD.id) ?: DnsProviders.ADGUARD.id

        val provider = DnsProviders.getAllProviders.find { it.id == savedId }

        if (provider != null) return provider

        val customUrl = sharedPrefs.getString("custom_url", "") ?: ""
        return DnsProvider.Custom(customUrl)

    }

    fun setProvider(providerId: String, url: String? = null) {

        val isActive = isAdBlockingActive()

        val edit = sharedPrefs.edit()
        edit.putString("selected_provider_id", providerId)

        Log.d("DnsRepository", "Setting provider to $providerId")

        if (providerId == "custom") {
            require(!url.isNullOrBlank() && android.util.Patterns.DOMAIN_NAME.matcher(url).matches()) {
                context.getString(R.string.invalid_dns_hostname)
            }
            edit.putString("custom_url", url)
        } else if (providerId == "nextdns" && !url.isNullOrBlank()) {
            edit.putString("enhanced_url", url)
        }

        edit.apply()

        if (isActive) {
            val newUrl = getDnsUrl()
            if (newUrl != null) {
                setAdBlockingState(true)
            } else throw IllegalStateException("No DNS URL configured")
        }


    }


    fun getDnsUrl(): String? {
        val selectedProvider = getSelectedProvider()

        return when (selectedProvider) {
            is DnsProvider.Standard -> selectedProvider.hostname
            is DnsProvider.Custom -> selectedProvider.userUrl
            is DnsProvider.Enhanced -> sharedPrefs.getString("enhanced_url", null)
        }

    }

    fun getDnsUrlFlow(): Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "custom_url" || key == "selected_provider_id" || key == "enhanced_url") {
                val url = getDnsUrl()
                if (url != null) {
                    trySend(url)
                }
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        val url = getDnsUrl()
        if (url != null) {
            trySend(url)
        } else {
            throw IllegalStateException("No DNS URL configured")
        }
        awaitClose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun saveStartTime(time: Long) {
        sharedPrefs.edit().putLong("start_time", time).apply()
    }

    fun getStartTime(): Long {
        val startTime = sharedPrefs.getLong("start_time", 0L)
        if (isAdBlockingActive() && startTime == 0L) {
            val now = System.currentTimeMillis()
            saveStartTime(now)
            return now
        }

        return startTime
    }

    fun updateShortcuts() {
        val isActive = isAdBlockingActive()
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return

        val toggleShortcut = ShortcutInfo.Builder(context, "toggle_dns")
            .setShortLabel(if (isActive) context.getString(R.string.disable_blocker) else context.getString(R.string.enable_blocker))
            .setLongLabel(if (isActive) context.getString(R.string.disable_ad_blocker) else context.getString(R.string.enable_ad_blocker))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_monochrome))
            .setIntent(Intent(context, MainActivity::class.java).apply {
                action = "com.eyalm.adns.TOGGLE_ACTION"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
            .build()

        try {
            shortcutManager.dynamicShortcuts = listOf(toggleShortcut)
        } catch (e: Exception) {
            Log.e("DnsRepository", "Failed to update shortcuts")
        }
    }

    fun updateNotification() {
        notificationManager.updateNotification(isAdBlockingActive())
    }

    fun isNotificationEnabled(): Boolean {
        return notificationManager.isNotificationEnabled()
    }

    fun setNotificationEnabled(enabled: Boolean) {
        notificationManager.setNotificationEnabled(enabled, isAdBlockingActive())
    }
}
