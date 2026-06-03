package com.coelacanth9.simplecustomlauncher.feature.screens.allapps

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.coelacanth9.simplecustomlauncher.R
import com.coelacanth9.simplecustomlauncher.platform.AppInfo
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext

/**
 * すべてのアプリ画面
 * アプリ一覧を表示し、タップで直接起動
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAppsScreen(
    onBack: () -> Unit,
    packageRemovedFlow: SharedFlow<String>? = null
) {
    val context = LocalContext.current
    val helper = remember { ShortcutHelper(context) }
    var apps by remember { mutableStateOf<List<AppInfo>?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // バックグラウンドでアプリ一覧を取得
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            helper.getInstalledApps()
        }
    }

    // アンインストール検知でリストから削除
    LaunchedEffect(packageRemovedFlow) {
        packageRemovedFlow?.collect { packageName ->
            apps = apps?.filter { it.packageName != packageName }
        }
    }

    // 優先アプリを上に、それ以外はそのまま
    val sortedApps = remember(apps) {
        apps?.let { list ->
            val priority = list.filter { isPriorityApp(it.packageName) }
                .sortedBy { getPriorityIndex(it.packageName) }
            val others = list.filter { !isPriorityApp(it.packageName) }
            priority + others
        }
    }

    val filteredApps = remember(searchQuery, sortedApps) {
        sortedApps?.let { sorted ->
            if (searchQuery.isBlank()) sorted
            else sorted.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shortcut_type_all_apps),
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
        if (filteredApps == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 検索フィールド
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                        var menuExpanded by remember { mutableStateOf(false) }
                        val isSelf = app.packageName == context.packageName

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { helper.startApp(app.packageName) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                app.icon?.let { icon ->
                                    val bitmap = remember(icon) { icon.toBitmap(48, 48) }
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = app.label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.app_info)) },
                                            onClick = {
                                                menuExpanded = false
                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.parse("package:${app.packageName}")
                                                }
                                                context.startActivity(intent)
                                            }
                                        )
                                        if (!isSelf) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.uninstall)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    val intent = Intent(Intent.ACTION_DELETE).apply {
                                                        data = Uri.parse("package:${app.packageName}")
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== 優先アプリ判定 =====

private val priorityAppPackages = listOf(
    // 1. 電話・連絡先（最重要）
    "com.android.dialer",
    "com.google.android.dialer",
    "com.android.contacts",
    "com.google.android.contacts",

    // 2. LINE（高齢者に人気）
    "jp.naver.line.android",

    // 3. SMS・メール
    "com.android.messaging",
    "com.google.android.apps.messaging",
    "com.google.android.gm",

    // 4. カメラ
    "com.android.camera",
    "com.android.camera2",
    "com.google.android.GoogleCamera",

    // 5. 写真・ギャラリー
    "com.google.android.apps.photos",
    "com.google.android.apps.nbu.files",
    "com.amazon.clouddrive.photos",

    // 6. 地図
    "com.google.android.apps.maps",

    // 7. ブラウザ・検索
    "com.android.chrome",
    "com.google.android.googlequicksearchbox",
    "com.microsoft.bing",

    // 8. 便利ツール
    "com.google.android.calendar",
    "com.google.android.calculator",

    // 9. 動画・SNS
    "com.google.android.youtube",
    "com.instagram.android",
    "com.twitter.android",

    // 10. ショッピング・その他
    "com.amazon.mShop.android.shopping",
    "com.android.vending",
    "com.google.android.apps.bard",
)

private fun isPriorityApp(packageName: String): Boolean {
    return priorityAppPackages.any {
        packageName == it || packageName.startsWith("$it.")
    }
}

private fun getPriorityIndex(packageName: String): Int {
    val index = priorityAppPackages.indexOfFirst {
        packageName == it || packageName.startsWith("$it.")
    }
    return if (index >= 0) index else Int.MAX_VALUE
}
