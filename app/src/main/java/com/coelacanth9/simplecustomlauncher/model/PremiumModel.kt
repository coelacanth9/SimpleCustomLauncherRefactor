package com.coelacanth9.simplecustomlauncher.model

enum class PremiumSource {
    AD_WATCH,
    ONE_TIME_PURCHASE,
    SUBSCRIPTION
}

data class PremiumStatus(
    val isActive: Boolean,
    val activeSources: Set<PremiumSource>,
    val adWatchExpiresAt: Long? = null
)
