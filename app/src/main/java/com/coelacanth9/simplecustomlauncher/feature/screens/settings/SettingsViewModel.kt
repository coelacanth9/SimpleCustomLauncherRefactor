package com.coelacanth9.simplecustomlauncher.feature.screens.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.coelacanth9.simplecustomlauncher.platform.BackupManager
import com.coelacanth9.simplecustomlauncher.platform.RestoreResult
import com.coelacanth9.simplecustomlauncher.data.SettingsRepository
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.PremiumStatus
import com.coelacanth9.simplecustomlauncher.platform.ads.AdManager
import com.coelacanth9.simplecustomlauncher.platform.billing.BillingManager
import com.coelacanth9.simplecustomlauncher.platform.billing.PremiumManager
import com.coelacanth9.simplecustomlauncher.usecase.ApplyDefaultLayoutUseCase
import kotlinx.coroutines.flow.StateFlow

/**
 * 設定画面の ViewModel。
 * プレミアム状態・広告・購入フロー・バックアップ・レイアウト操作を管理する。
 */
class SettingsViewModel(
    val settingsRepository: SettingsRepository,
    val premiumManager: PremiumManager,
    val shortcutRepository: ShortcutRepository,
    val applyDefaultLayoutUseCase: ApplyDefaultLayoutUseCase,
    val backupManager: BackupManager,
    val billingManager: BillingManager? = null,
    val adManager: AdManager? = null
) : ViewModel() {

    val premiumStatusFlow: StateFlow<PremiumStatus> get() = premiumManager.premiumStatusFlow

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

    // ===== レイアウト操作 =====

    fun resetToDefault() = applyDefaultLayoutUseCase.resetToDefault()

    fun clearLayout() = shortcutRepository.clearAllLayout()

    // ===== バックアップ / 復元 =====

    fun createBackupShareIntent(): Intent = backupManager.createShareIntent()

    fun restoreFromUri(uri: Uri): RestoreResult = backupManager.restoreFromUri(uri)

    // ===== デバッグ =====

    fun isDebugPremiumEnabled(): Boolean = premiumManager.isDebugPremiumEnabled()

    fun setDebugPremium(enabled: Boolean) = premiumManager.setDebugPremium(enabled)

    fun clearAllPremiumStatus() = premiumManager.clearAllPremiumStatus()
}
