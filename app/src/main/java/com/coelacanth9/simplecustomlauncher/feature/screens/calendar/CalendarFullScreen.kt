package com.coelacanth9.simplecustomlauncher.feature.screens.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * カレンダー全画面表示。
 * TODO: Phase6 で実装予定
 */
@Composable
fun CalendarFullScreen(
    hasPermission: Boolean,
    holidayMap: Map<Int, String>,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("CalendarFullScreen - TODO")
    }
}
