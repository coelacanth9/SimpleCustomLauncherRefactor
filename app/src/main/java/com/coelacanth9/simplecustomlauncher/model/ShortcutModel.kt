package com.coelacanth9.simplecustomlauncher.model

/**
 * ショートカットの種類
 */
enum class ShortcutType {
    APP,            // アプリ起動
    PHONE,          // 電話をかける
    SMS,            // SMSを送る
    DIALER,         // 電話アプリ（キーパッド画面で開く）
    INTENT,         // 外部アプリから受け取ったIntent（LINE等）
    CALENDAR,       // アプリ内カレンダー
    MEMO,           // アプリ内メモ帳
    SETTINGS,       // アプリ設定
    ALL_APPS,       // すべてのアプリ（アプリ一覧）
    DATE_DISPLAY,   // 日付表示
    TIME_DISPLAY,   // 時計表示
    DEVICE_SETTINGS, // 端末設定（Wi-Fi、Bluetooth、テザリング等）
    EMPTY           // 空きスロット
}

/**
 * ショートカットのデータ
 * NOTE: 起動用Intent生成（toIntent）はAndroid依存のため platform レイヤーで実装
 */
data class ShortcutItem(
    val id: String,
    val type: ShortcutType,
    val label: String,
    val packageName: String? = null,
    val intentUri: String? = null,
    val phoneNumber: String? = null,
    val iconUri: String? = null
)
