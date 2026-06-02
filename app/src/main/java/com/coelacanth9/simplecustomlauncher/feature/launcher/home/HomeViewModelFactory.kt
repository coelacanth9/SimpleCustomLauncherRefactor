package com.coelacanth9.simplecustomlauncher.feature.launcher.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.coelacanth9.simplecustomlauncher.data.SettingsRepository
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.platform.CalendarRepository
import com.coelacanth9.simplecustomlauncher.platform.ads.AdManager
import com.coelacanth9.simplecustomlauncher.platform.billing.BillingManager
import com.coelacanth9.simplecustomlauncher.platform.billing.DefaultPremiumManager

class HomeViewModelFactory(
    private val context: Context,
    private val shortcutRepository: ShortcutRepository,
    private val billingManager: BillingManager? = null,
    private val adManager: AdManager? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val settingsRepository = SettingsRepository(context)
            return HomeViewModel(
                shortcutRepository = shortcutRepository,
                settingsRepository = settingsRepository,
                calendarRepository = CalendarRepository(context),
                premiumManager = DefaultPremiumManager(context, settingsRepository),
                billingManager = billingManager,
                adManager = adManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
