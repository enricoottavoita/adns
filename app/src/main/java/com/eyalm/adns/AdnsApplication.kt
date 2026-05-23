package com.eyalm.adns

import android.app.Application
import com.eyalm.adns.data.network.ApiClient

class AdnsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}
