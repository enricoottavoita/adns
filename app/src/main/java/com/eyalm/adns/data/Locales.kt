package com.eyalm.adns.data

import android.content.Context
import com.eyalm.adns.BuildConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Locales {
    private var data: Map<String, Any> = emptyMap()

    @Synchronized
    fun init(context: Context) {
        if (data.isNotEmpty()) return

        data = context.assets
            .open("locales/nextdns/en.json")
            .bufferedReader()
            .use { reader ->
                Gson().fromJson(
                    reader,
                    object : TypeToken<Map<String, Any>>() {}.type
                )
            }
    }


    private fun missing(path: Array<out String>): String =
        if (BuildConfig.DEBUG) {
            "[missing:${path.joinToString(".")}]"
        } else {
            path.lastOrNull().orEmpty()
        }

    // Locales.getString("security", "feeds", "name")
    // "Threat Intelligence Feeds"
    fun getString(vararg path: String): String {
        var current: Any? = data
        for (key in path) {
            if (current is Map<*, *>) {
                current = current[key]
            } else {
                return missing(path)
            }
        }
        return (current as? String) ?: missing(path)
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