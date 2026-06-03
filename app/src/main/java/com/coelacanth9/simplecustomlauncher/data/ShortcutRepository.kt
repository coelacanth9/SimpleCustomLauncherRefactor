package com.coelacanth9.simplecustomlauncher.data

import android.content.Context
import android.content.SharedPreferences
import com.coelacanth9.simplecustomlauncher.model.HomeLayoutConfig
import com.coelacanth9.simplecustomlauncher.model.LayoutState
import com.coelacanth9.simplecustomlauncher.model.RowConfig
import com.coelacanth9.simplecustomlauncher.model.ShortcutPlacement
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class ShortcutRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val shortcutLock = Any()
    private val placementLock = Any()
    private val layoutLock = Any()
    private val isUpdating = AtomicBoolean(false)

    // ===== リアクティブ状態 =====

    private val _layoutState = MutableStateFlow(loadLayoutState())
    val layoutState: StateFlow<LayoutState> = _layoutState.asStateFlow()

    private fun loadLayoutState(): LayoutState = synchronized(shortcutLock) {
        synchronized(placementLock) {
            synchronized(layoutLock) {
                LayoutState(
                    config = getLayoutConfigInternal(),
                    shortcuts = getAllShortcutsInternal(),
                    placements = getAllPlacementsInternal()
                )
            }
        }
    }

    private fun notifyChange() {
        if (isUpdating.get()) return
        _layoutState.value = loadLayoutState()
    }

    fun update(block: ShortcutRepository.() -> Unit) {
        isUpdating.set(true)
        try {
            block()
        } finally {
            isUpdating.set(false)
        }
        notifyChange()
    }

    // ===== ショートカット =====

    fun saveShortcut(item: ShortcutItem) {
        synchronized(shortcutLock) {
            val shortcuts = getAllShortcutsInternal().toMutableMap()
            shortcuts[item.id] = item
            saveAllShortcuts(shortcuts.values.toList())
        }
        notifyChange()
    }

    fun deleteShortcut(id: String) {
        synchronized(shortcutLock) {
            val shortcuts = getAllShortcutsInternal().toMutableMap()
            shortcuts.remove(id)
            saveAllShortcuts(shortcuts.values.toList())
        }
        synchronized(placementLock) {
            val placements = getAllPlacementsInternal().filter { it.shortcutId != id }
            saveAllPlacements(placements)
        }
        notifyChange()
    }

    fun getShortcut(id: String): ShortcutItem? = getAllShortcuts()[id]

    fun getAllShortcuts(): Map<String, ShortcutItem> {
        synchronized(shortcutLock) { return getAllShortcutsInternal() }
    }

    fun getShortcutsByPackageName(packageName: String): List<ShortcutItem> =
        getAllShortcuts().values.filter { it.packageName == packageName }

    private fun getAllShortcutsInternal(): Map<String, ShortcutItem> {
        val json = prefs.getString(KEY_SHORTCUTS, null) ?: return emptyMap()
        return try {
            val array = JSONArray(json)
            val map = mutableMapOf<String, ShortcutItem>()
            for (i in 0 until array.length()) {
                val item = jsonToShortcutItem(array.getJSONObject(i))
                map[item.id] = item
            }
            map
        } catch (e: Exception) { emptyMap() }
    }

    private fun saveAllShortcuts(shortcuts: List<ShortcutItem>) {
        val array = JSONArray()
        shortcuts.forEach { array.put(shortcutItemToJson(it)) }
        prefs.edit().putString(KEY_SHORTCUTS, array.toString()).apply()
    }

    // ===== 配置情報 =====

    fun savePlacement(placement: ShortcutPlacement) {
        synchronized(placementLock) {
            val placements = getAllPlacementsInternal().toMutableList()
            placements.removeAll {
                it.pageIndex == placement.pageIndex &&
                it.row == placement.row &&
                it.column == placement.column
            }
            placements.add(placement)
            saveAllPlacements(placements)
        }
        notifyChange()
    }

    fun getPlacementsForPage(pageIndex: Int): List<ShortcutPlacement> =
        getAllPlacements().filter { it.pageIndex == pageIndex }

    fun removePlacement(shortcutId: String) {
        synchronized(placementLock) {
            val placements = getAllPlacementsInternal().filter { it.shortcutId != shortcutId }
            saveAllPlacements(placements)
        }
        notifyChange()
    }

    fun getAllPlacements(): List<ShortcutPlacement> {
        synchronized(placementLock) { return getAllPlacementsInternal() }
    }

    fun shiftPlacementsRow(pageIndex: Int, fromRowIndex: Int, delta: Int) {
        synchronized(placementLock) {
            val placements = getAllPlacementsInternal().map { placement ->
                if (placement.pageIndex == pageIndex && placement.row >= fromRowIndex)
                    placement.copy(row = placement.row + delta)
                else placement
            }
            saveAllPlacements(placements)
        }
        notifyChange()
    }

    private fun getAllPlacementsInternal(): List<ShortcutPlacement> {
        val json = prefs.getString(KEY_PLACEMENTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { jsonToPlacement(array.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    private fun saveAllPlacements(placements: List<ShortcutPlacement>) {
        val array = JSONArray()
        placements.forEach { array.put(placementToJson(it)) }
        prefs.edit().putString(KEY_PLACEMENTS, array.toString()).apply()
    }

    // ===== レイアウト設定 =====

    fun saveLayoutConfig(config: HomeLayoutConfig) {
        synchronized(layoutLock) { saveLayoutConfigInternal(config) }
        notifyChange()
    }

    private fun saveLayoutConfigInternal(config: HomeLayoutConfig) {
        val array = JSONArray()
        config.rows.forEach { row ->
            array.put(JSONObject().apply {
                put("pageIndex", row.pageIndex)
                put("rowIndex", row.rowIndex)
                put("columns", row.columns)
                if (row.fixedHeightDp != null) put("fixedHeightDp", row.fixedHeightDp)
                put("textOnly", row.textOnly)
            })
        }
        prefs.edit().putString(KEY_LAYOUT, array.toString()).apply()
    }

    fun getLayoutConfig(): HomeLayoutConfig {
        synchronized(layoutLock) { return getLayoutConfigInternal() }
    }

    private fun getLayoutConfigInternal(): HomeLayoutConfig {
        val json = prefs.getString(KEY_LAYOUT, null) ?: return HomeLayoutConfig()
        return try {
            val array = JSONArray(json)
            val rows = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                RowConfig(
                    pageIndex = obj.optInt("pageIndex", 0),
                    rowIndex = obj.getInt("rowIndex"),
                    columns = obj.getInt("columns"),
                    fixedHeightDp = if (obj.has("fixedHeightDp")) obj.getInt("fixedHeightDp") else null,
                    textOnly = obj.optBoolean("textOnly", false)
                )
            }
            HomeLayoutConfig(rows)
        } catch (e: Exception) { HomeLayoutConfig() }
    }

    // ===== デフォルトレイアウト =====

    fun isFirstLaunch(): Boolean = !prefs.getBoolean(KEY_INITIALIZED, false)

    fun markAsInitialized() {
        prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
    }

    fun clearAllLayout() {
        prefs.edit().remove(KEY_SHORTCUTS).remove(KEY_PLACEMENTS).remove(KEY_LAYOUT).apply()
        saveLayoutConfig(HomeLayoutConfig(rows = emptyList()))
    }

    // ===== JSON変換 =====

    private fun shortcutItemToJson(item: ShortcutItem): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("type", item.type.name)
        put("label", item.label)
        put("packageName", item.packageName)
        put("intentUri", item.intentUri)
        put("phoneNumber", item.phoneNumber)
        put("iconUri", item.iconUri)
    }

    private fun jsonToShortcutItem(json: JSONObject): ShortcutItem = ShortcutItem(
        id = json.getString("id"),
        type = ShortcutType.valueOf(json.getString("type")),
        label = json.getString("label"),
        packageName = json.optString("packageName", null),
        intentUri = json.optString("intentUri", null),
        phoneNumber = json.optString("phoneNumber", null),
        iconUri = json.optString("iconUri", null)
    )

    private fun placementToJson(placement: ShortcutPlacement): JSONObject = JSONObject().apply {
        put("shortcutId", placement.shortcutId)
        put("pageIndex", placement.pageIndex)
        put("row", placement.row)
        put("column", placement.column)
        put("spanX", placement.spanX)
        put("spanY", placement.spanY)
        if (placement.backgroundColor != null) put("backgroundColor", placement.backgroundColor)
        if (placement.textColor != null) put("textColor", placement.textColor)
    }

    private fun jsonToPlacement(json: JSONObject): ShortcutPlacement = ShortcutPlacement(
        shortcutId = json.getString("shortcutId"),
        pageIndex = json.optInt("pageIndex", 0),
        row = json.getInt("row"),
        column = json.getInt("column"),
        spanX = json.optInt("spanX", 1),
        spanY = json.optInt("spanY", 1),
        backgroundColor = json.optString("backgroundColor", "").takeIf { it.isNotEmpty() },
        textColor = json.optString("textColor", "").takeIf { it.isNotEmpty() }
    )

    // ===== Pinショートカット情報 =====

    private val pinPrefs: SharedPreferences = context.getSharedPreferences(
        PIN_PREFS_NAME, Context.MODE_PRIVATE
    )

    fun savePinShortcutInfo(itemId: String, shortcutId: String, packageName: String) {
        pinPrefs.edit()
            .putString("${itemId}_shortcut_id", shortcutId)
            .putString("${itemId}_package", packageName)
            .apply()
    }

    fun getPinShortcutInfo(itemId: String): Pair<String?, String?> {
        val shortcutId = pinPrefs.getString("${itemId}_shortcut_id", null)
        val packageName = pinPrefs.getString("${itemId}_package", null)
        return shortcutId to packageName
    }

    fun deletePinShortcutInfo(itemId: String) {
        pinPrefs.edit()
            .remove("${itemId}_shortcut_id")
            .remove("${itemId}_package")
            .apply()
    }

    fun findOrphanedPinShortcuts(): List<Triple<String, String, String>> {
        val shortcuts = getAllShortcuts()
        val itemIds = pinPrefs.all.keys
            .filter { it.endsWith("_shortcut_id") }
            .map { it.removeSuffix("_shortcut_id") }
        return itemIds.mapNotNull { itemId ->
            if (shortcuts.containsKey(itemId)) return@mapNotNull null
            val pinShortcutId = pinPrefs.getString("${itemId}_shortcut_id", null) ?: return@mapNotNull null
            val packageName = pinPrefs.getString("${itemId}_package", null) ?: return@mapNotNull null
            Triple(itemId, pinShortcutId, packageName)
        }
    }

    companion object {
        private const val PREFS_NAME = "launcher_shortcuts"
        private const val PIN_PREFS_NAME = "pin_shortcuts"
        private const val KEY_SHORTCUTS = "shortcuts"
        private const val KEY_PLACEMENTS = "placements"
        private const val KEY_LAYOUT = "layout"
        private const val KEY_INITIALIZED = "initialized"
    }
}
