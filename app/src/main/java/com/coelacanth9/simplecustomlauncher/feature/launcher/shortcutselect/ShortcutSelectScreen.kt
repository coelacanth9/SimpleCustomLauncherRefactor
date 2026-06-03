package com.coelacanth9.simplecustomlauncher.feature.launcher.shortcutselect

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.ContactsContract
import com.coelacanth9.simplecustomlauncher.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.coelacanth9.simplecustomlauncher.model.RAKUTEN_LINK_PACKAGE
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import com.coelacanth9.simplecustomlauncher.model.isLinkRelated
import com.coelacanth9.simplecustomlauncher.platform.AppInfo
import com.coelacanth9.simplecustomlauncher.platform.ShortcutData
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import com.coelacanth9.simplecustomlauncher.platform.isRakutenLinkInstalled
import com.coelacanth9.simplecustomlauncher.ui.components.ColumnOptionCard
import com.coelacanth9.simplecustomlauncher.ui.components.ContactTypeDialog
import com.coelacanth9.simplecustomlauncher.ui.components.CustomContentDialog
import com.coelacanth9.simplecustomlauncher.ui.components.InfoDialog
import com.coelacanth9.simplecustomlauncher.ui.components.LargeDangerConfirmDialog
import com.coelacanth9.simplecustomlauncher.ui.components.RowDeleteConfirmDialog
import com.coelacanth9.simplecustomlauncher.ui.theme.AppTheme

// ============ 画面状態 ============

private sealed class SelectScreenState {
    object Main : SelectScreenState()
    object AppList : SelectScreenState()
    data class AppShortcuts(val app: AppInfo) : SelectScreenState()
}

// ============ アプリ内機能定義 ============

private data class InternalFeature(
    val type: ShortcutType,
    val labelResId: Int,
    val hasIcon: Boolean
)

private val internalFeatures = listOf(
    InternalFeature(ShortcutType.CALENDAR,       R.string.shortcut_type_calendar, true),
    InternalFeature(ShortcutType.MEMO,           R.string.shortcut_type_memo,     true),
    InternalFeature(ShortcutType.DIALER,         R.string.shortcut_type_phone,    true),
    InternalFeature(ShortcutType.ALL_APPS,       R.string.shortcut_type_all_apps, true),
    InternalFeature(ShortcutType.DATE_DISPLAY,   R.string.shortcut_type_date,     false),
    InternalFeature(ShortcutType.TIME_DISPLAY,   R.string.shortcut_type_time,     false),
    InternalFeature(ShortcutType.DEVICE_SETTINGS,R.string.shortcut_type_device_settings, true),
)

// ============ 端末設定項目 ============

private data class DeviceSettingsItem(val action: String, val labelResId: Int)

private val deviceSettingsList = listOf(
    DeviceSettingsItem("android.settings.SETTINGS",          R.string.device_settings_main),
    DeviceSettingsItem("android.settings.WIFI_SETTINGS",     R.string.device_settings_wifi),
    DeviceSettingsItem("android.settings.BLUETOOTH_SETTINGS",R.string.device_settings_bluetooth),
    DeviceSettingsItem("#component:com.android.settings/com.android.settings.Settings\$TetherSettingsActivity", R.string.device_settings_tethering),
    DeviceSettingsItem("android.settings.DISPLAY_SETTINGS",  R.string.device_settings_display),
    DeviceSettingsItem("android.settings.SOUND_SETTINGS",    R.string.device_settings_sound),
    DeviceSettingsItem("android.settings.LOCATION_SOURCE_SETTINGS", R.string.device_settings_location),
    DeviceSettingsItem("android.settings.APPLICATION_SETTINGS", R.string.device_settings_apps),
    DeviceSettingsItem("android.settings.BATTERY_SAVER_SETTINGS", R.string.device_settings_battery),
    DeviceSettingsItem("android.settings.AIRPLANE_MODE_SETTINGS", R.string.device_settings_airplane),
)

// ============ 色セット ============

private data class ColorSet(val backgroundColor: String, val textColor: String, val name: String)

private val slotColorPalette = listOf(
    ColorSet("#E57373", "#FFFFFF", "Red"),
    ColorSet("#FFB74D", "#000000", "Orange"),
    ColorSet("#FFF176", "#000000", "Yellow"),
    ColorSet("#81C784", "#000000", "Green"),
    ColorSet("#4FC3F7", "#000000", "Light Blue"),
    ColorSet("#64B5F6", "#FFFFFF", "Blue"),
    ColorSet("#BA68C8", "#FFFFFF", "Purple"),
    ColorSet("#F06292", "#FFFFFF", "Pink"),
)

// ============ 連絡先ピッカー ============

