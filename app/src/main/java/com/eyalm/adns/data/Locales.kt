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

    fun getPlainString(
        path: Array<out String>,
        values: Map<String, String> = emptyMap(),
    ): String {
        val input = getString(*path)
        val builder = StringBuilder()
        var i = 0
        while (i < input.length) {
            val char = input[i]
            when {
                char == '{' && i + 1 < input.length && input[i + 1] == '{' -> {
                    val end = input.indexOf("}}", i + 2)
                    if (end != -1) {
                        val key = input.substring(i + 2, end)
                        builder.append(values[key].orEmpty())
                        i = end + 2
                    } else {
                        builder.append(char)
                        i++
                    }
                }
                char == '<' -> {
                    var j = i + 1
                    if (j < input.length && input[j] == '/') j++
                    val startDigits = j
                    while (j < input.length && input[j].isDigit()) j++
                    if (j > startDigits && j < input.length && input[j] == '>') {
                        i = j + 1
                    } else {
                        builder.append(char)
                        i++
                    }
                }
                else -> {
                    builder.append(char)
                    i++
                }
            }
        }
        return builder.toString()
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
