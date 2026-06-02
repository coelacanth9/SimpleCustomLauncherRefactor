package com.coelacanth9.simplecustomlauncher.platform

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.coelacanth9.simplecustomlauncher.core.shortcut.RAKUTEN_LINK_PACKAGE

/** 楽天LinkがインストールされているかどうかをPackageManagerで判定 */
fun PackageManager.isRakutenLinkInstalled(): Boolean = try {
    getApplicationInfo(RAKUTEN_LINK_PACKAGE, 0)
    true
} catch (e: Exception) { false }

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable?
)

data class ShortcutData(
    val id: String,
    val shortLabel: String,
    val longLabel: String?,
    val packageName: String,
    val icon: Drawable?,
    val shortcutInfo: ShortcutInfo
)

class ShortcutHelper(private val context: Context) {

    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val packageManager = context.packageManager

    fun getInstalledApps(): List<AppInfo> {
        return launcherApps.getActivityList(null, Process.myUserHandle())
            .map { AppInfo(label = it.label.toString(), packageName = it.applicationInfo.packageName, icon = it.getBadgedIcon(0)) }
            .sortedBy { it.label }
    }

    fun getShortcutsForApp(packageName: String): List<ShortcutData> {
        val shortcuts = mutableListOf<ShortcutData>()
        val query = LauncherApps.ShortcutQuery().apply {
            setPackage(packageName)
            setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        }
        try {
            launcherApps.getShortcuts(query, Process.myUserHandle())?.forEach { shortcut ->
                shortcuts.add(ShortcutData(
                    id = shortcut.id,
                    shortLabel = shortcut.shortLabel?.toString() ?: "",
                    longLabel = shortcut.longLabel?.toString(),
                    packageName = shortcut.`package`,
                    icon = launcherApps.getShortcutIconDrawable(shortcut, 0),
                    shortcutInfo = shortcut
                ))
            }
        } catch (e: Exception) {
            Log.e("ShortcutHelper", "Failed to get shortcuts for $packageName", e)
        }
        return shortcuts
    }

    fun startShortcut(shortcut: ShortcutData) {
        try {
            launcherApps.startShortcut(shortcut.packageName, shortcut.id, null, null, Process.myUserHandle())
        } catch (e: Exception) {
            Log.e("ShortcutHelper", "Failed to start shortcut", e)
        }
    }

    fun getAppIcon(packageName: String): Drawable? = try {
        packageManager.getUserBadgedIcon(packageManager.getApplicationIcon(packageName), Process.myUserHandle())
    } catch (e: Exception) {
        Log.e("ShortcutHelper", "Failed to get icon for $packageName", e)
        null
    }

    fun startApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ShortcutHelper", "Failed to start app $packageName", e)
        }
    }

    fun findLinkSmsShortcut(phoneNumber: String, contactName: String): ShortcutData? {
        val query = LauncherApps.ShortcutQuery().apply {
            setPackage(RAKUTEN_LINK_PACKAGE)
            setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED
            )
        }
        val shortcutList = launcherApps.getShortcuts(query, Process.myUserHandle()) ?: return null
        val candidates = mutableListOf<ShortcutData>()
        for (shortcut in shortcutList) {
            if (shortcut.id.startsWith("oa-") || shortcut.id == "shortcutid") continue
            val participantKey = shortcut.intent?.getStringExtra("CHAT_PARTICIPANT_KEY")
            val matchedByPhone = participantKey != null && PhoneNumberUtils.compare(participantKey, phoneNumber)
            val label = shortcut.shortLabel?.toString() ?: ""
            val matchedByName = !matchedByPhone && label.isNotBlank() && normalize(label) == normalize(contactName)
            if (matchedByPhone || matchedByName) {
                candidates.add(ShortcutData(
                    id = shortcut.id,
                    shortLabel = shortcut.shortLabel?.toString() ?: "",
                    longLabel = shortcut.longLabel?.toString(),
                    packageName = shortcut.`package`,
                    icon = try { launcherApps.getShortcutIconDrawable(shortcut, 0) } catch (e: Exception) { null },
                    shortcutInfo = shortcut
                ))
            }
        }
        val best = candidates.sortedWith(
            compareByDescending<ShortcutData> { it.shortcutInfo.isDynamic }
                .thenByDescending { it.shortcutInfo.lastChangedTimestamp }
        ).firstOrNull()
        if (best != null) pinShortcut(best.packageName, best.id)
        return best
    }

    fun pinShortcut(packageName: String, shortcutId: String) {
        try {
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            }
            val existing = launcherApps.getShortcuts(query, Process.myUserHandle())?.map { it.id } ?: emptyList()
            launcherApps.pinShortcuts(packageName, (existing + shortcutId).distinct(), Process.myUserHandle())
        } catch (e: Exception) {
            Log.e("ShortcutHelper", "Failed to pin shortcut: $packageName/$shortcutId", e)
        }
    }

    fun isShortcutValid(packageName: String, shortcutId: String): Boolean? {
        if (packageName != RAKUTEN_LINK_PACKAGE) return null
        val query = LauncherApps.ShortcutQuery().apply {
            setPackage(packageName)
            setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        }
        return try {
            launcherApps.getShortcuts(query, Process.myUserHandle())?.find { it.id == shortcutId } != null
        } catch (e: Exception) { null }
    }

    private fun normalize(name: String): String = name.replace("\\s+".toRegex(), "")
}
