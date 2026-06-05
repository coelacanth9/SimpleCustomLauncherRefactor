package com.coelacanth9.simplecustomlauncher.feature.launcher.home

import android.app.Activity
import android.content.Context
import android.content.pm.LauncherApps
import android.net.Uri
import android.content.Intent
import android.os.Process
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.coelacanth9.simplecustomlauncher.R
import com.coelacanth9.simplecustomlauncher.model.HomeLayoutConfig
import com.coelacanth9.simplecustomlauncher.model.LayoutState
import com.coelacanth9.simplecustomlauncher.model.RowConfig
import com.coelacanth9.simplecustomlauncher.navigation.NavDestination
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import com.coelacanth9.simplecustomlauncher.model.isLinkCallList
import com.coelacanth9.simplecustomlauncher.model.isLinkDialer
import com.coelacanth9.simplecustomlauncher.model.shouldDeleteOnRemove
import com.coelacanth9.simplecustomlauncher.data.SettingsRepository
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.data.CalendarRepository
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import com.coelacanth9.simplecustomlauncher.platform.ads.AdManager
import com.coelacanth9.simplecustomlauncher.platform.billing.BillingManager
import com.coelacanth9.simplecustomlauncher.platform.billing.PremiumManager
import com.coelacanth9.simplecustomlauncher.platform.toIntent
import com.coelacanth9.simplecustomlauncher.model.RAKUTEN_LINK_PACKAGE
import com.coelacanth9.simplecustomlauncher.usecase.ApplyDefaultLayoutUseCase
import com.coelacanth9.simplecustomlauncher.usecase.CleanupUseCase
import com.coelacanth9.simplecustomlauncher.usecase.DeletePageUseCase
import com.coelacanth9.simplecustomlauncher.usecase.DeleteShortcutUseCase
import com.coelacanth9.simplecustomlauncher.usecase.EditSlotUseCase
import com.coelacanth9.simplecustomlauncher.usecase.RowUseCase
import kotlinx.coroutines.flow.StateFlow

// ===== エラー型 =====

sealed class ErrorMessage {
    object NoEmptySlot : ErrorMessage()
    object LaunchFailed : ErrorMessage()
    data class LaunchFailedWithError(val errorDetail: String) : ErrorMessage()
    object ShortcutInfoNotFound : ErrorMessage()
    object CannotAddMoreRows : ErrorMessage()

    fun toDisplayString(context: Context): String = when (this) {
        is NoEmptySlot -> context.getString(R.string.no_empty_slot)
        is LaunchFailed -> context.getString(R.string.launch_failed)
        is LaunchFailedWithError -> context.getString(R.string.launch_failed_with_error, errorDetail)
        is ShortcutInfoNotFound -> context.getString(R.string.shortcut_info_not_found)
        is CannotAddMoreRows -> context.getString(R.string.cannot_add_more_rows)
    }
}

data class ErrorEvent(
    val errorMessage: ErrorMessage,
    val id: Long = System.currentTimeMillis()
)

// ===== ViewModel =====

/**
 * ホーム画面の ViewModel。
 * - LayoutState は ShortcutRepository の StateFlow を購読（直接 expose）
 * - ショートカット選択結果はリポジトリへの書き込みで自動反映（返値なし）
 * - 課金・広告の UI 状態は PremiumViewModel が持つが、
 *   「プレミアム判定」「ページ追加トリガー」はここで管理する
 */
