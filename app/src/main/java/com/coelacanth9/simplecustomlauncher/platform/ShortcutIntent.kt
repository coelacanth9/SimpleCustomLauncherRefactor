package com.coelacanth9.simplecustomlauncher.platform

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutItem
import com.coelacanth9.simplecustomlauncher.core.shortcut.ShortcutType

/**
 * ShortcutItem → Intent 変換。
 * Intent/Uri/ComponentName は Android 依存のため core ではなく platform 層に置く。
 */
fun ShortcutItem.toIntent(): Intent? = when (type) {
    ShortcutType.APP -> {
        packageName?.let { pkg ->
            Intent(Intent.ACTION_MAIN).apply {
                setPackage(pkg)
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
        }
    }
    ShortcutType.PHONE -> {
        phoneNumber?.let { number ->
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        }
    }
    ShortcutType.SMS -> {
        phoneNumber?.let { number ->
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
        }
    }
    ShortcutType.DIALER -> {
        Intent(Intent.ACTION_DIAL)
    }
    ShortcutType.INTENT -> {
        intentUri?.let { uri ->
            try {
                Intent.parseUri(uri, 0)
            } catch (e: Exception) {
                null
            }
        }
    }
    ShortcutType.DEVICE_SETTINGS -> {
        intentUri?.let { uri ->
            if (uri.startsWith("#component:")) {
                // コンポーネント名で直接指定
                // 例: "#component:com.android.settings/com.android.settings.Settings$TetherSettingsActivity"
                val parts = uri.removePrefix("#component:").split("/", limit = 2)
                if (parts.size == 2) {
                    Intent().apply {
                        component = ComponentName(parts[0], parts[1])
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                } else null
            } else {
                // アクション文字列で指定
                // 例: "android.settings.WIFI_SETTINGS"
                Intent(uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        }
    }
    else -> null
}
