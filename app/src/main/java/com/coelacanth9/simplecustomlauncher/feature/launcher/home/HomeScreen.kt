package com.coelacanth9.simplecustomlauncher.feature.launcher.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * ホーム画面。グリッドレイアウト・編集モード・ページ切替。
 * TODO: Phase6 で実装予定
 */
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    premiumViewModel: PremiumViewModel,
    snackbarHostState: SnackbarHostState
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("HomeScreen - TODO")
    }
}
