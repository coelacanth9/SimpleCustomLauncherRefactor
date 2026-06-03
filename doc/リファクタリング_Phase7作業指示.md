# リファクタリング Phase7 作業指示

作成: 2026-06-04

## 目的

`アーキテクチャ設計方針.md` に基づき、UseCase 層を新設して各層の責務を正しく分離する。

---

## 実装ステップ

### Step 1: ShortcutRepository — 準備

**追加**
- `isUpdating: AtomicBoolean` フィールド追加
- `update {}` メソッド追加
- `notifyChange()` に `if (isUpdating.get()) return` チェック追加

**削除（デッドコード）**
- `getShortcutsByRow()`
- `getShortcutsByRowForPage()`

---

### Step 2: ShortcutReceiverActivity — バグ修正

`saveShortcutInfo()` が `pin_shortcuts` SharedPreferences に直接書いているのを
`repository.savePinShortcutInfo()` を使うよう修正して削除。

---

### Step 3: UseCase 新規作成 / 追加

パッケージ: `com.coelacanth9.simplecustomlauncher.usecase`

#### ApplyDefaultLayoutUseCase（新規作成）
- `DefaultLayoutConfig.kt` の内容（`ItemDef` / `itemMapping` / `defaultLayout`）を同梱
- 移動元: `ShortcutRepository.applyDefaultLayout()` / `createShortcutFromDef()` / `resetToDefault()` / `getFixedHeightForRow()`
- 依存: `ShortcutRepository`, `Context`

#### AddShortcutUseCase（新規作成）
- 移動元: `ShortcutRepository.addShortcutToFirstEmpty()`
- `repository.update {}` で `saveShortcut` + `savePlacement` をバッチ化
- 依存: `ShortcutRepository`

#### RowUseCase（新規作成 + 追加）
- 移動元: `HomeViewModel.deleteRow()` / `insertRowAt()` / `changeRowColumns()`
- **追加**: `changeTextOnly(pageIndex, rowIndex, textOnly)` — HomeViewModel.changeRowTextOnly() / ShortcutSelectViewModel.changeTextOnly() が同一ロジックで重複
- 各操作を `repository.update {}` でラップ
- 依存: `ShortcutRepository`

#### DeletePageUseCase（新規作成）
- 移動元: `HomeViewModel.deletePage()` + `ShortcutRepository.clearPageLayout()` の内容
- `repository.update {}` で配置・行・ショートカット削除をバッチ化
- 依存: `ShortcutRepository`, `SettingsRepository`

#### CleanupUseCase（新規作成）
- 移動元: `HomeViewModel.cleanupOrphanedPinShortcuts()` / `cleanupUninstalledPackages()` / `onPackageRemoved()`
- LauncherApps 操作（unpin）は `ShortcutHelper` に新規メソッドとして追加し、UseCase は ShortcutHelper を呼ぶ
- 依存: `ShortcutRepository`, `ShortcutHelper`

**ShortcutHelper への追加メソッド:**
- `unpinShortcuts(packageName: String, idsToRemove: Set<String>)`
- `getUninstalledPackages(packageNames: Set<String>): Set<String>`

#### DeleteShortcutUseCase（新規作成）
- 移動元: `HomeViewModel.deleteUnplacedShortcut()`
- ShortcutSelectViewModel.deleteUnplacedShortcut() も同 UseCase を使う
- LauncherApps 操作（unpin）は `ShortcutHelper` を呼ぶ
- 依存: `ShortcutRepository`, `ShortcutHelper`

#### EditSlotUseCase（新規作成）
- 移動元: `HomeViewModel.clearSlot()` / `changeSlotColors()` + `ShortcutSelectViewModel.clearSlot()` / `changeColors()`（重複解消）
- 依存: `ShortcutRepository`

```kotlin
class EditSlotUseCase(private val repository: ShortcutRepository) {

    fun clearSlot(shortcut: ShortcutItem) {
        repository.update {
            removePlacement(shortcut.id)
            if (shouldDeleteOnRemove(shortcut)) deleteShortcut(shortcut.id)
        }
    }

    fun changeColors(pageIndex: Int, row: Int, column: Int, backgroundColor: String?, textColor: String?) {
        val target = repository.layoutState.value.placements
            .find { it.pageIndex == pageIndex && it.row == row && it.column == column } ?: return
        repository.savePlacement(target.copy(backgroundColor = backgroundColor, textColor = textColor))
    }
}
```