class HomeViewModel(
    private val shortcutRepository: ShortcutRepository,
    private val settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
    private val premiumManager: PremiumManager,
    private val applyDefaultLayoutUseCase: ApplyDefaultLayoutUseCase,
    private val rowUseCase: RowUseCase,
    private val deletePageUseCase: DeletePageUseCase,
    private val cleanupUseCase: CleanupUseCase,
    private val deleteShortcutUseCase: DeleteShortcutUseCase,
    private val editSlotUseCase: EditSlotUseCase,
    private val billingManager: BillingManager? = null,
    private val adManager: AdManager? = null
) : ViewModel() {

    // ===== リアクティブ状態 =====

    /** レイアウト全体。StateFlow を直接 expose して UI が collectAsState() する */
    val layoutState: StateFlow<LayoutState> = shortcutRepository.layoutState

    // ===== ナビゲーション =====

    var navDestination by mutableStateOf<NavDestination>(NavDestination.Home)
        private set

    fun navigateTo(dest: NavDestination) { navDestination = dest }
    fun navigateToHome() { navDestination = NavDestination.Home }
    fun navigateToShortcutSelect(pageIndex: Int, row: Int, column: Int) {
        navDestination = NavDestination.ShortcutSelect(pageIndex, row, column)
    }

    // ===== 現在ページ =====

    var currentPageIndex by mutableStateOf(0)
        private set

    var navigateToPageRequest by mutableStateOf<Int?>(null)
        private set

    fun setCurrentPage(pageIndex: Int) { currentPageIndex = pageIndex }
    fun navigateToPage(pageIndex: Int) { navigateToPageRequest = pageIndex }
    fun clearNavigateToPageRequest() { navigateToPageRequest = null }

    // ===== 編集モード =====

    var isEditMode by mutableStateOf(false)
        private set

    var showEditModeDialog by mutableStateOf(false)
        private set

    fun showEditModeConfirmDialog() { showEditModeDialog = true }
    fun dismissEditModeDialog() { showEditModeDialog = false }
    fun enterEditMode() { isEditMode = true; showEditModeDialog = false; navDestination = NavDestination.Home }
    fun exitEditMode() { isEditMode = false }

    // ===== ダイアログ =====

    var showAddRowDialog by mutableStateOf(false)
        private set
    var showAddPageConfirmDialog by mutableStateOf(false)
        private set
    var showPremiumRequiredForPageDialog by mutableStateOf(false)
        private set
    var showPageResetDialog by mutableStateOf(false)
        private set
    var showPageDeleteDialog by mutableStateOf(false)
        private set
    var pendingRowColumns by mutableStateOf(0)
        private set
    private var pendingInsertBeforeRowIndex: Int? = null
    var shortcutToConfirm by mutableStateOf<ShortcutItem?>(null)
        private set

    fun showShortcutConfirmDialog(item: ShortcutItem) { shortcutToConfirm = item }
    fun dismissShortcutConfirmDialog() { shortcutToConfirm = null }

    // ===== エラー =====

    var errorEvent by mutableStateOf<ErrorEvent?>(null)
        private set

    fun showError(errorMessage: ErrorMessage) { errorEvent = ErrorEvent(errorMessage) }
    fun clearError() { errorEvent = null }

    // ===== プレミアム =====

    var isPremium by mutableStateOf(premiumManager.isPremiumActive())
        private set
    var debugPremiumEnabled by mutableStateOf(premiumManager.isDebugPremiumEnabled())
        private set

    /** 購入完了後にページ追加を予約するフラグ */
    private var pendingAddPageAfterPurchase = false

    fun isPremiumActive(): Boolean = premiumManager.isPremiumActive()

    fun refreshPremiumStatus() {
        val newStatus = premiumManager.isPremiumActive()
        if (isPremium != newStatus) isPremium = newStatus
    }

    fun setDebugPremium(enabled: Boolean) {
        premiumManager.setDebugPremium(enabled)
        debugPremiumEnabled = enabled
        refreshPremiumStatus()
    }

    fun clearAllPremiumStatus() {
        premiumManager.clearAllPremiumStatus()
        debugPremiumEnabled = false
        refreshPremiumStatus()
    }

    fun recordAdWatch() {
        premiumManager.recordAdWatch()
        isPremium = premiumManager.isPremiumActive()
    }

    fun recordPurchase() {
        premiumManager.recordPurchase()
        isPremium = premiumManager.isPremiumActive()
    }

    /** MainActivity から課金完了時に呼ぶ */
    fun onPurchaseCompleted() {
        premiumManager.recordPurchase()
        isPremium = premiumManager.isPremiumActive()
        if (pendingAddPageAfterPurchase) {
            pendingAddPageAfterPurchase = false
            confirmAddPageWithRow()
        }
    }

    // ===== 設定 =====

    fun getAccessiblePageCount(): Int =
        if (premiumManager.isPremiumActive()) settingsRepository.pageCount else 1

    fun getTotalPageCount(): Int = settingsRepository.pageCount
    fun isLoopPagingEnabled(): Boolean = settingsRepository.loopPagingEnabled

    // ===== 祝日 =====

    fun getHolidaysForMonth(year: Int, month: Int, hasPermission: Boolean): Map<Int, String> =
        if (hasPermission) calendarRepository.getHolidaysForMonth(year, month) else emptyMap()

    // ===== 行追加ダイアログ =====

    fun showAddRowDialogAction() {
        pendingInsertBeforeRowIndex = null
        showAddRowDialog = true
    }

    fun showInsertRowDialog(insertBeforeRowIndex: Int) {
        pendingInsertBeforeRowIndex = insertBeforeRowIndex
        showAddRowDialog = true
    }

    fun dismissAddRowDialog() {
        showAddRowDialog = false
        pendingInsertBeforeRowIndex = null
    }

    fun addRow(columns: Int) {
        val insertBefore = pendingInsertBeforeRowIndex
        pendingInsertBeforeRowIndex = null
        if (insertBefore != null) insertRowAt(currentPageIndex, insertBefore, columns)
        else addRowToPage(currentPageIndex, columns)
    }

    fun addRowToPage(pageIndex: Int, columns: Int) {
        val currentConfig = shortcutRepository.layoutState.value.config
        val pageRows = currentConfig.getRowsForPage(pageIndex)
        if (pageRows.size >= MAX_ROWS_PER_PAGE) {
            handleRowLimitExceeded(columns)
            return
        }
        val newRowIndex = pageRows.maxOfOrNull { it.rowIndex }?.plus(1) ?: 0
        val newRows = currentConfig.rows + RowConfig(
            pageIndex = pageIndex,
            rowIndex = newRowIndex,
            columns = columns
        )
        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))
        showAddRowDialog = false
    }

    private fun insertRowAt(pageIndex: Int, beforeRowIndex: Int, columns: Int) {
        val currentConfig = shortcutRepository.layoutState.value.config
        val pageRows = currentConfig.getRowsForPage(pageIndex)
        if (pageRows.size >= MAX_ROWS_PER_PAGE) {
            handleRowLimitExceeded(columns)
            return
        }
        rowUseCase.insertRowAt(pageIndex, beforeRowIndex, columns)
        showAddRowDialog = false
    }

    private fun handleRowLimitExceeded(columns: Int) {
        val currentPageCount = settingsRepository.pageCount
        if (currentPageCount < SettingsRepository.MAX_PAGES) {
            pendingRowColumns = columns
            if (premiumManager.isPremiumActive()) {
                showAddPageConfirmDialog = true
            } else {
                showPremiumRequiredForPageDialog = true
            }
        } else {
            showError(ErrorMessage.CannotAddMoreRows)
        }
        showAddRowDialog = false
    }

    fun dismissAddPageConfirmDialog() { showAddPageConfirmDialog = false; pendingRowColumns = 0 }
    fun dismissPremiumRequiredForPageDialog() { showPremiumRequiredForPageDialog = false; pendingRowColumns = 0 }

    fun watchAdAndAddPage(activity: Activity) {
        showPremiumRequiredForPageDialog = false
        adManager?.showRewardedAd(activity) {
            recordAdWatch()
            confirmAddPageWithRow()
        }
    }

    fun purchaseAndAddPage(activity: Activity) {
        showPremiumRequiredForPageDialog = false
        pendingAddPageAfterPurchase = true
        billingManager?.launchPurchaseFlow(activity)
    }

    fun confirmAddPageWithRow() {
        val currentPageCount = settingsRepository.pageCount
        if (currentPageCount >= SettingsRepository.MAX_PAGES) {
            showAddPageConfirmDialog = false
            return
        }
        val newPageCount = currentPageCount + 1
        settingsRepository.pageCount = newPageCount
        val currentConfig = shortcutRepository.layoutState.value.config
        val newPageIndex = newPageCount - 1
        val newRows = currentConfig.rows + RowConfig(
            pageIndex = newPageIndex,
            rowIndex = 0,
            columns = pendingRowColumns
        )
        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))
        showAddPageConfirmDialog = false
        pendingRowColumns = 0
        navigateToPageRequest = newPageIndex
    }

    // ===== ページリセット・削除 =====

    fun showPageResetDialogAction() { showPageResetDialog = true }
    fun dismissPageResetDialog() { showPageResetDialog = false }
    fun confirmPageReset() { clearCurrentPageLayout(); showPageResetDialog = false }

    fun showPageDeleteDialogAction() { showPageDeleteDialog = true }
    fun dismissPageDeleteDialog() { showPageDeleteDialog = false }
    fun confirmPageDelete() { deletePage(currentPageIndex); showPageDeleteDialog = false }

    // ===== レイアウト操作 =====

    fun resetToDefault() {
        applyDefaultLayoutUseCase.resetToDefault()
        settingsRepository.pageCount = 1
        navigateToPageRequest = 0
    }

    fun clearLayout() = shortcutRepository.clearAllLayout()

    fun clearCurrentPageLayout() {
        val state = shortcutRepository.layoutState.value
        val pageIndex = currentPageIndex
        val pagePlacements = state.placements.filter { it.pageIndex == pageIndex }
        val newRows = state.config.rows.filter { it.pageIndex != pageIndex }
        shortcutRepository.update {
            pagePlacements.forEach { placement ->
                removePlacement(placement.shortcutId)
                state.shortcuts[placement.shortcutId]?.let { shortcut ->
                    if (shouldDeleteOnRemove(shortcut)) deleteShortcut(shortcut.id)
                }
            }
            saveLayoutConfig(HomeLayoutConfig(rows = newRows))
        }
    }

    // ===== 行操作 =====

    fun deleteRow(pageIndex: Int, rowIndex: Int) {
        rowUseCase.deleteRow(pageIndex, rowIndex)
    }

    fun changeRowColumns(pageIndex: Int, rowIndex: Int, newColumns: Int) {
        rowUseCase.changeRowColumns(pageIndex, rowIndex, newColumns)
    }

    fun changeRowTextOnly(pageIndex: Int, rowIndex: Int, textOnly: Boolean) {
        rowUseCase.changeTextOnly(pageIndex, rowIndex, textOnly)
    }

    // ===== スロット操作 =====

    fun clearSlot(shortcut: ShortcutItem) {
        editSlotUseCase.clearSlot(shortcut)
    }

    fun changeSlotColors(pageIndex: Int, row: Int, column: Int, backgroundColor: String?, textColor: String?) {
        editSlotUseCase.changeColors(pageIndex, row, column, backgroundColor, textColor)
    }

    // ===== ページ削除 =====

    fun deletePage(pageIndex: Int) {
        val deleted = deletePageUseCase.execute(pageIndex)
        if (deleted && currentPageIndex >= settingsRepository.pageCount) {
            navigateToPageRequest = maxOf(0, currentPageIndex - 1)
        }
    }

    // ===== クリーンアップ =====

    fun cleanupOrphanedPinShortcuts() {
        cleanupUseCase.cleanupOrphanedPinShortcuts()
    }

    fun onPackageRemoved(packageName: String) {
        cleanupUseCase.onPackageRemoved(packageName)
    }

    fun cleanupUninstalledPackages() {
        cleanupUseCase.cleanupUninstalledPackages()
    }

    fun deleteUnplacedShortcut(id: String) {
        val success = deleteShortcutUseCase.execute(id)
        if (!success) showError(ErrorMessage.LaunchFailed)
    }

    // ===== ショートカット起動 =====

    fun launchShortcut(context: Context, item: ShortcutItem, shortcutHelper: ShortcutHelper) {
        Log.d(TAG, "launchShortcut: type=${item.type}, label=${item.label}")
        try {
            when (item.type) {
                ShortcutType.APP -> {
                    item.packageName?.let { shortcutHelper.startApp(it) }
                }
                ShortcutType.INTENT -> {
                    val intent = item.toIntent()
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        launchPinShortcut(context, item, shortcutHelper)
                    }
                }
                ShortcutType.PHONE -> {
                    if (isLinkCallList(item)) {
                        try {
                            val callListIntent = Intent("$RAKUTEN_LINK_PACKAGE.ACTION_OPEN_HOME").apply {
                                setPackage(RAKUTEN_LINK_PACKAGE)
                                putExtra("KEY_OPEN_TAB", "open_missed_calls")
                            }
                            context.startActivity(callListIntent)
                        } catch (e: Exception) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(RAKUTEN_LINK_PACKAGE)
                            if (launchIntent != null) context.startActivity(launchIntent)
                            else showError(ErrorMessage.LaunchFailed)
                        }
                    } else {
                        item.toIntent()?.let { intent ->
                            if (item.packageName != null) {
                                intent.setPackage(item.packageName)
                            } else {
                                val dialerPackages = listOf("com.google.android.dialer", "com.android.dialer")
                                val installedDialer = dialerPackages.firstOrNull { pkg ->
                                    shortcutHelper.getAppIcon(pkg) != null
                                }
                                if (installedDialer != null) intent.setPackage(installedDialer)
                            }
                            context.startActivity(intent)
                        }
                    }
                }
                ShortcutType.SMS -> {
                    if (item.packageName == RAKUTEN_LINK_PACKAGE) {
                        try {
                            val chatIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://one.rakuten.co.jp/chat"))
                            context.startActivity(chatIntent)
                        } catch (e: Exception) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(item.packageName!!)
                            if (launchIntent != null) context.startActivity(launchIntent)
                            else showError(ErrorMessage.LaunchFailed)
                        }
                    } else if (item.packageName != null) {
                        item.toIntent()?.let { intent ->
                            intent.setPackage(item.packageName)
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(item.packageName!!)
                                if (launchIntent != null) context.startActivity(launchIntent)
                                else showError(ErrorMessage.LaunchFailed)
                            }
                        }
                    } else {
                        item.toIntent()?.let { context.startActivity(it) }
                    }
                }
                ShortcutType.DIALER -> {
                    if (isLinkDialer(item)) {
                        try {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:-")).apply {
                                setPackage(RAKUTEN_LINK_PACKAGE)
                            }
                            context.startActivity(dialIntent)
                        } catch (e: Exception) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(RAKUTEN_LINK_PACKAGE)
                            if (launchIntent != null) context.startActivity(launchIntent)
                            else showError(ErrorMessage.LaunchFailed)
                        }
                    } else {
                        item.toIntent()?.let { context.startActivity(it) }
                    }
                }
                ShortcutType.CALENDAR -> navigateTo(NavDestination.Calendar)
                ShortcutType.MEMO -> navigateTo(NavDestination.Memo)
                ShortcutType.SETTINGS -> navigateTo(NavDestination.Settings)
                ShortcutType.ALL_APPS -> navigateTo(NavDestination.AllApps)
                ShortcutType.DEVICE_SETTINGS -> {
                    val intent = item.toIntent()
                    if (intent != null) {
                        try {
                            context.startActivity(intent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            Log.w(TAG, "Device settings not available: ${item.intentUri}", e)
                            showError(ErrorMessage.LaunchFailedWithError("この端末では対応していません"))
                        }
                    } else {
                        showError(ErrorMessage.LaunchFailed)
                    }
                }
                ShortcutType.DATE_DISPLAY -> { /* 表示のみ */ }
                ShortcutType.TIME_DISPLAY -> { /* 表示のみ */ }
                ShortcutType.EMPTY -> { /* 何もしない */ }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch shortcut", e)
            showError(ErrorMessage.LaunchFailed)
        }
    }

    private fun launchPinShortcut(context: Context, item: ShortcutItem, shortcutHelper: ShortcutHelper) {
        val (shortcutId, packageName) = shortcutRepository.getPinShortcutInfo(item.id)
        Log.d(TAG, "launchPinShortcut: itemId=${item.id}, shortcutId=$shortcutId, package=$packageName")

        if (shortcutId != null && packageName != null) {
            val isValid = shortcutHelper.isShortcutValid(packageName, shortcutId)
            if (isValid == false) {
                Log.w(TAG, "Shortcut no longer exists: $shortcutId")
                showError(ErrorMessage.LaunchFailedWithError(
                    context.getString(R.string.shortcut_may_be_invalid)
                ))
                return
            }
            try {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                launcherApps.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to launch pin shortcut (SecurityException): ${e.message}", e)
                showError(ErrorMessage.LaunchFailedWithError(
                    context.getString(R.string.set_as_home_to_use_shortcut)
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch pin shortcut: ${e.message}", e)
                showError(ErrorMessage.LaunchFailedWithError(e.message ?: ""))
            }
        } else {
            Log.e(TAG, "Shortcut info not found for item: ${item.id}, label: ${item.label}")
            showError(ErrorMessage.ShortcutInfoNotFound)
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
        const val MAX_ROWS_PER_PAGE = 7
    }
}
