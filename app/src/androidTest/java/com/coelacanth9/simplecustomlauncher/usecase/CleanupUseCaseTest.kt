package com.coelacanth9.simplecustomlauncher.usecase

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CleanupUseCase のインスツルメンテッドテスト。
 * パッケージ削除・アンインストール検出・孤立ピン情報のクリーンアップを検証する。
 */
@RunWith(AndroidJUnit4::class)
class CleanupUseCaseTest {

    private lateinit var repository: ShortcutRepository
    private lateinit var useCase: CleanupUseCase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("launcher_shortcuts", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("pin_shortcuts", Context.MODE_PRIVATE).edit().clear().commit()
        repository = ShortcutRepository(context)
        useCase = CleanupUseCase(repository, ShortcutHelper(context))
    }

    // ===== onPackageRemoved =====

    @Test
    fun onPackageRemoved_deletesAllShortcutsMatchingPackage() {
        val removed = ShortcutItem(id = "r1", type = ShortcutType.APP, label = "Removed", packageName = "com.removed")
        val kept   = ShortcutItem(id = "k1", type = ShortcutType.APP, label = "Kept",    packageName = "com.kept")
        repository.update {
            saveShortcut(removed)
            saveShortcut(kept)
        }

        useCase.onPackageRemoved("com.removed")

        assertNull(repository.getShortcut("r1"))
        assertNotNull(repository.getShortcut("k1"))
    }

    @Test
    fun onPackageRemoved_intentShortcut_deletesPinInfoToo() {
        val item = ShortcutItem(id = "i1", type = ShortcutType.INTENT, label = "Pin", packageName = "com.removed")
        repository.saveShortcut(item)
        repository.savePinShortcutInfo("i1", "sc_id", "com.removed")

        useCase.onPackageRemoved("com.removed")

        assertNull(repository.getShortcut("i1"))
        val (pinId, _) = repository.getPinShortcutInfo("i1")
        assertNull(pinId)
    }

    @Test
    fun onPackageRemoved_noMatchingShortcuts_doesNothing() {
        val item = ShortcutItem(id = "k1", type = ShortcutType.APP, label = "Kept", packageName = "com.kept")
        repository.saveShortcut(item)

        useCase.onPackageRemoved("com.nonexistent")

        assertNotNull(repository.getShortcut("k1"))
    }

    // ===== cleanupUninstalledPackages =====

    @Test
    fun cleanupUninstalledPackages_removesShortcutsForUninstalledPackages() {
        // 存在しないパッケージ名を使う
        val uninstalled = ShortcutItem(
            id = "u1",
            type = ShortcutType.APP,
            label = "Uninstalled",
            packageName = "com.definitely.not.installed.xyz123"
        )
        val installed = ShortcutItem(
            id = "i1",
            type = ShortcutType.APP,
            label = "Installed",
            packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        )
        repository.update {
            saveShortcut(uninstalled)
            saveShortcut(installed)
        }

        useCase.cleanupUninstalledPackages()

        assertNull(repository.getShortcut("u1"))
        assertNotNull(repository.getShortcut("i1"))
    }

    @Test
    fun cleanupUninstalledPackages_noAppsOrIntents_doesNothing() {
        // APP / INTENT 以外のショートカットはチェック対象外
        val item = ShortcutItem(id = "c1", type = ShortcutType.CALENDAR, label = "Calendar")
        repository.saveShortcut(item)

        useCase.cleanupUninstalledPackages()

        assertNotNull(repository.getShortcut("c1"))
    }

    // ===== cleanupOrphanedPinShortcuts =====

    @Test
    fun cleanupOrphanedPinShortcuts_removesOrphanedPinInfo() {
        // ShortcutItem は存在しないが、ピン情報だけ残っている孤立状態
        repository.savePinShortcutInfo("orphaned_id", "pin_sc_id", "com.example")

        useCase.cleanupOrphanedPinShortcuts()

        val (pinId, _) = repository.getPinShortcutInfo("orphaned_id")
        assertNull(pinId)
    }

    @Test
    fun cleanupOrphanedPinShortcuts_keepsValidPinInfo() {
        // ShortcutItem が存在する場合は孤立ではないので残す
        val item = ShortcutItem(id = "valid_id", type = ShortcutType.INTENT, label = "Valid", packageName = "com.example")
        repository.saveShortcut(item)
        repository.savePinShortcutInfo("valid_id", "pin_sc_id", "com.example")

        useCase.cleanupOrphanedPinShortcuts()

        val (pinId, _) = repository.getPinShortcutInfo("valid_id")
        assertNotNull(pinId) // 有効なピン情報は削除されない
    }

    @Test
    fun cleanupOrphanedPinShortcuts_noOrphans_doesNothing() {
        val item = ShortcutItem(id = "id1", type = ShortcutType.APP, label = "App", packageName = "com.example")
        repository.saveShortcut(item)
        // ピン情報なし

        useCase.cleanupOrphanedPinShortcuts() // 例外が発生しないこと

        assertNotNull(repository.getShortcut("id1"))
    }
}
