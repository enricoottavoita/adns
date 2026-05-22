package com.eyalm.adns.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Locales {
    private var data: Map<String, Any> = emptyMap()


    fun init(context: Context) {
        if (data.isNotEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = context.assets.open("locales/nextdns/en.json")
                    .bufferedReader()
                    .use { it.readText() }
                val type = object : TypeToken<Map<String, Any>>() {}.type
                data = Gson().fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Locales", "Failed to load locales: ${e.message}")
            }
        }
    }


    // Locales.getString("security", "feeds", "name")
    // "Threat Intelligence Feeds"
    fun getString(vararg path: String): String {
        var current: Any? = data
        for (key in path) {
            if (current is Map<*, *>) {
                current = current[key]
            } else {
                return ""
            }
        }
        return (current as? String) ?: ""
    }


    // for getting lists of items from merged.json, for example, native tracker systems
    @Suppress("UNCHECKED_CAST")
    fun getMap(vararg path: String): Map<String, Any>? {
        var current: Any? = data
        for (key in path) {
            if (current is Map<*, *>) {
                current = current[key]
            } else {
                return null
            }
        }
        return current as? Map<String, Any>
    }
}