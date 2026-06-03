package com.coelacanth9.simplecustomlauncher.usecase

import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.HomeLayoutConfig
import com.coelacanth9.simplecustomlauncher.model.RowConfig
import com.coelacanth9.simplecustomlauncher.model.isLinkRelated
import com.coelacanth9.simplecustomlauncher.model.shouldDeleteOnRemove

class RowUseCase(
    private val repository: ShortcutRepository
) {

    fun deleteRow(pageIndex: Int, rowIndex: Int) {
        val state = repository.layoutState.value
        val newRows = state.config.rows.filter {
            !(it.pageIndex == pageIndex && it.rowIndex == rowIndex)
        }
        repository.update {
            state.placements.filter { it.pageIndex == pageIndex && it.row == rowIndex }.forEach { placement ->
                removePlacement(placement.shortcutId)
                state.shortcuts[placement.shortcutId]?.let { shortcut ->
                    if (shouldDeleteOnRemove(shortcut)) deleteShortcut(shortcut.id)
                }
            }
            saveLayoutConfig(HomeLayoutConfig(rows = newRows))
        }
    }

    fun insertRowAt(pageIndex: Int, beforeRowIndex: Int, columns: Int) {
        val currentConfig = repository.layoutState.value.config
        val newRows = currentConfig.rows.map { row ->
            if (row.pageIndex == pageIndex && row.rowIndex >= beforeRowIndex)
                row.copy(rowIndex = row.rowIndex + 1)
            else row
        } + RowConfig(pageIndex = pageIndex, rowIndex = beforeRowIndex, columns = columns)
        repository.update {
            shiftPlacementsRow(pageIndex, beforeRowIndex, 1)
            saveLayoutConfig(HomeLayoutConfig(rows = newRows))
        }
    }

    fun changeRowColumns(pageIndex: Int, rowIndex: Int, newColumns: Int) {
        val state = repository.layoutState.value
        val newRows = state.config.rows.map { row ->
            if (row.pageIndex == pageIndex && row.rowIndex == rowIndex) row.copy(columns = newColumns)
            else row
        }
        repository.update {
            // はみ出した列の配置を削除
            state.placements
                .filter { it.pageIndex == pageIndex && it.row == rowIndex && it.column >= newColumns }
                .forEach { placement ->
                    removePlacement(placement.shortcutId)
                    state.shortcuts[placement.shortcutId]?.let { shortcut ->
                        if (shouldDeleteOnRemove(shortcut)) deleteShortcut(shortcut.id)
                    }
                }
            // 3列以上に変更する場合、Link系を削除
            if (newColumns >= 3) {
                state.placements
                    .filter { it.pageIndex == pageIndex && it.row == rowIndex && it.column < newColumns }
                    .forEach { placement ->
                        state.shortcuts[placement.shortcutId]?.let { shortcut ->
                            if (isLinkRelated(shortcut)) {
                                removePlacement(placement.shortcutId)
                                if (shouldDeleteOnRemove(shortcut)) deleteShortcut(shortcut.id)
                            }
                        }
                    }
            }
            saveLayoutConfig(HomeLayoutConfig(rows = newRows))
        }
    }

    fun changeTextOnly(pageIndex: Int, rowIndex: Int, textOnly: Boolean) {
        val currentConfig = repository.layoutState.value.config
        val newRows = currentConfig.rows.map { row ->
            if (row.pageIndex == pageIndex && row.rowIndex == rowIndex) row.copy(textOnly = textOnly)
            else row
        }
        repository.update {
            saveLayoutConfig(HomeLayoutConfig(rows = newRows))
        }
    }
}
