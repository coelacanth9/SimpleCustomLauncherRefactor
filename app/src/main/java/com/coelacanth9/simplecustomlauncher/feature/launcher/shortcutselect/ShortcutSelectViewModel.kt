package com.coelacanth9.simplecustomlauncher.feature.launcher.shortcutselect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.coelacanth9.simplecustomlauncher.model.LayoutState
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.platform.AppInfo
import com.coelacanth9.simplecustomlauncher.platform.ShortcutData
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import com.coelacanth9.simplecustomlauncher.platform.billing.PremiumManager
import com.coelacanth9.simplecustomlauncher.usecase.DeleteShortcutUseCase
import com.coelacanth9.simplecustomlauncher.usecase.EditSlotUseCase
import com.coelacanth9.simplecustomlauncher.usecase.PlaceShortcutUseCase
import com.coelacanth9.simplecustomlauncher.usecase.RowUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ショートカット選択画面の ViewModel。
 * 選択結果は ShortcutRepository に書き込み、StateFlow 経由で HomeViewModel に反映される。
 * （ViewModel 間の直接参照・コールバックは持たない）
 */
@HiltViewModel(assistedFactory = ShortcutSelectViewModel.Factory::class)
class ShortcutSelectViewModel @AssistedInject constructor(
    private val shortcutRepository: ShortcutRepository,
    private val shortcutHelper: ShortcutHelper,
    private val premiumManager: PremiumManager,
    private val placeShortcutUseCase: PlaceShortcutUseCase,
    private val rowUseCase: RowUseCase,
    private val editSlotUseCase: EditSlotUseCase,
    private val deleteShortcutUseCase: DeleteShortcutUseCase,
    @Assisted("pageIndex") val targetPageIndex: Int,
    @Assisted("row") val targetRow: Int,
    @Assisted("column") val targetColumn: Int
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("pageIndex") targetPageIndex: Int,
            @Assisted("row") targetRow: Int,
            @Assisted("column") targetColumn: Int
        ): ShortcutSelectViewModel
    }

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
        placeShortcutUseCase.placeApp(targetPageIndex, targetRow, targetColumn, packageName, label)
    }

    fun placeInternalFeature(type: ShortcutType, label: String) {
        placeShortcutUseCase.placeInternalFeature(targetPageIndex, targetRow, targetColumn, type, label)
    }

    fun placeDeviceSettings(label: String, settingsAction: String) {
        placeShortcutUseCase.placeDeviceSettings(targetPageIndex, targetRow, targetColumn, label, settingsAction)
    }

    fun placeContact(
        name: String,
        phoneNumber: String,
        type: ShortcutType,
        targetPackage: String? = null
    ) {
        placeShortcutUseCase.placeContact(targetPageIndex, targetRow, targetColumn, name, phoneNumber, type, targetPackage)
    }

    fun placeIntent(shortLabel: String, packageName: String, shortcutId: String) {
        placeShortcutUseCase.placeIntent(targetPageIndex, targetRow, targetColumn, shortLabel, packageName, shortcutId)
    }

    /**
     * 配置済みショートカットをこのスロットと入れ替える。
     */
    fun swapShortcuts(targetShortcut: ShortcutItem) {
        placeShortcutUseCase.swapShortcuts(targetShortcut, targetPageIndex, targetRow, targetColumn)
    }

    // ===== スロット・行編集操作 =====

    /**
     * 現スロットを空にする。shouldDeleteOnRemove に従いショートカット本体も削除。
     */
    fun clearSlot() {
        val shortcut = currentShortcutItem() ?: return
        editSlotUseCase.clearSlot(shortcut)
    }

    /**
     * 行の分割数を変更する。
     */
    fun changeColumns(columns: Int) {
        rowUseCase.changeRowColumns(targetPageIndex, targetRow, columns)
    }

    /**
     * 行のテキストのみモードを切替。
     */
    fun changeTextOnly(textOnly: Boolean) {
        rowUseCase.changeTextOnly(targetPageIndex, targetRow, textOnly)
    }

    /**
     * 現スロットの背景色・文字色を変更。
     */
    fun changeColors(backgroundColor: String?, textColor: String?) {
        editSlotUseCase.changeColors(targetPageIndex, targetRow, targetColumn, backgroundColor, textColor)
    }

    /**
     * 行全体を削除（全配置削除 + RowConfig 削除）。
     */
    fun deleteRow() {
        rowUseCase.deleteRow(targetPageIndex, targetRow)
    }

    /**
     * 未配置ショートカットを完全削除。
     */
    fun deleteUnplacedShortcut(shortcut: ShortcutItem) {
        deleteShortcutUseCase.execute(shortcut.id)
    }

    // ===== 内部ヘルパー =====

    private fun currentShortcutItem(): ShortcutItem? {
        val state = shortcutRepository.layoutState.value
        val shortcutId = state.placements.find {
            it.pageIndex == targetPageIndex && it.row == targetRow && it.column == targetColumn
        }?.shortcutId
        return shortcutId?.let { state.shortcuts[it] }
    }
}
