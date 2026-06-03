package com.coelacanth9.simplecustomlauncher.usecase

import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.ShortcutPlacement

class AddShortcutUseCase(
    private val repository: ShortcutRepository
) {

    /**
     * 空きスロットを探してショートカットを配置する。
     * @return 空きスロットが見つかり配置できた場合 true、スロットなしで保存のみの場合 false
     */
    fun addToFirstEmpty(item: ShortcutItem): Boolean {
        val layout = repository.getLayoutConfig()
        val placements = repository.getAllPlacements()
        val emptySlot = layout.findFirstEmptySlot(placements)
        return if (emptySlot != null) {
            repository.update {
                saveShortcut(item)
                savePlacement(ShortcutPlacement(shortcutId = item.id, row = emptySlot.first, column = emptySlot.second))
            }
            true
        } else {
            repository.saveShortcut(item)
            false
        }
    }
}
