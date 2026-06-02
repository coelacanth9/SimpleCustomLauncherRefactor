package com.coelacanth9.simplecustomlauncher.core.navigation

/**
 * 画面遷移の目的地。
 * Navigation-Compose は使わず、HomeViewModel の mutableStateOf で管理する。
 */
sealed class NavDestination {
    /** ホーム画面 */
    object Home : NavDestination()

    /** ショートカット選択画面（配置先スロット情報を持つ） */
    data class ShortcutSelect(val pageIndex: Int, val row: Int, val column: Int) : NavDestination()

    /** スロット編集画面（スロット位置のみ。内容は LayoutState から引く） */
    data class SlotEdit(val pageIndex: Int, val row: Int, val column: Int) : NavDestination()

    /** アプリ内カレンダー */
    object Calendar : NavDestination()

    /** アプリ内メモ帳 */
    object Memo : NavDestination()

    /** アプリ設定 */
    object AppSettings : NavDestination()

    /** すべてのアプリ一覧 */
    object AllApps : NavDestination()
}