private data class ContactInfo(val name: String, val phoneNumber: String)

private data class ContactPickerState(
    val selectedContact: ContactInfo?,
    val showDialog: Boolean,
    val startPicker: () -> Unit,
    val dismissDialog: () -> Unit
)

@Composable
private fun rememberContactPicker(onContactSelected: (ContactInfo) -> Unit): ContactPickerState {
    val context = LocalContext.current
    var selectedContact by remember { mutableStateOf<ContactInfo?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            val info = getContactInfo(context, it)
            if (info != null) {
                selectedContact = info
                showDialog = true
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) contactPickerLauncher.launch(null)
    }

    val startPicker: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            contactPickerLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    return ContactPickerState(
        selectedContact = selectedContact,
        showDialog = showDialog,
        startPicker = startPicker,
        dismissDialog = { showDialog = false }
    )
}

// ============ アプリ優先順位 ============

private val priorityAppPackages = listOf(
    "com.android.dialer", "com.google.android.dialer",
    "com.android.contacts", "com.google.android.contacts",
    "jp.naver.line.android",
    "com.android.messaging", "com.google.android.apps.messaging",
    "com.google.android.gm",
    "com.android.camera", "com.android.camera2", "com.google.android.GoogleCamera",
    "com.google.android.apps.photos",
    "com.google.android.apps.maps",
    "com.android.chrome",
    "com.google.android.googlequicksearchbox",
    "com.google.android.calendar",
    "com.google.android.calculator",
    "com.google.android.youtube",
    "com.amazon.mShop.android.shopping",
    "com.android.vending",
)

private fun isPriorityApp(packageName: String): Boolean =
    priorityAppPackages.any { packageName == it || packageName.startsWith("$it.") }

private fun getPriorityIndex(packageName: String): Int {
    val index = priorityAppPackages.indexOfFirst {
        packageName == it || packageName.startsWith("$it.")
    }
    return if (index >= 0) index else Int.MAX_VALUE
}

// ============ メイン画面 ============

