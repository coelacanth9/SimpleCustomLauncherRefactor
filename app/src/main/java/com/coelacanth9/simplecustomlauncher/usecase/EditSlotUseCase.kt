package com.coelacanth9.simplecustomlauncher.usecase

import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.shouldDeleteOnRemove
import javax.inject.Inject

class EditSlotUseCase @Inject constructor(private val repository: ShortcutRepository) {

    fun clearSlot(shortcut: ShortcutItem) {
        repository.update {
            removePlacement(shortcut.id)
            if (shouldDeleteOnRemove(shortcut)) deleteShortcut(shortcut.id)
        }
    }

    fun changeColors(pageIndex: Int, row: Int, column: Int, backgroundColor: String?, textColor: String?) {
        val target = repository.layoutState.value.placements
            .find { it.pageIndex == pageIndex && it.row == row && it.column == column } ?: return
        repository.savePlacement(target.copy(backgroundColor = backgroundColor, textColor = textColor))
    }
}
