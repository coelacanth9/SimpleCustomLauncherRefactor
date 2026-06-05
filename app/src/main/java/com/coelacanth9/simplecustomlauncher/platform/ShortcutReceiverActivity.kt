package com.coelacanth9.simplecustomlauncher.platform

import android.app.Activity
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.coelacanth9.simplecustomlauncher.R
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.usecase.AddShortcutUseCase
import java.util.UUID

/**
 * 外部アプリからの「ホーム画面に追加」リクエストを受け取るActivity
 * ※ minSdk 33 のため INSTALL_SHORTCUT 経由でしか呼ばれず、
 *   現代のアプリはすべて requestPinShortcut → MainActivity 経由のため実質到達しない。
 */
class ShortcutReceiverActivity : Activity() {

    private lateinit var repository: ShortcutRepository
    private lateinit var addShortcutUseCase: AddShortcutUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ShortcutRepository(this)
        addShortcutUseCase = AddShortcutUseCase(repository)

        Log.d(TAG, "Received intent: ${intent.action}")

        when {
            intent.hasExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST) -> handlePinItemRequest()
            intent.action == ACTION_INSTALL_SHORTCUT -> handleInstallShortcut()
            else -> {
                Log.w(TAG, "Unknown intent action: ${intent.action}")
                finish()
            }
        }
    }

    private fun handlePinItemRequest() {
        val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
        val request = launcherApps.getPinItemRequest(intent) ?: run { finish(); return }

        when (request.requestType) {
            LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT -> {
                val shortcutInfo = request.shortcutInfo ?: run { finish(); return }
                Log.d(TAG, "Shortcut: ${shortcutInfo.shortLabel}, package: ${shortcutInfo.`package`}")
                val item = ShortcutItem(
                    id = UUID.randomUUID().toString(),
                    type = ShortcutType.INTENT,
                    label = shortcutInfo.shortLabel?.toString() ?: getString(R.string.shortcut),
                    packageName = shortcutInfo.`package`,
                    intentUri = null,
                    iconUri = null
                )
                repository.savePinShortcutInfo(item.id, shortcutInfo.id, shortcutInfo.`package`)
                if (addShortcutUseCase.addToFirstEmpty(item)) {
                    request.accept()
                    Toast.makeText(this, getString(R.string.item_added, item.label), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.no_empty_slot), Toast.LENGTH_SHORT).show()
                }
            }
            else -> Log.w(TAG, "Unknown request type: ${request.requestType}")
        }
        finish()
    }

    private fun handleInstallShortcut() {
        val name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ?: getString(R.string.shortcut)
        val shortcutIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)
        Log.d(TAG, "Install shortcut: $name")
        if (shortcutIntent != null) {
            val item = ShortcutItem(
                id = UUID.randomUUID().toString(),
                type = ShortcutType.INTENT,
                label = name,
                intentUri = shortcutIntent.toUri(0)
            )
            if (repository.addShortcutToFirstEmpty(item)) {
                Toast.makeText(this, getString(R.string.name_added, name), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.no_empty_slot), Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    companion object {
        private const val TAG = "ShortcutReceiver"
        private const val ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT"
    }
}
