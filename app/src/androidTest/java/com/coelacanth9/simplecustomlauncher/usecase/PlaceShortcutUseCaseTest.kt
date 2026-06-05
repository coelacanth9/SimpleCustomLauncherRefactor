package com.coelacanth9.simplecustomlauncher.usecase

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.HomeLayoutConfig
import com.coelacanth9.simplecustomlauncher.model.RowConfig
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PlaceShortcutUseCase のインスツルメンテッドテスト。
 * アプリ配置・重複排除・スワップ等のロジックを検証する。
 */
@RunWith(AndroidJUnit4::class)
class PlaceShortcutUseCaseTest {

    private lateinit var repository: ShortcutRepository
    private lateinit var useCase: PlaceShortcutUseCase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("launcher_shortcuts", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("pin_shortcuts", Context.MODE_PRIVATE).edit().clear().commit()
        repository = ShortcutRepository(context)
        useCase = PlaceShortcutUseCase(repository)

        // 共通レイアウト: ページ0、行0、2列
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2),
            RowConfig(pageIndex = 0, rowIndex = 1, columns = 2)
        )))
    }

    // ===== placeApp =====

    @Test
    fun placeApp_newApp_createsShortcutAndSavesPlacement() {
        useCase.placeApp(0, 0, 0, "com.example.app", "My App")

        val state = repository.layoutState.value
        val shortcut = state.shortcuts.values.find { it.packageName == "com.example.app" }
        assertNotNull(shortcut)
        assertEquals("My App", shortcut!!.label)
        assertEquals(ShortcutType.APP, shortcut.type)

        val placement = state.placements.find { it.row == 0 && it.column == 0 }
        assertNotNull(placement)
        assertEquals(shortcut.id, placement!!.shortcutId)
    }

    @Test
    fun placeApp_sameAppTwice_reusesExistingShortcut() {
        useCase.placeApp(0, 0, 0, "com.example.app", "My App")
        useCase.placeApp(0, 0, 1, "com.example.app", "My App")

        // ショートカットの重複がないこと
        val shortcuts = repository.layoutState.value.shortcuts.values
            .filter { it.packageName == "com.example.app" }
        assertEquals(1, shortcuts.size)

        // 両スロットに配置されていること
        val placements = repository.layoutState.value.placements
        assertEquals(2, placements.size)
    }

    @Test
    fun placeApp_overwritesExistingPlacement() {
        useCase.placeApp(0, 0, 0, "com.example.appA", "App A")
        useCase.placeApp(0, 0, 0, "com.example.appB", "App B") // 同じスロットに上書き

        val placement = repository.layoutState.value.placements
            .find { it.row == 0 && it.column == 0 }
        assertNotNull(placement)

        val shortcut = repository.layoutState.value.shortcuts[placement!!.shortcutId]
        assertEquals("com.example.appB", shortcut?.packageName)
    }

    // ===== placeInternalFeature =====

    @Test
    fun placeInternalFeature_calendar_createsOnceAndReuses() {
        useCase.placeInternalFeature(0, 0, 0, ShortcutType.CALENDAR, "カレンダー")
        useCase.placeInternalFeature(0, 0, 1, ShortcutType.CALENDAR, "カレンダー")

        // CALENDAR ショートカットは1つだけ
        val shortcuts = repository.layoutState.value.shortcuts.values
            .filter { it.type == ShortcutType.CALENDAR }
        assertEquals(1, shortcuts.size)
    }

    // ===== swapShortcuts =====

    @Test
    fun swapShortcuts_placedShortcut_swapsToTargetSlot() {
        useCase.placeApp(0, 0, 0, "com.example.a", "App A")
        useCase.placeApp(0, 0, 1, "com.example.b", "App B")

        val state = repository.layoutState.value
        val shortcutA = state.shortcuts.values.find { it.packageName == "com.example.a" }!!
        val shortcutB = state.shortcuts.values.find { it.packageName == "com.example.b" }!!

        // App A (row=0, col=0) を row=0, col=1 にスワップ
        useCase.swapShortcuts(shortcutA, 0, 0, 1)

        val newState = repository.layoutState.value
        val placementAtCol0 = newState.placements.find { it.row == 0 && it.column == 0 }
        val placementAtCol1 = newState.placements.find { it.row == 0 && it.column == 1 }

        // A と B が入れ替わっていること
        assertEquals(shortcutB.id, placementAtCol0?.shortcutId)
        assertEquals(shortcutA.id, placementAtCol1?.shortcutId)
    }

    @Test
    fun swapShortcuts_unplacedToPlaced_movesShortcut() {
        useCase.placeApp(0, 0, 0, "com.example.b", "App B")

        val state = repository.layoutState.value
        val shortcutA = com.coelacanth9.simplecustomlauncher.model.ShortcutItem(
            id = "unplaced",
            type = ShortcutType.APP,
            label = "Unplaced",
            packageName = "com.example.unplaced"
        )
        repository.saveShortcut(shortcutA) // 未配置で保存

        // 未配置ショートカットを col=0 へ移動
        useCase.swapShortcuts(shortcutA, 0, 0, 0)

        val newState = repository.layoutState.value
        val placementAtCol0 = newState.placements.find { it.row == 0 && it.column == 0 }
        assertEquals("unplaced", placementAtCol0?.shortcutId)
    }

    // ===== placeContact =====

    @Test
    fun placeContact_phone_createsShortcutWithPhoneNumber() {
        useCase.placeContact(0, 0, 0, "田中 太郎", "090-1234-5678", ShortcutType.PHONE)

        val shortcut = repository.layoutState.value.shortcuts.values
            .find { it.phoneNumber == "090-1234-5678" }
        assertNotNull(shortcut)
        assertEquals(ShortcutType.PHONE, shortcut!!.type)
        assertEquals("田中 太郎", shortcut.label)
    }
}
