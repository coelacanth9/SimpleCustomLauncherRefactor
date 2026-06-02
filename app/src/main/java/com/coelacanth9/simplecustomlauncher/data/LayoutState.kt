package com.coelacanth9.simplecustomlauncher.data

import com.coelacanth9.simplecustomlauncher.core.shortcut.HomeLayoutConfig
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutItem
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutPlacement

/**
 * レイアウト全体の状態スナップショット。
 * ShortcutRepository が StateFlow として公開し、複数の ViewModel が購読する。
 */
data class LayoutState(
    val config: HomeLayoutConfig,
    val shortcuts: Map<String, ShortcutItem>,
    val placements: List<ShortcutPlacement>
)
