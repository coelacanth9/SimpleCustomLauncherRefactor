package com.coelacanth9.simplecustomlauncher.feature.launcher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.coelacanth9.simplecustomlauncher.platform.ads.AdManager
import com.coelacanth9.simplecustomlauncher.platform.billing.BillingManager

class PremiumViewModelFactory(
    private val billingManager: BillingManager? = null,
    private val adManager: AdManager? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PremiumViewModel::class.java)) {
            return PremiumViewModel(billingManager, adManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
