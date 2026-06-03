package com.coelacanth9.simplecustomlauncher.model

/**
 * ショートカットの配置情報（どのスロットに何があるか）
 */
data class ShortcutPlacement(
    val shortcutId: String,
    val pageIndex: Int = 0,
    val row: Int,
    val column: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val backgroundColor: String? = null,
    val textColor: String? = null
)

/**
 * 行の設定
 */
data class RowConfig(
    val pageIndex: Int = 0,
    val rowIndex: Int,
    val columns: Int = 2,
    val fixedHeightDp: Int? = null,
    val textOnly: Boolean = false
)

/**
 * ホーム画面全体のレイアウト設定
 */
data class HomeLayoutConfig(
    val rows: List<RowConfig> = emptyList()
) {
    fun getRowsForPage(pageIndex: Int): List<RowConfig> =
        rows.filter { it.pageIndex == pageIndex }

    fun getPageCount(): Int =
        if (rows.isEmpty()) 1 else (rows.maxOfOrNull { it.pageIndex } ?: 0) + 1

    fun findFirstEmptySlot(
        placements: List<ShortcutPlacement>,
        maxPage: Int = getPageCount() - 1
    ): Triple<Int, Int, Int>? {
        for (pageIndex in 0..maxPage) {
            val pageRows = getRowsForPage(pageIndex)
            val occupied = placements.filter { it.pageIndex == pageIndex }
                .map { it.row to it.column }.toSet()
            for (row in pageRows) {
                for (col in 0 until row.columns) {
                    if ((row.rowIndex to col) !in occupied) {
                        return Triple(pageIndex, row.rowIndex, col)
                    }
                }
            }
        }
        return null
    }

    /** 後方互換用（ページ0のみ） */
    fun findFirstEmptySlot(placements: List<ShortcutPlacement>): Pair<Int, Int>? {
        val pageRows = getRowsForPage(0)
        val occupied = placements.filter { it.pageIndex == 0 }
            .map { it.row to it.column }.toSet()
        for (row in pageRows) {
            for (col in 0 until row.columns) {
                if ((row.rowIndex to col) !in occupied) return row.rowIndex to col
            }
        }
        return null
    }

    fun totalSlots(): Int = rows.sumOf { it.columns }

    fun totalSlotsForPage(pageIndex: Int): Int =
        getRowsForPage(pageIndex).sumOf { it.columns }

    fun getColumnsForRow(rowIndex: Int): Int =
        rows.find { it.rowIndex == rowIndex }?.columns ?: 2

    fun getColumnsForRow(pageIndex: Int, rowIndex: Int): Int =
        rows.find { it.pageIndex == pageIndex && it.rowIndex == rowIndex }?.columns ?: 2

    fun isTextOnlyForRow(pageIndex: Int, rowIndex: Int): Boolean =
        rows.find { it.pageIndex == pageIndex && it.rowIndex == rowIndex }?.textOnly ?: false
}
