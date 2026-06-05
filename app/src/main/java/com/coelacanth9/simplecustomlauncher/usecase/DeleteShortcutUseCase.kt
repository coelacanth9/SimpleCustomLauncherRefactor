package com.coelacanth9.simplecustomlauncher.usecase

import android.util.Log
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import javax.inject.Inject

class DeleteShortcutUseCase @Inject constructor(
    private val repository: ShortcutRepository,
    private val shortcutHelper: ShortcutHelper
) {

    /**
     * 未配置ショートカットを削除する。ピンショートカットの場合はunpinも行う。
     * @return 削除に成功した場合 true、unpin失敗の場合 false
     */
    fun execute(id: String): Boolean {
        val shortcut = repository.getShortcut(id)
        if (shortcut?.type == ShortcutType.INTENT) {
            val (pinShortcutId, pinPackageName) = repository.getPinShortcutInfo(id)
            if (pinShortcutId != null && pinPackageName != null) {
                try {
                    shortcutHelper.unpinShortcuts(pinPackageName, setOf(pinShortcutId))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unpin shortcut", e)
                    return false
                }
            }
            repository.deletePinShortcutInfo(id)
        }
        repository.deleteShortcut(id)
        return true
    }

    companion object {
        private const val TAG = "DeleteShortcutUseCase"
    }
}
