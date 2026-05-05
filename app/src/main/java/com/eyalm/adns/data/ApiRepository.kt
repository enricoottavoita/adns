package com.eyalm.adns.data

import android.content.Context
import android.util.Log
import android.util.Log.e
import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.network.NextDnsLoginRequest
import com.eyalm.adns.data.network.NextDnsProfile

class ApiRepository(context: Context) {

    private val sharedPrefs = context.getSharedPreferences("adns_settings", Context.MODE_PRIVATE)

    suspend fun NextDnsLogin(email: String, password: String): Boolean {
        return try {
            val loginRequest = NextDnsLoginRequest(email, password)
            val response = ApiClient.nextDnsApi.login(loginRequest)

            if (response.isSuccessful) {

                val cookiesList: List<String> = response.headers().values("Set-Cookie")

                var fullCookieString = ""
                for (cookieLine in cookiesList) {
                    val coreCookie = cookieLine.substringBefore(";")
                    fullCookieString += "$coreCookie; "
                }

                sharedPrefs.edit()
                    .putString("nextdns_cookie", fullCookieString.trim())
                    .apply()

                Log.d("ApiRepository", "Login Success! Cookies saved: $fullCookieString")

                true
            } else {
                e("ApiRepository", "Login Failed: ${response.code()} - ${response.message()}")
                false
            }


        } catch (e: Exception) {
            Log.e("ApiRepository", "Network Error during login", e)

            false
        }

    }

    suspend fun getNextDnsProfiles(): List<NextDnsProfile> {
        val cookie = sharedPrefs.getString("nextdns_cookie", null)

        if (cookie.isNullOrEmpty()) {
            Log.e("ApiRepository", "No cookie found. User must login first.")
            return emptyList()
        }

        return try {
            val response = ApiClient.nextDnsApi.getProfiles(cookie)
            response.profiles
        } catch (e: Exception) {
            Log.e("ApiRepository", "Error fetching profiles", e)
            emptyList()
        }
    }



}