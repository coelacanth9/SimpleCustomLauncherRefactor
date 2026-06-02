package com.coelacanth9.simplecustomlauncher.feature.launcher.home

import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.coelacanth9.simplecustomlauncher.R
import com.coelacanth9.simplecustomlauncher.core.navigation.NavDestination
import com.coelacanth9.simplecustomlauncher.core.shortcut.RAKUTEN_LINK_PACKAGE
import com.coelacanth9.simplecustomlauncher.core.shortcut.RowConfig
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutItem
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutPlacement
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutType
import com.coelacanth9.simplecustomlauncher.core.layout.LayoutState
import com.coelacanth9.simplecustomlauncher.data.SettingsRepository
import com.coelacanth9.simplecustomlauncher.data.TapMode
import com.coelacanth9.simplecustomlauncher.data.VibrationStrength
import com.coelacanth9.simplecustomlauncher.platform.CalendarRepository
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import com.coelacanth9.simplecustomlauncher.ui.components.AddPageConfirmDialog
import com.coelacanth9.simplecustomlauncher.ui.components.AddRowDialog
import com.coelacanth9.simplecustomlauncher.ui.components.EditModeConfirmDialog
import com.coelacanth9.simplecustomlauncher.ui.components.ExpandDirection
import com.coelacanth9.simplecustomlauncher.ui.components.ExpandableMenuItem
import com.coelacanth9.simplecustomlauncher.ui.components.FloatingExpandableButton
import com.coelacanth9.simplecustomlauncher.ui.components.PageDeleteConfirmDialog
import com.coelacanth9.simplecustomlauncher.ui.components.PageIndicator
import com.coelacanth9.simplecustomlauncher.ui.components.PageResetConfirmDialog
import com.coelacanth9.simplecustomlauncher.ui.components.PremiumLockOverlay
import com.coelacanth9.simplecustomlauncher.ui.components.PremiumRequiredForPageDialog
import com.coelacanth9.simplecustomlauncher.ui.components.ShortcutConfirmDialog
import com.coelacanth9.simplecustomlauncher.ui.theme.AppTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ===== ユーティリティ =====

/**
 * 内部機能のラベルをローカライズして取得
 */
@Composable
fun getLocalizedLabel(item: ShortcutItem): String {
    val context = LocalContext.current
    return when (item.type) {
        ShortcutType.CALENDAR -> stringResource(R.string.shortcut_type_calendar)
        ShortcutType.MEMO -> stringResource(R.string.shortcut_type_memo)
        ShortcutType.SETTINGS -> stringResource(R.string.settings)
        ShortcutType.DIALER -> if (item.packageName == RAKUTEN_LINK_PACKAGE) {
            item.label
        } else {
            stringResource(R.string.shortcut_type_phone)
        }
        ShortcutType.ALL_APPS -> stringResource(R.string.shortcut_type_all_apps)
        ShortcutType.DATE_DISPLAY -> stringResource(R.string.shortcut_type_date)
        ShortcutType.TIME_DISPLAY -> stringResource(R.string.shortcut_type_time)
        ShortcutType.APP -> {
            item.packageName?.let { pkg ->
                try {
                    val pm = context.packageManager
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    item.label
                }
            } ?: item.label
        }
        else -> item.label
    }
}

/**
 * タップモードに応じたクリック処理の Modifier
 */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.tapModeClickable(
    isEditMode: Boolean,
    tapMode: TapMode,
    onClick: () -> Unit
): Modifier = if (isEditMode) {
    this.clickable(onClick = onClick)
} else {
    when (tapMode) {
        TapMode.SINGLE_TAP -> this.clickable(onClick = onClick)
        TapMode.LONG_TAP -> this.combinedClickable(
            onClick = { /* タップでは何もしない */ },
            onLongClick = onClick
        )
    }
}

// ===== ホーム画面ヘッダー =====

@Composable
private fun HomeHeader(
    isEditMode: Boolean,
    onLayoutEdit: () -> Unit,
    onAppSettings: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.app_settings),
                fontSize = 14.sp,
                color = if (isEditMode) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .then(if (!isEditMode) Modifier.clickable { onAppSettings() } else Modifier)
                    .padding(8.dp)
            )
            Text(
                text = stringResource(R.string.layout_edit),
                fontSize = 14.sp,
                color = if (isEditMode) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .then(if (!isEditMode) Modifier.clickable { onLayoutEdit() } else Modifier)
                    .padding(8.dp)
            )
            Text(
                text = stringResource(R.string.phone_settings),
                fontSize = 14.sp,
                color = if (isEditMode) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .then(
                        if (!isEditMode) Modifier.clickable {
                            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.startActivity(intent)
                        } else Modifier
                    )
                    .padding(8.dp)
            )
        }
    }
}