#### PlaceShortcutUseCase（新規作成）
- 移動元: `ShortcutSelectViewModel` の配置操作群（placeApp / placeInternalFeature / placeDeviceSettings / placeContact / placeIntent / swapShortcuts）
- private fun `updateRowFixedHeight()` / `placeItem()` も同ファイルに含む
- `updateRowFixedHeight()` のビジネスルール: DATE_DISPLAY→56dp / TIME_DISPLAY→80dp
- 各 placeXxx は `repository.update {}` で saveShortcut + savePlacement をバッチ化
- 依存: `ShortcutRepository`

```kotlin
class PlaceShortcutUseCase(private val repository: ShortcutRepository) {

    fun placeApp(pageIndex: Int, rowIndex: Int, column: Int, packageName: String, label: String) { ... }
    fun placeInternalFeature(pageIndex: Int, rowIndex: Int, column: Int, type: ShortcutType, label: String) { ... }
    fun placeDeviceSettings(pageIndex: Int, rowIndex: Int, column: Int, label: String, settingsAction: String) { ... }
    fun placeContact(pageIndex: Int, rowIndex: Int, column: Int, name: String, phoneNumber: String, type: ShortcutType, targetPackage: String?) { ... }
    fun placeIntent(pageIndex: Int, rowIndex: Int, column: Int, shortLabel: String, packageName: String, shortcutId: String) { ... }
    fun swapShortcuts(targetShortcut: ShortcutItem, fromPageIndex: Int, fromRow: Int, fromColumn: Int, toPageIndex: Int, toRow: Int, toColumn: Int) { ... }

    private fun placeItem(item: ShortcutItem, pageIndex: Int, rowIndex: Int, column: Int) {
        repository.update {
            updateRowFixedHeight(pageIndex, rowIndex, item.type)
            savePlacement(ShortcutPlacement(shortcutId = item.id, pageIndex = pageIndex, row = rowIndex, column = column))
        }
    }

    private fun updateRowFixedHeight(pageIndex: Int, rowIndex: Int, type: ShortcutType) { ... }
}
```

> **PlaceShortcutUseCase の呼び出し側について**
>
> 現在 ShortcutSelectViewModel は `targetPageIndex` / `targetRow` / `targetColumn` をコンストラクタで受け取り、
> 各操作でこれを暗黙的に使っている。UseCase に移動するにあたり、
> 座標は呼び出し時の引数として明示的に渡す設計にする。
> ViewModel は自身が持つ `targetPageIndex` / `targetRow` / `targetColumn` を引数として UseCase に渡す。

---

### Step 4: ShortcutRepository — 移動済み関数を削除

- `applyDefaultLayout()`
- `createShortcutFromDef()`
- `resetToDefault()`
- `getFixedHeightForRow()`
- `addShortcutToFirstEmpty()`
- `clearPageLayout()`

---

### Step 5a: HomeViewModel — UseCase 呼び出しに置き換え

コンストラクタに UseCase を追加:

```kotlin
class HomeViewModel(
    private val shortcutRepository: ShortcutRepository,
    private val settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
    private val premiumManager: PremiumManager,
    private val applyDefaultLayoutUseCase: ApplyDefaultLayoutUseCase,
    private val rowUseCase: RowUseCase,
    private val deletePageUseCase: DeletePageUseCase,
    private val cleanupUseCase: CleanupUseCase,
    private val deleteShortcutUseCase: DeleteShortcutUseCase,
    private val editSlotUseCase: EditSlotUseCase,
    private val billingManager: BillingManager? = null,
    private val adManager: AdManager? = null
)
```

置き換え対象:

| 現在の呼び出し | 置き換え後 |
|---|---|
| `shortcutRepository.resetToDefault()` | `applyDefaultLayoutUseCase.resetToDefault()` |
| `deleteRow(...)` | `rowUseCase.deleteRow(...)` |
| `insertRowAt(...)` | `rowUseCase.insertRowAt(...)` |
| `changeRowColumns(...)` | `rowUseCase.changeRowColumns(...)` |
| `changeRowTextOnly(...)` | `rowUseCase.changeTextOnly(...)` |
| `deletePage(...)` | `deletePageUseCase.execute(...)` |
| `cleanupOrphanedPinShortcuts(...)` | `cleanupUseCase.cleanupOrphanedPinShortcuts(...)` |
| `cleanupUninstalledPackages(...)` | `cleanupUseCase.cleanupUninstalledPackages(...)` |
| `onPackageRemoved(...)` | `cleanupUseCase.onPackageRemoved(...)` |
| `deleteUnplacedShortcut(...)` | `deleteShortcutUseCase.execute(...)` |
| `clearSlot(...)` | `editSlotUseCase.clearSlot(...)` |
| `changeSlotColors(...)` | `editSlotUseCase.changeColors(...)` |

