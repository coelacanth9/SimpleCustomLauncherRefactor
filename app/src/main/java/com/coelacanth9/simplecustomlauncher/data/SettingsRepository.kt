package com.coelacanth9.simplecustomlauncher.data

import android.content.Context
import android.content.SharedPreferences

enum class TapMode {
    SINGLE_TAP,
    LONG_TAP
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class VibrationStrength {
    OFF,
    WEAK,
    MEDIUM,
    STRONG
}

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    var tapMode: TapMode
        get() {
            val value = prefs.getString(KEY_TAP_MODE, TapMode.SINGLE_TAP.name)
            return try { TapMode.valueOf(value ?: TapMode.SINGLE_TAP.name) } catch (e: Exception) { TapMode.SINGLE_TAP }
        }
        set(value) = prefs.edit().putString(KEY_TAP_MODE, value.name).apply()

    var showConfirmDialog: Boolean
        get() = prefs.getBoolean(KEY_CONFIRM_DIALOG, false)
        set(value) = prefs.edit().putBoolean(KEY_CONFIRM_DIALOG, value).apply()

    var tapFeedback: Boolean
        get() = prefs.getBoolean(KEY_TAP_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_TAP_FEEDBACK, value).apply()

    var vibrationStrength: VibrationStrength
        get() {
            val value = prefs.getString(KEY_VIBRATION_STRENGTH, null)
            if (value != null) {
                return try { VibrationStrength.valueOf(value) } catch (e: Exception) { VibrationStrength.MEDIUM }
            }
            val migrated = if (tapFeedback) VibrationStrength.MEDIUM else VibrationStrength.OFF
            vibrationStrength = migrated
            return migrated
        }
        set(value) = prefs.edit().putString(KEY_VIBRATION_STRENGTH, value.name).apply()

    var themeMode: ThemeMode
        get() {
            val value = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            return try { ThemeMode.valueOf(value ?: ThemeMode.SYSTEM.name) } catch (e: Exception) { ThemeMode.SYSTEM }
        }
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    var loopPagingEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOOP_PAGING, false)
        set(value) = prefs.edit().putBoolean(KEY_LOOP_PAGING, value).apply()

    var pageCount: Int
        get() = prefs.getInt(KEY_PAGE_COUNT, 1).coerceIn(1, MAX_PAGES)
        set(value) = prefs.edit().putInt(KEY_PAGE_COUNT, value.coerceIn(1, MAX_PAGES)).apply()

    var onboardingShown: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_SHOWN, value).apply()

    var showSystemWallpaper: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SYSTEM_WALLPAPER, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_SYSTEM_WALLPAPER, value).apply()

    var termsAccepted: Boolean
        get() = prefs.getBoolean(KEY_TERMS_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_TERMS_ACCEPTED, value).apply()

    var termsShown: Boolean
        get() = prefs.getBoolean(KEY_TERMS_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_TERMS_SHOWN, value).apply()

    companion object {
        private const val PREFS_NAME = "launcher_settings"
        private const val KEY_TAP_MODE = "tap_mode"
        private const val KEY_CONFIRM_DIALOG = "show_confirm_dialog"
        private const val KEY_TAP_FEEDBACK = "tap_feedback"
        private const val KEY_VIBRATION_STRENGTH = "vibration_strength"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LOOP_PAGING = "loop_paging"
        private const val KEY_PAGE_COUNT = "page_count"
        private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"
        private const val KEY_SHOW_SYSTEM_WALLPAPER = "show_system_wallpaper"
        private const val KEY_TERMS_ACCEPTED = "terms_accepted"
        private const val KEY_TERMS_SHOWN = "terms_shown"

        const val MAX_PAGES = 5
    }
}
