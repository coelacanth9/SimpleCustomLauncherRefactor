package com.coelacanth9.simplecustomlauncher.usecase

import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.HomeLayoutConfig
import com.coelacanth9.simplecustomlauncher.model.RowConfig
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.ShortcutPlacement
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import java.util.UUID
import javax.inject.Inject

class PlaceShortcutUseCase @Inject constructor(private val repository: ShortcutRepository) {

    fun placeApp(pageIndex: Int, rowIndex: Int, column: Int, packageName: String, label: String) {
        val allShortcuts = repository.layoutState.value.shortcuts
        val existing = allShortcuts.values.find {
            it.type == ShortcutType.APP && it.packageName == packageName
        }
        val item = if (existing != null && existing.label != label) existing.copy(label = label)
                   else existing ?: ShortcutItem(UUID.randomUUID().toString(), ShortcutType.APP, label, packageName = packageName)
        val newRows = computeUpdatedRows(pageIndex, rowIndex, item.type)
        repository.update {
            saveShortcut(item)
            saveLayoutConfig(HomeLayoutConfig(rows = newRows))
            savePlacement(ShortcutPlacement(shortcutId = item.id, pageIndex = pageIndex, row = rowIndex, column = column))
        }
    }

    fun placeInternalFeature(pageIndex: Int, rowIndex: Int, column: Int, type: ShortcutType, label: String) {
        val allShortcuts = repository.layoutState.value.shortcuts
        val existing = allShortcuts.values.find { it.type == type && it.packageName == null }
        val item = existing ?: ShortcutItem(UUID.randomUUID().toString(), type, label)
        val newRows = computeUpdatedRows(pageIndex, rowIndex, item.type)
        repository.update {
            if (existing == null) saveShortcut(item)
            saveLayoutConfig(HomeLayoutConfig(rows = newRows))
            savePlacement(ShortcutPlacement(shortcutId = item.id, pageIndex = pageIndex, row = rowIndex, column = column))
        }
    }

    fun placeDeviceSettings(pageIndex: Int, rowIndex: Int, column: Int, label: String, settingsAction: String) {
        val allShortcuts = repository.layoutState.value.shortcuts
        val existing = allShortcuts.values.find {
            it.type == ShortcutType.DEVICE_SETTINGS && it.intentUri == settingsAction
        }
        val item = existing ?: ShortcutItem(UUID.randomUUID().toString(), ShortcutType.DEVICE_SETTINGS, label, intentUri = settingsAction)
        val newRows = computeUpdatedRows(pageIndex, rowIndex, item.type)
        repository.update {
            if (existing == null) saveShortcut(item)
            saveLayoutConfig(HomeLayoutConfig(rows = newRows))
            savePlacement(ShortcutPlacement(shortcutId = item.id, pageIndex = pageIndex, row = rowIndex, column = column))
        }
    }

    fun placeContact(
        pageIndex: Int, rowIndex: Int, column: Int,
        name: String, phoneNumber: String, type: ShortcutType, targetPackage: String? = null
    ) {
        val allShortcuts = repository.layoutState.value.shortcuts
        val existing = allShortcuts.values.find {
            it.type == type && it.phoneNumber == phoneNumber && it.packageName == targetPackage
        }
        val item = if (existing != null && existing.label != name) existing.copy(label = name)
                   else existing ?: ShortcutItem(UUID.randomUUID().toString(), type, name, phoneNumber = phoneNumber, packageName = targetPackage)
        val newRows = computeUpdatedRows(pageIndex, rowIndex, item.type)
        repository.update {
            saveShortcut(item)
            saveLayoutConfig(HomeLayoutConfig(rows = newRows))
            savePlacement(ShortcutPlacement(shortcutId = item.id, pageIndex = pageIndex, row = rowIndex, column = column))
        }
    }

    fun placeIntent(
        pageIndex: Int, rowIndex: Int, column: Int,
        shortLabel: String, packageName: String, shortcutId: String
    ) {
        val allShortcuts = repository.layoutState.value.shortcuts
        val existing = allShortcuts.values.find {
            it.type == ShortcutType.INTENT && it.packageName == packageName &&
                repository.getPinShortcutInfo(it.id).first == shortcutId
        }
        val item = if (existing != null && existing.label != shortLabel) existing.copy(label = shortLabel)
                   else existing ?: ShortcutItem(UUID.randomUUID().toString(), ShortcutType.INTENT, shortLabel, packageName = packageName)
        val newRows = computeUpdatedRows(pageIndex, rowIndex, item.type)
        repository.update {
            saveShortcut(item)
            savePinShortcutInfo(item.id, shortcutId, packageName)
            saveLayoutConfig(HomeLayoutConfig(rows = newRows))
            savePlacement(ShortcutPlacement(shortcutId = item.id, pageIndex = pageIndex, row = rowIndex, column = column))
        }
    }

    fun swapShortcuts(targetShortcut: ShortcutItem, toPageIndex: Int, toRow: Int, toColumn: Int) {
        val state = repository.layoutState.value
        val currentShortcutId = state.placements
            .find { it.pageIndex == toPageIndex && it.row == toRow && it.column == toColumn }
            ?.shortcutId
        val currentShortcut = currentShortcutId?.let { state.shortcuts[it] }
        val existingPlacement = state.placements.find { it.shortcutId == targetShortcut.id }

        repository.update {
            if (existingPlacement != null) {
                if (currentShortcut != null) {
                    savePlacement(ShortcutPlacement(
                        shortcutId = currentShortcut.id,
                        pageIndex = existingPlacement.pageIndex,
                        row = existingPlacement.row,
                        column = existingPlacement.column
                    ))
                } else {
                    removePlacement(targetShortcut.id)
                }
            }
            savePlacement(ShortcutPlacement(
                shortcutId = targetShortcut.id,
                pageIndex = toPageIndex,
                row = toRow,
                column = toColumn
            ))
        }
    }

    private fun computeUpdatedRows(pageIndex: Int, rowIndex: Int, type: ShortcutType): List<RowConfig> {
        val newFixedHeight = when (type) {
            ShortcutType.TIME_DISPLAY -> 80
            ShortcutType.DATE_DISPLAY -> 56
            else -> null
        }
        return repository.layoutState.value.config.rows.map { row ->
            if (row.pageIndex == pageIndex && row.rowIndex == rowIndex) row.copy(fixedHeightDp = newFixedHeight)
            else row
        }
    }
}
