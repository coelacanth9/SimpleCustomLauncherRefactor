package com.coelacanth9.simplecustomlauncher.feature.screens.allapps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.SharedFlow

/**
 * 全アプリ一覧画面。
 * TODO: Phase6 で実装予定
 */
@Composable
fun AllAppsScreen(
    onBack: () -> Unit,
    packageRemovedFlow: SharedFlow<String>? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("AllAppsScreen - TODO")
    }
}
