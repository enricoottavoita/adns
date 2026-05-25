package com.eyalm.adns.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager

class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)

    private val aead: Aead by lazy {
        AeadConfig.register() // Registers the encryption algorithms with Tink

        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "tink_keyset", "tink_prefs_internal")
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri("android-keystore://secure_string_master_key")
            .build()
            .keysetHandle

        keysetHandle.getPrimitive(Aead::class.java)
    }

    fun saveApiKey(value: String) {
        val encryptedBytes = aead.encrypt(value.toByteArray(Charsets.UTF_8), null)
        val base64Encoded = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        prefs.edit { putString("api_key", base64Encoded) }
    }

    fun getApiKey(): String? {
        val base64Encoded = prefs.getString("api_key", null) ?: return null
        return try {
            val encryptedBytes = Base64.decode(base64Encoded, Base64.DEFAULT)
            val decryptedBytes = aead.decrypt(encryptedBytes, null)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun hasToken(): Boolean {
        return prefs.contains("api_key")
    }

    fun destroyApiKey() {
        prefs.edit { remove("api_key") }
    }

    fun saveEmail(value: String) {
        val encryptedBytes = aead.encrypt(value.toByteArray(Charsets.UTF_8), null)
        val base64Encoded = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        prefs.edit { putString("email", base64Encoded) }
    }

    fun getEmail(): String? {
        val base64Encoded = prefs.getString("email", null) ?: return null
        return try {
            val encryptedBytes = Base64.decode(base64Encoded, Base64.DEFAULT)
            val decryptedBytes = aead.decrypt(encryptedBytes, null)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun destroyEmail() {
        prefs.edit { remove("email") }
    }
}