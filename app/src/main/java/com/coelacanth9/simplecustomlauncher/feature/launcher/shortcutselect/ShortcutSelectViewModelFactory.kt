package com.coelacanth9.simplecustomlauncher.feature.launcher.shortcutselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import com.coelacanth9.simplecustomlauncher.platform.billing.PremiumManager

class ShortcutSelectViewModelFactory(
    private val shortcutRepository: ShortcutRepository,
    private val shortcutHelper: ShortcutHelper,
    private val premiumManager: PremiumManager,
    private val targetPageIndex: Int,
    private val targetRow: Int,
    private val targetColumn: Int
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShortcutSelectViewModel::class.java)) {
            return ShortcutSelectViewModel(
                shortcutRepository = shortcutRepository,
                shortcutHelper = shortcutHelper,
                premiumManager = premiumManager,
                targetPageIndex = targetPageIndex,
                targetRow = targetRow,
                targetColumn = targetColumn
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
