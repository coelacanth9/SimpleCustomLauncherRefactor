package com.coelacanth9.simplecustomlauncher.feature.launcher.shortcutselect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coelacanth9.simplecustomlauncher.core.layout.HomeLayoutConfig
import com.coelacanth9.simplecustomlauncher.core.layout.LayoutState
import com.coelacanth9.simplecustomlauncher.core.layout.ShortcutPlacement
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutItem
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutType
import com.coelacanth9.simplecustomlauncher.core.shortcut.isLinkRelated
import com.coelacanth9.simplecustomlauncher.core.shortcut.shouldDeleteOnRemove
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.platform.AppInfo
import com.coelacanth9.simplecustomlauncher.platform.ShortcutData
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import com.coelacanth9.simplecustomlauncher.platform.billing.PremiumManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * ショートカット選択画面の ViewModel。
 * 選択結果は ShortcutRepository に書き込み、StateFlow 経由で HomeViewModel に反映される。
 * （ViewModel 間の直接参照・コールバックは持たない）
 */
class ShortcutSelectViewModel(
    private val shortcutRepository: ShortcutRepository,
    private val shortcutHelper: ShortcutHelper,
    private val premiumManager: PremiumManager,
    val targetPageIndex: Int,
    val targetRow: Int,
    val targetColumn: Int
) : ViewModel() {

    // ===== リアクティブ状態（画面が collectAsState して使う）=====

    val layoutState: StateFlow<LayoutState> = shortcutRepository.layoutState

    // ===== UI 状態 =====

    var apps by mutableStateOf<List<AppInfo>?>(null)
        private set

    var shortcuts by mutableStateOf<List<ShortcutData>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    // ===== プレミアム状態 =====

    val isPremium: Boolean get() = premiumManager.isPremiumActive()

    // ===== 初期化 =====

    fun loadApps() {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { shortcutHelper.getInstalledApps() }
            apps = result
            isLoading = false
        }
    }

    fun loadShortcutsForApp(packageName: String) {
        shortcuts = shortcutHelper.getShortcutsForApp(packageName)
    }

    fun findLinkSmsShortcut(phoneNumber: String, contactName: String): ShortcutData? =
        shortcutHelper.findLinkSmsShortcut(phoneNumber, contactName)

    // ===== 配置操作（呼ぶと StateFlow 経由で HomeViewModel に自動反映）=====

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
                shortcutRepository.savePlacement(
                    ShortcutPlacement(
                        shortcutId = currentShortcut.id,
                        pageIndex = existingPlacement.pageIndex,
                        row = existingPlacement.row,
                        column = existingPlacement.column
                    )
                )
            } else {
                shortcutRepository.removePlacement(targetShortcut.id)
            }
        }
        shortcutRepository.savePlacement(
            ShortcutPlacement(
                shortcutId = targetShortcut.id,
                pageIndex = targetPageIndex,
                row = targetRow,
                column = targetColumn
            )
        )
    }

    // ===== スロット・行編集操作 =====

    /**
     * 現スロットを空にする。shouldDeleteOnRemove に従いショートカット本体も削除。
     */
    fun clearSlot() {
        val shortcut = currentShortcutItem() ?: return
        shortcutRepository.removePlacement(shortcut.id)
        if (shouldDeleteOnRemove(shortcut)) shortcutRepository.deleteShortcut(shortcut.id)
    }

    /**
     * 行の分割数を変更する。はみ出た列・Link 系ショートカットを削除。
     */
    fun changeColumns(columns: Int) {
        val state = shortcutRepository.layoutState.value
        // はみ出した列の配置を削除
        state.placements
            .filter { it.pageIndex == targetPageIndex && it.row == targetRow && it.column >= columns }
            .forEach { placement ->
                shortcutRepository.removePlacement(placement.shortcutId)
                state.shortcuts[placement.shortcutId]?.let { shortcut ->
                    if (shouldDeleteOnRemove(shortcut)) shortcutRepository.deleteShortcut(shortcut.id)
                }
            }
        // 3列以上に変更する場合、Link 系を削除（アイコンのみで区別不能）
        if (columns >= 3) {
            state.placements
                .filter { it.pageIndex == targetPageIndex && it.row == targetRow && it.column < columns }
                .forEach { placement ->
                    state.shortcuts[placement.shortcutId]?.let { shortcut ->
                        if (isLinkRelated(shortcut)) {
                            shortcutRepository.removePlacement(placement.shortcutId)
                            if (shouldDeleteOnRemove(shortcut)) shortcutRepository.deleteShortcut(shortcut.id)
                        }
                    }
                }
        }
        val newRows = state.config.rows.map { row ->
            if (row.pageIndex == targetPageIndex && row.rowIndex == targetRow) row.copy(columns = columns)
            else row
        }
        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))
    }

    /**
     * 行のテキストのみモードを切替。
     */
    fun changeTextOnly(textOnly: Boolean) {
        val currentConfig = shortcutRepository.layoutState.value.config
        val newRows = currentConfig.rows.map { row ->
            if (row.pageIndex == targetPageIndex && row.rowIndex == targetRow) row.copy(textOnly = textOnly)
            else row
        }
        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))
    }

    /**
     * 現スロットの背景色・文字色を変更。
     */
    fun changeColors(backgroundColor: String?, textColor: String?) {
        val targetPlacement = shortcutRepository.layoutState.value.placements.find {
            it.pageIndex == targetPageIndex && it.row == targetRow && it.column == targetColumn
        } ?: return
        shortcutRepository.savePlacement(
            targetPlacement.copy(backgroundColor = backgroundColor, textColor = textColor)
        )
    }

    /**
     * 行全体を削除（全配置削除 + RowConfig 削除）。
     */
    fun deleteRow() {
        val state = shortcutRepository.layoutState.value
        state.placements
            .filter { it.pageIndex == targetPageIndex && it.row == targetRow }
            .forEach { placement ->
                shortcutRepository.removePlacement(placement.shortcutId)
                state.shortcuts[placement.shortcutId]?.let { shortcut ->
                    if (shouldDeleteOnRemove(shortcut)) shortcutRepository.deleteShortcut(shortcut.id)
                }
            }
        val newRows = state.config.rows.filter {
            !(it.pageIndex == targetPageIndex && it.rowIndex == targetRow)
        }
        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))
    }

    /**
     * 未配置ショートカットを完全削除。
     */
    fun deleteUnplacedShortcut(shortcut: ShortcutItem) {
        shortcutRepository.deleteShortcut(shortcut.id)
    }

    // ===== 内部ヘルパー =====

    private fun currentShortcutItem(): ShortcutItem? {
        val state = shortcutRepository.layoutState.value
        val shortcutId = state.placements.find {
            it.pageIndex == targetPageIndex && it.row == targetRow && it.column == targetColumn
        }?.shortcutId
        return shortcutId?.let { state.shortcuts[it] }
    }

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
