package com.coelacanth9.simplecustomlauncher.platform.billing

import android.content.Context
import android.content.SharedPreferences
import com.coelacanth9.simplecustomlauncher.data.SettingsRepository
import com.coelacanth9.simplecustomlauncher.model.PremiumSource
import com.coelacanth9.simplecustomlauncher.model.PremiumStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface PremiumManager {
    val premiumStatusFlow: StateFlow<PremiumStatus>
    fun isPremiumActive(): Boolean
    fun getPremiumStatus(): PremiumStatus
    fun recordAdWatch()
    fun recordPurchase()
    fun clearPurchase()
    fun updateSubscriptionStatus(isActive: Boolean)
    fun getMaxAccessiblePageIndex(): Int
    fun setDebugPremium(enabled: Boolean)
    fun isDebugPremiumEnabled(): Boolean
    fun clearAllPremiumStatus()
}

class DefaultPremiumManager(
    context: Context,
    private val settingsRepository: SettingsRepository
) : PremiumManager {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _premiumStatusFlow = MutableStateFlow(getPremiumStatus())
    override val premiumStatusFlow: StateFlow<PremiumStatus> = _premiumStatusFlow

    override fun isPremiumActive(): Boolean = getPremiumStatus().isActive

    override fun getPremiumStatus(): PremiumStatus {
        val sources = mutableSetOf<PremiumSource>()
        var adWatchExpiresAt: Long? = null

        val debugPremium = prefs.getBoolean(KEY_DEBUG_PREMIUM, false)

        if (prefs.getBoolean(KEY_ONE_TIME_PURCHASE, false)) sources.add(PremiumSource.ONE_TIME_PURCHASE)
        if (prefs.getBoolean(KEY_SUBSCRIPTION_ACTIVE, false)) sources.add(PremiumSource.SUBSCRIPTION)

        val adExpiry = prefs.getLong(KEY_AD_WATCH_EXPIRY, 0)
        if (adExpiry > System.currentTimeMillis()) {
            sources.add(PremiumSource.AD_WATCH)
            adWatchExpiresAt = adExpiry
        }

        return PremiumStatus(
            isActive = sources.isNotEmpty() || debugPremium,
            activeSources = sources,
            adWatchExpiresAt = adWatchExpiresAt
        )
    }

    override fun recordAdWatch() {
        prefs.edit().putLong(KEY_AD_WATCH_EXPIRY, System.currentTimeMillis() + AD_WATCH_DURATION_MS).apply()
        _premiumStatusFlow.value = getPremiumStatus()
    }

    override fun recordPurchase() {
        prefs.edit().putBoolean(KEY_ONE_TIME_PURCHASE, true).apply()
        _premiumStatusFlow.value = getPremiumStatus()
    }

    override fun clearPurchase() {
        prefs.edit().putBoolean(KEY_ONE_TIME_PURCHASE, false).apply()
        _premiumStatusFlow.value = getPremiumStatus()
    }

    override fun updateSubscriptionStatus(isActive: Boolean) {
        prefs.edit().putBoolean(KEY_SUBSCRIPTION_ACTIVE, isActive).apply()
        _premiumStatusFlow.value = getPremiumStatus()
    }

    override fun getMaxAccessiblePageIndex(): Int =
        if (isPremiumActive()) settingsRepository.pageCount - 1 else 0

    override fun setDebugPremium(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEBUG_PREMIUM, enabled).apply()
        _premiumStatusFlow.value = getPremiumStatus()
    }

    override fun isDebugPremiumEnabled(): Boolean = prefs.getBoolean(KEY_DEBUG_PREMIUM, false)

    override fun clearAllPremiumStatus() {
        prefs.edit()
            .putBoolean(KEY_ONE_TIME_PURCHASE, false)
            .putBoolean(KEY_SUBSCRIPTION_ACTIVE, false)
            .putLong(KEY_AD_WATCH_EXPIRY, 0)
            .putBoolean(KEY_DEBUG_PREMIUM, false)
            .apply()
        _premiumStatusFlow.value = getPremiumStatus()
    }

    companion object {
        private const val PREFS_NAME = "premium_status"
        private const val KEY_ONE_TIME_PURCHASE = "one_time_purchase"
        private const val KEY_SUBSCRIPTION_ACTIVE = "subscription_active"
        private const val KEY_AD_WATCH_EXPIRY = "ad_watch_expiry"
        private const val KEY_DEBUG_PREMIUM = "debug_premium"
        private const val AD_WATCH_DURATION_MS = 24 * 60 * 60 * 1000L
    }
}
