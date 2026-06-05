package com.coelacanth9.simplecustomlauncher.model

import org.junit.Assert.*
import org.junit.Test

/**
 * HomeLayoutConfig のビジネスロジックを検証するユニットテスト。
 * Android 依存なし（純 Kotlin）。
 */
class HomeLayoutConfigTest {

    // ===== findFirstEmptySlot =====

    @Test
    fun findFirstEmptySlot_emptyLayout_returnsFirstSlot() {
        val config = HomeLayoutConfig(
            rows = listOf(RowConfig(rowIndex = 0, columns = 2))
        )
        val result = config.findFirstEmptySlot(emptyList())
        assertEquals(0 to 0, result)
    }

    @Test
    fun findFirstEmptySlot_firstColumnOccupied_returnsSecondColumn() {
        val config = HomeLayoutConfig(
            rows = listOf(RowConfig(rowIndex = 0, columns = 2))
        )
        val placements = listOf(
            ShortcutPlacement(shortcutId = "a", row = 0, column = 0)
        )
        val result = config.findFirstEmptySlot(placements)
        assertEquals(0 to 1, result)
    }

    @Test
    fun findFirstEmptySlot_allSlotsFull_returnsNull() {
        val config = HomeLayoutConfig(
            rows = listOf(RowConfig(rowIndex = 0, columns = 2))
        )
        val placements = listOf(
            ShortcutPlacement(shortcutId = "a", row = 0, column = 0),
            ShortcutPlacement(shortcutId = "b", row = 0, column = 1)
        )
        val result = config.findFirstEmptySlot(placements)
        assertNull(result)
    }

    @Test
    fun findFirstEmptySlot_firstRowFull_returnsSlotInNextRow() {
        val config = HomeLayoutConfig(
            rows = listOf(
                RowConfig(rowIndex = 0, columns = 2),
                RowConfig(rowIndex = 1, columns = 2)
            )
        )
        val placements = listOf(
            ShortcutPlacement(shortcutId = "a", row = 0, column = 0),
            ShortcutPlacement(shortcutId = "b", row = 0, column = 1)
        )
        val result = config.findFirstEmptySlot(placements)
        assertEquals(1 to 0, result)
    }

    @Test
    fun findFirstEmptySlot_noRows_returnsNull() {
        val config = HomeLayoutConfig(rows = emptyList())
        val result = config.findFirstEmptySlot(emptyList())
        assertNull(result)
    }

    // ===== getPageCount =====

    @Test
    fun getPageCount_noRows_returnsOne() {
        val config = HomeLayoutConfig(rows = emptyList())
        assertEquals(1, config.getPageCount())
    }

    @Test
    fun getPageCount_singlePage_returnsOne() {
        val config = HomeLayoutConfig(
            rows = listOf(
                RowConfig(pageIndex = 0, rowIndex = 0),
                RowConfig(pageIndex = 0, rowIndex = 1)
            )
        )
        assertEquals(1, config.getPageCount())
    }

    @Test
    fun getPageCount_multiplePages_returnsCorrectCount() {
        val config = HomeLayoutConfig(
            rows = listOf(
                RowConfig(pageIndex = 0, rowIndex = 0),
                RowConfig(pageIndex = 1, rowIndex = 0),
                RowConfig(pageIndex = 2, rowIndex = 0)
            )
        )
        assertEquals(3, config.getPageCount())
    }

    // ===== totalSlots =====

    @Test
    fun totalSlots_sumsColumnsAcrossAllRows() {
        val config = HomeLayoutConfig(
            rows = listOf(
                RowConfig(rowIndex = 0, columns = 2),
                RowConfig(rowIndex = 1, columns = 3)
            )
        )
        assertEquals(5, config.totalSlots())
    }

    @Test
    fun totalSlots_emptyRows_returnsZero() {
        val config = HomeLayoutConfig(rows = emptyList())
        assertEquals(0, config.totalSlots())
    }

    // ===== getColumnsForRow =====

    @Test
    fun getColumnsForRow_existingRow_returnsColumns() {
        val config = HomeLayoutConfig(
            rows = listOf(RowConfig(pageIndex = 0, rowIndex = 0, columns = 3))
        )
        assertEquals(3, config.getColumnsForRow(0, 0))
    }

    @Test
    fun getColumnsForRow_nonExistingRow_returnsDefault() {
        val config = HomeLayoutConfig(rows = emptyList())
        assertEquals(2, config.getColumnsForRow(0, 99))
    }

    // ===== isTextOnlyForRow =====

    @Test
    fun isTextOnlyForRow_textOnlyTrue_returnsTrue() {
        val config = HomeLayoutConfig(
            rows = listOf(RowConfig(pageIndex = 0, rowIndex = 0, textOnly = true))
        )
        assertTrue(config.isTextOnlyForRow(0, 0))
    }

    @Test
    fun isTextOnlyForRow_textOnlyFalse_returnsFalse() {
        val config = HomeLayoutConfig(
            rows = listOf(RowConfig(pageIndex = 0, rowIndex = 0, textOnly = false))
        )
        assertFalse(config.isTextOnlyForRow(0, 0))
    }
}
