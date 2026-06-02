package com.coelacanth9.simplecustomlauncher.feature.screens.memo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * メモ帳画面。
 * TODO: Phase6 で実装予定
 */
@Composable
fun MemoScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("MemoScreen - TODO")
    }
}
