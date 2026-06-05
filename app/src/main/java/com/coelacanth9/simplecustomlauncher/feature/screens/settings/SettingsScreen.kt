package com.coelacanth9.simplecustomlauncher.feature.screens.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.coelacanth9.simplecustomlauncher.BuildConfig
import com.coelacanth9.simplecustomlauncher.R
import com.coelacanth9.simplecustomlauncher.data.SettingsRepository
import com.coelacanth9.simplecustomlauncher.data.TapMode
import com.coelacanth9.simplecustomlauncher.data.ThemeMode
import com.coelacanth9.simplecustomlauncher.data.VibrationStrength
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coelacanth9.simplecustomlauncher.model.PremiumSource
import com.coelacanth9.simplecustomlauncher.platform.RestoreResult
import com.coelacanth9.simplecustomlauncher.ui.components.ConfirmDialog
import com.coelacanth9.simplecustomlauncher.ui.components.DangerConfirmDialog
import com.coelacanth9.simplecustomlauncher.ui.components.InfoDialog
import com.coelacanth9.simplecustomlauncher.ui.components.PremiumFeatureDialog
import com.coelacanth9.simplecustomlauncher.ui.components.SelectionDialog
import com.coelacanth9.simplecustomlauncher.ui.components.TermsConsentDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onEnterEditMode: () -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onWallpaperSettingChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val sr = viewModel.settingsRepository

    // プレミアム状態（StateFlow で即時反映）
    val premiumStatus by viewModel.premiumStatusFlow.collectAsStateWithLifecycle()
    val isPremium = premiumStatus.isActive
    val formattedPrice = viewModel.getFormattedPrice()
    val isAdReady = viewModel.isAdReady()

    // UI 状態（SettingsRepository の値を初期値に）
    var tapMode by remember { mutableStateOf(sr.tapMode) }
    var showConfirmOnLaunch by remember { mutableStateOf(sr.showConfirmDialog) }
    var vibrationStrength by remember { mutableStateOf(sr.vibrationStrength) }
    var themeMode by remember { mutableStateOf(sr.themeMode) }
    var showSystemWallpaper by remember { mutableStateOf(sr.showSystemWallpaper) }
    var pageCount by remember { mutableStateOf(sr.pageCount) }
    var loopPagingEnabled by remember { mutableStateOf(sr.loopPagingEnabled) }

    // ダイアログフラグ
    var showTapModeDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showVibrationDialog by remember { mutableStateOf(false) }
    var showPageCountDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showTermsForPurchase by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    // バックアップ / 復元
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var restoreResultMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreResultDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                pendingRestoreUri = uri
                showRestoreConfirmDialog = true
            }
        }
    }

    // ===== ダイアログ =====

    if (showTapModeDialog) {
        val singleLabel = stringResource(R.string.tap_mode_single)
        val longLabel = stringResource(R.string.tap_mode_long)
        SelectionDialog(
            title = stringResource(R.string.select_tap_mode),
            options = listOf(TapMode.SINGLE_TAP to singleLabel, TapMode.LONG_TAP to longLabel),
            selectedOption = tapMode,
            onSelect = { mode ->
                tapMode = mode
                sr.tapMode = mode
                showTapModeDialog = false
            },
            onDismiss = { showTapModeDialog = false }
        )
    }

    if (showThemeModeDialog) {
        SelectionDialog(
            title = stringResource(R.string.select_theme),
            options = listOf(
                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                ThemeMode.LIGHT  to stringResource(R.string.theme_light),
                ThemeMode.DARK   to stringResource(R.string.theme_dark)
            ),
            selectedOption = themeMode,
            onSelect = { mode ->
                themeMode = mode
                sr.themeMode = mode
                onThemeChanged(mode)
                showThemeModeDialog = false
            },
            onDismiss = { showThemeModeDialog = false }
        )
    }

    if (showVibrationDialog) {
        SelectionDialog(
            title = stringResource(R.string.select_vibration_strength),
            options = listOf(
                VibrationStrength.OFF    to stringResource(R.string.vibration_off),
                VibrationStrength.WEAK   to stringResource(R.string.vibration_weak),
                VibrationStrength.MEDIUM to stringResource(R.string.vibration_medium),
                VibrationStrength.STRONG to stringResource(R.string.vibration_strong)
            ),
            selectedOption = vibrationStrength,
            onSelect = { strength ->
                vibrationStrength = strength
                sr.vibrationStrength = strength
                showVibrationDialog = false
            },
            onDismiss = { showVibrationDialog = false }
        )
    }

    if (showPageCountDialog) {
        val pageOptions = (1..SettingsRepository.MAX_PAGES).map { n ->
            n to stringResource(R.string.page_count_format, n)
        }
        SelectionDialog(
            title = stringResource(R.string.select_page_count),
            options = pageOptions,
            selectedOption = pageCount,
            onSelect = { count ->
                pageCount = count
                sr.pageCount = count
                showPageCountDialog = false
            },
            onDismiss = { showPageCountDialog = false }
        )
    }

    if (showPremiumDialog) {
        PremiumFeatureDialog(
            description = stringResource(R.string.premium_page_only),
            formattedPrice = formattedPrice,
            isAdReady = isAdReady,
            onWatchAd = {
                activity?.let { viewModel.showRewardedAd(it) { viewModel.premiumManager.recordAdWatch() } }
                showPremiumDialog = false
            },
            onPurchase = {
                if (sr.termsAccepted) {
                    activity?.let { viewModel.launchPurchase(it) }
                    showPremiumDialog = false
                } else {
                    showTermsForPurchase = true
                }
            },
            onDismiss = { showPremiumDialog = false }
        )
    }

    if (showTermsForPurchase) {
        TermsConsentDialog(
            required = true,
            onAgree = {
                sr.termsAccepted = true
                showTermsForPurchase = false
                showPremiumDialog = false
                activity?.let { viewModel.launchPurchase(it) }
            },
            onDismiss = { showTermsForPurchase = false }
        )
    }

    if (showTermsDialog) {
        TermsConsentDialog(
            required = false,
            onAgree = {
                sr.termsAccepted = true
                showTermsDialog = false
            },
            onDismiss = { showTermsDialog = false }
        )
    }

    if (showResetDialog) {
        DangerConfirmDialog(
            title = stringResource(R.string.reset_to_default),
            message = stringResource(R.string.reset_confirm_message),
            confirmText = stringResource(R.string.reset_to_default_short),
            onConfirm = { viewModel.resetToDefault(); showResetDialog = false },
            onDismiss = { showResetDialog = false }
        )
    }

    if (showClearDialog) {
        DangerConfirmDialog(
            title = stringResource(R.string.clear_layout),
            message = stringResource(R.string.clear_confirm_message),
            confirmText = stringResource(R.string.delete),
            onConfirm = { viewModel.clearLayout(); showClearDialog = false },
            onDismiss = { showClearDialog = false }
        )
    }

    if (showRestoreConfirmDialog) {
        ConfirmDialog(
            title = stringResource(R.string.restore_confirm_title),
            message = stringResource(R.string.restore_confirm_message),
            confirmText = stringResource(R.string.restore),
            onConfirm = {
                pendingRestoreUri?.let { uri ->
                    val result = viewModel.restoreFromUri(uri)
                    restoreResultMessage = when (result) {
                        is RestoreResult.Success -> context.getString(
                            R.string.restore_success, result.shortcutCount, result.pageCount
                        )
                        is RestoreResult.Error -> "${context.getString(R.string.restore_error)}: ${result.message}"
                    }
                    if (result is RestoreResult.Success) {
                        // 設定値を再読み込みして UI を同期
                        tapMode = sr.tapMode
                        showConfirmOnLaunch = sr.showConfirmDialog
                        vibrationStrength = sr.vibrationStrength
                        pageCount = sr.pageCount
                        loopPagingEnabled = sr.loopPagingEnabled
                        themeMode = sr.themeMode
                        onThemeChanged(sr.themeMode)
                        showSystemWallpaper = sr.showSystemWallpaper
                        onWallpaperSettingChanged(sr.showSystemWallpaper)
                    }
                    showRestoreResultDialog = true
                }
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            onDismiss = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            }
        )
    }

    if (showRestoreResultDialog) {
        InfoDialog(
            title = stringResource(R.string.restore_confirm_title),
            message = restoreResultMessage ?: "",
            onDismiss = { showRestoreResultDialog = false }
        )
    }

    // ===== Scaffold =====

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_settings),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ホームアプリ設定
            item {
                SettingsActionItem(
                    title = stringResource(R.string.home_app_settings),
                    description = stringResource(R.string.set_as_default_home),
                    onClick = { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // 起動操作・振動
            item {
                val label = when (tapMode) {
                    TapMode.SINGLE_TAP -> stringResource(R.string.tap_mode_single)
                    TapMode.LONG_TAP   -> stringResource(R.string.tap_mode_long)
                }
                SettingsSelectItem(
                    title = stringResource(R.string.tap_mode),
                    description = stringResource(R.string.tap_mode_desc),
                    currentValue = label,
                    onClick = { showTapModeDialog = true }
                )
            }

            if (tapMode == TapMode.LONG_TAP) {
                item {
                    SettingsSwitchItem(
                        title = stringResource(R.string.confirm_before_launch),
                        description = stringResource(R.string.confirm_before_launch_desc),
                        checked = showConfirmOnLaunch,
                        onCheckedChange = {
                            showConfirmOnLaunch = it
                            sr.showConfirmDialog = it
                        }
                    )
                }
            }

            item {
                val label = when (vibrationStrength) {
                    VibrationStrength.OFF    -> stringResource(R.string.vibration_off)
                    VibrationStrength.WEAK   -> stringResource(R.string.vibration_weak)
                    VibrationStrength.MEDIUM -> stringResource(R.string.vibration_medium)
                    VibrationStrength.STRONG -> stringResource(R.string.vibration_strong)
                }
                SettingsSelectItem(
                    title = stringResource(R.string.vibration_strength),
                    description = stringResource(R.string.vibration_strength_desc),
                    currentValue = label,
                    onClick = { showVibrationDialog = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // レイアウト・表示設定
            item {
                SettingsActionItem(
                    title = stringResource(R.string.layout_edit),
                    description = stringResource(R.string.edit_layout_desc),
                    onClick = { onEnterEditMode(); onBack() }
                )
            }

            item {
                val label = when (themeMode) {
                    ThemeMode.LIGHT  -> stringResource(R.string.theme_light)
                    ThemeMode.DARK   -> stringResource(R.string.theme_dark)
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                }
                SettingsSelectItem(
                    title = stringResource(R.string.theme),
                    description = stringResource(R.string.change_theme_desc),
                    currentValue = label,
                    onClick = { showThemeModeDialog = true }
                )
            }

            item {
                SettingsSwitchItem(
                    title = stringResource(R.string.show_system_wallpaper),
                    description = stringResource(R.string.show_system_wallpaper_desc),
                    checked = showSystemWallpaper,
                    onCheckedChange = {
                        showSystemWallpaper = it
                        sr.showSystemWallpaper = it
                        onWallpaperSettingChanged(it)
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // プレミアム購入・広告（未購入時）
            if (!isPremium) {
                item {
                    SettingsActionItem(
                        title = if (formattedPrice != null)
                            stringResource(R.string.purchase_with_price, formattedPrice)
                        else
                            stringResource(R.string.purchase_unlock),
                        description = stringResource(R.string.premium_unlock_purchase_desc),
                        onClick = {
                            if (sr.termsAccepted) {
                                activity?.let { viewModel.launchPurchase(it) }
                            } else {
                                showTermsForPurchase = true
                            }
                        }
                    )
                }

                if (isAdReady) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    item {
                        SettingsActionItem(
                            title = stringResource(R.string.watch_ad_unlock),
                            description = stringResource(R.string.premium_unlock_ad_desc),
                            onClick = {
                                activity?.let {
                                    viewModel.showRewardedAd(it) { viewModel.premiumManager.recordAdWatch() }
                                }
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }
            }

            // ページ設定（Premium）
            item {
                SettingsPremiumSelectItem(
                    title = stringResource(R.string.home_page_count),
                    description = if (isPremium) stringResource(R.string.multi_page_swipe)
                                  else stringResource(R.string.premium_multi_page),
                    currentValue = if (isPremium) stringResource(R.string.page_count_format, pageCount) else null,
                    isPremiumActive = isPremium,
                    onClick = { if (isPremium) showPageCountDialog = true else showPremiumDialog = true }
                )
            }

            item {
                SettingsPremiumSwitchItem(
                    title = stringResource(R.string.page_loop),
                    description = stringResource(R.string.page_loop_desc),
                    checked = loopPagingEnabled,
                    enabled = isPremium,
                    onCheckedChange = {
                        loopPagingEnabled = it
                        sr.loopPagingEnabled = it
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // バックアップ / 復元（Premium）
            item {
                SettingsPremiumItem(
                    title = stringResource(R.string.export_backup),
                    description = stringResource(R.string.export_backup_desc),
                    isPremiumActive = isPremium,
                    onClick = {
                        if (isPremium) {
                            val intent = Intent.createChooser(
                                viewModel.createBackupShareIntent(),
                                context.getString(R.string.backup_share_title)
                            )
                            context.startActivity(intent)
                        } else {
                            showPremiumDialog = true
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                SettingsPremiumItem(
                    title = stringResource(R.string.import_backup),
                    description = stringResource(R.string.import_backup_desc),
                    isPremiumActive = isPremium,
                    onClick = {
                        if (isPremium) {
                            filePickerLauncher.launch(
                                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "*/*"
                                }
                            )
                        } else {
                            showPremiumDialog = true
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 復元エリア（常に表示）
            item {
                when {
                    PremiumSource.ONE_TIME_PURCHASE in premiumStatus.activeSources ->
                        SettingsInfoItem(
                            title = stringResource(R.string.premium_status_permanent),
                            value = ""
                        )
                    PremiumSource.AD_WATCH in premiumStatus.activeSources ->
                        SettingsInfoItem(
                            title = stringResource(R.string.premium_status_ad),
                            value = ""
                        )
                    else ->
                        SettingsLinkItem(
                            title = stringResource(R.string.purchase_restored),
                            description = stringResource(R.string.billing_unavailable),
                            onClick = { viewModel.restorePurchases() }
                        )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // 危険操作
            item {
                SettingsDangerItem(
                    title = stringResource(R.string.reset_to_default),
                    description = stringResource(R.string.reset_layout_desc),
                    onClick = { showResetDialog = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // リンク
            item {
                SettingsLinkItem(
                    title = stringResource(R.string.how_to_use),
                    description = stringResource(R.string.how_to_use_desc),
                    onClick = {
                        CustomTabsIntent.Builder().build()
                            .launchUrl(context, Uri.parse("https://coelacanth9.github.io/SimpleCustomLauncher/"))
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                SettingsLinkItem(
                    title = stringResource(R.string.terms_of_service),
                    description = stringResource(R.string.terms_of_service_desc),
                    onClick = {
                        if (sr.termsAccepted) {
                            CustomTabsIntent.Builder().build()
                                .launchUrl(context, Uri.parse("https://coelacanth9.github.io/SimpleCustomLauncher/TERMS_OF_SERVICE"))
                        } else {
                            showTermsDialog = true
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                SettingsLinkItem(
                    title = stringResource(R.string.privacy_policy),
                    description = stringResource(R.string.privacy_policy_desc),
                    onClick = {
                        CustomTabsIntent.Builder().build()
                            .launchUrl(context, Uri.parse("https://coelacanth9.github.io/SimpleCustomLauncher/PRIVACY_POLICY"))
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                SettingsLinkItem(
                    title = stringResource(R.string.oss_licenses),
                    description = stringResource(R.string.oss_licenses_desc),
                    onClick = {
                        context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // バージョン情報
            item {
                val buildType = if (BuildConfig.DEBUG) "debug" else "release"
                SettingsInfoItem(
                    title = stringResource(R.string.version),
                    value = "${BuildConfig.VERSION_NAME} ($buildType)"
                )
            }

            // デバッグセクション（DEBUG ビルドのみ）
            if (BuildConfig.DEBUG) {
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

                item {
                    Text(
                        text = "Debug Options",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                item {
                    SettingsSwitchItem(
                        title = "Debug Premium",
                        description = "強制的にプレミアム状態にする",
                        checked = viewModel.isDebugPremiumEnabled(),
                        onCheckedChange = { viewModel.setDebugPremium(it) }
                    )
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }

                item {
                    SettingsDangerItem(
                        title = "Clear All Premium",
                        description = "全てのプレミアム状態をクリア",
                        onClick = { viewModel.clearAllPremiumStatus() }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ===== 設定項目 UI コンポーネント =====

@Composable
private fun SettingsSelectItem(
    title: String,
    description: String,
    currentValue: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(text = description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = currentValue, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (enabled) Modifier.clickable { onCheckedChange(!checked) } else Modifier
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = description, fontSize = 14.sp,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

@Composable
private fun SettingsPremiumSelectItem(
    title: String,
    description: String,
    currentValue: String?,
    isPremiumActive: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isPremiumActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isPremiumActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = contentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Premium", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
                Text(text = description, fontSize = 14.sp, color = contentColor.copy(alpha = 0.8f))
            }
            if (currentValue != null) {
                Text(text = currentValue, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SettingsPremiumSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (enabled) Modifier.clickable { onCheckedChange(!checked) } else Modifier
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Premium", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
                Text(
                    text = description, fontSize = 14.sp,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

@Composable
private fun SettingsPremiumItem(
    title: String,
    description: String,
    isPremiumActive: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isPremiumActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isPremiumActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = contentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Premium", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
                Text(text = description, fontSize = 14.sp, color = contentColor.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun SettingsInfoItem(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(text = value, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsActionItem(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun SettingsDangerItem(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(text = description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun SettingsLinkItem(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(text = description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
