package com.eyalm.adns.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.eyalm.adns.ProviderLoginActivity
import com.eyalm.adns.data.ApiRepository
import com.eyalm.adns.data.network.NextDnsProfile

class ProviderLoginViewModel(application: Application) : AndroidViewModel(application)  {

    private val apiRepository = ApiRepository(application)
    private val dnsRepository = com.eyalm.adns.data.DnsRepository(application)

    var currentStep by mutableStateOf(ProviderLoginActivity.Step.LOGIN)
        private set

    var profiles by mutableStateOf(listOf<NextDnsProfile>())
        private set

    fun nextStep() {

        currentStep = when (currentStep) {
            ProviderLoginActivity.Step.LOGIN -> ProviderLoginActivity.Step.LOADING
            ProviderLoginActivity.Step.SIGNUP -> ProviderLoginActivity.Step.LOADING
            ProviderLoginActivity.Step.LOADING -> ProviderLoginActivity.Step.PROFILE
            ProviderLoginActivity.Step.PROFILE -> ProviderLoginActivity.Step.SUCCESS
            ProviderLoginActivity.Step.SUCCESS -> ProviderLoginActivity.Step.LOGIN

        }
    }

    suspend fun ProviderLogin(email: String, password: String, providrId: String) {
        nextStep()
        if (providrId == "nextdns") {
            apiRepository.NextDnsLogin(email, password)
            Log.d("ProviderLoginViewModel", "Login attempt for provider $providrId with email $email")
            profiles = apiRepository.getNextDnsProfiles()

            nextStep()

        }
    }

    fun setProfile() {

    }



}