/**
 * ショートカット選択・配置画面。
 * 旧 ShortcutAddScreen + SlotEditScreen を統合。
 * 選択結果は ShortcutRepository に書き込み、HomeViewModel が StateFlow 経由で受信する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutSelectScreen(
    viewModel: ShortcutSelectViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // layoutState を収集して派生ステートを計算
    val layoutState by viewModel.layoutState.collectAsState()

    val currentShortcut = remember(layoutState) {
        val shortcutId = layoutState.placements.find {
            it.pageIndex == viewModel.targetPageIndex &&
            it.row == viewModel.targetRow &&
            it.column == viewModel.targetColumn
        }?.shortcutId
        shortcutId?.let { layoutState.shortcuts[it] }
    }

    val currentPlacement = remember(layoutState) {
        layoutState.placements.find {
            it.pageIndex == viewModel.targetPageIndex &&
            it.row == viewModel.targetRow &&
            it.column == viewModel.targetColumn
        }
    }

    val currentRowConfig = remember(layoutState) {
        layoutState.config.rows.find {
            it.pageIndex == viewModel.targetPageIndex && it.rowIndex == viewModel.targetRow
        }
    }

    val unplacedShortcuts = remember(layoutState) {
        val placedIds = layoutState.placements.map { it.shortcutId }.toSet()
        layoutState.shortcuts.values
            .filter { it.id !in placedIds && it.type != ShortcutType.EMPTY }
            .sortedBy { it.label }
    }

    val currentColumns = currentRowConfig?.columns ?: 2
    val currentTextOnly = currentRowConfig?.textOnly ?: false
    val currentBackgroundColor = currentPlacement?.backgroundColor
    val currentTextColor = currentPlacement?.textColor

    val hasLinkShortcutsInRow = remember(layoutState) {
        layoutState.placements
            .filter { it.pageIndex == viewModel.targetPageIndex && it.row == viewModel.targetRow }
            .any { placement -> layoutState.shortcuts[placement.shortcutId]?.let { isLinkRelated(it) } ?: false }
    }

    // Link判定
    val isLinkInstalled = remember { context.packageManager.isRakutenLinkInstalled() }
    val isLinkAvailable = isLinkInstalled && currentColumns < 3

    // 画面遷移ステート
    var screenState by remember { mutableStateOf<SelectScreenState>(SelectScreenState.Main) }

    // ダイアログ表示フラグ
    var showDeviceSettingsDialog by remember { mutableStateOf(false) }
    var showColumnsDialog by remember { mutableStateOf(false) }
    var showDeleteRowDialog by remember { mutableStateOf(false) }
    var showClearSlotDialog by remember { mutableStateOf(false) }
    var showPremiumInfoDialog by remember { mutableStateOf(false) }
    var pendingColumnChange by remember { mutableStateOf(0) }
    var shortcutToDelete by remember { mutableStateOf<ShortcutItem?>(null) }
    var linkSmsError by remember { mutableStateOf<String?>(null) }

    // 連絡先ピッカー
    val contactPicker = rememberContactPicker { /* not used */ }

    // ===== ダイアログ =====

    linkSmsError?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { linkSmsError = null },
            title = { Text(stringResource(R.string.link_sms)) },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { linkSmsError = null }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    if (showDeviceSettingsDialog) {
        DeviceSettingsPickerDialog(
            onSelect = { item ->
                viewModel.placeDeviceSettings(context.getString(item.labelResId), item.action)
                showDeviceSettingsDialog = false
                onBack()
            },
            onDismiss = { showDeviceSettingsDialog = false }
        )
    }

    shortcutToDelete?.let { shortcut ->
        LargeDangerConfirmDialog(
            title = stringResource(R.string.delete_unplaced_shortcut),
            message = stringResource(R.string.delete_unplaced_confirm),
            onConfirm = { viewModel.deleteUnplacedShortcut(shortcut); shortcutToDelete = null },
            onDismiss = { shortcutToDelete = null }
        )
    }

    if (showDeleteRowDialog) {
        RowDeleteConfirmDialog(
            onConfirm = { showDeleteRowDialog = false; viewModel.deleteRow(); onBack() },
            onDismiss = { showDeleteRowDialog = false }
        )
    }

    if (showClearSlotDialog) {
        LargeDangerConfirmDialog(
            title = stringResource(R.string.clear_slot),
            message = stringResource(R.string.clear_slot_warning),
            confirmText = stringResource(R.string.delete_action),
            onConfirm = { showClearSlotDialog = false; viewModel.clearSlot(); onBack() },
            onDismiss = { showClearSlotDialog = false }
        )
    }

    if (showPremiumInfoDialog) {
        InfoDialog(
            title = stringResource(R.string.premium_feature),
            message = stringResource(R.string.background_color_premium) + "\n" +
                      stringResource(R.string.app_settings) + "から購入できます。",
            onDismiss = { showPremiumInfoDialog = false }
        )
    }

    // Link削除警告（3列変更時）
    if (pendingColumnChange > 0) {
        AlertDialog(
            onDismissRequest = { pendingColumnChange = 0 },
            title = { Text(stringResource(R.string.change_column_count)) },
            text = { Text(stringResource(R.string.link_removed_by_column_change)) },
            confirmButton = {
                TextButton(onClick = {
                    val cols = pendingColumnChange
                    pendingColumnChange = 0
                    viewModel.changeColumns(cols)
                }) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingColumnChange = 0 }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // 分割数変更ダイアログ
    if (showColumnsDialog) {
        CustomContentDialog(
            title = stringResource(R.string.change_column_count),
            onDismiss = { showColumnsDialog = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_row_column_count),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val descriptions = listOf(R.string.column_1_desc, R.string.column_2_desc, R.string.column_3_desc)
                listOf(1, 2, 3).forEachIndexed { index, columns ->
                    ColumnOptionCard(
                        columns = columns,
                        description = stringResource(descriptions[index]),
                        isSelected = columns == currentColumns,
                        onClick = {
                            if (columns >= 3 && hasLinkShortcutsInRow) {
                                pendingColumnChange = columns
                                showColumnsDialog = false
                            } else {
                                viewModel.changeColumns(columns)
                                showColumnsDialog = false
                            }
                        }
                    )
                }
                Text(
                    text = stringResource(R.string.column_reduce_warning),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // 連絡先タイプ選択ダイアログ
    if (contactPicker.showDialog) {
        contactPicker.selectedContact?.let { contact ->
            ContactTypeDialog(
                contactName = contact.name,
                onSelectPhone = {
                    viewModel.placeContact(contact.name, contact.phoneNumber, ShortcutType.PHONE, null)
                    contactPicker.dismissDialog()
                    onBack()
                },
                onSelectSms = {
                    viewModel.placeContact(contact.name, contact.phoneNumber, ShortcutType.SMS, null)
                    contactPicker.dismissDialog()
                    onBack()
                },
                onSelectLinkPhone = if (isLinkAvailable) {
                    {
                        viewModel.placeContact("[通話]${contact.name}", contact.phoneNumber, ShortcutType.PHONE, RAKUTEN_LINK_PACKAGE)
                        contactPicker.dismissDialog()
                        onBack()
                    }
                } else null,
                onSelectLinkSms = if (isLinkAvailable) {
                    {
                        try {
                            val matched = viewModel.findLinkSmsShortcut(contact.phoneNumber, contact.name)
                            if (matched != null) {
                                viewModel.placeIntent(
                                    "[SMS]${matched.shortLabel}",
                                    matched.packageName,
                                    matched.id
                                )
                                contactPicker.dismissDialog()
                                onBack()
                            } else {
                                linkSmsError = context.getString(R.string.link_sms_no_history)
                                contactPicker.dismissDialog()
                            }
                        } catch (e: SecurityException) {
                            linkSmsError = context.getString(R.string.set_as_home_to_use_shortcut)
                            contactPicker.dismissDialog()
                        }
                    }
                } else null,
                onDismiss = { contactPicker.dismissDialog() }
            )
        }
    }

    // ===== Scaffold =====

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (screenState) {
                            is SelectScreenState.Main -> stringResource(
                                if (currentShortcut == null) R.string.add_shortcut else R.string.place_in_slot
                            )
                            is SelectScreenState.AppList -> stringResource(R.string.app_list)
                            is SelectScreenState.AppShortcuts ->
                                (screenState as SelectScreenState.AppShortcuts).app.label
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (screenState) {
                            is SelectScreenState.Main -> onBack()
                            is SelectScreenState.AppList -> screenState = SelectScreenState.Main
                            is SelectScreenState.AppShortcuts -> screenState = SelectScreenState.AppList
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        when (screenState) {
            is SelectScreenState.Main -> {
                MainContent(
                    viewModel = viewModel,
                    currentShortcut = currentShortcut,
                    currentColumns = currentColumns,
                    currentTextOnly = currentTextOnly,
                    currentBackgroundColor = currentBackgroundColor,
                    currentTextColor = currentTextColor,
                    unplacedShortcuts = unplacedShortcuts,
                    isLinkAvailable = isLinkAvailable,
                    onGoToAppList = { screenState = SelectScreenState.AppList },
                    onContactPicker = contactPicker.startPicker,
                    onSelectLinkMessage = if (isLinkAvailable) {
                        { viewModel.placeContact(context.getString(R.string.link_sms_list), "", ShortcutType.SMS, RAKUTEN_LINK_PACKAGE); onBack() }
                    } else null,
                    onSelectLinkCallList = if (isLinkAvailable) {
                        { viewModel.placeContact(context.getString(R.string.link_call_list), "", ShortcutType.PHONE, RAKUTEN_LINK_PACKAGE); onBack() }
                    } else null,
                    onSelectLinkDialpad = if (isLinkAvailable) {
                        { viewModel.placeContact(context.getString(R.string.link_dialpad), "", ShortcutType.DIALER, RAKUTEN_LINK_PACKAGE); onBack() }
                    } else null,
                    onSelectInternal = { feature ->
                        if (feature.type == ShortcutType.DEVICE_SETTINGS) {
                            showDeviceSettingsDialog = true
                        } else {
                            viewModel.placeInternalFeature(feature.type, context.getString(feature.labelResId))
                            onBack()
                        }
                    },
                    onSelectUnplaced = { shortcut -> viewModel.swapShortcuts(shortcut); onBack() },
                    onDeleteUnplaced = { shortcut -> shortcutToDelete = shortcut },
                    onShowColumnsDialog = { showColumnsDialog = true },
                    onChangeTextOnly = { viewModel.changeTextOnly(it) },
                    onChangeColors = { bg, tc -> viewModel.changeColors(bg, tc) },
                    onPremiumRequired = { showPremiumInfoDialog = true },
                    onClear = { showClearSlotDialog = true },
                    onDeleteRow = { showDeleteRowDialog = true },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is SelectScreenState.AppList -> {
                LaunchedEffect(Unit) { viewModel.loadApps() }
                AppListContent(
                    apps = viewModel.apps,
                    onSelectApp = { app ->
                        viewModel.loadShortcutsForApp(app.packageName)
                        screenState = SelectScreenState.AppShortcuts(app)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is SelectScreenState.AppShortcuts -> {
                val app = (screenState as SelectScreenState.AppShortcuts).app
                AppShortcutsContent(
                    app = app,
                    shortcuts = viewModel.shortcuts,
                    onSelectApp = {
                        viewModel.placeApp(app.packageName, app.label)
                        onBack()
                    },
                    onSelectShortcut = { shortcut ->
                        viewModel.placeIntent(shortcut.shortLabel, shortcut.packageName, shortcut.id)
                        onBack()
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

// ============ メインコンテンツ ============

@Composable
private fun MainContent(
    viewModel: ShortcutSelectViewModel,
    currentShortcut: ShortcutItem?,
    currentColumns: Int,
    currentTextOnly: Boolean,
    currentBackgroundColor: String?,
    currentTextColor: String?,
    unplacedShortcuts: List<ShortcutItem>,
    isLinkAvailable: Boolean,
    onGoToAppList: () -> Unit,
    onContactPicker: () -> Unit,
    onSelectLinkMessage: (() -> Unit)?,
    onSelectLinkCallList: (() -> Unit)?,
    onSelectLinkDialpad: (() -> Unit)?,
    onSelectInternal: (InternalFeature) -> Unit,
    onSelectUnplaced: (ShortcutItem) -> Unit,
    onDeleteUnplaced: (ShortcutItem) -> Unit,
    onShowColumnsDialog: () -> Unit,
    onChangeTextOnly: (Boolean) -> Unit,
    onChangeColors: (String?, String?) -> Unit,
    onPremiumRequired: () -> Unit,
    onClear: () -> Unit,
    onDeleteRow: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 共通選択コンテンツ
        commonSelectContent(
            unplacedShortcuts = unplacedShortcuts,
            onSelectUnplaced = onSelectUnplaced,
            onSelectInternal = onSelectInternal,
            onGoToAppList = onGoToAppList,
            onContactPicker = onContactPicker,
            onSelectLinkMessage = onSelectLinkMessage,
            onSelectLinkCallList = onSelectLinkCallList,
            onSelectLinkDialpad = onSelectLinkDialpad,
            onDeleteUnplaced = onDeleteUnplaced
        )

        // ─── 区切り線 ───
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 表示モード切替
        item {
            DisplayModeCard(
                textOnly = currentTextOnly,
                onToggle = { onChangeTextOnly(!currentTextOnly) }
            )
        }

        // 背景色・文字色（プレミアム）
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item {
            ColorSetCard(
                currentBackgroundColor = currentBackgroundColor,
                currentTextColor = currentTextColor,
                isPremium = viewModel.isPremium,
                onSelectColors = onChangeColors,
                onPremiumRequired = onPremiumRequired
            )
        }

        // 分割数変更
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item {
            ActionCard(
                text = stringResource(R.string.current_column_count, currentColumns),
                color = MaterialTheme.colorScheme.primary,
                onClick = onShowColumnsDialog
            )
        }

        // スロットを空にする（現スロットにショートカットがある場合のみ）
        if (currentShortcut != null && currentShortcut.type != ShortcutType.EMPTY) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            item {
                ActionCard(
                    text = stringResource(R.string.clear_slot),
                    color = MaterialTheme.colorScheme.error,
                    onClick = onClear
                )
            }
        }

        // 行を削除
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item {
            ActionCard(
                text = stringResource(R.string.delete_row),
                color = MaterialTheme.colorScheme.error,
                onClick = onDeleteRow
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ============ 共通選択コンテンツ（LazyListScope 拡張）============

private fun LazyListScope.commonSelectContent(
    unplacedShortcuts: List<ShortcutItem>,
    onSelectUnplaced: (ShortcutItem) -> Unit,
    onSelectInternal: (InternalFeature) -> Unit,
    onGoToAppList: () -> Unit,
    onContactPicker: () -> Unit,
    onSelectLinkMessage: (() -> Unit)? = null,
    onSelectLinkCallList: (() -> Unit)? = null,
    onSelectLinkDialpad: (() -> Unit)? = null,
    onDeleteUnplaced: (ShortcutItem) -> Unit
) {
    // アプリ一覧へ
    item {
        NavigationCard(
            icon = {
                Icon(
                    Icons.Default.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = AppTheme.extendedColors.iconAllApps
                )
            },
            text = stringResource(R.string.select_from_app_list),
            onClick = onGoToAppList
        )
    }

    // 連絡先から追加
    item {
        NavigationCard(
            icon = {
                val context = LocalContext.current
                val contactsIcon = remember {
                    val pm = context.packageManager
                    listOf("com.google.android.contacts", "com.android.contacts")
                        .firstNotNullOfOrNull { pkg ->
                            try { pm.getApplicationIcon(pkg) } catch (e: Exception) { null }
                        }
                }
                if (contactsIcon != null) {
                    Image(
                        bitmap = contactsIcon.toBitmap(64, 64).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
            text = stringResource(R.string.add_from_contact),
            onClick = onContactPicker
        )
    }

    // アプリ内機能
    item { SectionHeader(text = stringResource(R.string.internal_features)) }
    item {
        InternalFeaturesColumn(
            features = internalFeatures,
            onSelectInternal = onSelectInternal
        )
    }

    // 楽天Link機能
    if (onSelectLinkMessage != null || onSelectLinkCallList != null || onSelectLinkDialpad != null) {
        item {
            val context = LocalContext.current
            val linkIcon = remember {
                try { context.packageManager.getApplicationIcon(RAKUTEN_LINK_PACKAGE) }
                catch (e: Exception) { null }
            }
            val containerColor = MaterialTheme.colorScheme.surfaceVariant
            val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onSelectLinkMessage != null) {
                    LinkFeatureCard(
                        linkIcon = linkIcon,
                        labelResId = R.string.link_sms_list,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        onClick = onSelectLinkMessage
                    )
                }
                if (onSelectLinkCallList != null) {
                    LinkFeatureCard(
                        linkIcon = linkIcon,
                        labelResId = R.string.link_call_list,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        onClick = onSelectLinkCallList
                    )
                }
                if (onSelectLinkDialpad != null) {
                    val dialerIcon = remember {
                        ContextCompat.getDrawable(context, R.drawable.ic_phone_keypad)
                    }
                    val dialerBitmap = remember(dialerIcon) {
                        dialerIcon?.toBitmap(64, 64)?.asImageBitmap()
                    }
                    LinkFeatureCard(
                        linkIcon = linkIcon,
                        customIcon = dialerBitmap,
                        customIconTint = AppTheme.extendedColors.iconDialer,
                        labelResId = R.string.link_dialpad,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        onClick = onSelectLinkDialpad
                    )
                }
            }
        }
    }

    // 未配置ショートカット
    if (unplacedShortcuts.isNotEmpty()) {
        item { SectionHeader(text = stringResource(R.string.unplaced_shortcuts)) }
        items(unplacedShortcuts) { shortcut ->
            UnplacedShortcutCard(
                shortcut = shortcut,
                onClick = { onSelectUnplaced(shortcut) },
                onDelete = { onDeleteUnplaced(shortcut) }
            )
        }
    }
}

// ============ アプリ一覧コンテンツ ============

@Composable
private fun AppListContent(
    apps: List<AppInfo>?,
    onSelectApp: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val sortedApps = remember(apps) {
        val appList = apps ?: return@remember emptyList()
        val priority = appList.filter { isPriorityApp(it.packageName) }
            .sortedBy { getPriorityIndex(it.packageName) }
        val others = appList.filter { !isPriorityApp(it.packageName) }
        priority + others
    }

    val filteredApps = remember(searchQuery, sortedApps) {
        if (searchQuery.isBlank()) sortedApps
        else sortedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text(stringResource(R.string.search_app_name)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (apps == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Text(
                text = stringResource(R.string.app_count, filteredApps.size),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps) { app ->
                    AppCard(app = app, onClick = { onSelectApp(app) })
                }
            }
        }
    }
}

// ============ アプリショートカットコンテンツ ============

@Composable
private fun AppShortcutsContent(
    app: AppInfo,
    shortcuts: List<ShortcutData>,
    onSelectApp: () -> Unit,
    onSelectShortcut: (ShortcutData) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelectApp() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    app.icon?.let { DrawableImage(it, 48) }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(text = stringResource(R.string.launch_app), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = app.label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (shortcuts.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_shortcuts_for_app),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item { SectionHeader(text = stringResource(R.string.shortcut_count, shortcuts.size)) }
            items(shortcuts) { shortcut ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectShortcut(shortcut) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        shortcut.icon?.let { DrawableImage(it, 40) }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(text = shortcut.shortLabel, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            shortcut.longLabel?.let {
                                Text(text = it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============ 端末設定選択ダイアログ ============

@Composable
private fun DeviceSettingsPickerDialog(
    onSelect: (DeviceSettingsItem) -> Unit,
    onDismiss: () -> Unit
) {
    CustomContentDialog(
        title = stringResource(R.string.select_device_settings),
        onDismiss = onDismiss
    ) {
        val scrollState = rememberScrollState()
        Box {
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                deviceSettingsList.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(item) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(item.labelResId),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            if (scrollState.value > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Transparent)
                            )
                        )
                )
            }
            if (scrollState.value < scrollState.maxValue) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f))
                            )
                        )
                )
            }
        }
    }
}

// ============ UI 部品 ============

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun NavigationCard(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(text = "→", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InternalFeaturesColumn(
    features: List<InternalFeature>,
    onSelectInternal: (InternalFeature) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        features.forEach { feature ->
            val label = stringResource(feature.labelResId)
            val iconColor = when (feature.type) {
                ShortcutType.CALENDAR -> AppTheme.extendedColors.iconCalendar
                ShortcutType.MEMO     -> AppTheme.extendedColors.iconMemo
                ShortcutType.DIALER   -> AppTheme.extendedColors.iconDialer
                ShortcutType.ALL_APPS -> AppTheme.extendedColors.iconAllApps
                else                  -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelectInternal(feature) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!feature.hasIcon) {
                        // 日付・時刻: ラベルのみ大きく
                        Text(
                            text = label,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        when (feature.type) {
                            ShortcutType.DIALER -> {
                                val icon = remember { ContextCompat.getDrawable(context, R.drawable.ic_phone_keypad) }
                                if (icon != null) {
                                    val bmp = remember(icon) { icon.toBitmap(64, 64) }
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = label, modifier = Modifier.size(32.dp),
                                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(iconColor))
                                }
                            }
                            ShortcutType.MEMO -> {
                                val icon = remember { ContextCompat.getDrawable(context, R.drawable.ic_memo) }
                                if (icon != null) {
                                    val bmp = remember(icon) { icon.toBitmap(64, 64) }
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = label, modifier = Modifier.size(32.dp),
                                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(iconColor))
                                }
                            }
                            ShortcutType.CALENDAR -> {
                                val icon = remember { ContextCompat.getDrawable(context, R.drawable.ic_calendar) }
                                if (icon != null) {
                                    val bmp = remember(icon) { icon.toBitmap(64, 64) }
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = label, modifier = Modifier.size(32.dp),
                                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(iconColor))
                                }
                            }
                            ShortcutType.ALL_APPS -> {
                                Icon(Icons.Default.Apps, contentDescription = label, modifier = Modifier.size(32.dp), tint = iconColor)
                            }
                            ShortcutType.DEVICE_SETTINGS -> {
                                Icon(Icons.Default.Settings, contentDescription = label, modifier = Modifier.size(32.dp), tint = iconColor)
                            }
                            else -> {}
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnplacedShortcutCard(
    shortcut: ShortcutItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val shortcutHelper = remember { ShortcutHelper(context) }
    val contentColor = MaterialTheme.colorScheme.onSurface
    val iconColor = when (shortcut.type) {
        ShortcutType.CALENDAR -> AppTheme.extendedColors.iconCalendar
        ShortcutType.MEMO     -> AppTheme.extendedColors.iconMemo
        ShortcutType.DIALER   -> AppTheme.extendedColors.iconDialer
        ShortcutType.ALL_APPS -> AppTheme.extendedColors.iconAllApps
        ShortcutType.PHONE    -> AppTheme.extendedColors.iconCalendar
        ShortcutType.SMS      -> AppTheme.extendedColors.iconMemo
        else                  -> contentColor
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                shortcut.type == ShortcutType.DATE_DISPLAY || shortcut.type == ShortcutType.TIME_DISPLAY -> {
                    Text(
                        text = shortcut.label,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                shortcut.type == ShortcutType.APP || shortcut.type == ShortcutType.INTENT -> {
                    val appIcon = remember(shortcut.packageName) {
                        shortcut.packageName?.let { shortcutHelper.getAppIcon(it) }
                    }
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon.toBitmap(64, 64).asImageBitmap(),
                            contentDescription = shortcut.label,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(Icons.Default.Apps, contentDescription = shortcut.label, modifier = Modifier.size(32.dp), tint = contentColor)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = shortcut.label, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = contentColor)
                        Text(text = stringResource(R.string.tap_to_place), fontSize = 12.sp, color = contentColor.copy(alpha = 0.7f))
                    }
                }
                shortcut.type == ShortcutType.MEMO -> {
                    val icon = remember { ContextCompat.getDrawable(context, R.drawable.ic_memo) }
                    if (icon != null) {
                        Image(bitmap = icon.toBitmap(64, 64).asImageBitmap(), contentDescription = shortcut.label, modifier = Modifier.size(32.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(iconColor))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = shortcut.label, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = contentColor)
                        Text(text = stringResource(R.string.tap_to_place), fontSize = 12.sp, color = contentColor.copy(alpha = 0.7f))
                    }
                }
                shortcut.type == ShortcutType.PHONE -> {
                    Icon(Icons.Default.Phone, contentDescription = shortcut.label, modifier = Modifier.size(32.dp), tint = iconColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = shortcut.label, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = contentColor)
                        Text(text = stringResource(R.string.tap_to_place), fontSize = 12.sp, color = contentColor.copy(alpha = 0.7f))
                    }
                }
                shortcut.type == ShortcutType.SMS -> {
                    val smsIcon = remember {
                        val pm = context.packageManager
                        listOf("com.google.android.apps.messaging", "com.android.mms", "com.samsung.android.messaging")
                            .firstNotNullOfOrNull { pkg -> try { pm.getApplicationIcon(pkg) } catch (e: Exception) { null } }
                    }
                    if (smsIcon != null) {
                        Image(bitmap = smsIcon.toBitmap(64, 64).asImageBitmap(), contentDescription = shortcut.label, modifier = Modifier.size(32.dp))
                    } else {
                        Icon(Icons.Default.Email, contentDescription = shortcut.label, modifier = Modifier.size(32.dp), tint = iconColor)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = shortcut.label, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = contentColor)
                        Text(text = stringResource(R.string.tap_to_place), fontSize = 12.sp, color = contentColor.copy(alpha = 0.7f))
                    }
                }
                else -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = shortcut.label, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = contentColor)
                        Text(text = stringResource(R.string.tap_to_place), fontSize = 12.sp, color = contentColor.copy(alpha = 0.7f))
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.delete_unplaced_shortcut),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppCard(app: AppInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            app.icon?.let { DrawableImage(it, 48) }
            Spacer(Modifier.width(16.dp))
            Text(text = app.label, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ActionCard(text: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }
}

@Composable
private fun DisplayModeCard(textOnly: Boolean, onToggle: () -> Unit) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = stringResource(R.string.display_mode),
                fontSize = 14.sp,
                color = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.weight(1f).clickable { if (textOnly) onToggle() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = !textOnly, onClick = { if (textOnly) onToggle() })
                    Text(text = stringResource(R.string.display_mode_icon_text), fontSize = 14.sp, color = contentColor)
                }
                Row(
                    modifier = Modifier.weight(1f).clickable { if (!textOnly) onToggle() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = textOnly, onClick = { if (!textOnly) onToggle() })
                    Text(text = stringResource(R.string.display_mode_text_only), fontSize = 14.sp, color = contentColor)
                }
            }
        }
    }
}

@Composable
private fun ColorSetCard(
    currentBackgroundColor: String?,
    currentTextColor: String?,
    isPremium: Boolean,
    onSelectColors: (String?, String?) -> Unit,
    onPremiumRequired: () -> Unit
) {
    val containerColor = if (isPremium) MaterialTheme.colorScheme.surface
                         else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isPremium) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth()
            .then(if (!isPremium) Modifier.clickable(onClick = onPremiumRequired) else Modifier),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.background_color),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Premium", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isPremium) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ColorSetButton(colorSet = null, isSelected = currentBackgroundColor == null,
                        onClick = { onSelectColors(null, null) })
                    slotColorPalette.take(4).forEach { cs ->
                        ColorSetButton(colorSet = cs, isSelected = currentBackgroundColor == cs.backgroundColor,
                            onClick = { onSelectColors(cs.backgroundColor, cs.textColor) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    slotColorPalette.drop(4).forEach { cs ->
                        ColorSetButton(colorSet = cs, isSelected = currentBackgroundColor == cs.backgroundColor,
                            onClick = { onSelectColors(cs.backgroundColor, cs.textColor) })
                    }
                    Spacer(modifier = Modifier.size(44.dp))
                }
            } else {
                Text(
                    text = stringResource(R.string.background_color_premium),
                    fontSize = 14.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ColorSetButton(colorSet: ColorSet?, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (colorSet != null) Color(android.graphics.Color.parseColor(colorSet.backgroundColor))
                  else MaterialTheme.colorScheme.surfaceVariant
    val txtColor = if (colorSet != null) Color(android.graphics.Color.parseColor(colorSet.textColor))
                   else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = if (colorSet == null) "×" else "Aa", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = txtColor)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Red)
            )
        }
    }
}

@Composable
private fun LinkFeatureCard(
    linkIcon: Drawable?,
    labelResId: Int,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    customIcon: androidx.compose.ui.graphics.ImageBitmap? = null,
    customIconTint: Color? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (customIcon != null) {
                Image(
                    bitmap = customIcon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    colorFilter = customIconTint?.let { androidx.compose.ui.graphics.ColorFilter.tint(it) }
                )
            } else if (linkIcon != null) {
                Image(bitmap = linkIcon.toBitmap(64, 64).asImageBitmap(), contentDescription = null, modifier = Modifier.size(32.dp))
            } else {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(32.dp), tint = contentColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = stringResource(labelResId), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = contentColor)
        }
    }
}

@Composable
private fun DrawableImage(drawable: Drawable, size: Int) {
    val bitmap = remember(drawable) { drawable.toBitmap(size * 2, size * 2) }
    Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(size.dp))
}

// ============ 連絡先取得 ============

private fun getContactInfo(context: android.content.Context, contactUri: android.net.Uri): ContactInfo? {
    var name: String? = null
    var phoneNumber: String? = null

    context.contentResolver.query(
        contactUri,
        arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID),
        null, null, null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
            val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))

            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { phoneCursor ->
                if (phoneCursor.moveToFirst()) {
                    phoneNumber = phoneCursor.getString(
                        phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    )
                }
            }
        }
    }

    return if (name != null && phoneNumber != null) ContactInfo(name!!, phoneNumber!!) else null
}
