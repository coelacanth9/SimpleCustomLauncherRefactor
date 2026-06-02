package com.coelacanth9.simplecustomlauncher.core.layout

import com.coelacanth9.simplecustomlauncher.core.shortcut.HomeLayoutConfig
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutItem
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutPlacement

/**
 * レイアウト全体の状態スナップショット。
 * ShortcutRepository が StateFlow として公開し、複数の ViewModel が購読する。
 *
 * shortcuts を含む理由: placements と shortcuts は常にセットで参照されるため分離すると
 * ViewModel 側で再結合が必要になる。core/shortcut.ShortcutItem への依存は
 * 同じ core 内なので依存方向として問題ない。
 */
data class LayoutState(
    val config: HomeLayoutConfig,
    val shortcuts: Map<String, ShortcutItem>,
    val placements: List<ShortcutPlacement>
)
