package com.coelacanth9.simplecustomlauncher.feature.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.coelacanth9.simplecustomlauncher.data.SettingsRepository
import com.coelacanth9.simplecustomlauncher.platform.ads.AdManager
import com.coelacanth9.simplecustomlauncher.platform.billing.BillingManager
import com.coelacanth9.simplecustomlauncher.platform.billing.DefaultPremiumManager

class SettingsViewModelFactory(
    private val context: Context,
    private val billingManager: BillingManager? = null,
    private val adManager: AdManager? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val settingsRepository = SettingsRepository(context)
            return SettingsViewModel(
                settingsRepository = settingsRepository,
                premiumManager = DefaultPremiumManager(context, settingsRepository),
                billingManager = billingManager,
                adManager = adManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
