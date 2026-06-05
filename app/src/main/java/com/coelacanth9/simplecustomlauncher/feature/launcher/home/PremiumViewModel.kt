package com.coelacanth9.simplecustomlauncher.feature.launcher.home

import android.app.Activity
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.coelacanth9.simplecustomlauncher.platform.ads.AdManager
import com.coelacanth9.simplecustomlauncher.platform.ads.AdState
import com.coelacanth9.simplecustomlauncher.platform.billing.BillingConnectionState
import com.coelacanth9.simplecustomlauncher.platform.billing.BillingManager
import com.coelacanth9.simplecustomlauncher.platform.billing.ProductInfo
import com.coelacanth9.simplecustomlauncher.platform.billing.PurchaseState
import kotlinx.coroutines.flow.StateFlow

/**
 * 課金・広告の状態を管理する ViewModel。
 * ビジネスロジック（プレミアム判定、ページ追加等）は HomeViewModel が持つ。
 */
@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val adManager: AdManager
) : ViewModel() {

    // ===== 課金状態 =====

    val billingConnectionState: StateFlow<BillingConnectionState>
        get() = billingManager.connectionState

    val billingProductInfo: StateFlow<ProductInfo?>
        get() = billingManager.productInfo

    val billingPurchaseState: StateFlow<PurchaseState>
        get() = billingManager.purchaseState

    fun getFormattedPrice(): String? = billingManager.productInfo.value?.formattedPrice

    fun launchPurchase(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    // ===== 広告状態 =====

    val adState: StateFlow<AdState>
        get() = adManager.adState

    fun isAdReady(): Boolean = adManager.isAdReady()

    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit) {
        adManager.showRewardedAd(activity, onRewarded)
    }

    fun reloadAd() {
        adManager.loadRewardedAd()
    }
}
