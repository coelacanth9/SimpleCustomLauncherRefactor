package com.coelacanth9.simplecustomlauncher.data

import androidx.annotation.StringRes
import com.coelacanth9.simplecustomlauncher.R
import com.coelacanth9.simplecustomlauncher.model.ShortcutType

data class ItemDef(
    val type: ShortcutType,
    @StringRes val labelResId: Int,
    val packageNames: List<String> = emptyList()
)

val itemMapping = mapOf(
    "電話"     to ItemDef(ShortcutType.DIALER,       R.string.shortcut_type_phone),
    "メモ帳"   to ItemDef(ShortcutType.MEMO,         R.string.shortcut_type_memo),
    "カレンダー" to ItemDef(ShortcutType.CALENDAR,   R.string.shortcut_type_calendar),
    "日付"     to ItemDef(ShortcutType.DATE_DISPLAY,  R.string.shortcut_type_date),
    "時刻"     to ItemDef(ShortcutType.TIME_DISPLAY,  R.string.shortcut_type_time),
    "ALL_APPS" to ItemDef(ShortcutType.ALL_APPS,      R.string.shortcut_type_all_apps),
    "フォト"   to ItemDef(ShortcutType.APP, R.string.app_photos,    listOf("com.google.android.apps.photos")),
    "Google"   to ItemDef(ShortcutType.APP, R.string.app_google,    listOf("com.google.android.googlequicksearchbox")),
    "連絡先"   to ItemDef(ShortcutType.APP, R.string.contact,       listOf("com.google.android.contacts", "com.android.contacts")),
    "カメラ"   to ItemDef(ShortcutType.APP, R.string.app_camera,    listOf("com.google.android.GoogleCamera", "com.android.camera", "com.android.camera2")),
    "LINE"     to ItemDef(ShortcutType.APP, R.string.app_line,      listOf("jp.naver.line.android")),
    "メッセージ" to ItemDef(ShortcutType.APP, R.string.app_messages, listOf("com.google.android.apps.messaging", "com.android.mms")),
    "Chrome"   to ItemDef(ShortcutType.APP, R.string.app_chrome,    listOf("com.android.chrome")),
    "YouTube"  to ItemDef(ShortcutType.APP, R.string.app_youtube,   listOf("com.google.android.youtube")),
    "Gmail"    to ItemDef(ShortcutType.APP, R.string.app_gmail,     listOf("com.google.android.gm")),
    "マップ"   to ItemDef(ShortcutType.APP, R.string.app_maps,      listOf("com.google.android.apps.maps")),
    "設定"     to ItemDef(ShortcutType.APP, R.string.settings,      listOf("com.android.settings")),
)

val defaultLayout = listOf(
    listOf("日付"),
    listOf("時刻"),
    listOf("電話", "メモ帳"),
    listOf("ALL_APPS", "カレンダー", "連絡先"),
    listOf("Google", "フォト"),
)
