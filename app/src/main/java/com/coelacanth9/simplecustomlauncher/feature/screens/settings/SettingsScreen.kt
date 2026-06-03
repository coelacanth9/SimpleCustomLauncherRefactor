package com.coelacanth9.simplecustomlauncher.feature.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coelacanth9.simplecustomlauncher.data.ThemeMode

/**
 * アプリ設定画面（旧 AppSettingsScreen に相当）。
 * プレミアム・広告・購入フローは SettingsViewModel が管理する。
 * TODO: Phase6 で実装予定
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onEnterEditMode: () -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onWallpaperSettingChanged: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("SettingsScreen - TODO")
    }
}
