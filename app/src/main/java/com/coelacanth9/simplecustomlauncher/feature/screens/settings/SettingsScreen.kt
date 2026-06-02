package com.coelacanth9.simplecustomlauncher.feature.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coelacanth9.simplecustomlauncher.data.ThemeMode
import com.coelacanth9.simplecustomlauncher.platform.ads.AdManager
import com.coelacanth9.simplecustomlauncher.platform.billing.BillingManager

/**
 * アプリ設定画面（旧 AppSettingsScreen に相当）。
 * プレミアム・広告・購入フローは内部の SettingsViewModel が管理する。
 * TODO: Phase6 で実装予定
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEnterEditMode: () -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onWallpaperSettingChanged: (Boolean) -> Unit,
    billingManager: BillingManager? = null,
    adManager: AdManager? = null
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context, billingManager, adManager)
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("SettingsScreen - TODO")
    }
}
