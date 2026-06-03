package com.coelacanth9.simplecustomlauncher.model

/** 楽天Link アプリのパッケージ名 */
const val RAKUTEN_LINK_PACKAGE = "jp.co.rakuten.mobile.rcs"

/** LinkSMS一覧ショートカットかどうか */
fun isLinkSmsList(shortcut: ShortcutItem): Boolean =
    shortcut.type == ShortcutType.SMS &&
        shortcut.packageName == RAKUTEN_LINK_PACKAGE &&
        shortcut.phoneNumber.isNullOrEmpty()

/** Link通話一覧ショートカットかどうか */
fun isLinkCallList(shortcut: ShortcutItem): Boolean =
    shortcut.type == ShortcutType.PHONE &&
        shortcut.packageName == RAKUTEN_LINK_PACKAGE &&
        shortcut.phoneNumber.isNullOrEmpty()

/** Linkダイアルパッドショートカットかどうか */
fun isLinkDialer(shortcut: ShortcutItem): Boolean =
    shortcut.type == ShortcutType.DIALER &&
        shortcut.packageName == RAKUTEN_LINK_PACKAGE

/** Link関連ショートカットかどうか（APP起動を除く） */
fun isLinkRelated(shortcut: ShortcutItem): Boolean =
    shortcut.packageName == RAKUTEN_LINK_PACKAGE &&
        shortcut.type != ShortcutType.APP
