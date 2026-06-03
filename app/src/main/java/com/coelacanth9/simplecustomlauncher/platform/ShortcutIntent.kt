package com.coelacanth9.simplecustomlauncher.platform

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.ShortcutType

/**
 * ShortcutItem から起動用 Intent を生成する拡張関数。
 * Intent / Uri 依存の配管なので platform 層に置く（core から除外）。
 */
fun ShortcutItem.toIntent(): Intent? = when (type) {
    ShortcutType.APP -> packageName?.let { pkg ->
        Intent(Intent.ACTION_MAIN).apply {
            setPackage(pkg)
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
    }
    ShortcutType.PHONE -> phoneNumber?.let { number ->
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
    }
    ShortcutType.SMS -> phoneNumber?.let { number ->
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
    }
    ShortcutType.DIALER -> Intent(Intent.ACTION_DIAL)
    ShortcutType.INTENT -> intentUri?.let { uri ->
        try { Intent.parseUri(uri, 0) } catch (e: Exception) { null }
    }
    ShortcutType.DEVICE_SETTINGS -> intentUri?.let { uri ->
        if (uri.startsWith("#component:")) {
            val parts = uri.removePrefix("#component:").split("/", limit = 2)
            if (parts.size == 2) {
                Intent().apply {
                    component = ComponentName(parts[0], parts[1])
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else null
        } else {
            Intent(uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        }
    }
    ShortcutType.CALENDAR,
    ShortcutType.MEMO,
    ShortcutType.SETTINGS,
    ShortcutType.ALL_APPS,
    ShortcutType.DATE_DISPLAY,
    ShortcutType.TIME_DISPLAY,
    ShortcutType.EMPTY -> null
}
