package com.coelacanth9.simplecustomlauncher.usecase

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.HomeLayoutConfig
import com.coelacanth9.simplecustomlauncher.model.RowConfig
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.ShortcutPlacement
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * RowUseCase のインスツルメンテッドテスト。
 * 行の削除・挿入・列変更・textOnly 切替を検証する。
 */
@RunWith(AndroidJUnit4::class)
class RowUseCaseTest {

    private lateinit var repository: ShortcutRepository
    private lateinit var useCase: RowUseCase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("launcher_shortcuts", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("pin_shortcuts", Context.MODE_PRIVATE).edit().clear().commit()
        repository = ShortcutRepository(context)
        useCase = RowUseCase(repository)
    }

    // ===== deleteRow =====

    @Test
    fun deleteRow_removesRowConfigFromLayout() {
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2),
            RowConfig(pageIndex = 0, rowIndex = 1, columns = 2)
        )))

        useCase.deleteRow(0, 0)

        val rows = repository.layoutState.value.config.rows
        assertEquals(1, rows.size)
        assertEquals(1, rows[0].rowIndex)
    }

    @Test
    fun deleteRow_removesPlacementsInDeletedRow() {
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2),
            RowConfig(pageIndex = 0, rowIndex = 1, columns = 2)
        )))
        val item = ShortcutItem(id = "id1", type = ShortcutType.PHONE, label = "連絡先", phoneNumber = "090-0000-0000")
        repository.saveShortcut(item)
        repository.savePlacement(ShortcutPlacement(shortcutId = "id1", pageIndex = 0, row = 0, column = 0))

        useCase.deleteRow(0, 0)

        val placements = repository.layoutState.value.placements
        assertTrue(placements.none { it.shortcutId == "id1" })
    }

    @Test
    fun deleteRow_appShortcut_deletesShortcutToo() {
        // APP は shouldDeleteOnRemove=true のため削除される
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2)
        )))
        val item = ShortcutItem(id = "app1", type = ShortcutType.APP, label = "App", packageName = "com.example")
        repository.saveShortcut(item)
        repository.savePlacement(ShortcutPlacement(shortcutId = "app1", pageIndex = 0, row = 0, column = 0))

        useCase.deleteRow(0, 0)

        assertNull(repository.getShortcut("app1"))
    }

    @Test
    fun deleteRow_phoneShortcut_keepsShortcutInStorage() {
        // PHONE は shouldDeleteOnRemove=false のため未配置リストに残る
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2)
        )))
        val item = ShortcutItem(id = "phone1", type = ShortcutType.PHONE, label = "田中", phoneNumber = "090-0000-0000")
        repository.saveShortcut(item)
        repository.savePlacement(ShortcutPlacement(shortcutId = "phone1", pageIndex = 0, row = 0, column = 0))

        useCase.deleteRow(0, 0)

        assertNotNull(repository.getShortcut("phone1")) // ショートカット自体は残る
        assertTrue(repository.getAllPlacements().none { it.shortcutId == "phone1" }) // 配置だけ消える
    }

    // ===== insertRowAt =====

    @Test
    fun insertRowAt_insertsNewRowAtSpecifiedIndex() {
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2),
            RowConfig(pageIndex = 0, rowIndex = 1, columns = 2)
        )))

        useCase.insertRowAt(0, 1, 3)

        val rows = repository.layoutState.value.config.rows
        assertEquals(3, rows.size)
        val inserted = rows.find { it.rowIndex == 1 }
        assertNotNull(inserted)
        assertEquals(3, inserted!!.columns)
    }

    @Test
    fun insertRowAt_shiftsExistingPlacementsDown() {
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2),
            RowConfig(pageIndex = 0, rowIndex = 1, columns = 2)
        )))
        val item = ShortcutItem(id = "id1", type = ShortcutType.PHONE, label = "連絡先", phoneNumber = "090-0000-0000")
        repository.saveShortcut(item)
        repository.savePlacement(ShortcutPlacement(shortcutId = "id1", pageIndex = 0, row = 1, column = 0))

        useCase.insertRowAt(0, 1, 2) // row=1 の前に挿入

        val placement = repository.layoutState.value.placements.find { it.shortcutId == "id1" }
        assertEquals(2, placement?.row) // row=1 → row=2 にシフト
    }

    @Test
    fun insertRowAt_doesNotShiftPlacementsBeforeInsertPoint() {
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2),
            RowConfig(pageIndex = 0, rowIndex = 1, columns = 2)
        )))
        val item = ShortcutItem(id = "id0", type = ShortcutType.PHONE, label = "連絡先", phoneNumber = "090-0000-0000")
        repository.saveShortcut(item)
        repository.savePlacement(ShortcutPlacement(shortcutId = "id0", pageIndex = 0, row = 0, column = 0))

        useCase.insertRowAt(0, 1, 2) // row=1 の前に挿入

        val placement = repository.layoutState.value.placements.find { it.shortcutId == "id0" }
        assertEquals(0, placement?.row) // row=0 はシフトされない
    }

    // ===== changeRowColumns =====

    @Test
    fun changeRowColumns_reducingColumns_updatesRowConfig() {
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 3)
        )))

        useCase.changeRowColumns(0, 0, 2)

        val row = repository.layoutState.value.config.rows.find { it.rowIndex == 0 }
        assertEquals(2, row?.columns)
    }

    @Test
    fun changeRowColumns_reducingColumns_removesExcessPlacements() {
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 3)
        )))
        repository.update {
            saveShortcut(ShortcutItem(id = "a", type = ShortcutType.APP, label = "A", packageName = "com.a"))
            saveShortcut(ShortcutItem(id = "b", type = ShortcutType.APP, label = "B", packageName = "com.b"))
            saveShortcut(ShortcutItem(id = "c", type = ShortcutType.APP, label = "C", packageName = "com.c"))
            savePlacement(ShortcutPlacement(shortcutId = "a", pageIndex = 0, row = 0, column = 0))
            savePlacement(ShortcutPlacement(shortcutId = "b", pageIndex = 0, row = 0, column = 1))
            savePlacement(ShortcutPlacement(shortcutId = "c", pageIndex = 0, row = 0, column = 2))
        }

        useCase.changeRowColumns(0, 0, 2)

        val placements = repository.layoutState.value.placements
        assertEquals(2, placements.size)
        assertTrue(placements.none { it.column >= 2 })
    }

    @Test
    fun changeRowColumns_doesNotAffectOtherRows() {
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 3),
            RowConfig(pageIndex = 0, rowIndex = 1, columns = 3)
        )))
        repository.update {
            saveShortcut(ShortcutItem(id = "x", type = ShortcutType.APP, label = "X", packageName = "com.x"))
            savePlacement(ShortcutPlacement(shortcutId = "x", pageIndex = 0, row = 1, column = 2))
        }

        useCase.changeRowColumns(0, 0, 2) // row=0 のみ変更

        val placement = repository.layoutState.value.placements.find { it.shortcutId == "x" }
        assertNotNull(placement) // row=1 の配置は影響を受けない
    }

    // ===== changeTextOnly =====

    @Test
    fun changeTextOnly_setsTextOnlyTrue() {
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2, textOnly = false)
        )))

        useCase.changeTextOnly(0, 0, true)

        val row = repository.layoutState.value.config.rows.find { it.rowIndex == 0 }
        assertTrue(row!!.textOnly)
    }

    @Test
    fun changeTextOnly_setsTextOnlyFalse() {
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2, textOnly = true)
        )))

        useCase.changeTextOnly(0, 0, false)

        val row = repository.layoutState.value.config.rows.find { it.rowIndex == 0 }
        assertFalse(row!!.textOnly)
    }
}
