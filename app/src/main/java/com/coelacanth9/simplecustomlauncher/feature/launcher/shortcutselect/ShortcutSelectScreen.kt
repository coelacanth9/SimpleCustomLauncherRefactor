package com.coelacanth9.simplecustomlauncher.feature.launcher.shortcutselect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * ショートカット選択・配置画面。新規配置・スロット編集を統合。
 * 選択結果は ShortcutRepository に書き込み、HomeViewModel が StateFlow 経由で受信する。
 * TODO: Phase6 で実装予定
 */
@Composable
fun ShortcutSelectScreen(
    viewModel: ShortcutSelectViewModel,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("ShortcutSelectScreen - TODO")
    }
}
