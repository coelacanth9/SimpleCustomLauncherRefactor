package com.coelacanth9.simplecustomlauncher.core.shortcut

/**
 * アプリ内機能タイプかどうか。
 * いつでもアプリ内から再選択できるため、未配置リストには表示しない。
 */
fun isInternal(type: ShortcutType): Boolean = type in setOf(
    ShortcutType.CALENDAR,
    ShortcutType.MEMO,
    ShortcutType.DIALER,
    ShortcutType.ALL_APPS,
    ShortcutType.DATE_DISPLAY,
    ShortcutType.TIME_DISPLAY,
    ShortcutType.DEVICE_SETTINGS
)

/**
 * 配置解除時にショートカット自体を削除するかどうか。
 * - 内部機能・APP・SETTINGS: 再選択が容易なため削除
 * - Link系一覧: 再配置不可のため削除
 * - INTENT / PHONE / SMS（連絡先）: 再作成に手間がかかるため一時保管に残す
 */
fun shouldDeleteOnRemove(shortcut: ShortcutItem): Boolean =
    shortcut.type in setOf(
        ShortcutType.CALENDAR,
        ShortcutType.MEMO,
        ShortcutType.DIALER,
        ShortcutType.ALL_APPS,
        ShortcutType.DATE_DISPLAY,
        ShortcutType.TIME_DISPLAY,
        ShortcutType.SETTINGS,
        ShortcutType.DEVICE_SETTINGS,
        ShortcutType.APP
    ) || isLinkSmsList(shortcut) || isLinkCallList(shortcut) || isLinkDialer(shortcut)
