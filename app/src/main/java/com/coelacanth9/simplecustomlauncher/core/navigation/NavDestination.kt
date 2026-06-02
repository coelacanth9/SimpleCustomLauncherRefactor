package com.coelacanth9.simplecustomlauncher.core.navigation

/**
 * 画面遷移の目的地。
 * Navigation-Compose は使わず、HomeViewModel の mutableStateOf で管理する。
 * app の NavHost（MainActivity）が対応表を引いて画面を差し替える。
 */
sealed class NavDestination {
    /** ホーム画面 */
    object Home : NavDestination()

    /** ショートカット選択画面（新規配置・編集どちらも共通） */
    data class ShortcutSelect(val pageIndex: Int, val row: Int, val column: Int) : NavDestination()

    /** アプリ設定 */
    object Settings : NavDestination()

    /** アプリ内カレンダー */
    object Calendar : NavDestination()

    /** アプリ内メモ帳 */
    object Memo : NavDestination()

    /** すべてのアプリ一覧 */
    object AllApps : NavDestination()
}