// ===== ホーム画面 =====

/**
 * ホーム画面
 */
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    premiumViewModel: PremiumViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val shortcutHelper = remember { ShortcutHelper(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    val showSystemWallpaper = settingsRepository.showSystemWallpaper

    // エラー表示
    LaunchedEffect(homeViewModel.errorEvent) {
        homeViewModel.errorEvent?.let { error ->
            snackbarHostState.showSnackbar(error.errorMessage.toDisplayString(context))
            homeViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = if (showSystemWallpaper) Color.Transparent
                         else MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                HomeHeader(
                    isEditMode = homeViewModel.isEditMode,
                    onLayoutEdit = { homeViewModel.showEditModeConfirmDialog() },
                    onAppSettings = { homeViewModel.navigateTo(NavDestination.Settings) }
                )

                // ホームコンテンツ（上スワイプでアプリ一覧を表示）
                var swipeActivated by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(homeViewModel.isEditMode) {
                            var totalDrag = 0f
                            detectVerticalDragGestures(
                                onDragStart = {
                                    totalDrag = 0f
                                    swipeActivated = false
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    totalDrag += dragAmount
                                    swipeActivated = totalDrag < -100f
                                },
                                onDragEnd = {
                                    if (totalDrag < -100f) {
                                        if (!homeViewModel.isEditMode) {
                                            homeViewModel.navigateTo(NavDestination.AllApps)
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.cannot_open_all_apps_in_edit_mode),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    swipeActivated = false
                                },
                                onDragCancel = { swipeActivated = false }
                            )
                        }
                ) {
                    HomeContent(
                        homeViewModel = homeViewModel,
                        premiumViewModel = premiumViewModel,
                        shortcutHelper = shortcutHelper
                    )

                    // 上スワイプ時のインジケータ
                    if (swipeActivated) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(
                                modifier = Modifier.padding(bottom = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.shortcut_type_all_apps),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 編集モード時の FAB パイメニュー
            if (homeViewModel.isEditMode) {
                FloatingExpandableButton(
                    onClick = { homeViewModel.exitEditMode() },
                    icon = Icons.Default.Check,
                    contentDescription = stringResource(R.string.edit_complete),
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White,
                    expandDirection = ExpandDirection.TopStart,
                    menuItems = buildList {
                        add(
                            ExpandableMenuItem(
                                label = stringResource(R.string.reset_page),
                                icon = Icons.Default.Refresh,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                onSelect = { homeViewModel.showPageResetDialogAction() }
                            )
                        )
                        if (homeViewModel.getTotalPageCount() > 1) {
                            add(
                                ExpandableMenuItem(
                                    label = stringResource(R.string.delete_page),
                                    icon = Icons.Default.Delete,
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                    onSelect = { homeViewModel.showPageDeleteDialogAction() }
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // 編集モード確認ダイアログ
    if (homeViewModel.showEditModeDialog) {
        EditModeConfirmDialog(
            onConfirm = { homeViewModel.enterEditMode() },
            onDismiss = { homeViewModel.dismissEditModeDialog() }
        )
    }

    // ショートカット実行確認ダイアログ
    homeViewModel.shortcutToConfirm?.let { item ->
        ShortcutConfirmDialog(
            label = getLocalizedLabel(item),
            onConfirm = {
                homeViewModel.launchShortcut(context, item, shortcutHelper)
                homeViewModel.dismissShortcutConfirmDialog()
            },
            onDismiss = { homeViewModel.dismissShortcutConfirmDialog() }
        )
    }

    // 行追加ダイアログ
    if (homeViewModel.showAddRowDialog) {
        AddRowDialog(
            onAddRow = { columns -> homeViewModel.addRow(columns) },
            onDismiss = { homeViewModel.dismissAddRowDialog() }
        )
    }

    // ページ追加確認ダイアログ
    if (homeViewModel.showAddPageConfirmDialog) {
        AddPageConfirmDialog(
            onConfirm = { homeViewModel.confirmAddPageWithRow() },
            onDismiss = { homeViewModel.dismissAddPageConfirmDialog() }
        )
    }

    // プレミアム誘導ダイアログ（ページ追加時）
    if (homeViewModel.showPremiumRequiredForPageDialog) {
        val activity = context as? android.app.Activity
        PremiumRequiredForPageDialog(
            onWatchAd = { activity?.let { homeViewModel.watchAdAndAddPage(it) } },
            onPurchase = { activity?.let { homeViewModel.purchaseAndAddPage(it) } },
            onDismiss = { homeViewModel.dismissPremiumRequiredForPageDialog() }
        )
    }

    // ページリセット確認ダイアログ
    if (homeViewModel.showPageResetDialog) {
        PageResetConfirmDialog(
            onConfirm = { homeViewModel.confirmPageReset() },
            onDismiss = { homeViewModel.dismissPageResetDialog() }
        )
    }

    // ページ削除確認ダイアログ
    if (homeViewModel.showPageDeleteDialog) {
        PageDeleteConfirmDialog(
            onConfirm = { homeViewModel.confirmPageDelete() },
            onDismiss = { homeViewModel.dismissPageDeleteDialog() }
        )
    }
}

// ===== ホームコンテンツ（ページャー） =====

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    homeViewModel: HomeViewModel,
    premiumViewModel: PremiumViewModel,
    shortcutHelper: ShortcutHelper
) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val tapMode = settingsRepository.tapMode
    val showConfirmDialog = settingsRepository.showConfirmDialog
    val vibrationStrength = settingsRepository.vibrationStrength
    val cardAlpha = if (settingsRepository.showSystemWallpaper) 0.7f else 1f

    // LayoutState を購読（StateFlow → 変更時自動再コンポーズ）
    val layoutState by homeViewModel.layoutState.collectAsState()

    val totalPageCount = homeViewModel.getTotalPageCount()
    val isPremium = homeViewModel.isPremium
    val loopEnabled = homeViewModel.isLoopPagingEnabled()

    val loopMultiplier = 1000
    val effectivePageCount = if (loopEnabled && totalPageCount > 1) {
        totalPageCount * loopMultiplier
    } else {
        totalPageCount
    }
    val initialPage = if (loopEnabled && totalPageCount > 1) {
        totalPageCount * (loopMultiplier / 2) + homeViewModel.currentPageIndex
    } else {
        homeViewModel.currentPageIndex.coerceIn(0, maxOf(0, totalPageCount - 1))
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { effectivePageCount }
    )

    // 現在ページを ViewModel に同期
    LaunchedEffect(pagerState.currentPage, totalPageCount, loopEnabled) {
        val actualPage = if (loopEnabled && totalPageCount > 1) {
            pagerState.currentPage % totalPageCount
        } else {
            pagerState.currentPage.coerceIn(0, maxOf(0, totalPageCount - 1))
        }
        homeViewModel.setCurrentPage(actualPage)
    }

    // ページ遷移リクエストを処理
    LaunchedEffect(homeViewModel.navigateToPageRequest) {
        homeViewModel.navigateToPageRequest?.let { targetPage ->
            val targetPagerPage = if (loopEnabled && totalPageCount > 1) {
                val currentActual = pagerState.currentPage % totalPageCount
                pagerState.currentPage + (targetPage - currentActual)
            } else {
                targetPage
            }
            pagerState.animateScrollToPage(targetPagerPage)
            homeViewModel.clearNavigateToPageRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ページインジケーター
            if (totalPageCount > 1) {
                val currentActualPage = if (loopEnabled && totalPageCount > 1) {
                    pagerState.currentPage % totalPageCount
                } else {
                    pagerState.currentPage
                }
                PageIndicator(
                    pageCount = totalPageCount,
                    currentPage = currentActualPage,
                    lockedFromPage = if (isPremium) -1 else 1
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val actualPage = if (loopEnabled && totalPageCount > 1) {
                    page % totalPageCount
                } else {
                    page
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    HomePageContent(
                        homeViewModel = homeViewModel,
                        pageIndex = actualPage,
                        layoutState = layoutState,
                        shortcutHelper = shortcutHelper,
                        tapMode = tapMode,
                        showConfirmDialog = showConfirmDialog,
                        vibrationStrength = vibrationStrength,
                        cardAlpha = cardAlpha
                    )

                    // 非プレミアム時、2ページ目以降はロックオーバーレイ
                    if (!isPremium && actualPage > 0) {
                        val activity = context as? android.app.Activity
                        PremiumLockOverlay(
                            onWatchAd = {
                                activity?.let { act ->
                                    premiumViewModel.showRewardedAd(act) {
                                        homeViewModel.recordAdWatch()
                                    }
                                }
                            },
                            onPurchase = {
                                activity?.let { premiumViewModel.launchPurchase(it) }
                            },
                            formattedPrice = premiumViewModel.getFormattedPrice(),
                            isAdReady = premiumViewModel.isAdReady()
                        )
                    }
                }
            }
        }
    }
}

// ===== ページコンテンツ =====

@Composable
private fun HomePageContent(
    homeViewModel: HomeViewModel,
    pageIndex: Int,
    layoutState: LayoutState,
    shortcutHelper: ShortcutHelper,
    tapMode: TapMode,
    showConfirmDialog: Boolean,
    vibrationStrength: VibrationStrength,
    cardAlpha: Float = 1f
) {
    val context = LocalContext.current

    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val performFeedback: () -> Unit = {
        when (vibrationStrength) {
            VibrationStrength.OFF -> {}
            VibrationStrength.WEAK -> vibrator?.vibrate(
                VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            )
            VibrationStrength.MEDIUM -> vibrator?.vibrate(
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            )
            VibrationStrength.STRONG -> vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 60, 40, 60, 40, 60), -1)
            )
        }
    }

    val pageRows = layoutState.config.getRowsForPage(pageIndex)
    val placements = layoutState.placements.filter { it.pageIndex == pageIndex }
    val shortcuts = layoutState.shortcuts
    val placementsByRow = placements.groupBy { it.row }

    val sortedPageRows = pageRows.sortedBy { it.rowIndex }

    val rowTopPositions = remember { mutableStateMapOf<Int, Float>() }
    val rowBottomPositions = remember { mutableStateMapOf<Int, Float>() }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
    ) {
        // レイヤー1: 行のレイアウト
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sortedPageRows.forEachIndexed { index, rowConfig ->
                val rowPlacements = placementsByRow[rowConfig.rowIndex] ?: emptyList()

                val dynamicFixedHeight = rowConfig.fixedHeightDp ?: run {
                    val rowShortcuts = rowPlacements.mapNotNull { shortcuts[it.shortcutId] }
                    when {
                        rowShortcuts.any { it.type == ShortcutType.TIME_DISPLAY } -> 80
                        rowShortcuts.any { it.type == ShortcutType.DATE_DISPLAY } -> 56
                        else -> null
                    }
                }

                HomeRow(
                    rowConfig = rowConfig,
                    placements = rowPlacements,
                    shortcuts = shortcuts,
                    isEditMode = homeViewModel.isEditMode,
                    tapMode = tapMode,
                    textOnly = rowConfig.textOnly,
                    cardAlpha = cardAlpha,
                    onShortcutClick = { item ->
                        performFeedback()
                        if (showConfirmDialog) {
                            homeViewModel.showShortcutConfirmDialog(item)
                        } else {
                            homeViewModel.launchShortcut(context, item, shortcutHelper)
                        }
                    },
                    onSlotClickInEditMode = { row, column, _ ->
                        performFeedback()
                        homeViewModel.navigateToShortcutSelect(pageIndex, row, column)
                    },
                    modifier = (if (dynamicFixedHeight != null) {
                        Modifier.height(dynamicFixedHeight.dp)
                    } else {
                        Modifier.weight(1f)
                    }).onGloballyPositioned { coordinates ->
                        rowTopPositions[index] = coordinates.positionInParent().y
                        rowBottomPositions[index] = coordinates.positionInParent().y + coordinates.size.height
                    }
                )
            }
        }

        // レイヤー2: 挿入インジケータのオーバーレイ（編集モード時のみ）
        if (homeViewModel.isEditMode && sortedPageRows.isEmpty()) {
            RowInsertionIndicator(
                onClick = { homeViewModel.showAddRowDialogAction() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(1f)
            )
        }
        if (homeViewModel.isEditMode && sortedPageRows.isNotEmpty()) {
            val indicatorHalfHeight = with(density) { 10.dp.roundToPx() }

            val firstTop = rowTopPositions[0]
            if (firstTop != null) {
                RowInsertionIndicator(
                    onClick = { homeViewModel.showInsertRowDialog(sortedPageRows.first().rowIndex) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset(0, firstTop.toInt() - indicatorHalfHeight) }
                        .zIndex(1f)
                )
            }

            for (i in 1 until sortedPageRows.size) {
                val prevBottom = rowBottomPositions[i - 1]
                val currTop = rowTopPositions[i]
                if (prevBottom != null && currTop != null) {
                    val centerY = (prevBottom + currTop) / 2f
                    RowInsertionIndicator(
                        onClick = { homeViewModel.showInsertRowDialog(sortedPageRows[i].rowIndex) },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset(0, centerY.toInt() - indicatorHalfHeight) }
                            .zIndex(1f)
                    )
                }
            }

            val lastBottom = rowBottomPositions[sortedPageRows.lastIndex]
            if (lastBottom != null) {
                RowInsertionIndicator(
                    onClick = { homeViewModel.showAddRowDialogAction() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset(0, lastBottom.toInt() - indicatorHalfHeight) }
                        .zIndex(1f)
                )
            }
        }
    }
}

/**
 * 行間の挿入インジケータ（編集モード用・オーバーレイ）
 */
@Composable
private fun RowInsertionIndicator(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = "＋",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 14.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }
    }
}

// ===== 行 =====

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeRow(
    rowConfig: RowConfig,
    placements: List<ShortcutPlacement>,
    shortcuts: Map<String, ShortcutItem>,
    isEditMode: Boolean,
    tapMode: TapMode,
    textOnly: Boolean = false,
    cardAlpha: Float = 1f,
    onShortcutClick: (ShortcutItem) -> Unit,
    onSlotClickInEditMode: (row: Int, column: Int, currentShortcut: ShortcutItem?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (colIndex in 0 until rowConfig.columns) {
            val placement = placements.find { it.column == colIndex }
            val shortcut = placement?.let { shortcuts[it.shortcutId] }

            Box(modifier = Modifier.weight(1f)) {
                if (shortcut != null && shortcut.type != ShortcutType.EMPTY) {
                    when (shortcut.type) {
                        ShortcutType.DATE_DISPLAY -> {
                            DateDisplayButton(
                                isEditMode = isEditMode,
                                tapMode = tapMode,
                                cardAlpha = cardAlpha,
                                onClick = {
                                    if (isEditMode) {
                                        onSlotClickInEditMode(rowConfig.rowIndex, colIndex, shortcut)
                                    }
                                }
                            )
                        }
                        ShortcutType.TIME_DISPLAY -> {
                            TimeDisplayButton(
                                isEditMode = isEditMode,
                                tapMode = tapMode,
                                cardAlpha = cardAlpha,
                                onClick = {
                                    if (isEditMode) {
                                        onSlotClickInEditMode(rowConfig.rowIndex, colIndex, shortcut)
                                    }
                                }
                            )
                        }
                        else -> {
                            ShortcutButton(
                                item = shortcut,
                                columns = rowConfig.columns,
                                isEditMode = isEditMode,
                                tapMode = tapMode,
                                textOnly = textOnly,
                                cardAlpha = cardAlpha,
                                backgroundColor = placement?.backgroundColor,
                                textColor = placement?.textColor,
                                onClick = {
                                    if (isEditMode) {
                                        onSlotClickInEditMode(rowConfig.rowIndex, colIndex, shortcut)
                                    } else {
                                        onShortcutClick(shortcut)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    EmptySlotButton(
                        isEditMode = isEditMode,
                        tapMode = tapMode,
                        cardAlpha = cardAlpha,
                        onClick = {
                            if (isEditMode) {
                                onSlotClickInEditMode(rowConfig.rowIndex, colIndex, null)
                            }
                        }
                    )
                }
            }
        }
    }
}

// ===== ショートカットボタン =====

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutButton(
    item: ShortcutItem,
    columns: Int,
    isEditMode: Boolean,
    tapMode: TapMode,
    textOnly: Boolean = false,
    cardAlpha: Float = 1f,
    backgroundColor: String? = null,
    textColor: String? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val shortcutHelper = remember { ShortcutHelper(context) }

    val appIcon = remember(item.packageName) {
        item.packageName?.let { shortcutHelper.getAppIcon(it) }
    }

    val cardBackgroundColor = (if (backgroundColor != null) {
        try {
            Color(android.graphics.Color.parseColor(backgroundColor))
        } catch (e: Exception) {
            AppTheme.extendedColors.cardBackground
        }
    } else {
        AppTheme.extendedColors.cardBackground
    }).copy(alpha = cardAlpha)

    val labelColor = if (textColor != null) {
        try {
            Color(android.graphics.Color.parseColor(textColor))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.onSurface
        }
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .tapModeClickable(isEditMode, tapMode, onClick)
            .then(
                if (isEditMode) Modifier.border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (cardAlpha < 1f) 0.dp else 4.dp)
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val buttonHeight = maxHeight
            val buttonWidth = maxWidth

            if (textOnly) {
                val labelSize = when (columns) {
                    1 -> minOf(buttonHeight.value * 0.35f, 40f).sp
                    2 -> minOf(buttonHeight.value * 0.28f, 32f).sp
                    else -> minOf(buttonHeight.value * 0.22f, 24f).sp
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = getLocalizedLabel(item),
                        color = labelColor,
                        fontSize = labelSize,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isEditMode) {
                        Text(
                            text = stringResource(R.string.tap_to_edit),
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = minOf(buttonHeight.value * 0.12f, 14f).sp
                        )
                    }
                }
            } else {
                when (columns) {
                    1 -> {
                        val iconSize = minOf(buttonHeight * 0.65f, 80.dp)
                        val labelSize = minOf(buttonHeight.value * 0.26f, 32f).sp

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 480.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            ShortcutIcon(item = item, appIcon = appIcon, size = iconSize)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = getLocalizedLabel(item),
                                    color = labelColor,
                                    fontSize = labelSize,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isEditMode) {
                                    Text(
                                        text = stringResource(R.string.tap_to_edit),
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = minOf(buttonHeight.value * 0.14f, 16f).sp
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        val iconSize = minOf(buttonHeight * 0.55f, buttonWidth * 0.65f, 64.dp)
                        val labelSize = minOf(buttonHeight.value * 0.18f, 24f).sp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ShortcutIcon(item = item, appIcon = appIcon, size = iconSize)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = getLocalizedLabel(item),
                                color = labelColor,
                                fontSize = labelSize,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isEditMode) {
                                Text(
                                    text = stringResource(R.string.tap_to_edit),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = minOf(buttonHeight.value * 0.12f, 14f).sp
                                )
                            }
                        }
                    }
                    else -> {
                        val iconSize = minOf(buttonHeight * 0.7f, buttonWidth * 0.8f, 56.dp)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ShortcutIcon(item = item, appIcon = appIcon, size = iconSize)
                            if (isEditMode) {
                                Text(
                                    text = stringResource(R.string.tap_to_edit),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutIcon(
    item: ShortcutItem,
    appIcon: android.graphics.drawable.Drawable?,
    size: Dp
) {
    val context = LocalContext.current
    val shortcutHelper = remember { ShortcutHelper(context) }

    when (item.type) {
        ShortcutType.APP, ShortcutType.INTENT -> {
            if (appIcon != null) {
                val bitmap = remember(appIcon) { appIcon.toBitmap(128, 128) }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = item.label,
                    modifier = Modifier.size(size)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = item.label,
                    modifier = Modifier.size(size),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        ShortcutType.CALENDAR -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_calendar),
                contentDescription = stringResource(R.string.shortcut_type_calendar),
                modifier = Modifier.size(size),
                tint = AppTheme.extendedColors.iconCalendar
            )
        }
        ShortcutType.MEMO -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_memo),
                contentDescription = stringResource(R.string.shortcut_type_memo),
                modifier = Modifier.size(size),
                tint = AppTheme.extendedColors.iconMemo
            )
        }
        ShortcutType.SETTINGS -> {
            Icon(Icons.Default.Settings, stringResource(R.string.settings), Modifier.size(size), MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ShortcutType.DEVICE_SETTINGS -> {
            Icon(Icons.Default.Settings, item.label, Modifier.size(size), MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ShortcutType.PHONE -> {
            val phoneIcon = remember(item.packageName) {
                item.packageName?.let { shortcutHelper.getAppIcon(it) }
                    ?: shortcutHelper.getAppIcon("com.google.android.dialer")
                    ?: shortcutHelper.getAppIcon("com.android.dialer")
            }
            if (phoneIcon != null) {
                val bitmap = remember(phoneIcon) { phoneIcon.toBitmap(128, 128) }
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = item.label, modifier = Modifier.size(size))
            } else {
                Icon(Icons.Default.Phone, item.label, Modifier.size(size), AppTheme.extendedColors.iconCalendar)
            }
        }
        ShortcutType.SMS -> {
            val smsIcon = remember(item.packageName) {
                item.packageName?.let { shortcutHelper.getAppIcon(it) }
                    ?: shortcutHelper.getAppIcon("com.google.android.apps.messaging")
                    ?: shortcutHelper.getAppIcon("com.android.mms")
            }
            if (smsIcon != null) {
                val bitmap = remember(smsIcon) { smsIcon.toBitmap(128, 128) }
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = item.label, modifier = Modifier.size(size))
            } else {
                Icon(Icons.Default.Email, item.label, Modifier.size(size), AppTheme.extendedColors.iconMemo)
            }
        }
        ShortcutType.DIALER -> {
            val iconColor = AppTheme.extendedColors.iconDialer
            val dialerIcon = remember {
                ContextCompat.getDrawable(context, R.drawable.ic_phone_keypad)
            }
            if (dialerIcon != null) {
                val bitmap = remember(dialerIcon) { dialerIcon.toBitmap(128, 128) }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = item.label,
                    modifier = Modifier.size(size),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(iconColor)
                )
            } else {
                Icon(Icons.Default.Phone, item.label, Modifier.size(size), iconColor)
            }
        }
        ShortcutType.ALL_APPS -> {
            Icon(Icons.Default.Apps, stringResource(R.string.shortcut_type_all_apps), Modifier.size(size), AppTheme.extendedColors.iconAllApps)
        }
        ShortcutType.DATE_DISPLAY -> { /* 専用コンポーネントで表示 */ }
        ShortcutType.TIME_DISPLAY -> { /* 専用コンポーネントで表示 */ }
        ShortcutType.EMPTY -> {}
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmptySlotButton(
    isEditMode: Boolean,
    tapMode: TapMode,
    cardAlpha: Float = 1f,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .tapModeClickable(isEditMode, tapMode, onClick)
            .then(
                if (isEditMode) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = cardAlpha)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (cardAlpha < 1f) 0.dp else 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = if (isEditMode) "＋" else stringResource(R.string.empty_slot),
                fontSize = if (isEditMode) 28.sp else 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// ===== 日付・時刻表示ボタン =====

/**
 * 日付表示ボタン
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DateDisplayButton(
    isEditMode: Boolean,
    tapMode: TapMode,
    cardAlpha: Float = 1f,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { CalendarRepository(context) }

    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentDateTime = LocalDateTime.now()
            delay(60_000)
        }
    }

    val datePattern = stringResource(R.string.date_format)
    val dateFormatter = DateTimeFormatter.ofPattern(datePattern, Locale.getDefault())

    val holidayName = remember(currentDateTime.toLocalDate()) {
        repository.getHolidayName(currentDateTime.toLocalDate())
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isEditMode) Modifier
                    .tapModeClickable(true, tapMode, onClick)
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(16.dp)
                    )
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = AppTheme.extendedColors.cardBackground.copy(alpha = cardAlpha)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (cardAlpha < 1f) 0.dp else 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentDateTime.format(dateFormatter),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (holidayName != null) {
                    Text(
                        text = holidayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (isEditMode) {
                    Text(
                        text = stringResource(R.string.tap_to_edit),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

/**
 * 時計表示ボタン
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeDisplayButton(
    isEditMode: Boolean,
    tapMode: TapMode,
    cardAlpha: Float = 1f,
    onClick: () -> Unit
) {
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentDateTime = LocalDateTime.now()
            delay(1000)
        }
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isEditMode) Modifier
                    .tapModeClickable(true, tapMode, onClick)
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(16.dp)
                    )
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = AppTheme.extendedColors.cardBackground.copy(alpha = cardAlpha)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (cardAlpha < 1f) 0.dp else 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentDateTime.format(timeFormatter),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isEditMode) {
                    Text(
                        text = stringResource(R.string.tap_to_edit),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
