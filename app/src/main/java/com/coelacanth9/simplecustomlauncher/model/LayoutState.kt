package com.coelacanth9.simplecustomlauncher.model

/**
 * レイアウト全体の状態スナップショット。
 * ShortcutRepository が StateFlow として公開し、複数の ViewModel が購読する。
 *
 * shortcuts を含む理由: placements と shortcuts は常にセットで参照されるため分離すると
 * ViewModel 側で再結合が必要になる。
 */
data class LayoutState(
    val config: HomeLayoutConfig,
    val shortcuts: Map<String, ShortcutItem>,
    val placements: List<ShortcutPlacement>
)
