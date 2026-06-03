package com.coelacanth9.simplecustomlauncher.usecase

import com.coelacanth9.simplecustomlauncher.data.SettingsRepository
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.HomeLayoutConfig
import com.coelacanth9.simplecustomlauncher.model.shouldDeleteOnRemove

class DeletePageUseCase(
    private val repository: ShortcutRepository,
    private val settingsRepository: SettingsRepository
) {

    /**
     * 指定ページを削除する。
     * @return 削除した場合 true、ページが1枚しかなく削除できない場合 false
     */
    fun execute(pageIndex: Int): Boolean {
        val currentPageCount = settingsRepository.pageCount
        if (currentPageCount <= 1) return false

        val state = repository.layoutState.value
        val pagePlacements = state.placements.filter { it.pageIndex == pageIndex }
        val remainingPlacements = state.placements.filter { it.pageIndex != pageIndex }
        val newRows = state.config.rows
            .filter { it.pageIndex != pageIndex }
            .map { row ->
                if (row.pageIndex > pageIndex) row.copy(pageIndex = row.pageIndex - 1) else row
            }

        repository.update {
            // 削除ページの配置・ショートカットを削除
            pagePlacements.forEach { placement ->
                removePlacement(placement.shortcutId)
                state.shortcuts[placement.shortcutId]?.let { shortcut ->
                    if (shouldDeleteOnRemove(shortcut)) deleteShortcut(shortcut.id)
                }
            }
            // 行設定を更新（削除ページを除外し、後続ページ番号を繰り上げ）
            saveLayoutConfig(HomeLayoutConfig(rows = newRows))
            // 後続ページの配置をページ番号繰り上げ
            remainingPlacements.filter { it.pageIndex > pageIndex }.forEach { placement ->
                savePlacement(placement.copy(pageIndex = placement.pageIndex - 1))
            }
        }

        settingsRepository.pageCount = currentPageCount - 1
        return true
    }
}
