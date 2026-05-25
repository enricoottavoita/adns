package com.eyalm.adns.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.ProviderLoginActivity
import com.eyalm.adns.data.ApiRepository
import com.eyalm.adns.data.network.NextDnsProfile
import kotlinx.coroutines.launch

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
            ProviderLoginActivity.Step.LOADING -> ProviderLoginActivity.Step.PROFILE
            ProviderLoginActivity.Step.PROFILE -> ProviderLoginActivity.Step.SUCCESS
            ProviderLoginActivity.Step.SUCCESS -> ProviderLoginActivity.Step.LOGIN

        }
    }

    suspend fun providerLogin(email: String, password: String, providrId: String) {
        currentStep = ProviderLoginActivity.Step.LOADING
        if (providrId == "nextdns") {
            val loginSuccess = apiRepository.NextDnsLogin(email, password)
            if (loginSuccess) {
                val profilesList = apiRepository.getNextDnsProfiles()
                profiles = profilesList
                Log.d(
                    "ProviderLoginViewModel",
                    "Login attempt for provider $providrId with email $email"
                )
                nextStep()
                return
            } else {
                Toast.makeText(getApplication(), "Login failed. Please check your credentials.", Toast.LENGTH_LONG).show()
            }
            currentStep = ProviderLoginActivity.Step.LOGIN
        }
    }

    fun setProfile(profile: NextDnsProfile) {
        apiRepository.setNextDnsProfile(profile)
        Log.d("ProviderLoginViewModel", "Profile set: ${profile.name} (${profile.id})")
        currentStep = ProviderLoginActivity.Step.SUCCESS
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            apiRepository.createNextDnsProfile(name)
            profiles = apiRepository.getNextDnsProfiles()
        }
    }

}