削除する private/local ロジック（UseCase に移動済みのため）:
- `deleteRow()` の本体
- private `insertRowAt()` の本体
- `changeRowColumns()` の本体
- `changeRowTextOnly()` の本体
- `clearSlot()` の本体
- `changeSlotColors()` の本体
- `deletePage()` の本体
- `cleanupOrphanedPinShortcuts()` の本体
- `cleanupUninstalledPackages()` の本体
- `onPackageRemoved()` の本体
- `deleteUnplacedShortcut()` の本体

---

### Step 5b: ShortcutSelectViewModel — UseCase 呼び出しに置き換え

コンストラクタに UseCase を追加:

```kotlin
class ShortcutSelectViewModel(
    private val shortcutRepository: ShortcutRepository,
    private val shortcutHelper: ShortcutHelper,
    private val premiumManager: PremiumManager,
    private val placeShortcutUseCase: PlaceShortcutUseCase,
    private val rowUseCase: RowUseCase,
    private val editSlotUseCase: EditSlotUseCase,
    private val deleteShortcutUseCase: DeleteShortcutUseCase,
    val targetPageIndex: Int,
    val targetRow: Int,
    val targetColumn: Int
)
```

置き換え対象:

| 現在の呼び出し | 置き換え後 |
|---|---|
| `placeApp(packageName, label)` | `placeShortcutUseCase.placeApp(targetPageIndex, targetRow, targetColumn, packageName, label)` |
| `placeInternalFeature(type, label)` | `placeShortcutUseCase.placeInternalFeature(targetPageIndex, targetRow, targetColumn, type, label)` |
| `placeDeviceSettings(label, action)` | `placeShortcutUseCase.placeDeviceSettings(targetPageIndex, targetRow, targetColumn, label, action)` |
| `placeContact(name, phone, type, pkg)` | `placeShortcutUseCase.placeContact(targetPageIndex, targetRow, targetColumn, name, phone, type, pkg)` |
| `placeIntent(label, pkg, id)` | `placeShortcutUseCase.placeIntent(targetPageIndex, targetRow, targetColumn, label, pkg, id)` |
| `swapShortcuts(targetShortcut)` | `placeShortcutUseCase.swapShortcuts(targetShortcut, existingCoords, targetPageIndex, targetRow, targetColumn)` |
| `deleteRow()` | `rowUseCase.deleteRow(targetPageIndex, targetRow)` |
| `changeColumns(columns)` | `rowUseCase.changeRowColumns(targetPageIndex, targetRow, columns)` |
| `changeTextOnly(textOnly)` | `rowUseCase.changeTextOnly(targetPageIndex, targetRow, textOnly)` |
| `clearSlot()` | `editSlotUseCase.clearSlot(currentShortcutItem())` |
| `changeColors(bg, fg)` | `editSlotUseCase.changeColors(targetPageIndex, targetRow, targetColumn, bg, fg)` |
| `deleteUnplacedShortcut(shortcut)` | `deleteShortcutUseCase.execute(shortcut.id)` |

削除する private/local ロジック（UseCase に移動済みのため）:
- `placeApp()` の本体
- `placeInternalFeature()` の本体
- `placeDeviceSettings()` の本体
- `placeContact()` の本体
- `placeIntent()` の本体
- `swapShortcuts()` の本体
- private `placeItem()` の本体
- private `updateRowFixedHeight()` の本体
- `deleteRow()` の本体
- `changeColumns()` の本体
- `changeTextOnly()` の本体
- `clearSlot()` の本体
- `changeColors()` の本体
- `deleteUnplacedShortcut()` の本体

---

### Step 6: SettingsViewModel — UseCase 呼び出しに置き換え

```kotlin
// 変更前
fun resetToDefault() = shortcutRepository.resetToDefault()

// 変更後
fun resetToDefault() = applyDefaultLayoutUseCase.resetToDefault()
```

コンストラクタに `ApplyDefaultLayoutUseCase` を追加:

```kotlin
class SettingsViewModel(
    val settingsRepository: SettingsRepository,
    val premiumManager: PremiumManager,
    val shortcutRepository: ShortcutRepository,
    val applyDefaultLayoutUseCase: ApplyDefaultLayoutUseCase,   // 追加
    val backupManager: BackupManager,
    val billingManager: BillingManager? = null,
    val adManager: AdManager? = null
)
```

---

### Step 7: BackupManager — restoreFromJson() を update{} でラップ

`restoreFromJson()` 内の書き込みシーケンスを `repository.update {}` でまとめ、
復元中の StateFlow 複数更新を防ぐ。

```kotlin
// 変更前
shortcutRepository.clearAllLayout()
shortcuts.forEach { shortcutRepository.saveShortcut(it) }
placements.forEach { shortcutRepository.savePlacement(it) }
shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows))

// 変更後
shortcutRepository.update {
    clearAllLayout()
    shortcuts.forEach { saveShortcut(it) }
    placements.forEach { savePlacement(it) }
    saveLayoutConfig(HomeLayoutConfig(rows))
}
```

---

### Step 8: MainActivity — UseCase の生成を追加

`viewModelFactory` 内で UseCase をインスタンス化して各 ViewModel に渡す。

**HomeViewModel:**

```kotlin
val shortcutHelper = ShortcutHelper(context)
HomeViewModel(
    shortcutRepository = shortcutRepository,
    settingsRepository = settingsRepo,
    calendarRepository = CalendarRepository(context),
    premiumManager = DefaultPremiumManager(context, settingsRepo),
    applyDefaultLayoutUseCase = ApplyDefaultLayoutUseCase(shortcutRepository, context),
    rowUseCase = RowUseCase(shortcutRepository),
    deletePageUseCase = DeletePageUseCase(shortcutRepository, settingsRepo),
    cleanupUseCase = CleanupUseCase(shortcutRepository, shortcutHelper),
    deleteShortcutUseCase = DeleteShortcutUseCase(shortcutRepository, shortcutHelper),
    editSlotUseCase = EditSlotUseCase(shortcutRepository),
    billingManager = billingManager,
    adManager = adManager
)
```

**SettingsViewModel:**

```kotlin
SettingsViewModel(
    settingsRepository = settingsRepo,
    premiumManager = DefaultPremiumManager(context, settingsRepo),
    shortcutRepository = shortcutRepository,
    applyDefaultLayoutUseCase = ApplyDefaultLayoutUseCase(shortcutRepository, context),  // 追加
    backupManager = BackupManager(context, shortcutRepository, settingsRepo),
    billingManager = billingManager,
    adManager = adManager
)
```

**ShortcutSelectViewModel:**

```kotlin
ShortcutSelectViewModel(
    shortcutRepository = shortcutRepository,
    shortcutHelper = ShortcutHelper(context),
    premiumManager = DefaultPremiumManager(context, settingsRepo),
    placeShortcutUseCase = PlaceShortcutUseCase(shortcutRepository),
    rowUseCase = RowUseCase(shortcutRepository),
    editSlotUseCase = EditSlotUseCase(shortcutRepository),
    deleteShortcutUseCase = DeleteShortcutUseCase(shortcutRepository, ShortcutHelper(context)),
    targetPageIndex = dest.pageIndex,
    targetRow = dest.row,
    targetColumn = dest.column
)
```

また `MainActivity.onCreate()` の初回起動処理も置き換え:

```kotlin
// 変更前
if (shortcutRepository.isFirstLaunch()) {
    shortcutRepository.applyDefaultLayout()
}

// 変更後
if (shortcutRepository.isFirstLaunch()) {
    ApplyDefaultLayoutUseCase(shortcutRepository, this).applyDefaultLayout()
}
```

---

### Step 9: DefaultLayoutConfig.kt 削除

`data/DefaultLayoutConfig.kt` を削除。
内容は `usecase/ApplyDefaultLayoutUseCase.kt` に同梱済み。

---

## 完了条件

- ビルドが通ること
- `ShortcutRepository` にビジネスロジックが残っていないこと
- `HomeViewModel` にビジネスロジックが残っていないこと
- `ShortcutSelectViewModel` にビジネスロジックが残っていないこと
- `SettingsViewModel.resetToDefault()` が UseCase を呼んでいること
- `BackupManager.restoreFromJson()` が `repository.update{}` を使っていること
- `usecase/` パッケージに 8 ファイルが存在すること
  （ApplyDefaultLayout / AddShortcut / Row / DeletePage / Cleanup / DeleteShortcut / EditSlot / PlaceShortcut）
