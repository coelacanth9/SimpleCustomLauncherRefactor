package com.coelacanth9.simplecustomlauncher.usecase

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.model.ShortcutItem
import com.coelacanth9.simplecustomlauncher.model.ShortcutPlacement
import com.coelacanth9.simplecustomlauncher.model.ShortcutType
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DeleteShortcutUseCase のインスツルメンテッドテスト。
 * APP / INTENT 各タイプの削除ロジックを検証する。
 * ※ INTENT の unpin 処理はランチャー権限が必要なため、
 *   ShortcutHelper 内部で例外がキャッチされ、リポジトリ操作は必ず実行される。
 */
@RunWith(AndroidJUnit4::class)
class DeleteShortcutUseCaseTest {

    private lateinit var repository: ShortcutRepository
    private lateinit var useCase: DeleteShortcutUseCase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("launcher_shortcuts", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("pin_shortcuts", Context.MODE_PRIVATE).edit().clear().commit()
        repository = ShortcutRepository(context)
        useCase = DeleteShortcutUseCase(repository, ShortcutHelper(context))
    }

    @Test
    fun execute_appShortcut_deletesFromRepositoryAndReturnsTrue() {
        val item = ShortcutItem(
            id = "app1",
            type = ShortcutType.APP,
            label = "My App",
            packageName = "com.example.app"
        )
        repository.saveShortcut(item)
        repository.savePlacement(ShortcutPlacement(shortcutId = "app1", row = 0, column = 0))

        val result = useCase.execute("app1")

        assertTrue(result)
        assertNull(repository.getShortcut("app1"))
        assertTrue(repository.getAllPlacements().none { it.shortcutId == "app1" })
    }

    @Test
    fun execute_phoneShortcut_deletesFromRepositoryAndReturnsTrue() {
        val item = ShortcutItem(
            id = "phone1",
            type = ShortcutType.PHONE,
            label = "田中 太郎",
            phoneNumber = "090-1234-5678"
        )
        repository.saveShortcut(item)

        val result = useCase.execute("phone1")

        assertTrue(result)
        assertNull(repository.getShortcut("phone1"))
    }

    @Test
    fun execute_intentShortcut_deletesBothShortcutAndPinInfo() {
        val item = ShortcutItem(
            id = "intent1",
            type = ShortcutType.INTENT,
            label = "Pinned Shortcut",
            packageName = "com.example.app"
        )
        repository.saveShortcut(item)
        repository.savePinShortcutInfo("intent1", "pin_shortcut_id", "com.example.app")

        useCase.execute("intent1")

        // ショートカット本体が削除されていること
        assertNull(repository.getShortcut("intent1"))
        // ピン情報が削除されていること
        val (pinId, _) = repository.getPinShortcutInfo("intent1")
        assertNull(pinId)
    }

    @Test
    fun execute_intentShortcut_withNoPinInfo_deletesShortcutAndReturnsTrue() {
        val item = ShortcutItem(
            id = "intent2",
            type = ShortcutType.INTENT,
            label = "No Pin Info",
            packageName = "com.example.app"
        )
        repository.saveShortcut(item)
        // ピン情報なし

        val result = useCase.execute("intent2")

        assertTrue(result)
        assertNull(repository.getShortcut("intent2"))
    }

    @Test
    fun execute_nonExistentId_deletesNothingAndReturnsTrue() {
        val result = useCase.execute("nonexistent")

        // ショートカットが存在しない場合は INTENT チェックをスキップして削除操作を実行
        assertTrue(result)
    }
}
