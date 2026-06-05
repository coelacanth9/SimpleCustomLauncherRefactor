package com.coelacanth9.simplecustomlauncher.usecase

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.HomeLayoutConfig
import com.coelacanth9.simplecustomlauncher.model.RowConfig
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.ShortcutPlacement
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AddShortcutUseCase のインスツルメンテッドテスト。
 * 空きスロットへの配置・全スロット埋まり時の動作を検証する。
 */
@RunWith(AndroidJUnit4::class)
class AddShortcutUseCaseTest {

    private lateinit var context: Context
    private lateinit var repository: ShortcutRepository
    private lateinit var useCase: AddShortcutUseCase

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // テストごとにリポジトリをクリーンな状態で初期化
        context.getSharedPreferences("launcher_shortcuts", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("pin_shortcuts", Context.MODE_PRIVATE).edit().clear().commit()
        repository = ShortcutRepository(context)
        useCase = AddShortcutUseCase(repository)
    }

    @Test
    fun addToFirstEmpty_whenEmptySlotExists_placesShortcutAndReturnsTrue() {
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(rowIndex = 0, columns = 2)
        )))
        val item = ShortcutItem(
            id = "id1",
            type = ShortcutType.APP,
            label = "Test App",
            packageName = "com.example.test"
        )

        val result = useCase.addToFirstEmpty(item)

        assertTrue(result)
        val placements = repository.getAllPlacements()
        assertEquals(1, placements.size)
        assertEquals("id1", placements[0].shortcutId)
        assertEquals(0, placements[0].row)
        assertEquals(0, placements[0].column)
    }

    @Test
    fun addToFirstEmpty_whenAllSlotsFull_savesShortcutWithoutPlacementAndReturnsFalse() {
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(rowIndex = 0, columns = 1)
        )))
        // スロットを埋めておく
        repository.savePlacement(ShortcutPlacement(shortcutId = "existing", row = 0, column = 0))

        val item = ShortcutItem(
            id = "id1",
            type = ShortcutType.APP,
            label = "Test App",
            packageName = "com.example.test"
        )

        val result = useCase.addToFirstEmpty(item)

        assertFalse(result)
        // ショートカットは保存されているが配置はない
        assertNotNull(repository.getShortcut("id1"))
        val placements = repository.getAllPlacements()
        assertEquals(1, placements.size) // 既存の1件のみ
        assertTrue(placements.none { it.shortcutId == "id1" })
    }

    @Test
    fun addToFirstEmpty_placesIntoFirstEmptyColumn() {
        repository.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(rowIndex = 0, columns = 3)
        )))
        // 0列目は埋まっている
        repository.savePlacement(ShortcutPlacement(shortcutId = "existing", row = 0, column = 0))

        val item = ShortcutItem(
            id = "id1",
            type = ShortcutType.APP,
            label = "App",
            packageName = "com.example"
        )

        useCase.addToFirstEmpty(item)

        val placement = repository.getAllPlacements().find { it.shortcutId == "id1" }
        assertNotNull(placement)
        assertEquals(0, placement!!.row)
        assertEquals(1, placement.column) // 1列目に配置される
    }
}
