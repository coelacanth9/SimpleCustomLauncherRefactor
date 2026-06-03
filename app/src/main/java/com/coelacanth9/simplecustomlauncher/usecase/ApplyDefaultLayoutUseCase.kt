package com.coelacanth9.simplecustomlauncher.usecase

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import com.coelacanth9.simplecustomlauncher.R
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.HomeLayoutConfig
import com.coelacanth9.simplecustomlauncher.model.RowConfig
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.ShortcutPlacement
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import java.util.UUID

// ===== デフォルトレイアウト定義 =====

data class ItemDef(
    val type: ShortcutType,
    @StringRes val labelResId: Int,
    val packageNames: List<String> = emptyList()
)

val itemMapping = mapOf(
    "電話"       to ItemDef(ShortcutType.DIALER,       R.string.shortcut_type_phone),
    "メモ帳"     to ItemDef(ShortcutType.MEMO,         R.string.shortcut_type_memo),
    "カレンダー" to ItemDef(ShortcutType.CALENDAR,     R.string.shortcut_type_calendar),
    "日付"       to ItemDef(ShortcutType.DATE_DISPLAY,  R.string.shortcut_type_date),
    "時刻"       to ItemDef(ShortcutType.TIME_DISPLAY,  R.string.shortcut_type_time),
    "ALL_APPS"   to ItemDef(ShortcutType.ALL_APPS,      R.string.shortcut_type_all_apps),
    "フォト"     to ItemDef(ShortcutType.APP, R.string.app_photos,    listOf("com.google.android.apps.photos")),
    "Google"     to ItemDef(ShortcutType.APP, R.string.app_google,    listOf("com.google.android.googlequicksearchbox")),
    "連絡先"     to ItemDef(ShortcutType.APP, R.string.contact,       listOf("com.google.android.contacts", "com.android.contacts")),
    "カメラ"     to ItemDef(ShortcutType.APP, R.string.app_camera,    listOf("com.google.android.GoogleCamera", "com.android.camera", "com.android.camera2")),
    "LINE"       to ItemDef(ShortcutType.APP, R.string.app_line,      listOf("jp.naver.line.android")),
    "メッセージ" to ItemDef(ShortcutType.APP, R.string.app_messages,  listOf("com.google.android.apps.messaging", "com.android.mms")),
    "Chrome"     to ItemDef(ShortcutType.APP, R.string.app_chrome,    listOf("com.android.chrome")),
    "YouTube"    to ItemDef(ShortcutType.APP, R.string.app_youtube,   listOf("com.google.android.youtube")),
    "Gmail"      to ItemDef(ShortcutType.APP, R.string.app_gmail,     listOf("com.google.android.gm")),
    "マップ"     to ItemDef(ShortcutType.APP, R.string.app_maps,      listOf("com.google.android.apps.maps")),
    "設定"       to ItemDef(ShortcutType.APP, R.string.settings,      listOf("com.android.settings")),
)

val defaultLayout = listOf(
    listOf("日付"),
    listOf("時刻"),
    listOf("電話", "メモ帳"),
    listOf("ALL_APPS", "カレンダー", "連絡先"),
    listOf("Google", "フォト"),
)

// ===== UseCase =====

class ApplyDefaultLayoutUseCase(
    private val repository: ShortcutRepository,
    private val context: Context
) {

    fun applyDefaultLayout() {
        val rows = defaultLayout.mapIndexed { index, row ->
            RowConfig(rowIndex = index, columns = row.size, fixedHeightDp = getFixedHeightForRow(row))
        }
        repository.update {
            // ストレージをクリアしてから再構築
            clearAllLayout()
            saveLayoutConfig(HomeLayoutConfig(rows))
            defaultLayout.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { colIndex, itemName ->
                    val itemDef = itemMapping[itemName]
                    if (itemDef != null) {
                        val shortcutItem = createShortcutFromDef(itemDef)
                        if (shortcutItem != null) {
                            saveShortcut(shortcutItem)
                            savePlacement(ShortcutPlacement(shortcutId = shortcutItem.id, row = rowIndex, column = colIndex))
                        }
                    }
                }
            }
        }
        repository.markAsInitialized()
    }

    fun resetToDefault() = applyDefaultLayout()

    private fun getFixedHeightForRow(row: List<String>): Int? {
        val hasDateDisplay = row.any { itemMapping[it]?.type == ShortcutType.DATE_DISPLAY }
        val hasTimeDisplay = row.any { itemMapping[it]?.type == ShortcutType.TIME_DISPLAY }
        return when {
            hasTimeDisplay -> 80
            hasDateDisplay -> 56
            else -> null
        }
    }

    private fun createShortcutFromDef(def: ItemDef): ShortcutItem? {
        val label = context.getString(def.labelResId)
        return when (def.type) {
            ShortcutType.APP -> {
                val installedPackage = def.packageNames.firstOrNull { isAppInstalled(it) }
                if (installedPackage != null)
                    ShortcutItem(id = UUID.randomUUID().toString(), type = ShortcutType.APP, label = label, packageName = installedPackage)
                else null
            }
            else -> ShortcutItem(id = UUID.randomUUID().toString(), type = def.type, label = label)
        }
    }

    private fun isAppInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) { false }
}
