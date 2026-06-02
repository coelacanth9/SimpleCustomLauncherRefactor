package com.coelacanth9.simplecustomlauncher.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
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
class PremiumViewModel(
    private val billingManager: BillingManager? = null,
    private val adManager: AdManager? = null
) : ViewModel() {

    // === 課金状態 ===

    val billingConnectionState: StateFlow<BillingConnectionState>?
        get() = billingManager?.connectionState

    val billingProductInfo: StateFlow<ProductInfo?>?
        get() = billingManager?.productInfo

    val billingPurchaseState: StateFlow<PurchaseState>?
        get() = billingManager?.purchaseState

    fun getFormattedPrice(): String? = billingManager?.productInfo?.value?.formattedPrice

    fun launchPurchase(activity: Activity) {
        billingManager?.launchPurchaseFlow(activity)
    }

    fun restorePurchases() {
        billingManager?.restorePurchases()
    }

    // === 広告状態 ===

    val adState: StateFlow<AdState>?
        get() = adManager?.adState

    fun isAdReady(): Boolean = adManager?.isAdReady() == true

    /**
     * リワード広告を表示する。
     * 報酬獲得時の処理（recordAdWatch 等）は呼び出し元（HomeViewModel 経由の UI）が行う。
     */
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit) {
        adManager?.showRewardedAd(activity, onRewarded)
    }

    fun reloadAd() {
        adManager?.loadRewardedAd()
    }
}
