package com.coelacanth9.simplecustomlauncher.usecase

import android.util.Log
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import javax.inject.Inject

class CleanupUseCase @Inject constructor(
    private val repository: ShortcutRepository,
    private val shortcutHelper: ShortcutHelper
) {

    /**
     * ショートカット情報が存在しない孤立ピンショートカットをunpinして情報を削除する。
     */
    fun cleanupOrphanedPinShortcuts() {
        val orphaned = repository.findOrphanedPinShortcuts()
        if (orphaned.isEmpty()) return

        val byPackage = orphaned.groupBy { it.third }
        for ((packageName, entries) in byPackage) {
            val orphanedPinIds = entries.map { it.second }.toSet()
            shortcutHelper.unpinShortcuts(packageName, orphanedPinIds)
        }
        for ((itemId, _, _) in orphaned) {
            repository.deletePinShortcutInfo(itemId)
        }
    }

    /**
     * アンインストール済みパッケージのショートカットを削除する。
     */
    fun cleanupUninstalledPackages() {
        val allShortcuts = repository.layoutState.value.shortcuts.values
        val targets = allShortcuts.filter {
            (it.type == ShortcutType.APP || it.type == ShortcutType.INTENT) && it.packageName != null
        }
        val packageNames = targets.mapNotNull { it.packageName }.toSet()
        val uninstalled = shortcutHelper.getUninstalledPackages(packageNames)
        if (uninstalled.isEmpty()) return

        for (shortcut in targets.filter { it.packageName in uninstalled }) {
            if (shortcut.type == ShortcutType.INTENT) repository.deletePinShortcutInfo(shortcut.id)
            repository.deleteShortcut(shortcut.id)
        }
    }

    /**
     * パッケージ削除時にそのパッケージのショートカットを削除する。
     */
    fun onPackageRemoved(packageName: String) {
        val shortcuts = repository.getShortcutsByPackageName(packageName)
        if (shortcuts.isEmpty()) return
        for (shortcut in shortcuts) {
            if (shortcut.type == ShortcutType.INTENT) repository.deletePinShortcutInfo(shortcut.id)
            repository.deleteShortcut(shortcut.id)
        }
    }

    companion object {
        private const val TAG = "CleanupUseCase"
    }
}
