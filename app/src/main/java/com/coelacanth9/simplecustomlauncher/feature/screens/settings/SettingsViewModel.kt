package com.coelacanth9.simplecustomlauncher.feature.screens.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.coelacanth9.simplecustomlauncher.data.SettingsRepository
import com.coelacanth9.simplecustomlauncher.platform.ads.AdManager
import com.coelacanth9.simplecustomlauncher.platform.billing.BillingManager
import com.coelacanth9.simplecustomlauncher.platform.billing.PremiumManager

/**
 * 設定画面の ViewModel。
 * プレミアム状態・広告・購入フローを管理する。
 * テーマ変更・壁紙設定・編集モード開始は MainActivity のラムダ経由で行う。
 * TODO: Phase6（SettingsScreen 実装時）に詳細実装予定
 */
class SettingsViewModel(
    val settingsRepository: SettingsRepository,
    val premiumManager: PremiumManager,
    val billingManager: BillingManager? = null,
    val adManager: AdManager? = null
) : ViewModel() {

    fun isPremiumActive(): Boolean = premiumManager.isPremiumActive()

    fun getFormattedPrice(): String? = billingManager?.productInfo?.value?.formattedPrice

    fun isAdReady(): Boolean = adManager?.isAdReady() == true

    fun launchPurchase(activity: Activity) {
        billingManager?.launchPurchaseFlow(activity)
    }

    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit) {
        adManager?.showRewardedAd(activity, onRewarded)
    }

    fun restorePurchases() {
        billingManager?.restorePurchases()
    }
}
