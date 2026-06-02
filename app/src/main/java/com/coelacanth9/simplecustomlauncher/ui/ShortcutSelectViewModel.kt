package com.coelacanth9.simplecustomlauncher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.coelacanth9.simplecustomlauncher.core.shortcut.HomeLayoutConfig
import com.coelacanth9.simplecustomlauncher.core.shortcut.RowConfig
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutItem
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutPlacement
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutType
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.platform.AppInfo
import com.coelacanth9.simplecustomlauncher.platform.ShortcutData
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import java.util.UUID

/**
 * ショートカット選択画面の ViewModel。
 * 選択結果は ShortcutRepository に書き込み、StateFlow 経由で HomeViewModel に反映される。
 * （ViewModel 間の直接参照・コールバックは持たない）
 */
class ShortcutSelectViewModel(
    private val shortcutRepository: ShortcutRepository,
    private val shortcutHelper: ShortcutHelper,
    val targetPageIndex: Int,
    val targetRow: Int,
    val targetColumn: Int
) : ViewModel() {

    // === UI 状態 ===

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var shortcuts by mutableStateOf<List<ShortcutData>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    // === 初期化 ===

    fun loadApps() {
        isLoading = true
        apps = shortcutHelper.getInstalledApps()
        isLoading = false
    }

    fun loadShortcutsForApp(packageName: String) {
        shortcuts = shortcutHelper.getShortcutsForApp(packageName)
    }

    // === 配置操作（呼ぶと StateFlow 経由で HomeViewModel に自動反映）===

    fun placeApp(packageName: String, label: String) {
        val allShortcuts = shortcutRepository.layoutState.value.shortcuts
        val existing = allShortcuts.values.find {
            it.type == ShortcutType.APP && it.packageName == packageName
        }
        val item = if (existing != null && existing.label != label) {
            existing.copy(label = label)
        } else {
            existing ?: ShortcutItem(
                id = UUID.randomUUID().toString(),
                type = ShortcutType.APP,
                label = label,
                packageName = packageName
            )
        }
        shortcutRepository.saveShortcut(item)
        placeItem(item)
    }

    fun placeInternalFeature(type: ShortcutType, label: String) {
        val allShortcuts = shortcutRepository.layoutState.value.shortcuts
        val existing = allShortcuts.values.find { it.type == type && it.packageName == null }
        val item = existing ?: ShortcutItem(
            id = UUID.randomUUID().toString(),
            type = type,
            label = label
        )
        if (existing == null) shortcutRepository.saveShortcut(item)
        placeItem(item)
    }

    fun placeDeviceSettings(label: String, settingsAction: String) {
        val allShortcuts = shortcutRepository.layoutState.value.shortcuts
        val existing = allShortcuts.values.find {
            it.type == ShortcutType.DEVICE_SETTINGS && it.intentUri == settingsAction
        }
        val item = existing ?: ShortcutItem(
            id = UUID.randomUUID().toString(),
            type = ShortcutType.DEVICE_SETTINGS,
            label = label,
            intentUri = settingsAction
        )
        if (existing == null) shortcutRepository.saveShortcut(item)
        placeItem(item)
    }

    fun placeContact(
        name: String,
        phoneNumber: String,
        type: ShortcutType,
        targetPackage: String? = null
    ) {
        val allShortcuts = shortcutRepository.layoutState.value.shortcuts
        val existing = allShortcuts.values.find {
            it.type == type && it.phoneNumber == phoneNumber && it.packageName == targetPackage
        }
        val item = if (existing != null && existing.label != name) {
            existing.copy(label = name)
        } else {
            existing ?: ShortcutItem(
                id = UUID.randomUUID().toString(),
                type = type,
                label = name,
                phoneNumber = phoneNumber,
                packageName = targetPackage
            )
        }
        shortcutRepository.saveShortcut(item)
        placeItem(item)
    }

    fun placeIntent(shortLabel: String, packageName: String, shortcutId: String) {
        val allShortcuts = shortcutRepository.layoutState.value.shortcuts
        val existing = allShortcuts.values.find {
            it.type == ShortcutType.INTENT && it.packageName == packageName &&
                shortcutRepository.getPinShortcutInfo(it.id).first == shortcutId
        }
        val item = if (existing != null && existing.label != shortLabel) {
            existing.copy(label = shortLabel)
        } else {
            existing ?: ShortcutItem(
                id = UUID.randomUUID().toString(),
                type = ShortcutType.INTENT,
                label = shortLabel,
                packageName = packageName
            )
        }
        shortcutRepository.saveShortcut(item)
        shortcutRepository.savePinShortcutInfo(item.id, shortcutId, packageName)
        placeItem(item)
    }

    /**
     * 配置済みショートカットをこのスロットと入れ替える。
     */
    fun swapShortcuts(targetShortcut: ShortcutItem) {
        val state = shortcutRepository.layoutState.value
        val currentShortcutId = state.placements
            .find { it.pageIndex == targetPageIndex && it.row == targetRow && it.column == targetColumn }
            ?.shortcutId
        val currentShortcut = currentShortcutId?.let { state.shortcuts[it] }
        val existingPlacement = state.placements.find { it.shortcutId == targetShortcut.id }

        if (existingPlacement != null) {
            if (currentShortcut != null) {
                // 現在のスロットのショートカットを相手の位置へ
                shortcutRepository.savePlacement(
                    ShortcutPlacement(
                        shortcutId = currentShortcut.id,
                        pageIndex = existingPlacement.pageIndex,
                        row = existingPlacement.row,
                        column = existingPlacement.column
                    )
                )
            } else {
                // 現在が空なら相手の配置を削除
                shortcutRepository.removePlacement(targetShortcut.id)
            }
        }
        // 選択ショートカットをこのスロットに配置
        shortcutRepository.savePlacement(
            ShortcutPlacement(
                shortcutId = targetShortcut.id,
                pageIndex = targetPageIndex,
                row = targetRow,
                column = targetColumn
            )
        )
    }

    // === 内部ヘルパー ===

    private fun placeItem(item: ShortcutItem) {
        updateRowFixedHeight(targetPageIndex, targetRow, item.type)
        shortcutRepository.savePlacement(
            ShortcutPlacement(
                shortcutId = item.id,
                pageIndex = targetPageIndex,
                row = targetRow,
                column = targetColumn
            )
        )
    }

    /**
     * 日付・時刻表示を配置した行の固定高さを自動設定する。
     */
    private fun updateRowFixedHeight(pageIndex: Int, rowIndex: Int, placedType: ShortcutType) {
        val currentConfig = shortcutRepository.layoutState.value.config
        val newFixedHeight = when (placedType) {
            ShortcutType.TIME_DISPLAY -> 80
            ShortcutType.DATE_DISPLAY -> 56
            else -> null
        }
        val newRows = currentConfig.rows.map { row ->
            if (row.pageIndex == pageIndex && row.rowIndex == rowIndex) {
                row.copy(fixedHeightDp = newFixedHeight)
            } else row
        }
        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))
    }
}
