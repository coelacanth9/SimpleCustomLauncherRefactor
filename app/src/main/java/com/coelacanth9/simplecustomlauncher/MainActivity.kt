package com.coelacanth9.simplecustomlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.coelacanth9.simplecustomlauncher.core.navigation.NavDestination
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutItem
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutType
import com.coelacanth9.simplecustomlauncher.data.BackupManager
import com.coelacanth9.simplecustomlauncher.data.SettingsRepository
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.data.ThemeMode
import com.coelacanth9.simplecustomlauncher.feature.launcher.home.HomeScreen
import com.coelacanth9.simplecustomlauncher.feature.launcher.home.HomeViewModel
import com.coelacanth9.simplecustomlauncher.feature.launcher.home.PremiumViewModel
import com.coelacanth9.simplecustomlauncher.feature.launcher.shortcutselect.ShortcutSelectScreen
import com.coelacanth9.simplecustomlauncher.feature.launcher.shortcutselect.ShortcutSelectViewModel
import com.coelacanth9.simplecustomlauncher.feature.screens.allapps.AllAppsScreen
import com.coelacanth9.simplecustomlauncher.feature.screens.calendar.CalendarScreen
import com.coelacanth9.simplecustomlauncher.feature.screens.memo.MemoScreen
import com.coelacanth9.simplecustomlauncher.feature.screens.settings.SettingsScreen
import com.coelacanth9.simplecustomlauncher.feature.screens.settings.SettingsViewModel
import com.coelacanth9.simplecustomlauncher.platform.CalendarRepository
import com.coelacanth9.simplecustomlauncher.platform.PermissionManager
import com.coelacanth9.simplecustomlauncher.platform.PermissionManager.CALENDAR_PERMISSIONS
import com.coelacanth9.simplecustomlauncher.platform.RequestPermissions
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import com.coelacanth9.simplecustomlauncher.platform.ads.AdManager
import com.coelacanth9.simplecustomlauncher.platform.billing.BillingManager
import com.coelacanth9.simplecustomlauncher.platform.billing.DefaultPremiumManager
import com.coelacanth9.simplecustomlauncher.platform.billing.PurchaseState
import com.coelacanth9.simplecustomlauncher.ui.components.LargeConfirmDialog
import com.coelacanth9.simplecustomlauncher.ui.components.TermsConsentDialog
import com.coelacanth9.simplecustomlauncher.ui.theme.SimpleCustomLauncherTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDate
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var shortcutRepository: ShortcutRepository
    private lateinit var billingManager: BillingManager
    private lateinit var adManager: AdManager

    /** ホームジェスチャーで HOME intent を受けたことを Compose 側に通知 */
    private val _homeIntent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homeIntent: SharedFlow<Unit> = _homeIntent.asSharedFlow()

    /** アプリアンインストール通知を Compose 側に送信 */
    private val _packageRemoved = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val packageRemoved: SharedFlow<String> = _packageRemoved.asSharedFlow()

    private val packageRemovedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_PACKAGE_REMOVED) return
            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return  // 更新時は無視
            val packageName = intent.data?.schemeSpecificPart ?: return
            Log.d(TAG, "Package removed: $packageName")
            _packageRemoved.tryEmit(packageName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // タブレット以外は縦固定
        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        if (!isTablet) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        shortcutRepository = ShortcutRepository(this)

        // BillingManager 初期化（購入完了処理は Compose 側の LaunchedEffect が担う）
        billingManager = BillingManager(
            context = this,
            onPurchaseComplete = {},  // Compose 側が PurchaseState を監視して処理
            onPurchaseCleared = {}    // onResume の refreshPremiumStatus() でカバー
        )
        billingManager.initialize()

        adManager = AdManager(this)
        adManager.initialize()

        // アプリアンインストール検知
        registerReceiver(
            packageRemovedReceiver,
            IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply { addDataScheme("package") }
        )

        // 初回起動時にデフォルトレイアウトを適用
        if (shortcutRepository.isFirstLaunch()) {
            shortcutRepository.applyDefaultLayout()
        }

        handleIntent(intent)

        setContent {
            val settingsRepository = remember { SettingsRepository(this@MainActivity) }
            var themeMode by remember { mutableStateOf(settingsRepository.themeMode) }
            val showWallpaper = remember { mutableStateOf(settingsRepository.showSystemWallpaper) }

            SimpleCustomLauncherTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    color = if (showWallpaper.value) Color.Transparent
                            else MaterialTheme.colorScheme.background
                ) {
                    MainLauncherScreen(
                        shortcutRepository = shortcutRepository,
                        billingManager = billingManager,
                        adManager = adManager,
                        onThemeChanged = { themeMode = it },
                        onWallpaperSettingChanged = { showWallpaper.value = it }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        // ホームジェスチャーでの intent を検知して Compose 側に通知
        if (intent.action == Intent.ACTION_MAIN &&
            intent.categories?.contains(Intent.CATEGORY_HOME) == true) {
            _homeIntent.tryEmit(Unit)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageRemovedReceiver)
        billingManager.endConnection()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        Log.d(TAG, "handleIntent: action=${intent.action}")
        if (intent.hasExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST)) {
            handlePinShortcut(intent)
        }
    }

    private fun handlePinShortcut(intent: Intent) {
        val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
        val request = launcherApps.getPinItemRequest(intent)
        if (request == null) {
            Log.e(TAG, "PinItemRequest is null")
            return
        }
        if (request.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            val shortcutInfo = request.shortcutInfo ?: return
            Log.d(TAG, "Pin shortcut: ${shortcutInfo.shortLabel}, package: ${shortcutInfo.`package`}")
            val item = ShortcutItem(
                id = UUID.randomUUID().toString(),
                type = ShortcutType.INTENT,
                label = shortcutInfo.shortLabel?.toString() ?: getString(R.string.shortcut),
                packageName = shortcutInfo.`package`
            )
            shortcutRepository.savePinShortcutInfo(item.id, shortcutInfo.id, shortcutInfo.`package`)
            shortcutRepository.saveShortcut(item)
            request.accept()
            Toast.makeText(
                this,
                getString(R.string.item_added_to_storage, item.label),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

// =============================================================================

@Composable
fun MainLauncherScreen(
    shortcutRepository: ShortcutRepository,
    billingManager: BillingManager,
    adManager: AdManager,
    onThemeChanged: (ThemeMode) -> Unit = {},
    onWallpaperSettingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val mainActivity = activity as? MainActivity

    // ViewModel
    val homeViewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val settingsRepo = SettingsRepository(context)
                HomeViewModel(
                    shortcutRepository = shortcutRepository,
                    settingsRepository = settingsRepo,
                    calendarRepository = CalendarRepository(context),
                    premiumManager = DefaultPremiumManager(context, settingsRepo),
                    billingManager = billingManager,
                    adManager = adManager
                )
            }
        }
    )
    val premiumViewModel: PremiumViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PremiumViewModel(billingManager, adManager) }
        }
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val settingsRepo = SettingsRepository(context)
                SettingsViewModel(
                    settingsRepository = settingsRepo,
                    premiumManager = DefaultPremiumManager(context, settingsRepo),
                    shortcutRepository = shortcutRepository,
                    backupManager = BackupManager(context),
                    billingManager = billingManager,
                    adManager = adManager
                )
            }
        }
    )

    // 起動時に孤立ピンショートカットをクリーンアップ
    LaunchedEffect(Unit) {
        homeViewModel.cleanupOrphanedPinShortcuts(context)
    }

    // ホームジェスチャーでホーム画面に戻る
    LaunchedEffect(Unit) {
        mainActivity?.homeIntent?.collect {
            homeViewModel.navigateToHome()
        }
    }

    // アプリアンインストール検知
    LaunchedEffect(Unit) {
        mainActivity?.packageRemoved?.collect { packageName ->
            homeViewModel.onPackageRemoved(packageName)
        }
    }

    // 購入完了を監視
    val purchaseState by (premiumViewModel.billingPurchaseState?.collectAsState()
        ?: remember { mutableStateOf(PurchaseState.Idle) })
    LaunchedEffect(purchaseState) {
        if (purchaseState == PurchaseState.Purchased) {
            homeViewModel.onPurchaseCompleted()
        }
    }

    // ライフサイクル監視（onResume でプレミアム状態を再チェック）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.refreshPremiumStatus()
                homeViewModel.cleanupUninstalledPackages(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // カレンダー権限
    var hasPermission by remember {
        mutableStateOf(PermissionManager.checkPermissions(context, CALENDAR_PERMISSIONS))
    }
    val settingsRepository = remember { SettingsRepository(context) }
    var permissionHandled by remember { mutableStateOf(false) }
    var showWelcomeDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    RequestPermissions(
        context = context,
        permissions = CALENDAR_PERMISSIONS,
        onResult = { isGranted ->
            hasPermission = isGranted
            permissionHandled = true
        }
    )

    // 権限処理完了後に初回案内・利用規約を表示
    LaunchedEffect(permissionHandled) {
        if (permissionHandled) {
            when {
                !settingsRepository.onboardingShown -> showWelcomeDialog = true
                !settingsRepository.termsShown -> showTermsDialog = true
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // 初回起動時の使い方案内ダイアログ
    if (showWelcomeDialog) {
        LargeConfirmDialog(
            title = context.getString(R.string.welcome_title),
            message = context.getString(R.string.welcome_message),
            confirmText = context.getString(R.string.open_how_to_use),
            cancelText = context.getString(R.string.close),
            onConfirm = {
                settingsRepository.onboardingShown = true
                showWelcomeDialog = false
                CustomTabsIntent.Builder().build()
                    .launchUrl(context, Uri.parse("https://coelacanth9.github.io/SimpleCustomLauncher/"))
                if (!settingsRepository.termsShown) showTermsDialog = true
            },
            onDismiss = {
                settingsRepository.onboardingShown = true
                showWelcomeDialog = false
                if (!settingsRepository.termsShown) showTermsDialog = true
            }
        )
    }

    // 利用規約同意ダイアログ（初回起動時: スキップ可能）
    if (showTermsDialog) {
        TermsConsentDialog(
            required = false,
            onAgree = {
                settingsRepository.termsAccepted = true
                settingsRepository.termsShown = true
                showTermsDialog = false
            },
            onDismiss = {
                settingsRepository.termsShown = true
                showTermsDialog = false
            }
        )
    }

    // BackHandler（ランチャーなのでバックを常に消費）
    BackHandler(enabled = true) {
        when {
            homeViewModel.navDestination !is NavDestination.Home ->
                homeViewModel.navigateToHome()
            homeViewModel.isEditMode ->
                homeViewModel.exitEditMode()
            homeViewModel.currentPageIndex > 0 ->
                homeViewModel.navigateToPage(0)
            // 1ページ目のホーム → バックを消費して終了を防ぐ
        }
    }

    // NavHost: navDestination に応じて画面を差し替え
    when (val dest = homeViewModel.navDestination) {
        is NavDestination.Home -> {
            HomeScreen(
                homeViewModel = homeViewModel,
                premiumViewModel = premiumViewModel,
                snackbarHostState = snackbarHostState
            )
        }
        is NavDestination.ShortcutSelect -> {
            val ssViewModel: ShortcutSelectViewModel = viewModel(
                key = "shortcut_${dest.pageIndex}_${dest.row}_${dest.column}",
                factory = viewModelFactory {
                    initializer {
                        val settingsRepo = SettingsRepository(context)
                        ShortcutSelectViewModel(
                            shortcutRepository = shortcutRepository,
                            shortcutHelper = ShortcutHelper(context),
                            premiumManager = DefaultPremiumManager(context, settingsRepo),
                            targetPageIndex = dest.pageIndex,
                            targetRow = dest.row,
                            targetColumn = dest.column
                        )
                    }
                }
            )
            ShortcutSelectScreen(
                viewModel = ssViewModel,
                onBack = { homeViewModel.navigateToHome() }
            )
        }
        is NavDestination.Settings -> {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { homeViewModel.navigateToHome() },
                onEnterEditMode = { homeViewModel.enterEditMode() },
                onThemeChanged = onThemeChanged,
                onWallpaperSettingChanged = onWallpaperSettingChanged
            )
        }
        is NavDestination.Calendar -> {
            val now = LocalDate.now()
            CalendarScreen(
                hasPermission = hasPermission,
                holidayMap = remember(hasPermission) {
                    homeViewModel.getHolidaysForMonth(now.year, now.monthValue, hasPermission)
                },
                onBack = { homeViewModel.navigateToHome() }
            )
        }
        is NavDestination.Memo -> {
            MemoScreen(onBack = { homeViewModel.navigateToHome() })
        }
        is NavDestination.AllApps -> {
            AllAppsScreen(
                onBack = { homeViewModel.navigateToHome() },
                packageRemovedFlow = mainActivity?.packageRemoved
            )
        }
    }
}
